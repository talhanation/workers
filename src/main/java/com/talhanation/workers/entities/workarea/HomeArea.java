package com.talhanation.workers.entities.workarea;

import com.talhanation.workers.client.gui.HomeAreaScreen;
import com.talhanation.workers.entities.AbstractWorkerEntity;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class HomeArea extends AbstractWorkAreaEntity implements IPermissionArea {

    public static final EntityDataAccessor<String>  RESIDENT_NAME = SynchedEntityData.defineId(HomeArea.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Boolean> IS_PLAYER_HOME = SynchedEntityData.defineId(HomeArea.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Integer> ROOM_QUALITY = SynchedEntityData.defineId(HomeArea.class, EntityDataSerializers.INT);

    @Nullable public UUID    residentUUID         = null;
    public          long     lastResidentSeenTick = 0L;
    @Nullable public BlockPos assignedBedPos      = null;

    private static final long EVICTION_TIMEOUT_TICKS = 24000L * 3L;

    private static final IntegerProperty MULTI_BLOCK_INDEX = IntegerProperty.create("multi_block_index", 0, 2);

    public Map<BlockPos, Container> foodContainerMap = new HashMap<>();

    public HomeArea(EntityType<?> type, Level level) {
        super(type, level);
    }

    //////////////////////////////// LIFECYCLE //////////////////////////////////

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(RESIDENT_NAME, "");
        this.entityData.define(IS_PLAYER_HOME, false);
        this.entityData.define(ROOM_QUALITY, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getCommandSenderWorld().isClientSide()) return;

        if (residentUUID != null) {
            long elapsed = this.getCommandSenderWorld().getGameTime() - lastResidentSeenTick;
            if (elapsed > EVICTION_TIMEOUT_TICKS) {
                clearResident();
                return;
            }
        }

        if (assignedBedPos != null) {
            BlockState state = this.getCommandSenderWorld().getBlockState(assignedBedPos);
            if (state.isBed(this.getCommandSenderWorld(), assignedBedPos, null)) {
                if (state.hasProperty(BlockStateProperties.OCCUPIED) && !state.getValue(BlockStateProperties.OCCUPIED)) {
                    this.getCommandSenderWorld().setBlock(assignedBedPos, state.setValue(BlockStateProperties.OCCUPIED, true), 3);
                }
            }
            else {
                wakeResidentIfSleeping();
                assignedBedPos = null;
            }
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("residentUUID")) {
            this.residentUUID         = tag.getUUID("residentUUID");
            this.lastResidentSeenTick = tag.getLong("lastResidentSeen");
            this.setResidentName(tag.getString("residentName"));
            this.setPlayerHome(tag.getBoolean("isPlayerHome"));
        }
        if (tag.contains("assignedBedX")) {
            this.assignedBedPos = new BlockPos(
                    tag.getInt("assignedBedX"),
                    tag.getInt("assignedBedY"),
                    tag.getInt("assignedBedZ"));
        }
        if (tag.contains("roomQuality")) {
            this.entityData.set(ROOM_QUALITY, tag.getInt("roomQuality"));
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
        if (assignedBedPos != null) {
            tag.putInt("assignedBedX", assignedBedPos.getX());
            tag.putInt("assignedBedY", assignedBedPos.getY());
            tag.putInt("assignedBedZ", assignedBedPos.getZ());
        }
        tag.putInt("roomQuality", this.entityData.get(ROOM_QUALITY));
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

    //////////////////////////////// RESIDENT ///////////////////////////////////

    public boolean isOccupied() {
        return residentUUID != null;
    }

    public boolean isResidentOf(UUID workerUUID) {
        return workerUUID != null && workerUUID.equals(residentUUID);
    }

    public void setResident(UUID workerUUID, String workerName) {
        this.residentUUID         = workerUUID;
        this.lastResidentSeenTick = this.getCommandSenderWorld().getGameTime();
        this.setResidentName(workerName);
    }

    public void updateResidentSeen() {
        this.lastResidentSeenTick = this.getCommandSenderWorld().getGameTime();
    }

    public void clearResident() {
        wakeResidentIfSleeping();
        releaseBed();
        this.residentUUID         = null;
        this.lastResidentSeenTick = 0L;
        this.setResidentName("");
    }

    private void wakeResidentIfSleeping() {
        if (residentUUID == null) return;
        if (this.getCommandSenderWorld().isClientSide()) return;

        for (AbstractWorkerEntity worker : this.getCommandSenderWorld().getEntitiesOfClass(
                AbstractWorkerEntity.class, this.getBoundingBox().inflate(8))) {
            if (residentUUID.equals(worker.getUUID()) && worker.isSleeping()) {
                worker.stopSleeping();
                worker.clearSleepingPos();
            }
        }
    }

    public String  getResidentName()          { return this.entityData.get(RESIDENT_NAME); }
    public void    setResidentName(String n)  { this.entityData.set(RESIDENT_NAME, n); }
    public boolean isPlayerHome()             { return this.entityData.get(IS_PLAYER_HOME); }
    public void    setPlayerHome(boolean b)   { this.entityData.set(IS_PLAYER_HOME, b); }

    //////////////////////////////// BED MANAGEMENT /////////////////////////////


    public void scanAndClaimBed(AbstractWorkerEntity worker) {
        if (area == null) this.area = getArea();

        if (assignedBedPos != null) {
            BlockState state = this.getCommandSenderWorld().getBlockState(assignedBedPos);
            if (state.isBed(this.getCommandSenderWorld(), assignedBedPos, worker)) {
                ensureBedOccupied(assignedBedPos);
                return;
            }
            assignedBedPos = null; // was destroyed
        }

        for (BlockPos pos : BlockPos.betweenClosed(
                BlockPos.containing(area.minX, area.minY, area.minZ),
                BlockPos.containing(area.maxX - 1, area.maxY - 1, area.maxZ - 1))) {

            BlockState state = this.getCommandSenderWorld().getBlockState(pos);
            if (!state.isBed(this.getCommandSenderWorld(), pos, worker)) continue;

            try {
                if (state.getValue(MULTI_BLOCK_INDEX) == 1) {
                    assignedBedPos = pos.immutable();
                    ensureBedOccupied(assignedBedPos);
                    return;
                }
            } catch (IllegalArgumentException ignored) {}

            if (state.hasProperty(BlockStateProperties.BED_PART)
                    && state.getValue(BlockStateProperties.BED_PART) == BedPart.HEAD) {
                assignedBedPos = pos.immutable();
                ensureBedOccupied(assignedBedPos);
                return;
            }
        }
    }

    private void ensureBedOccupied(BlockPos pos) {
        BlockState state = this.getCommandSenderWorld().getBlockState(pos);
        if (state.hasProperty(BlockStateProperties.OCCUPIED)
                && !state.getValue(BlockStateProperties.OCCUPIED)) {
            this.getCommandSenderWorld().setBlock(pos, state.setValue(BlockStateProperties.OCCUPIED, true), 3);
        }
    }

    public void releaseBed() {
        if (assignedBedPos == null) return;
        BlockState state = this.getCommandSenderWorld().getBlockState(assignedBedPos);
        if (state.isBed(this.getCommandSenderWorld(), assignedBedPos, null)
                && state.hasProperty(BlockStateProperties.OCCUPIED)) {
            this.getCommandSenderWorld().setBlock(
                    assignedBedPos, state.setValue(BlockStateProperties.OCCUPIED, false), 3);
        }
        assignedBedPos = null;
    }

    //////////////////////////////// ROOM QUALITY SCAN //////////////////////////

    /**
     * Scans the area once when a worker arrives home.
     * Result is stored in the ROOM_QUALITY bitmask and synced to the client
     * for display in the HomeAreaScreen.
     *
     * bit 0 = enclosed walls/roof
     * bit 1 = door present
     * bit 2 = light source present
     * bit 3 = bed present
     * bit 4 = chest present
     */
    public BlockPos chestPos;
    private int quality;

    public void scanRoomQuality() {
        if (area == null) this.area = getArea();

        // int[] wrapper so the value is mutable inside the lambda
        int[] quality = {0};

        if (checkEnclosed()) quality[0] |= 1;

        BlockPos.betweenClosedStream(area.inflate(1)).forEach(pos -> {
            BlockPos immutable = pos.immutable();
            BlockState state   = level().getBlockState(immutable);
            Block      block   = state.getBlock();

            if (block instanceof DoorBlock) {
                quality[0] |= 2;
            }

            Container container = getContainer(immutable);
            if (container != null) {
                chestPos     = immutable;
                quality[0]  |= 8;
            }

            boolean isBed = block instanceof BedBlock || state.isBed(level(), immutable, null);
            if (!isBed) {
                try { isBed = state.getValue(MULTI_BLOCK_INDEX) == 1; }
                catch (IllegalArgumentException ignored) {}
            }
            if (isBed) {
                assignedBedPos = immutable;
                ensureBedOccupied(assignedBedPos);
                quality[0] |= 16;
            }

            if (level().getBrightness(LightLayer.BLOCK, immutable) > 0) {
                quality[0] |= 4;
            }
        });

        this.entityData.set(ROOM_QUALITY, quality[0]);
    }

    public boolean hasWalls()  { return (entityData.get(ROOM_QUALITY) & 1) != 0; }
    public boolean hasDoor()   { return (entityData.get(ROOM_QUALITY) & 2) != 0; }
    public boolean hasLight()  { return (entityData.get(ROOM_QUALITY) & 4) != 0; }
    public boolean hasBed() { return (entityData.get(ROOM_QUALITY) & 16) != 0; }
    public boolean hasChest() { return (entityData.get(ROOM_QUALITY) & 8) != 0; }
    public int getQualityScore() { return Integer.bitCount(entityData.get(ROOM_QUALITY)); }
    public boolean canMoveIn() { return hasWalls() && hasDoor() && hasLight() && hasBed() && hasChest(); }


    private boolean checkEnclosed() {
        if (area == null) this.area = getArea();
        Level level = getCommandSenderWorld();

        // Search space — 2 blocks larger than the interior on every side
        AABB searchAABB = area.inflate(4);
        BlockPos searchMin = BlockPos.containing(searchAABB.minX, searchAABB.minY, searchAABB.minZ);
        BlockPos searchMax = BlockPos.containing(searchAABB.maxX - 1, searchAABB.maxY - 1, searchAABB.maxZ - 1);

        // Interior bounds (the actual room — blocks the BFS must not reach)
        BlockPos intMin = BlockPos.containing(area.minX, area.minY, area.minZ);
        BlockPos intMax = BlockPos.containing(area.maxX - 1, area.maxY - 1, area.maxZ - 1);

        // BFS — seed from every air block on the search-box boundary
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue   = new ArrayDeque<>();

        for (int x = searchMin.getX(); x <= searchMax.getX(); x++) {
            for (int y = searchMin.getY(); y <= searchMax.getY(); y++) {
                for (int z = searchMin.getZ(); z <= searchMax.getZ(); z++) {
                    boolean onBoundary =
                            x == searchMin.getX() || x == searchMax.getX() ||
                                    y == searchMin.getY() || y == searchMax.getY() ||
                                    z == searchMin.getZ() || z == searchMax.getZ();
                    if (!onBoundary) continue;

                    BlockPos pos = new BlockPos(x, y, z);
                    if (level.getBlockState(pos).isAir() && visited.add(pos)) {
                        queue.add(pos);
                    }
                }
            }
        }

        while (!queue.isEmpty()) {
            BlockPos cur = queue.poll();

            for (Direction dir : Direction.values()) {
                BlockPos nb = cur.relative(dir);

                // Stay inside the search box
                if (nb.getX() < searchMin.getX() || nb.getX() > searchMax.getX() ||
                        nb.getY() < searchMin.getY() || nb.getY() > searchMax.getY() ||
                        nb.getZ() < searchMin.getZ() || nb.getZ() > searchMax.getZ()) continue;

                if (!visited.add(nb)) continue;
                if (!level.getBlockState(nb).isAir()) continue; // barrier — stop here

                // Outside air has reached the interior → room has a gap
                if (nb.getX() >= intMin.getX() && nb.getX() <= intMax.getX() &&
                        nb.getY() >= intMin.getY() && nb.getY() <= intMax.getY() &&
                        nb.getZ() >= intMin.getZ() && nb.getZ() <= intMax.getZ()) {
                    return false;
                }

                queue.add(nb);
            }
        }

        // Outside air could not reach the interior — room is enclosed
        return true;
    }
}