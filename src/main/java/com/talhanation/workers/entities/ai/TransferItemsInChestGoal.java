package com.talhanation.workers.entities.ai;

import com.talhanation.workers.Main;
import com.talhanation.workers.entities.AbstractWorkerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags.Blocks;

public class TransferItemsInChestGoal extends Goal {
    private final AbstractWorkerEntity worker;
    private final MutableComponent CANT_FIND_CHEST = Component.translatable("chat.workers.cantFindChest");
    private final MutableComponent CHEST_FULL = Component.translatable("chat.workers.chestFull");

    private PathNavigation pathFinder;

    public TransferItemsInChestGoal(AbstractWorkerEntity worker) {
        this.worker = worker;
        this.pathFinder = this.worker.getNavigation();
    }

    @Override
    public boolean canUse() {
        if (
            this.worker.getOwner() == null ||
            this.worker.needsChest() ||
            this.worker.needsToSleep()
        ) {
            return false;
        }
        SimpleContainer inventory = this.worker.getInventory();
        if (inventory == null) {
            return false;
        }
        for (ItemStack item : inventory.items) {
            // If there's at least one item that the worker wants to save in the chest
            if (this.worker.wantsToPickUp(item) && !this.worker.wantsToKeep(item)) {
                return true;
            }
        }
        return false;
    }

    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void tick() {
        LivingEntity owner = worker.getOwner();
        if (owner == null) return;
        if (worker.isSleeping()) return;
        
        BlockPos chestPos = this.worker.getChestPos();
        if (chestPos == null) {
            this.worker.tellPlayer(owner, CANT_FIND_CHEST);
            this.worker.setNeedsChest(true);
            return;
        }
        
        Container containerEntity = null;
        BlockState chest = worker.level.getBlockState(this.worker.getChestPos());
        if (worker.level.getBlockEntity(chestPos) instanceof Container container) {
            containerEntity = container;
        }
        if (
            containerEntity == null || chest == null || (
                !chest.is(Blocks.CHESTS) && 
                !chest.is(Blocks.BARRELS)
            )
        ) {
            this.worker.tellPlayer(owner, CANT_FIND_CHEST);
            this.worker.setNeedsChest(true);
            return;
        }


        Main.LOGGER.debug("Moving to chest");
        pathFinder.moveTo(chestPos.getX(), chestPos.getY(), chestPos.getZ(), 1.1D);

        if (chestPos.closerThan(worker.getOnPos(), 1)) {
            pathFinder.stop();
            this.worker.getLookControl().setLookAt(
                chestPos.getX(),
                chestPos.getY() + 1,
                chestPos.getZ(),
                10.0F,
                (float) this.worker.getMaxHeadXRot()
            );

            Main.LOGGER.debug("Depositing to chest");
            worker.level.playSound(
                null, 
                chestPos,
                SoundEvents.CHEST_OPEN,
                worker.getSoundSource(),
                0.7F, 
                0.8F + 0.4F * worker.getRandom().nextFloat()
            );
            this.depositItems(containerEntity);
            worker.level.playSound(
                null, 
                chestPos,
                SoundEvents.CHEST_CLOSE,
                worker.getSoundSource(),
                0.7F, 
                0.8F + 0.4F * worker.getRandom().nextFloat()
            );
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
            if (
                stack.isEmpty() ||
                !worker.wantsToPickUp(stack) ||
                worker.wantsToKeep(stack)
            ) {
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
            this.worker.tellPlayer(worker.getOwner(), CHEST_FULL);
            this.worker.setNeedsChest(true);
        }
    }

    
    /**
     * Deposits a stack in a target container.
     * @return The shrinked stack with the remaining items that were not deposited.
     */
    private ItemStack deposit(ItemStack stack, Container container) {
        // Attempt to fill matching stacks first.
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack targetSlot = container.getItem(i);
            if (targetSlot.sameItem(stack)) {
                int amountToDeposit = Math.min(
                    stack.getCount(), 
                    targetSlot.getMaxStackSize() - targetSlot.getCount()
                );
                targetSlot.grow(amountToDeposit);
                stack.shrink(amountToDeposit);
                container.setItem(i, targetSlot);
                if (stack.isEmpty()) {
                    return stack;
                }
            }
        }
        // Put the remainder in the first empty slot we can find.
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack targetSlot = container.getItem(i);
            if (targetSlot.isEmpty()) {
                container.setItem(i, stack);
                return ItemStack.EMPTY;
            }
        }
        // If we haven't returned at this point, the item can't be inserted. 
        // Return the remainder.
        return stack;
    }
}
