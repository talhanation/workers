package com.talhanation.workers.entities.workarea;

import com.talhanation.workers.client.gui.KitchenAreaScreen;
import com.talhanation.workers.entities.AbstractWorkerEntity;
import com.talhanation.workers.entities.CookEntity;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;

public class KitchenArea extends AbstractWorkAreaEntity implements IPermissionArea {

    public static final EntityDataAccessor<Boolean> FEED_VILLAGERS = SynchedEntityData.defineId(KitchenArea.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Integer> FURNACE_COUNT     = SynchedEntityData.defineId(KitchenArea.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> CONTAINER_COUNT   = SynchedEntityData.defineId(KitchenArea.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<String>  COOK_NAME         = SynchedEntityData.defineId(KitchenArea.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Integer> TOTAL_SLOTS       = SynchedEntityData.defineId(KitchenArea.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> FREE_SLOTS        = SynchedEntityData.defineId(KitchenArea.class, EntityDataSerializers.INT);

    public Map<BlockPos, AbstractFurnaceBlockEntity> furnaceMap   = new HashMap<>();
    public Map<BlockPos, Container>                  containerMap = new HashMap<>();

    public KitchenArea(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(FEED_VILLAGERS, true);
        this.entityData.define(FURNACE_COUNT, 0);
        this.entityData.define(CONTAINER_COUNT, 0);
        this.entityData.define(COOK_NAME, "None");
        this.entityData.define(TOTAL_SLOTS, 0);
        this.entityData.define(FREE_SLOTS, 0);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        // Read new key first, fall back to the old (buggy) key so existing saves keep their setting.
        if (tag.contains("feedVillagers")) {
            this.setFeedVillagers(tag.getBoolean("feedVillagers"));
        } else if (tag.contains("sellToVillagers")) {
            this.setFeedVillagers(tag.getBoolean("sellToVillagers"));
        }
        setBeingWorkedOn(false);
        this.setCookName("None");
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("feedVillagers", this.getFeedVillagers());
    }

    @Override
    public Item getRenderItem() {
        return Items.FURNACE;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public Screen getScreen(Player player) {
        return new KitchenAreaScreen(this, player);
    }

    // ── Scanning ──────────────────────────────────────────────────────────────

    public void scanArea() {
        if (this.getCommandSenderWorld().isClientSide()) return;
        if (area == null) area = this.getArea();

        furnaceMap.clear();
        containerMap.clear();

        BlockPos.betweenClosedStream(area).forEach(pos -> {
            BlockEntity be = this.getCommandSenderWorld().getBlockEntity(pos);
            BlockState  bs = this.getCommandSenderWorld().getBlockState(pos);

            if (be instanceof AbstractFurnaceBlockEntity furnace && !furnace.isRemoved()) {
                furnaceMap.put(pos.immutable(), furnace);
            }
            else if (bs.getBlock() instanceof ChestBlock chestBlock) {
                Container chest = ChestBlock.getContainer(chestBlock, bs, this.getCommandSenderWorld(), pos, false);
                if (chest != null && !isAlreadyMapped(containerMap, chest)) {
                    containerMap.put(pos.immutable(), chest);
                }
            }
            else if (be instanceof Container container && !be.isRemoved()) {
                if (!isAlreadyMapped(containerMap, container)) {
                    containerMap.put(pos.immutable(), container);
                }
            }
        });

        this.setFurnaceCount(furnaceMap.size());
        this.setContainerCount(containerMap.size());
        this.setTotalSlots(containerMap.values().stream().mapToInt(Container::getContainerSize).sum());
        this.setFreeSlots(containerMap.values().stream().mapToInt(this::countEmptySpace).sum());
    }

    public int countEmptySpace(Container container) {
        int empty = 0;
        for (int j = 0; j < container.getContainerSize(); ++j) {
            if (container.getItem(j).isEmpty()) empty++;
        }
        return empty;
    }

    public boolean hasMinimumSetup() {
        if (furnaceMap.isEmpty() || containerMap.isEmpty()) scanArea();
        return !furnaceMap.isEmpty() && !containerMap.isEmpty();
    }

    // ── Container helpers ─────────────────────────────────────────────────────

    public int countItemInContainers(ItemStack stack) {
        if (containerMap.isEmpty()) scanArea();
        int total = 0;
        for (Container container : containerMap.values()) {
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack slot = container.getItem(i);
                if (!slot.isEmpty() && ItemStack.isSameItemSameTags(slot, stack)) {
                    total += slot.getCount();
                }
            }
        }
        return total;
    }

    public boolean shrinkItemFromContainers(ItemStack stack, int count) {
        if (containerMap.isEmpty()) scanArea();
        int toShrink = count;
        for (Container container : containerMap.values()) {
            boolean changed = false;
            for (int i = 0; i < container.getContainerSize() && toShrink > 0; i++) {
                ItemStack slot = container.getItem(i);
                if (!slot.isEmpty() && ItemStack.isSameItemSameTags(slot, stack)) {
                    int take = Math.min(toShrink, slot.getCount());
                    slot.shrink(take);
                    toShrink -= take;
                    changed   = true;
                }
            }
            if (changed) container.setChanged();
        }
        return toShrink <= 0;
    }

    public boolean depositItemToContainers(ItemStack stack, int count) {
        if (containerMap.isEmpty()) scanArea();
        int remaining = count;
        for (Container container : containerMap.values()) {
            if (remaining <= 0) break;
            boolean changed = false;
            for (int i = 0; i < container.getContainerSize() && remaining > 0; i++) {
                ItemStack slot = container.getItem(i);
                if (slot.isEmpty()) {
                    int put = Math.min(remaining, stack.getMaxStackSize());
                    ItemStack newStack = stack.copy();
                    newStack.setCount(put);
                    container.setItem(i, newStack);
                    remaining -= put;
                    changed    = true;
                }
                else if (ItemStack.isSameItemSameTags(slot, stack) && slot.getCount() < slot.getMaxStackSize()) {
                    int put = Math.min(remaining, slot.getMaxStackSize() - slot.getCount());
                    slot.grow(put);
                    remaining -= put;
                    changed    = true;
                }
            }
            if (changed) container.setChanged();
        }
        return remaining <= 0;
    }

    public boolean canAddItem(ItemStack toAdd) {
        if (containerMap.isEmpty()) scanArea();
        for (Container container : containerMap.values()) {
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack slot = container.getItem(i);
                if (slot.isEmpty()) return true;
                if (ItemStack.isSameItemSameTags(slot, toAdd) && slot.getCount() < slot.getMaxStackSize()) return true;
            }
        }
        return false;
    }

    // ── Furnace helpers ───────────────────────────────────────────────────────

    public int countFurnaceOutput(ItemStack stack) {
        pruneRemovedFurnaces();
        int total = 0;
        for (AbstractFurnaceBlockEntity furnace : furnaceMap.values()) {
            ItemStack output = furnace.getItem(2);
            if (!output.isEmpty() && ItemStack.isSameItemSameTags(output, stack)) {
                total += output.getCount();
            }
        }
        return total;
    }

    /** Removes stale furnace entries whose BlockEntity has been removed from the world. */
    public void pruneRemovedFurnaces() {
        boolean dirty = furnaceMap.values().removeIf(AbstractFurnaceBlockEntity::isRemoved);
        if (dirty) setFurnaceCount(furnaceMap.size());
    }

    /** Removes stale container entries whose BlockEntity has been removed from the world. */
    public void pruneRemovedContainers() {
        boolean dirty = containerMap.entrySet().removeIf(e -> {
            if (e.getValue() instanceof BlockEntity be) return be.isRemoved();
            return false;
        });
        if (dirty) setContainerCount(containerMap.size());
    }

    // ── canWorkHere ───────────────────────────────────────────────────────────

    @Override
    public boolean canWorkHere(AbstractWorkerEntity worker) {
        if (!(worker instanceof CookEntity)) return false;
        if (!super.canWorkHere(worker)) return false;
        if (isBeingWorkedOn()) return false;
        return true;
    }

    // ── Synced data ───────────────────────────────────────────────────────────

    public boolean getFeedVillagers()        { return this.entityData.get(FEED_VILLAGERS); }
    public void setFeedVillagers(boolean b) { this.entityData.set(FEED_VILLAGERS, b); }

    public int getFurnaceCount()              { return this.entityData.get(FURNACE_COUNT); }
    public void setFurnaceCount(int x)        { this.entityData.set(FURNACE_COUNT, x); }

    public int getContainerCount()            { return this.entityData.get(CONTAINER_COUNT); }
    public void setContainerCount(int x)      { this.entityData.set(CONTAINER_COUNT, x); }

    public String getCookName()               { return this.entityData.get(COOK_NAME); }
    public void setCookName(String name)      { this.entityData.set(COOK_NAME, name); }

    public int getTotalSlots()                { return this.entityData.get(TOTAL_SLOTS); }
    public void setTotalSlots(int x)          { this.entityData.set(TOTAL_SLOTS, x); }

    public int getFreeSlots()                 { return this.entityData.get(FREE_SLOTS); }
    public void setFreeSlots(int x)           { this.entityData.set(FREE_SLOTS, x); }
}