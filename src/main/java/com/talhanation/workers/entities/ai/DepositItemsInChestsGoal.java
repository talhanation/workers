package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.Stack;

public class DepositItemsInChestsGoal extends AbstractChestGoal {

    public DepositItemsInChestsGoal(AbstractWorkerEntity worker){
        super(worker);
        setFlags(EnumSet.of(Flag.LOOK, Flag.MOVE));
    }
    @Override
    public boolean canUse() {
        return worker.needsToDeposit() && super.canUse();
    }

    @Override
    public void start() {
        state = State.SELECT_CHEST;
        timer = 0;

    }
    int timer;
    public State state;
    public String errorMessage;
    public boolean errorMessageDone;
    public void tick(){
        if(this.worker.getCommandSenderWorld().isClientSide()) return;

        if(this.chestPos != null) worker.getLookControl().setLookAt(chestPos.getCenter());

        switch (state){
            case SELECT_CHEST -> {
                blockPosStack = new Stack<>();
                for(BlockPos pos : worker.chestPositions){
                    this.blockPosStack.push(pos);
                }

                if(blockPosStack.isEmpty()) {
                    state = State.ERROR;
                    errorMessage = worker.getName().getString() + ": No Deposit Chests, please assign me Chests";
                    return;
                }

                blockPosStack.sort(Comparator.comparing(pos -> pos.getCenter().distanceToSqr(worker.position())));
                blockPosStack.sort(Comparator.reverseOrder());

                chestPos = blockPosStack.pop();

                state = State.MOVE_TO_CHEST;
            }

            case MOVE_TO_CHEST -> {
                if(moveToPosition(chestPos)) return;

                state = State.CHECK_CHEST;
            }

            case CHECK_CHEST -> {
                container = getContainer(chestPos);
                if(container == null || this.isContainerFull(container)){
                    worker.chestPositions.remove(chestPos);
                    if(!blockPosStack.isEmpty()) chestPos = blockPosStack.pop();
                    state = State.SELECT_CHEST;
                    return;
                }

                state = State.OPEN_CHEST;
            }

            case OPEN_CHEST -> {

                this.interactChest(container, true);

                if(timer++ < 40){
                    return;
                }
                timer = 0;

                state = State.DEPOSIT;
            }

            case DEPOSIT -> {
                if(depositItems()){
                    state = State.CLOSE_CHEST_DEPOSIT_DONE;
                }
                else{
                    state = State.CLOSE_CHEST_FULL_CHEST;
                }

            }

            case CLOSE_CHEST_DEPOSIT_DONE -> {
                this.interactChest(container, false);

                if(timer++ < 20){
                    return;
                }
                timer = 0;

                state = State.DONE;
            }

            case CLOSE_CHEST_FULL_CHEST -> {
                this.interactChest(container, false);

                if(timer++ < 20){
                    return;
                }
                timer = 0;

                state = State.SELECT_CHEST;
            }

            case DONE -> {
                worker.farmedItems = 0;
                worker.forcedDeposit = false;
            }

            case ERROR -> {
                if(!errorMessageDone){
                    if(worker.getOwner() != null) worker.getOwner().sendSystemMessage(Component.literal(errorMessage));
                    errorMessageDone = true;
                }

                if(!worker.chestPositions.isEmpty()){
                    state = State.SELECT_CHEST;
                }
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

            // This avoids depositing items such as tools, food,
            // or anything the workers wouldn't pick up while they're working.
            // It also avoids depositing items that the worker needs to continue working.
            if (stack.is(Items.AIR) || stack.isEmpty() || !worker.wantsToPickUp(stack) || (worker.wantsToKeep(stack) && getAmountOfItem(stack.getItem()) <= 64)) {
                continue;
            }
            // Attempt to deposit the stack in the container, keep the remainder
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

    public enum State{
        SELECT_CHEST,
        MOVE_TO_CHEST,
        CHECK_CHEST,
        OPEN_CHEST,
        DEPOSIT,
        CLOSE_CHEST_FULL_CHEST,
        CLOSE_CHEST_DEPOSIT_DONE,
        DONE,
        ERROR

    }
}
