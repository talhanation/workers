package com.talhanation.workers.entities.workarea;

import com.talhanation.workers.client.gui.StorageAreaScreen;
import com.talhanation.workers.entities.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.*;

public class StorageArea extends AbstractWorkAreaEntity implements IPermissionArea, Container {

    public static final EntityDataAccessor<Integer> STORAGE_TYPES = SynchedEntityData.defineId(StorageArea.class, EntityDataSerializers.INT);
    public Map<BlockPos, Container> storageMap = new LinkedHashMap<>();
    /** Storekeeper order: which chest holds which category or item, see StorageSorter. Saved. */
    private final Map<BlockPos, String> chestLabels = new HashMap<>();
    /** Items with their own chests, worked out on every plan of the storekeeper. Not saved. */
    private final Set<Item> dedicatedItems = new HashSet<>();
    /** The storekeeper of this storage, one per storage. Saved. */
    @Nullable private UUID storekeeperUUID;

    public StorageArea(EntityType<?> type, Level level) {
        super(type, level);
    }
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        // a new storage is open for every worker, the player only unticks what he does not want
        builder.define(STORAGE_TYPES, getAllTypesMask());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("StorageTypes")) this.entityData.set(STORAGE_TYPES, tag.getInt("StorageTypes"));

        chestLabels.clear();
        ListTag labels = tag.getList("ChestLabels", Tag.TAG_COMPOUND);
        for (int i = 0; i < labels.size(); i++) {
            CompoundTag entry = labels.getCompound(i);
            chestLabels.put(NbtUtils.readBlockPos(entry, "Pos").orElse(BlockPos.ZERO), entry.getString("Label"));
        }
        this.storekeeperUUID = tag.hasUUID("Storekeeper") ? tag.getUUID("Storekeeper") : null;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("StorageTypes", this.entityData.get(STORAGE_TYPES));

        ListTag labels = new ListTag();
        for (Map.Entry<BlockPos, String> entry : chestLabels.entrySet()) {
            CompoundTag nbt = new CompoundTag();
            nbt.put("Pos", NbtUtils.writeBlockPos(entry.getKey()));
            nbt.putString("Label", entry.getValue());
            labels.add(nbt);
        }
        tag.put("ChestLabels", labels);
        if (storekeeperUUID != null) tag.putUUID("Storekeeper", storekeeperUUID);
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

                if(container != null && !isAlreadyMapped(storageMap, container)){
                    storageMap.put(pos.immutable(), container);
                }
            }
        });
    }

    public Map<BlockPos, String> getChestLabels() {
        return chestLabels;
    }

    @Nullable
    public String getChestLabel(BlockPos pos) {
        return chestLabels.get(pos);
    }

    public Set<Item> getDedicatedItems() {
        return dedicatedItems;
    }

    @Nullable
    public UUID getStorekeeperUUID() {
        return storekeeperUUID;
    }

    public void setStorekeeperUUID(@Nullable UUID uuid) {
        this.storekeeperUUID = uuid;
    }

    public int getStorageMask(EnumSet<StorageType> types){
        int mask = 0;
        for (StorageType type : types) {
            mask |= (1 << type.getIndex());
        }

        return mask;
    }
    public static int getAllTypesMask() {
        int mask = 0;
        for (StorageType type : StorageType.values()) {
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
    //TODO: REMOVE ONCE RECRUITS HAS UPDATED OTHERWISE UPKEEP ON STORAGE DOES NOT WORK
    @Override
    public int getContainerSize() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public ItemStack getItem(int p_18941_) {
        return null;
    }

    @Override
    public ItemStack removeItem(int p_18942_, int p_18943_) {
        return null;
    }

    @Override
    public ItemStack removeItemNoUpdate(int p_18951_) {
        return null;
    }

    @Override
    public void setItem(int p_18944_, ItemStack p_18945_) {

    }

    @Override
    public void setChanged() {

    }

    @Override
    public boolean stillValid(Player p_18946_) {
        return false;
    }

    @Override
    public void clearContent() {

    }
    //TODO ABOVE IS DESCRIPTION
    public enum StorageType {
        MINERS(0),
        LUMBERS(1),
        BUILDERS(2),
        FARMERS(3),
        MERCHANTS(4),
        FISHERMAN(5),
        ANIMAL_FARMERS(6),
        COOK(7),
        COURIER(8),
        CRAFTSMAN(10);
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
            else if( worker instanceof CraftsmanEntity){
                return this.getStorageTypes().contains(StorageType.CRAFTSMAN);
            }
            else if( worker instanceof StorekeeperEntity){
                return true;
            }
        }
        return false;
    }
}