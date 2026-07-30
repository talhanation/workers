package com.talhanation.workers.entities.workarea;

import com.talhanation.workers.entities.*;
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

import java.util.*;

public class StorageArea extends AbstractWorkAreaEntity implements IPermissionArea, Container {

    public static final EntityDataAccessor<Integer> STORAGE_TYPES = SynchedEntityData.defineId(StorageArea.class, EntityDataSerializers.INT);
    public Map<BlockPos, Container> storageMap = new LinkedHashMap<>();

    public StorageArea(EntityType<?> type, Level level) {
        super(type, level);
    }
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STORAGE_TYPES, 0);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(STORAGE_TYPES, tag.getInt("StorageTypes"));
        this.storageMap.clear();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("StorageTypes", this.entityData.get(STORAGE_TYPES));
    }

    public Item getRenderItem(){
        return Items.CHEST;
    }
    public void scanStorageBlocks(){
        if(area == null) area = this.getArea();

        storageMap.clear();

        BlockPos.betweenClosedStream(area).forEach(pos -> {
            BlockState stateAbove = this.getCommandSenderWorld().getBlockState(pos.above());

            if(stateAbove.isAir()){
                Container container = getContainer(pos);

                if(container != null && !isAlreadyMapped(storageMap, container)){
                    storageMap.put(pos.immutable(), container);
                }
            }
        });
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
    /**
     * Exposes every detected storage block as one logical container. Slot order follows the
     * deterministic scan order of {@link BlockPos#betweenClosedStream}, so a global slot keeps
     * referring to the same underlying chest until the area is rescanned.
     */
    @Override
    public int getContainerSize() {
        ensureStorageScanned();
        return storageMap.values().stream().mapToInt(Container::getContainerSize).sum();
    }

    @Override
    public boolean isEmpty() {
        ensureStorageScanned();
        return storageMap.values().stream().allMatch(Container::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        ContainerSlot resolved = resolveSlot(slot);
        return resolved == null ? ItemStack.EMPTY : resolved.container().getItem(resolved.slot());
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ContainerSlot resolved = resolveSlot(slot);
        return resolved == null
                ? ItemStack.EMPTY
                : resolved.container().removeItem(resolved.slot(), amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ContainerSlot resolved = resolveSlot(slot);
        return resolved == null
                ? ItemStack.EMPTY
                : resolved.container().removeItemNoUpdate(resolved.slot());
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        ContainerSlot resolved = resolveSlot(slot);
        if (resolved != null) {
            resolved.container().setItem(resolved.slot(), stack);
        }
    }

    @Override
    public void setChanged() {
        ensureStorageScanned();
        storageMap.values().forEach(Container::setChanged);
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.isRemoved()) {
            return false;
        }
        ensureStorageScanned();
        return storageMap.values().stream().allMatch(container -> container.stillValid(player));
    }

    @Override
    public void clearContent() {
        ensureStorageScanned();
        storageMap.values().forEach(Container::clearContent);
    }

    private void ensureStorageScanned() {
        if (storageMap.isEmpty() && !this.getCommandSenderWorld().isClientSide()) {
            scanStorageBlocks();
        }
    }

    private ContainerSlot resolveSlot(int globalSlot) {
        if (globalSlot < 0) {
            return null;
        }

        ensureStorageScanned();
        int slot = globalSlot;
        for (Container container : storageMap.values()) {
            int size = container.getContainerSize();
            if (slot < size) {
                return new ContainerSlot(container, slot);
            }
            slot -= size;
        }
        return null;
    }

    private record ContainerSlot(Container container, int slot) {
    }
    public enum StorageType {
        MINERS(0),
        LUMBERS(1),
        BUILDERS(2),
        FARMERS(3),
        MERCHANTS(4),
        FISHERMAN(5),
        ANIMAL_FARMERS(6),
        COOK(7),
        COURIER(8);
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
            else if( worker instanceof CookEntity){
                return this.getStorageTypes().contains(StorageType.COOK);
            }
        }
        return false;
    }
}
