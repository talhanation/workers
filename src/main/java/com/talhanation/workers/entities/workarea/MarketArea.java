package com.talhanation.workers.entities.workarea;

import com.talhanation.workers.client.gui.MarketAreaScreen;
import com.talhanation.workers.entities.AbstractWorkerEntity;
import com.talhanation.workers.entities.MerchantEntity;
import com.talhanation.workers.world.MarketDirectory;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.*;

public class MarketArea extends AbstractWorkAreaEntity implements IPermissionArea {

    public static final EntityDataAccessor<Boolean> IS_OPEN = SynchedEntityData.defineId(MarketArea.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<String> MARKET_NAME = SynchedEntityData.defineId(MarketArea.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Integer> TOTAL_SLOTS = SynchedEntityData.defineId(MarketArea.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> FREE_SLOTS = SynchedEntityData.defineId(MarketArea.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<String> MERCHANT_NAME = SynchedEntityData.defineId(MarketArea.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Boolean> USE_STORAGE = SynchedEntityData.defineId(MarketArea.class, EntityDataSerializers.BOOLEAN);
    /** The merchant never buys from villagers if less than this amount of currency would stay in the market. */
    public static final EntityDataAccessor<Integer> CURRENCY_RESERVE = SynchedEntityData.defineId(MarketArea.class, EntityDataSerializers.INT);
    public static final int MAX_CURRENCY_RESERVE = 9999;
    /** Travelling merchants of other players may trade here. */
    public static final EntityDataAccessor<Boolean> ACCEPT_FOREIGN = SynchedEntityData.defineId(MarketArea.class, EntityDataSerializers.BOOLEAN);
    /** The merchant of this market is on a trading trip. */
    public static final EntityDataAccessor<Boolean> MERCHANT_TRAVELLING = SynchedEntityData.defineId(MarketArea.class, EntityDataSerializers.BOOLEAN);
    private static final int DIRECTORY_UPDATE_INTERVAL = 200;
    /** Storages within this range around the market are used by the merchant. */
    public static final int STORAGE_RANGE = 64;
    public Map<BlockPos, Container> containerMap = new HashMap<>();

    /** Persistent server-side UUID of the assigned merchant (one per market). */
    @Nullable public UUID assignedMerchantUUID = null;

    public MarketArea(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(IS_OPEN, true);
        this.entityData.define(MARKET_NAME, "Market");
        this.entityData.define(MERCHANT_NAME, "None");
        this.entityData.define(TOTAL_SLOTS, 0);
        this.entityData.define(FREE_SLOTS, 0);
        this.entityData.define(USE_STORAGE, true);
        this.entityData.define(CURRENCY_RESERVE, 0);
        this.entityData.define(ACCEPT_FOREIGN, true);
        this.entityData.define(MERCHANT_TRAVELLING, false);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setOpen(tag.getBoolean("isOpen"));
        if (tag.contains("marketName"))   this.setMarketName(tag.getString("marketName"));
        if (tag.contains("merchantName")) this.setMerchantName(tag.getString("merchantName"));
        if (tag.contains("assignedMerchantUUID")) this.assignedMerchantUUID = tag.getUUID("assignedMerchantUUID");
        if (tag.contains("useStorage")) this.setUseStorage(tag.getBoolean("useStorage"));
        this.setCurrencyReserve(tag.getInt("currencyReserve"));
        if (tag.contains("acceptForeign")) this.setAcceptForeignMerchants(tag.getBoolean("acceptForeign"));
        this.setMerchantTravelling(tag.getBoolean("merchantTravelling"));

        setBeingWorkedOn(false);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("isOpen", this.isOpen());
        tag.putString("marketName", this.getMarketName());
        tag.putString("merchantName", this.getMerchantName());
        if (assignedMerchantUUID != null) tag.putUUID("assignedMerchantUUID", assignedMerchantUUID);
        tag.putBoolean("useStorage", this.getUseStorage());
        tag.putInt("currencyReserve", this.getCurrencyReserve());
        tag.putBoolean("acceptForeign", this.getAcceptForeignMerchants());
        tag.putBoolean("merchantTravelling", this.isMerchantTravelling());
    }

    @Override
    public Item getRenderItem() {
        return Items.EMERALD;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public Screen getScreen(Player player) {
        return new MarketAreaScreen(this, player);
    }

    @Override
    public void tick() {
        super.tick();
        // travelling merchants find markets in unloaded chunks through the directory
        if (this.getCommandSenderWorld() instanceof ServerLevel level && (tickCount == 20 || tickCount % DIRECTORY_UPDATE_INTERVAL == 0)) {
            MarketDirectory.update(level, this);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (reason.shouldDestroy() && this.getCommandSenderWorld() instanceof ServerLevel level) {
            MarketDirectory.remove(level, this.getUUID());
        }
        super.remove(reason);
    }

    /** Storages with the merchant checkbox within range of the market, nearest first, already scanned. */
    public List<StorageArea> findStorages(ServerLevel level, AbstractWorkerEntity worker) {
        if (!this.getUseStorage()) return new ArrayList<>();
        List<StorageArea> storages = level.getEntitiesOfClass(StorageArea.class, new AABB(this.blockPosition()).inflate(STORAGE_RANGE),
                storage -> storage.isAlive() && storage.canWorkHere(worker));
        storages.sort(Comparator.comparingDouble(storage -> storage.distanceToSqr(this)));
        storages.forEach(StorageArea::scanStorageBlocks);
        storages.removeIf(storage -> storage.storageMap.isEmpty());
        return storages;
    }

    public void scanContainers() {
        if (area == null) area = this.getArea();
        containerMap.clear();

        BlockPos.betweenClosedStream(area).forEach(pos -> {
            BlockState stateAbove = this.getCommandSenderWorld().getBlockState(pos.above());
            if (stateAbove.isAir()) {
                Container container = getContainer(pos);
                if (container != null && !isAlreadyMapped(containerMap, container)) {
                    containerMap.put(pos.immutable(), container);

                }
            }
        });
        setTotalSlots(containerMap.values().stream().mapToInt(Container::getContainerSize).sum());
        setFreeSlots(containerMap.values().stream().mapToInt(this::countEmptySpace).sum());
    }

    public int countEmptySpace(Container container){
        int i = 0;

        for(int j = 0; j < container.getContainerSize(); ++j) {
            ItemStack itemstack = container.getItem(j);
            if (itemstack.isEmpty()) {
                i ++;
            }
        }

        return i;
    }

    public boolean shrinkItemFromContainers(ItemStack stack, int count, boolean allowDamaged) {
        if (containerMap.isEmpty()) scanContainers();
        int toShrink = count;
        for (Container container : containerMap.values()) {
            boolean changed = false;
            for (int i = 0; i < container.getContainerSize() && toShrink > 0; i++) {
                ItemStack slot = container.getItem(i);
                if (!slot.isEmpty() && itemsMatch(slot, stack, allowDamaged)) {
                    int take = Math.min(toShrink, slot.getCount());
                    slot.shrink(take);
                    toShrink -= take;
                    changed = true;
                }
            }
            if (changed) container.setChanged();
        }
        return toShrink <= 0;
    }

    public int countItemInContainers(ItemStack stack, boolean allowDamaged) {
        if (containerMap.isEmpty()) scanContainers();
        int total = 0;
        for (Container container : containerMap.values()) {
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack slot = container.getItem(i);
                if (!slot.isEmpty() && itemsMatch(slot, stack, allowDamaged)) {
                    total += slot.getCount();
                }
            }
        }
        return total;
    }

    /** Puts count of the stack into the market containers, returns what did not fit. */
    public int depositItemToContainers(ItemStack stack, int count) {
        if (containerMap.isEmpty()) scanContainers();
        int remaining = count;
        for (Container container : containerMap.values()) {
            if (remaining <= 0) break;
            int before = remaining;
            for (int i = 0; i < container.getContainerSize() && remaining > 0; i++) {
                ItemStack slot = container.getItem(i);
                if (slot.isEmpty()) {
                    int put = Math.min(remaining, stack.getMaxStackSize());
                    ItemStack newStack = stack.copy();
                    newStack.setCount(put);
                    container.setItem(i, newStack);
                    remaining -= put;
                }
                else if (ItemStack.isSameItemSameTags(slot, stack) && slot.getCount() < slot.getMaxStackSize()) {
                    int put = Math.min(remaining, slot.getMaxStackSize() - slot.getCount());
                    slot.grow(put);
                    remaining -= put;
                }
            }
            if (remaining != before) container.setChanged();
        }
        return remaining;
    }

    public boolean canAddItem(ItemStack toAdd) {
        if (containerMap.isEmpty()) scanContainers();
        boolean flag = false;
        for (Container container : containerMap.values()) {
            for(int i = 0; i < container.getContainerSize(); i ++) {
                ItemStack itemStack = container.getItem(i);
                if (itemStack.isEmpty() || ItemStack.isSameItemSameTags(itemStack, toAdd) && itemStack.getCount() < itemStack.getMaxStackSize()) {
                    flag = true;
                    break;
                }
            }
        }

        return flag;
    }

    private static boolean itemsMatch(ItemStack a, ItemStack b, boolean allowDamaged) {
        if (allowDamaged) return a.getItem() == b.getItem();
        return ItemStack.isSameItemSameTags(a, b);
    }
    @Override
    public boolean canWorkHere(AbstractWorkerEntity worker) {
        if (!(worker instanceof MerchantEntity merchant)) return false;
        if (!super.canWorkHere(worker)) return false;
        if (!isOpen()) return false;
        if (isBeingWorkedOn()) return false;
        // If a specific merchant is assigned, only they may work here
        if (assignedMerchantUUID != null && !assignedMerchantUUID.equals(worker.getUUID())) return false;
        return true;
    }

    //////////////////////////////// MERCHANT ASSIGNMENT ////////////////////////

    public boolean isAssigned() {
        return assignedMerchantUUID != null;
    }

    public boolean isAssignedTo(UUID merchantUUID) {
        return merchantUUID != null && merchantUUID.equals(assignedMerchantUUID);
    }

    public void assignMerchant(UUID merchantUUID, String merchantName) {
        this.assignedMerchantUUID = merchantUUID;
        this.setMerchantName(merchantName);
    }

    public void clearAssignedMerchant() {
        this.assignedMerchantUUID = null;
        this.setMerchantName("None");
    }
    public boolean isOpen() { return this.entityData.get(IS_OPEN); }
    public void setOpen(boolean open) { this.entityData.set(IS_OPEN, open); }

    public String getMarketName() { return this.entityData.get(MARKET_NAME); }
    public void setMarketName(String name) { this.entityData.set(MARKET_NAME, name); }
    public String getMerchantName() { return this.entityData.get(MERCHANT_NAME); }
    public void setMerchantName(String name) { this.entityData.set(MERCHANT_NAME, name); }
    public int getTotalSlots() { return this.entityData.get(TOTAL_SLOTS); }
    public void setTotalSlots(int x) {
        this.entityData.set(TOTAL_SLOTS, x);
    }
    public int getFreeSlots() {
        return this.entityData.get(FREE_SLOTS);
    }
    public boolean getUseStorage() { return this.entityData.get(USE_STORAGE); }
    public void setUseStorage(boolean b) { this.entityData.set(USE_STORAGE, b); }
    public boolean getAcceptForeignMerchants() { return this.entityData.get(ACCEPT_FOREIGN); }
    public void setAcceptForeignMerchants(boolean b) { this.entityData.set(ACCEPT_FOREIGN, b); }
    public boolean isMerchantTravelling() { return this.entityData.get(MERCHANT_TRAVELLING); }
    public void setMerchantTravelling(boolean b) { this.entityData.set(MERCHANT_TRAVELLING, b); }
    public int getCurrencyReserve() { return this.entityData.get(CURRENCY_RESERVE); }
    public void setCurrencyReserve(int x) { this.entityData.set(CURRENCY_RESERVE, Math.max(0, Math.min(MAX_CURRENCY_RESERVE, x))); }
    public void setFreeSlots(int x) {
        this.entityData.set(FREE_SLOTS, x);
    }
}