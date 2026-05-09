package com.talhanation.workers.entities.workarea;

import com.talhanation.workers.client.gui.HomeAreaScreen;
import com.talhanation.workers.entities.AbstractWorkerEntity;
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
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.*;
import java.util.Comparator;

public class HomeArea extends AbstractWorkAreaEntity implements IPermissionArea{

    public static final EntityDataAccessor<String> RESIDENT_NAME = SynchedEntityData.defineId(HomeArea.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Boolean> IS_PLAYER_HOME = SynchedEntityData.defineId(HomeArea.class, EntityDataSerializers.BOOLEAN);
    @Nullable public UUID residentUUID = null;
    public long lastResidentSeenTick = 0L;

    // Capacity: exactly one worker per HomeArea
    private static final long EVICTION_TIMEOUT_TICKS = 24000L * 3L; // 3 in-game days

    public Map<BlockPos, Container> foodContainerMap = new HashMap<>();

    public HomeArea(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(RESIDENT_NAME, "");
        this.entityData.define(IS_PLAYER_HOME, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getCommandSenderWorld().isClientSide()) return;

        if (residentUUID != null) {
            long elapsed = this.getCommandSenderWorld().getGameTime() - lastResidentSeenTick;
            if (elapsed > EVICTION_TIMEOUT_TICKS) {
                clearResident();
            }
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("residentUUID")) {
            this.residentUUID = tag.getUUID("residentUUID");
            this.lastResidentSeenTick = tag.getLong("lastResidentSeen");
            this.setResidentName(tag.getString("residentName"));
            this.setPlayerHome(tag.getBoolean("isPlayerHome"));
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (residentUUID != null) {
            tag.putUUID("residentUUID", residentUUID);
            tag.putLong("lastResidentSeen", lastResidentSeenTick);
            tag.putString("residentName", getResidentName());
            tag.putBoolean("isPlayerHome", isPlayerHome());
        }
    }

    @Override
    public Item getRenderItem() {
        return Items.RED_BED;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public Screen getScreen(Player player) {
        return new HomeAreaScreen(this, player);
    }

    //////////////////////////////// RESIDENT ////////////////////////////////////

    public boolean isOccupied() {
        return residentUUID != null;
    }

    public boolean isResidentOf(UUID workerUUID) {
        return workerUUID != null && workerUUID.equals(residentUUID);
    }

    public void setResident(UUID workerUUID, String workerName) {
        this.residentUUID = workerUUID;
        this.lastResidentSeenTick = this.getCommandSenderWorld().getGameTime();
        this.setResidentName(workerName);
    }

    public void updateResidentSeen() {
        this.lastResidentSeenTick = this.getCommandSenderWorld().getGameTime();
    }

    public void clearResident() {
        this.residentUUID = null;
        this.lastResidentSeenTick = 0L;
        this.setResidentName("");
    }

    public String getResidentName() {
        return this.entityData.get(RESIDENT_NAME);
    }

    public void setResidentName(String name) {
        this.entityData.set(RESIDENT_NAME, name);
    }

    public boolean isPlayerHome() {
        return this.entityData.get(IS_PLAYER_HOME);
    }

    public void setPlayerHome(boolean b) {
        this.entityData.set(IS_PLAYER_HOME, b);
    }

    //////////////////////////////// SCANNING ////////////////////////////////////

    // Fantasy Furniture multi-block beds use this property; index 1 = the sleepable part
    private static final IntegerProperty MULTI_BLOCK_INDEX = IntegerProperty.create("multi_block_index", 0, 2);

    public Stack<BlockPos> getBedsStack(AbstractWorkerEntity worker) {
        if (area == null) this.area = getArea();

        Stack<BlockPos> stack = new Stack<>();

        BlockPos.betweenClosedStream(area).forEach(pos -> {
            BlockState state = this.getCommandSenderWorld().getBlockState(pos);

            if (!state.isBed(this.getCommandSenderWorld(), pos, worker)) return;
            if (state.hasProperty(BlockStateProperties.OCCUPIED)
                    && state.getValue(BlockStateProperties.OCCUPIED)) return;

            // Fantasy Furniture multi-block beds
            try {
                if (state.getValue(MULTI_BLOCK_INDEX) == 1) {
                    stack.push(pos.immutable());
                    return;
                }
            } catch (IllegalArgumentException ignored) {}

            // Vanilla beds — use the FOOT block so startSleeping() orients correctly
            if (state.hasProperty(BlockStateProperties.BED_PART)
                    && state.getValue(BlockStateProperties.BED_PART) == BedPart.HEAD) {
                stack.push(pos.immutable());
            }
        });

        // Nearest bed first (pop returns last element, so sort descending)
        stack.sort(Comparator.comparingDouble(pos -> worker.distanceToSqr(pos.getCenter())));
        return stack;
    }

    public void scanFoodContainers() {
        if (area == null) this.area = getArea();
        foodContainerMap.clear();

        BlockPos.betweenClosedStream(area).forEach(pos -> {
            Container container = getContainer(pos);
            if (container != null && !foodContainerMap.containsValue(container) && containerHasFood(container)) {
                foodContainerMap.put(pos.immutable(), container);
            }
        });
    }

    private boolean containerHasFood(Container container) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty() && stack.isEdible()) return true;
        }
        return false;
    }

    private Container getContainer(BlockPos pos) {
        BlockEntity entity = this.getCommandSenderWorld().getBlockEntity(pos);
        BlockState blockState = this.getCommandSenderWorld().getBlockState(pos);

        if (blockState.getBlock() instanceof ChestBlock chestBlock) {
            return ChestBlock.getContainer(chestBlock, blockState, this.getCommandSenderWorld(), pos, false);
        }
        else if (entity instanceof Container containerEntity) {
            return containerEntity;
        }
        return null;
    }
}