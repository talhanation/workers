package com.talhanation.workers.entities.workarea;

import com.talhanation.workers.client.gui.MarketAreaScreen;
import com.talhanation.workers.entities.AbstractWorkerEntity;
import com.talhanation.workers.entities.MerchantEntity;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.*;

public class MarketArea extends AbstractWorkAreaEntity {

    public static final EntityDataAccessor<Boolean> IS_OPEN = SynchedEntityData.defineId(MarketArea.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<String> MARKET_NAME = SynchedEntityData.defineId(MarketArea.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Integer> TOTAL_SLOTS = SynchedEntityData.defineId(MarketArea.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> FREE_SLOTS = SynchedEntityData.defineId(MarketArea.class, EntityDataSerializers.INT);
    public Map<BlockPos, Container> containerMap = new HashMap<>();

    public MarketArea(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(IS_OPEN, true);
        this.entityData.define(MARKET_NAME, "Market");
        this.entityData.define(TOTAL_SLOTS, 0);
        this.entityData.define(FREE_SLOTS, 0);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setOpen(tag.getBoolean("isOpen"));
        if (tag.contains("marketName")) this.setMarketName(tag.getString("marketName"));

        setBeingWorkedOn(false);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("isOpen", this.isOpen());
        tag.putString("marketName", this.getMarketName());
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

    public void scanContainers() {
        if (area == null) area = this.getArea();
        containerMap.clear();

        BlockPos.betweenClosedStream(area).forEach(pos -> {
            BlockState stateAbove = this.getCommandSenderWorld().getBlockState(pos.above());
            if (stateAbove.isAir()) {
                Container container = getContainer(pos);
                if (container != null && !containerMap.containsValue(container)) {
                    containerMap.put(pos.immutable(), container);

                }
            }
        });
        setTotalSlots(containerMap.values().stream().mapToInt(Container::getContainerSize).sum());
        setFreeSlots(containerMap.values().stream().mapToInt(container -> container.countItem(ItemStack.EMPTY.getItem())).sum());
    }

    public Container getContainer(BlockPos pos) {
        BlockEntity entity = this.getCommandSenderWorld().getBlockEntity(pos);
        BlockState blockState = this.getCommandSenderWorld().getBlockState(pos);
        if (blockState.getBlock() instanceof ChestBlock chestBlock) {
            return ChestBlock.getContainer(chestBlock, blockState, this.getCommandSenderWorld(), pos, false);
        } else if (entity instanceof Container containerEntity) {
            return containerEntity;
        }
        return null;
    }


    public boolean shrinkItemFromContainers(ItemStack stack, int count, boolean allowDamaged) {
        if (containerMap.isEmpty()) scanContainers();
        int toShrink = count;
        for (Container container : containerMap.values()) {
            for (int i = 0; i < container.getContainerSize() && toShrink > 0; i++) {
                ItemStack slot = container.getItem(i);
                if (!slot.isEmpty() && itemsMatch(slot, stack, allowDamaged)) {
                    int take = Math.min(toShrink, slot.getCount());
                    slot.shrink(take);
                    toShrink -= take;
                }
            }
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

    public void depositItemToContainers(ItemStack stack, int count) {
        if (containerMap.isEmpty()) scanContainers();
        int remaining = count;
        for (Container container : containerMap.values()) {
            if (remaining <= 0) break;
            for (int i = 0; i < container.getContainerSize() && remaining > 0; i++) {
                ItemStack slot = container.getItem(i);
                if (slot.isEmpty()) {
                    int put = Math.min(remaining, stack.getMaxStackSize());
                    ItemStack newStack = stack.copy();
                    newStack.setCount(put);
                    container.setItem(i, newStack);
                    remaining -= put;
                } else if (ItemStack.isSameItemSameTags(slot, stack) && slot.getCount() < slot.getMaxStackSize()) {
                    int put = Math.min(remaining, slot.getMaxStackSize() - slot.getCount());
                    slot.grow(put);
                    remaining -= put;
                }
            }
        }
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
        return true;
    }
    public boolean isOpen() { return this.entityData.get(IS_OPEN); }
    public void setOpen(boolean open) { this.entityData.set(IS_OPEN, open); }

    public String getMarketName() { return this.entityData.get(MARKET_NAME); }
    public void setMarketName(String name) { this.entityData.set(MARKET_NAME, name); }

    public int getTotalSlots() { return this.entityData.get(TOTAL_SLOTS); }
    public void setTotalSlots(int x) { this.entityData.set(TOTAL_SLOTS, x); }
    public int getFreeSlots() { return this.entityData.get(FREE_SLOTS); }
    public void setFreeSlots(int x) { this.entityData.set(FREE_SLOTS, x); }
}
