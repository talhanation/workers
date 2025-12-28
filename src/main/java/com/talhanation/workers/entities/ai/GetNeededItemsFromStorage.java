package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import com.talhanation.workers.entities.workarea.StorageArea;
import com.talhanation.workers.world.NeededItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public class GetNeededItemsFromStorage extends AbstractChestGoal {
    int timer;
    public State state;
    public boolean errorMessageDone;
    private int retryTime;
    boolean itemNotInStorage;
    public GetNeededItemsFromStorage(AbstractWorkerEntity worker) {
        super(worker);
        setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return !worker.needsToDeposit() && worker.needsToGetItems() && super.canUse();
    }

    @Override
    public void start() {
        super.start();
        errorMessageDone = false;
        retryTime = 0;
        setState(State.SELECT_STORAGE);
    }

    public void tick(){
        if(this.worker.getCommandSenderWorld().isClientSide()) return;

        switch (state){
            case SELECT_STORAGE -> {
                errorMessageDone = false;
                List<StorageArea> areas = getAvailableStorageAreas();

                if (!areas.isEmpty()) {
                    this.storageArea = areas.get(0);
                }
                else{
                    setState(State.ERROR_NO_STORAGE_FOUND);
                    return;
                }

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
                    if(storageArea != null) this.visited.add(storageArea.getUUID());
                    setState(State.ERROR_STORAGE_NO_CONTAINERS);
                    return;
                }

                setState(State.SELECT_CHEST);
            }

            case SELECT_CHEST -> {
                if(blockPosStack.isEmpty()){
                    if(storageArea != null) this.visited.add(storageArea.getUUID());
                    setState(State.ERROR_ITEM_NOT_IN_STORAGE);
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
                    if(!blockPosStack.isEmpty()) chestPos = blockPosStack.pop();

                    setState(State.SELECT_CHEST);
                    return;
                }

                setState(State.OPEN_CHEST);
            }

            case OPEN_CHEST -> {
                worker.getLookControl().setLookAt(chestPos.getCenter());
                this.interactChest(container, true);
                if(timer++ < 20){
                    return;
                }
                timer = 0;

                setState(State.TAKE_NEEDED_ITEMS);
            }

            case TAKE_NEEDED_ITEMS -> {
                worker.getLookControl().setLookAt(chestPos.getCenter());

                if(takeNeededItems()){
                    setState(State.CLOSE_CHEST_DONE);
                }
                else{
                    setState(State.CLOSE_CHEST_NOT_DONE);
                }
            }

            case CLOSE_CHEST_DONE -> {
                worker.getLookControl().setLookAt(chestPos.getCenter());
                this.interactChest(container, false);

                if(timer++ < 20){
                    return;
                }
                timer = 0;

                setState(State.DONE);
            }

            case CLOSE_CHEST_NOT_DONE -> {
                worker.getLookControl().setLookAt(chestPos.getCenter());
                this.interactChest(container, false);

                if(timer++ < 20){
                    return;
                }
                timer = 0;

                if(!worker.hasFreeInvSlot()){
                    setState(State.ERROR_OWN_INVENTORY_FULL);
                    return;
                }

                setState(State.SELECT_CHEST);
            }

            case DONE -> {
                worker.neededItems.clear();
                worker.lastStorage = this.storageArea.getUUID();
            }

            case ERROR_NO_STORAGE_FOUND -> {
                if(!errorMessageDone) {
                    if (worker.getOwner() != null){
                        worker.getOwner().sendSystemMessage(Component.literal(worker.getName().getString() + ": No available storage found nearby... I need "  + worker.neededItems));
                    }

                    worker.neededItems.removeIf(neededItem -> neededItem.optional);
                    errorMessageDone = true;
                }

                if(itemNotInStorage){
                    visited.clear();
                    this.worker.lastStorage = null;
                }

                if(++retryTime >= 20*60){
                    retryTime = 0;
                    this.start();
                }
            }

            case ERROR_ITEM_NOT_IN_STORAGE -> {
                if(!errorMessageDone){
                    if(worker.getOwner() != null && storageArea != null)
                        worker.getOwner().sendSystemMessage(Component.literal(worker.getName().getString() + ": Storage [" + storageArea.getName().getString() + "] has no: " + worker.neededItems));
                    errorMessageDone = true;
                }
                itemNotInStorage = true;
                setState(State.SELECT_STORAGE);
            }

            case ERROR_STORAGE_NO_CONTAINERS -> {
                if(!errorMessageDone){
                    if(worker.getOwner() != null && storageArea != null) worker.getOwner().sendSystemMessage(Component.literal(worker.getName().getString() + ": Storage [" + storageArea.getName().getString() + "] has no containers!"));
                    errorMessageDone = true;
                }

                setState(State.SELECT_STORAGE);
            }

            case ERROR_OWN_INVENTORY_FULL -> {
                if(!errorMessageDone){
                    errorMessageDone = true;
                    if(worker.getOwner() != null) worker.getOwner().sendSystemMessage(Component.literal(worker.getName().getString() + ": My Inventory is full!"));

                    worker.forcedDeposit = true;
                }
            }
        }
    }

    private boolean takeNeededItems() {
        SimpleContainer inventory = worker.getInventory();

        if (container == null || container.isEmpty()) {
            return false;
        }

        List<NeededItem> neededItems = worker.neededItems;
        if (neededItems.isEmpty()) return false;

        boolean anyTaken = false;

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack itemInChest = container.getItem(i);
            if (itemInChest.isEmpty()) continue;

            for (int j = neededItems.size() - 1; j >= 0; j--) {
                NeededItem needed = neededItems.get(j);
                if (needed.matches(itemInChest)) {
                    int neededCount = needed.count;
                    int availableCount = itemInChest.getCount();
                    int toExtract = Math.min(neededCount, availableCount);

                    ItemStack extracted = itemInChest.split(toExtract);
                    ItemStack leftover = inventory.addItem(extracted);

                    if (!leftover.isEmpty()) {
                        itemInChest.grow(toExtract);
                        return false;
                    }


                    NeededItem.applyToNeededItems(extracted, neededItems);
                    anyTaken = true;

                    if (itemInChest.isEmpty()) {
                        break;
                    }
                }
            }
        }


        if (!anyTaken) {
            return false;
        }


        for (NeededItem needed : neededItems) {
            if (!needed.optional) {
                return false;
            }
        }

        return true;
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
        TAKE_NEEDED_ITEMS,
        CLOSE_CHEST_NOT_DONE,
        CLOSE_CHEST_DONE,
        DONE,
        ERROR_NO_STORAGE_FOUND,
        ERROR_ITEM_NOT_IN_STORAGE,
        ERROR_STORAGE_NO_CONTAINERS,
        ERROR_OWN_INVENTORY_FULL
    }
}
