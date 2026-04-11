package com.talhanation.workers.entities.workarea;

import com.talhanation.workers.client.gui.StorageAreaScreen;
import com.talhanation.workers.entities.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.*;

public class StorageArea extends AbstractWorkAreaEntity {

    public static final EntityDataAccessor<Integer> STORAGE_TYPES = SynchedEntityData.defineId(StorageArea.class, EntityDataSerializers.INT);
    public Map<BlockPos, Container> storageMap = new HashMap<>();

    public StorageArea(EntityType<?> type, Level level) {
        super(type, level);
    }
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(STORAGE_TYPES, 0);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(STORAGE_TYPES, tag.getInt("StorageTypes"));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("StorageTypes", this.entityData.get(STORAGE_TYPES));
    }

    public Item getRenderItem(){
        return Items.CHEST;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public Screen getScreen(Player player) {
        return new StorageAreaScreen(this, player);
    }
    public void scanStorageBlocks(){
        if(area == null) area = this.getArea();

        storageMap.clear();

        BlockPos.betweenClosedStream(area).forEach(pos -> {
            BlockState stateAbove = this.getCommandSenderWorld().getBlockState(pos.above());

            if(stateAbove.isAir()){
                Container container = getContainer(pos);

                if(container != null && !storageMap.containsValue(container)){
                    storageMap.put(pos.immutable(), container);
                }
            }
        });
    }
    public Container getContainer(BlockPos chestPos) {
        BlockEntity entity = this.getCommandSenderWorld().getBlockEntity(chestPos);
        BlockState blockState = this.getCommandSenderWorld().getBlockState(chestPos);
        if (blockState.getBlock() instanceof ChestBlock chestBlock) {
            return ChestBlock.getContainer(chestBlock, blockState, this.getCommandSenderWorld(), chestPos, false);
        } else if (entity instanceof Container containerEntity) {
            return containerEntity;
        }
        return null;
    }

    public int getStorageMask(EnumSet<StorageType> types){
        int mask = 0;
        for (StorageType type : types) {
            mask |= (1 << type.getIndex());
        }

        return mask;
    }
    public void setStorageTypes(int mask) {
        this.entityData.set(STORAGE_TYPES, mask);
    }
    public EnumSet<StorageType> getStorageTypes() {
        int mask = this.entityData.get(STORAGE_TYPES);
        EnumSet<StorageType> set = EnumSet.noneOf(StorageType.class);

        for (StorageType type : StorageType.values()) {
            if ((mask & (1 << type.getIndex())) != 0) {
                set.add(type);
            }
        }
        return set;
    }

    public enum StorageType {
        MINERS(0),
        LUMBERS(1),
        BUILDERS(2),
        FARMERS(3),
        MERCHANTS(4),
        FISHERMAN(5),
        ANIMAL_FARMERS(6),
        COURIER(7);
        private final int index;
        StorageType(int index){
            this.index = index;
        }
        public int getIndex(){
            return this.index;
        }

        public static StorageType fromIndex(int index) {
            for (StorageType messengerState : StorageType.values()) {
                if (messengerState.getIndex() == index) {
                    return messengerState;
                }
            }
            throw new IllegalArgumentException("Invalid State index: " + index);
        }
    }

    public boolean canWorkHere(AbstractWorkerEntity worker){
        EnumSet<StorageType> types = this.getStorageTypes();
        if(super.canWorkHere(worker)){
            if(worker instanceof FarmerEntity){
                return types.contains(StorageType.FARMERS);
            }
            else if( worker instanceof LumberjackEntity){
                return types.contains(StorageType.LUMBERS);
            }
            else if( worker instanceof MinerEntity){
                return types.contains(StorageType.MINERS);
            }
            else if( worker instanceof BuilderEntity){
                return types.contains(StorageType.BUILDERS);
            }
            else if( worker instanceof MerchantEntity){
                return types.contains(StorageType.MERCHANTS);
            }
            else if( worker instanceof FishermanEntity){
                return types.contains(StorageType.FISHERMAN);
            }
            else if( worker instanceof AnimalFarmerEntity){
                return this.getStorageTypes().contains(StorageType.ANIMAL_FARMERS);
            }
            else if( worker instanceof CourierEntity){
                return this.getStorageTypes().contains(StorageType.COURIER);
            }

        }
        return false;
    }
}
