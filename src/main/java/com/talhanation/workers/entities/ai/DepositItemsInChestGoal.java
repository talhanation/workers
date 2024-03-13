package com.talhanation.workers.entities.ai;

import com.talhanation.workers.Translatable;
import com.talhanation.workers.entities.AbstractWorkerEntity;
import com.talhanation.workers.entities.ChickenFarmerEntity;
import com.talhanation.workers.entities.MinerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

public class DepositItemsInChestGoal extends Goal {
    private final AbstractWorkerEntity worker;
    public BlockPos chestPos;
    public Container container;
    public boolean message;
    public int timer = 0;
    public boolean setTimer = false;

    public DepositItemsInChestGoal(AbstractWorkerEntity worker) {
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
        this.chestPos = worker.getChestPos();
        if (chestPos != null) {
            BlockEntity entity = worker.level.getBlockEntity(chestPos);
            BlockState blockState = worker.getCommandSenderWorld().getBlockState(chestPos);
            if (blockState.getBlock() instanceof ChestBlock chestBlock) {
                this.container = ChestBlock.getContainer(chestBlock, blockState, worker.getCommandSenderWorld(), chestPos, false);
            } else if (entity instanceof Container containerEntity) {
                this.container = containerEntity;
            } else
                message = true;
        }
    }

    @Override
    public void stop() {
        super.stop();
        if(container != null) this.interactChest(container,false);
        timer = 0;
        setTimer = false;
        this.worker.resetFarmedItems();
    }

    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void tick() {
        this.chestPos = worker.getChestPos();

        if (chestPos != null) {

            BlockEntity entity = worker.getCommandSenderWorld().getBlockEntity(chestPos);
            if (entity instanceof Container containerEntity) {
                this.container = containerEntity;
            }
            else message = true;
			
            this.worker.getNavigation().moveTo(chestPos.getX(), chestPos.getY(), chestPos.getZ(), 1.1D);

            if (chestPos.closerThan(worker.getOnPos(), 2.5) && container != null) {
                this.worker.getNavigation().stop();
                this.worker.getLookControl().setLookAt(chestPos.getX(), chestPos.getY() + 1, chestPos.getZ(), 10.0F, (float) this.worker.getMaxHeadXRot());

                if(!setTimer){
                    this.interactChest(container, true);
                    this.depositItems(container);
                    this.reequipTool();

                    if(this.worker instanceof MinerEntity){
                        this.reequipTorch();
                    }

                    if(this.worker instanceof ChickenFarmerEntity chickenFarmer && chickenFarmer.getUseEggs()){
                        this.getEggsFromChest();
                    }

                    timer = 50;
                    setTimer = true;
                }
            }
        }
        else
            message = true;

        if(message && worker.getOwner() != null){
            this.worker.tellPlayer(worker.getOwner(), Translatable.TEXT_CANT_FIND_CHEST);
            this.worker.setNeedsChest(true);
        }

        if(setTimer){
            if(timer > 0) timer--;
            if(timer == 0) stop();
        }

    }

    public void interactChest(Container container, boolean open) {
        if (container instanceof ChestBlockEntity chest) {
            if (open) {
                this.worker.getCommandSenderWorld().blockEvent(this.chestPos, chest.getBlockState().getBlock(), 1, 1);
                this.worker.getCommandSenderWorld().playSound(null, chestPos, SoundEvents.CHEST_OPEN, worker.getSoundSource(), 0.7F, 0.8F + 0.4F * worker.getRandom().nextFloat());
            }
            else {
                this.worker.getCommandSenderWorld().blockEvent(this.chestPos, chest.getBlockState().getBlock(), 1, 0);
                worker.getCommandSenderWorld().playSound(null, chestPos, SoundEvents.CHEST_CLOSE, worker.getSoundSource(), 0.7F, 0.8F + 0.4F * worker.getRandom().nextFloat());
            }
            this.worker.getCommandSenderWorld().gameEvent(this.worker, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, chestPos);
        }
    }

    private void reequipTool(){
        if(worker.needsTool()){
            boolean hasMainHand = worker.getInventory().hasAnyMatching(worker::isRequiredMainTool);
            boolean hasOffHand = worker.getInventory().hasAnyMatching(worker::isRequiredSecondTool);

            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack stack = container.getItem(i);

                if((!hasMainHand && worker.isRequiredMainTool(stack))){
                    take(stack);
                }
                if(!hasOffHand && worker.isRequiredSecondTool(stack)){
                    take(stack);
                }
            }
            worker.setNeedsTool(false);
        }
    }

    private void reequipTorch(){
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if(stack.is(Items.TORCH)){
                worker.getInventory().addItem(stack.copy());
                container.removeItemNoUpdate(i);
                break;
            }
        }
    }

    private void getEggsFromChest(){
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if(stack.is(Items.EGG)){
                worker.getInventory().addItem(stack.copy());
                container.removeItemNoUpdate(i);
                break;
            }
        }
    }

    private void take(ItemStack stack){
        ItemStack tool = stack.copy();
        worker.getInventory().addItem(tool);
        stack.shrink(1);
    }

    private void depositItems(Container container) {
        SimpleContainer inventory = worker.getInventory();

        if (this.isContainerFull(container)) {
            this.worker.setNeedsChest(true);

            if(worker.getOwner() != null) {
                this.worker.tellPlayer(worker.getOwner(), Translatable.TEXT_CHEST_FULL);
            }
            return;
        }

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            
            // This avoids depositing items such as tools, food, 
            // or anything the workers wouldn't pick up while they're working.
            // It also avoids depositing items that the worker needs to continue working.
            if (stack.is(Items.AIR) || stack.isEmpty() || !worker.wantsToPickUp(stack) || worker.wantsToKeep(stack)) {
                continue;
            }
            // Attempt to deposit the stack in the container, keep the remainder
            ItemStack remainder = this.deposit(stack, container);
            inventory.setItem(i, remainder);


            //Main.LOGGER.debug("Stored {} x {}", stack.getCount() - remainder.getCount(), stack.getDisplayName().getString());
            //Main.LOGGER.debug("Kept {} x {}", remainder.getCount(), stack.getDisplayName().getString());
        }
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


}
