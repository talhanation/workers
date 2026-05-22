package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import com.talhanation.workers.entities.workarea.StorageArea;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;

public class DepositItemsToStorage extends AbstractChestGoal {

    public DepositItemsToStorage(AbstractWorkerEntity worker){
        super(worker);
        setFlags(EnumSet.of(Flag.LOOK, Flag.MOVE));
    }
    @Override
    public boolean canUse() {
        return worker.needsToDeposit() && super.canUse();
    }

    @Override
    public void start() {
        this.visited = new ArrayList<>();
        errorMessageDone = false;
        setState(State.SELECT_STORAGE);
        timer = 0;
    }

    int timer;
    public State state;
    public boolean errorMessageDone;
    public int retryTime;
    public boolean chestFull;
    public void tick(){
        if(this.worker.getCommandSenderWorld().isClientSide()) return;

        if(this.chestPos != null) worker.getLookControl().setLookAt(chestPos.getCenter());

        switch (state){
            case SELECT_STORAGE -> {
                errorMessageDone = false;

                if (storageAreaStack.isEmpty()) {

                    scanAvailableStorageAreas();

                    if(storageAreaStack.isEmpty()){
                        setState(State.ERROR_NO_STORAGE_FOUND);
                        return;
                    }
                }

                this.storageArea = storageAreaStack.pop();

                setState(State.MOVE_TO_STORAGE);
            }

            case MOVE_TO_STORAGE -> {
                if(moveToPosition(storageArea.getOnPos())) return;

                setState(State.SCAN_STORAGE);
            }

            case SCAN_STORAGE -> {
                storageArea.scanStorageBlocks();
                blockPosStack = new Stack<>();

                for(BlockPos pos : storageArea.storageMap.keySet()){
                    this.blockPosStack.push(pos);
                }

                if(blockPosStack.isEmpty()) {
                    this.visited.add(storageArea.getUUID());

                    setState(State.ERROR_STORAGE_NO_CONTAINERS);
                    return;
                }

                setState(State.SELECT_CHEST);
            }

            case SELECT_CHEST -> {
                if(blockPosStack.isEmpty()){
                    this.visited.add(storageArea.getUUID());
                    setState(State.ERROR_STORAGE_FULL);
                    return;
                }
                else{
                    blockPosStack.sort(Comparator.comparing(pos -> pos.getCenter().distanceToSqr(worker.position())));
                    blockPosStack.sort(Comparator.reverseOrder());

                    chestPos = blockPosStack.pop();
                }

                setState(State.MOVE_TO_CHEST);
            }

            case MOVE_TO_CHEST -> {
                if(moveToPosition(chestPos)) return;

                setState(State.CHECK_CHEST);
            }

            case CHECK_CHEST -> {
                container = getContainer(chestPos);
                if(container == null){
                    this.storageArea.storageMap.remove(chestPos);
                    setState(State.SELECT_CHEST);
                    return;
                }
                else if(this.isContainerFull(container)){
                    chestFull = true;
                    setState(State.SELECT_CHEST);
                    return;
                }

                setState(State.OPEN_CHEST);
            }

            case OPEN_CHEST -> {
                this.interactChest(container, true);

                if(timer++ < 40){
                    return;
                }
                timer = 0;

                setState(State.DEPOSIT);
            }

            case DEPOSIT -> {
                if(depositItems()){
                    setState(State.CLOSE_CHEST_DEPOSIT_DONE);
                }
                else{
                    setState(State.CLOSE_CHEST_FULL_CHEST);
                }

            }

            case CLOSE_CHEST_DEPOSIT_DONE -> {
                this.interactChest(container, false);

                if(timer++ < 20){
                    return;
                }
                timer = 0;

                setState(State.DONE);
            }

            case CLOSE_CHEST_FULL_CHEST -> {
                this.interactChest(container, false);

                if(timer++ < 20){
                    return;
                }
                timer = 0;

                setState(State.SELECT_CHEST);
            }

            case DONE -> {
                worker.farmedItems = 0;
                worker.forcedDeposit = false;
                this.worker.lastStorage = storageArea.getUUID();
            }

            case ERROR_NO_STORAGE_FOUND -> {
                if(!errorMessageDone){
                    worker.notifyOwner(Component.literal("No available Storage found nearby."));
                    errorMessageDone = true;
                }

                if(++retryTime >= 20*60){
                    retryTime = 0;
                    this.start();
                }
            }

            case ERROR_STORAGE_FULL -> {
                if(!errorMessageDone){
                    if(storageArea != null) worker.notifyOwner(Component.literal("" + storageArea.getName().getString() + " is full!"));
                    errorMessageDone = true;
                }
                if(storageArea != null) this.visited.add(storageArea.getUUID());
                setState(State.SELECT_STORAGE);
            }

            case ERROR_STORAGE_NO_CONTAINERS -> {
                if(!errorMessageDone){
                    if(storageArea != null) worker.notifyOwner(Component.literal("" + storageArea.getName().getString() + " has no containers!"));
                    errorMessageDone = true;
                }

                if(storageArea != null) this.visited.add(storageArea.getUUID());
                setState(State.SELECT_STORAGE);
            }
        }
    }

    private boolean depositItems() {
        SimpleContainer inventory = worker.getInventory();

        if (container == null || this.isContainerFull(container)) {
            return false;
        }

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);

            boolean isAir = stack.is(Items.AIR) || stack.isEmpty();
            boolean wantToKeep = worker.wantsToKeep(stack);
            if (isAir || wantToKeep) {
                continue;
            }

            ItemStack remainder = this.deposit(stack, container);
            inventory.setItem(i, remainder);
        }
        return true;
    }

    private ItemStack deposit(ItemStack stack, Container container) {
        // Attempt to fill matching stacks first.
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack targetStack = container.getItem(i);
            if (targetStack.is(stack.getItem())) {
                int amountToDeposit = Math.min(stack.getCount(), targetStack.getMaxStackSize() - targetStack.getCount());

                targetStack.grow(amountToDeposit);
                stack.shrink(amountToDeposit);

                container.setItem(i, targetStack);

                if (stack.isEmpty()) {
                    return stack;
                }
            }
        }
        // Put the remainder in the first empty slot we can find.
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack targetStack = container.getItem(i);
            if (targetStack.isEmpty()) {
                container.setItem(i, stack);
                return ItemStack.EMPTY;
            }
        }
        // If we haven't returned at this point, the item can't be inserted.
        // Return the remainder.
        return stack;
    }

    public void setState(State state) {
        //if(worker.getOwner() != null) worker.getOwner().sendSystemMessage(Component.literal(state.toString()));
        this.state = state;
    }

    public enum State{
        SELECT_STORAGE,
        MOVE_TO_STORAGE,
        SCAN_STORAGE,
        SELECT_CHEST,
        MOVE_TO_CHEST,
        CHECK_CHEST,
        OPEN_CHEST,
        DEPOSIT,
        CLOSE_CHEST_FULL_CHEST,
        CLOSE_CHEST_DEPOSIT_DONE,
        DONE,
        ERROR_NO_STORAGE_FOUND,
        ERROR_STORAGE_FULL,
        ERROR_STORAGE_NO_CONTAINERS

    }
}
