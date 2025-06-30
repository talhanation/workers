package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.Stack;

public abstract class AbstractChestGoal extends Goal {
    public Stack<BlockPos> blockPosStack = new Stack<>();
    public Container container;
    public AbstractWorkerEntity worker;
    public BlockPos chestPos;

    public AbstractChestGoal(AbstractWorkerEntity worker){
        this.worker = worker;
    }

    @Override
    public boolean canUse(){
        return !worker.chestPositions.isEmpty() && worker.getFollowState() != 1;
    }

    public boolean moveToPosition(BlockPos pos){
        if(pos == null){
            return false;
        }
        else{
            double distance = pos.getCenter().distanceToSqr(worker.position());
            if(distance < 15){
                return false;
            }
            else{
                this.worker.getNavigation().moveTo(pos.getX(), pos.getY(), pos.getZ(), 0.8F);
                this.worker.setFollowState(6); //Working
                this.worker.getLookControl().setLookAt(pos.getCenter());
            }
            return true;
        }
    }

    public int getAmountOfItem(Item item){
        int amount = 0;
        for (int i = 0; i < worker.getInventory().getContainerSize(); i++) {
            ItemStack containerItem = worker.getInventory().getItem(i);
            if(containerItem.is(item)){
                amount += containerItem.getCount();
            }
        }
        return amount;
    }

    public boolean isContainerFull(Container container){
        for(int i = 0; i < container.getContainerSize(); i++){
            ItemStack itemStack = container.getItem(i);
            if(itemStack.isEmpty()) return false;
        }
        return true;
    }

    public void interactChest(Container container, boolean open) {
        if(container instanceof CompoundContainer || container instanceof ChestBlockEntity){
            BlockState state = this.worker.getCommandSenderWorld().getBlockState(this.chestPos);
            Block block = state.getBlock();
            boolean isOpened = false;
            CompoundTag compoundTag = new CompoundTag();
            if(worker.getCommandSenderWorld().getBlockEntity(chestPos) instanceof ChestBlockEntity chestBlockEntity){
                compoundTag = chestBlockEntity.getPersistentData();
                if(compoundTag.contains("isOpened"))
                    isOpened = compoundTag.getBoolean("isOpened");
                else
                    compoundTag.putBoolean("isOpened", false);
            }

            if (open) {
                if(!isOpened){
                    this.worker.getCommandSenderWorld().blockEvent(this.chestPos, block, 1, 1);
                    this.worker.getCommandSenderWorld().playSound(null, chestPos, SoundEvents.CHEST_OPEN, worker.getSoundSource(), 0.7F, 0.8F + 0.4F * worker.getRandom().nextFloat());
                    compoundTag.putBoolean("isOpened", true);
                }
            }
            else {
                if(isOpened){
                    this.worker.getCommandSenderWorld().blockEvent(this.chestPos, block, 1, 0);
                    this.worker.getCommandSenderWorld().playSound(null, chestPos, SoundEvents.CHEST_CLOSE, worker.getSoundSource(), 0.7F, 0.8F + 0.4F * worker.getRandom().nextFloat());
                    compoundTag.putBoolean("isOpened", false);
                }
            }
            this.worker.getCommandSenderWorld().gameEvent(this.worker, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, chestPos);
        }
    }
    public Container getContainer(BlockPos chestPos) {
        BlockEntity entity = worker.getCommandSenderWorld().getBlockEntity(chestPos);
        BlockState blockState = worker.getCommandSenderWorld().getBlockState(chestPos);
        if (blockState.getBlock() instanceof ChestBlock chestBlock) {
            return ChestBlock.getContainer(chestBlock, blockState, worker.getCommandSenderWorld(), chestPos, false);
        } else if (entity instanceof Container containerEntity) {
            return containerEntity;
        }
        else {
            //messageCantFindChest = true;
        }
        return null;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public boolean isInterruptable() {
        return false;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

}
