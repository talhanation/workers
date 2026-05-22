package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import com.talhanation.workers.entities.FarmerEntity;
import com.talhanation.workers.entities.workarea.CropArea;
import com.talhanation.workers.entities.workarea.StorageArea;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
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

import javax.annotation.Nullable;
import java.util.*;

public abstract class AbstractChestGoal extends Goal {
    public StorageArea storageArea;
    public Stack<BlockPos> blockPosStack = new Stack<>();
    public Stack<StorageArea> storageAreaStack = new Stack<>();
    public Container container;
    public AbstractWorkerEntity worker;
    public BlockPos chestPos;
    public List<UUID> visited = new ArrayList<>();
    public AbstractChestGoal(AbstractWorkerEntity worker){
        this.worker = worker;
    }

    @Override
    public boolean canUse(){
        return worker.getFollowState() != 1;
    }

    public boolean moveToPosition(BlockPos pos){
        if(pos == null){
            return false;
        }
        else{
            double distance = pos.getCenter().distanceToSqr(worker.position());
            if(distance < 20){
                worker.getNavigation().stop();
                return false;
            }
            else{
                this.worker.setFollowState(6); //Deposit
                this.worker.getNavigation().moveTo(pos.getX(), pos.getY(), pos.getZ(), 0.9F);
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
        if(this.chestPos != null && (container instanceof CompoundContainer || container instanceof ChestBlockEntity)){
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
        return this.storageArea.storageMap.get(chestPos);
    }

    public void scanAvailableStorageAreas() {
        List<StorageArea> list = this.worker.getCommandSenderWorld().getEntitiesOfClass(StorageArea.class, this.worker.getBoundingBox().inflate(64));

        list.removeIf(storageArea -> !storageArea.canWorkHere(worker));

        if(this.worker.lastStorage != null && list.stream().anyMatch(area -> area.getUUID().equals(this.worker.lastStorage))){
            list.removeIf(area -> !area.getUUID().equals(this.worker.lastStorage));
        }

        list.removeIf(storageArea -> this.visited.contains(storageArea.getUUID()));
        list.sort(Comparator.comparing(area -> area.distanceToSqr(this.worker.position())));

        list.forEach(area -> storageAreaStack.push(area));
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public boolean isInterruptable() {
        return true;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void stop() {
        if (this.chestPos != null && this.container != null) {
            this.interactChest(this.container, false);
        }
    }

}
