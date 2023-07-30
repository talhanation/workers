package com.talhanation.workers.entities.ai;

import com.talhanation.workers.Main;
import com.talhanation.workers.Translatable;
import com.talhanation.workers.entities.AbstractWorkerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.gameevent.GameEvent;

public class TransferItemsInChestGoal extends Goal {
    private final AbstractWorkerEntity worker;
    public BlockPos chestPos;
    public Container container;
    public boolean message;

    public TransferItemsInChestGoal(AbstractWorkerEntity worker) {
        this.worker = worker;
    }

    @Override
    public boolean canUse() {
        return this.worker.needsToDeposit();
    }

    @Override
    public void start() {
        super.start();
        message = false;
    }

    @Override
    public void stop() {
        super.stop();
        if(container != null) this.interactChest(container,false);
    }

    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void tick() {
        this.chestPos = worker.getChestPos();

        if (chestPos != null) {
            BlockEntity entity = worker.level.getBlockEntity(chestPos);
            if (entity instanceof Container containerEntity) {
                this.container = containerEntity;
            }
            else message = true;

            this.worker.getNavigation().moveTo(chestPos.getX(), chestPos.getY(), chestPos.getZ(), 1.1D);

            if (chestPos.closerThan(worker.getOnPos(), 2.5) && container != null) {
                this.worker.getNavigation().stop();
                this.worker.getLookControl().setLookAt(chestPos.getX(), chestPos.getY() + 1, chestPos.getZ(), 10.0F, (float) this.worker.getMaxHeadXRot());
                this.interactChest(container, true);
                this.depositItems(container);
                this.reequipTool();
            }
        }
        else message = true;

        if(message && worker.getOwner() != null){
            this.worker.tellPlayer(worker.getOwner(), Translatable.TEXT_CANT_FIND_CHEST);
            this.worker.setNeedsChest(true);
        }


    }

    public void interactChest(Container container, boolean open) {
        if (container instanceof ChestBlockEntity chest) {
            if (open) {
                this.worker.getLevel().blockEvent(this.chestPos, chest.getBlockState().getBlock(), 1, 1);
                this.worker.level.playSound(null, chestPos, SoundEvents.CHEST_OPEN, worker.getSoundSource(), 0.7F, 0.8F + 0.4F * worker.getRandom().nextFloat());
            }
            else {
                this.worker.getLevel().blockEvent(this.chestPos, chest.getBlockState().getBlock(), 1, 0);
                worker.level.playSound(null, chestPos, SoundEvents.CHEST_CLOSE, worker.getSoundSource(), 0.7F, 0.8F + 0.4F * worker.getRandom().nextFloat());
            }
            this.worker.getLevel().gameEvent(this.worker, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, chestPos);
        }
    }

    private void reequipTool() {
        if(worker.needsTool()){

        }
    }



    private void depositItems(Container container) {
        SimpleContainer inventory = worker.getInventory();
        boolean couldDepositSomething = false;

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            
            // This avoids depositing items such as tools, food, 
            // or anything the workers wouldn't pick up while they're working.
            // It also avoids depositing items that the worker needs to continue working.
            if (stack.isEmpty() || !worker.wantsToPickUp(stack) || worker.wantsToKeep(stack)) {
                continue;
            }
            int originalAmount = stack.getCount();
            // Attempt to deposit the stack in the container, keep the remainder
            ItemStack remainder = this.deposit(stack, container);
            inventory.setItem(i, remainder);
            if (originalAmount != remainder.getCount()) {
                couldDepositSomething = true;
            }

            Main.LOGGER.debug(
                "Stored {} x {}",
                stack.getCount() - remainder.getCount(),
                stack.getDisplayName().getString()
            );
            Main.LOGGER.debug(
                "Kept {} x ", 
                remainder.getCount(), 
                stack.getDisplayName().getString()
            );
        }
        if (!couldDepositSomething) {
            this.worker.setNeedsChest(true);

            if(worker.getOwner() != null) {
                if (this.isContainerFull(container)) {
                    this.worker.tellPlayer(worker.getOwner(), Translatable.TEXT_CHEST_FULL);
                }
                else
                    this.worker.tellPlayer(worker.getOwner(), Translatable.TEXT_COULD_NOT_DEPOSIT);
            }
        }

        this.worker.resetFarmedItems();
    }

    private boolean isContainerFull(Container container){
        for(int i = 0; i < container.getContainerSize(); i++){
            ItemStack itemStack = container.getItem(i);
            if(itemStack.isEmpty()) return false;
        }
        return true;
    }
    /**
     * Deposits a stack in a target container.
     * @return The shrinked stack with the remaining items that were not deposited.
     */
    private ItemStack deposit(ItemStack stack, Container container) {
        // Attempt to fill matching stacks first.
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack targetStack = container.getItem(i);
            if (targetStack.sameItem(stack)) {
                int amountToDeposit = Math.min(
                    stack.getCount(), 
                    targetStack.getMaxStackSize() - targetStack.getCount()
                );
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


}
