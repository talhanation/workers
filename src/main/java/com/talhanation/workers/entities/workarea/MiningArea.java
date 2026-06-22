package com.talhanation.workers.entities.workarea;

import com.talhanation.workers.client.gui.MiningAreaScreen;
import com.talhanation.workers.config.WorkersServerConfig;
import com.talhanation.workers.entities.MinerEntity;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.Tags;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Stack;
import java.util.stream.Stream;

public class MiningArea extends AbstractWorkAreaEntity {
    public static final EntityDataAccessor<Integer> HEIGHT_OFFSET = SynchedEntityData.defineId(MiningArea.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Boolean> CLOSE_FLOOR = SynchedEntityData.defineId(MiningArea.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> CLOSE_FLUIDS = SynchedEntityData.defineId(MiningArea.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> MINE_WALL_ORES = SynchedEntityData.defineId(MiningArea.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Integer> MODE = SynchedEntityData.defineId(MiningArea.class, EntityDataSerializers.INT);
    public Stack<BlockPos> stackToPlace = new Stack<>();
    public Stack<BlockPos> stackToBreak = new Stack<>();

    public MiningArea(EntityType<?> type, Level level) {
        super(type, level);
    }
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(HEIGHT_OFFSET, 1);
        this.entityData.define(CLOSE_FLOOR, false);
        this.entityData.define(CLOSE_FLUIDS, true);
        this.entityData.define(MINE_WALL_ORES, true);
        this.entityData.define(MODE, MiningMode.CUSTOM.getIndex());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setCloseFloor(tag.getBoolean("closeFloor"));
        this.setCloseFluids(tag.getBoolean("closeFluids"));
        this.setMineWallOres(tag.getBoolean("mineWallOres"));
        this.setMode(tag.getInt("miningMode"));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("closeFloor", this.getCloseFloor());
        tag.putBoolean("closeFluids", this.getCloseFluids());
        tag.putBoolean("mineWallOres", this.getMineWallOres());
        tag.putInt("miningMode", this.getMode().getIndex());
    }

    @Override
    public void tick() {
        super.tick();
        if(this.isDone()) this.remove(RemovalReason.DISCARDED);
    }

    public Item getRenderItem()  {
        return Items.IRON_PICKAXE;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public Screen getScreen(Player player) {
        return new MiningAreaScreen(this, player);
    }
    @Override
    public AABB createArea() {
        Direction facing = getFacing();
        int width = getWidthSize() - 1;
        int depth = getDepthSize() - 1;
        int height = getHeightSize();

        // STAIRS: the entity sits at the near-top corner of the staircase profile.
        // STAIRS_DOWN carves the box below the entity, STAIRS_UP above it.
        if (isStairs()) {
            int yReach = (getMode() == MiningMode.STAIRS_UP) ? (height - 1) : -(height - 1);
            BlockPos start = this.getOnPos();
            BlockPos end;
            switch (facing) {
                case NORTH -> end = start.offset(width, yReach, -depth);
                case SOUTH -> end = start.offset(-width, yReach, depth);
                case WEST  -> end = start.offset(-width, yReach, -depth);
                default    -> end = start.offset(width, yReach, depth);//EAST
            }
            return new AABB(start, end);
        }

        BlockPos start = this.getOnPos().offset(0, this.getHeightOffset(), 0);
        BlockPos end;
        switch (facing) {
            case NORTH -> end = start.offset(width, height, -depth);
            case SOUTH -> end = start.offset(-width, height, depth);
            case WEST  -> end = start.offset(-width, height, -depth);
            default  -> end = start.offset(width, height, depth);//EAST
        }
        return new AABB(start, end);
    }

    public boolean isStairs() {
        MiningMode mode = getMode();
        return mode == MiningMode.STAIRS_DOWN || mode == MiningMode.STAIRS_UP;
    }

    public int getHeightOffset() {
        return this.entityData.get(HEIGHT_OFFSET);
    }
    public void setHeightOffset(int offset) {
        this.entityData.set(HEIGHT_OFFSET, offset);
        area = this.createArea();
    }

    public boolean getCloseFloor() {
        return this.entityData.get(CLOSE_FLOOR);
    }
    public void setCloseFloor(boolean close) {
        this.entityData.set(CLOSE_FLOOR, close);
    }

    public boolean getCloseFluids() {
        return this.entityData.get(CLOSE_FLUIDS);
    }
    public void setCloseFluids(boolean close) {
        this.entityData.set(CLOSE_FLUIDS, close);
    }

    public boolean getMineWallOres() {
        return this.entityData.get(MINE_WALL_ORES);
    }
    public void setMineWallOres(boolean mine) {
        this.entityData.set(MINE_WALL_ORES, mine);
    }

    public MiningMode getMode() {
        return MiningMode.fromIndex(this.entityData.get(MODE));
    }
    public void setMode(int index) {
        this.entityData.set(MODE, index);
        area = this.createArea();
    }


    public void scanBreakArea() {
        if (area == null) area = this.getArea();
        stackToBreak.clear();

        Level level = this.getCommandSenderWorld();
        boolean stairs = isStairs();

        BlockPos.betweenClosedStream(area).forEach(pos -> {
            BlockState state = level.getBlockState(pos);
            FluidState fluidState = level.getFluidState(pos);

            // STAIRS: only carve out the air cells of the staircase profile; the
            // step blocks (#) are left standing. Custom keeps its full-box behaviour.
            if (stairs) {
                if (isStairsAir(pos) && !state.isAir() && fluidState.isEmpty()
                        && !isAir(state) && !shouldIgnore(state)) {
                    stackToBreak.push(pos.immutable());
                }
                return;
            }

            if (pos.getY() != area.maxY && !state.isAir() && fluidState.isEmpty()) {
                if (!isAir(state) && !shouldIgnore(state)) {
                    stackToBreak.push(pos.immutable());
                }
            }
        });
    }

    /**
     * Stairs profile test. Returns true if the given position is an "air" cell of
     * the staircase (i.e. should be mined out). The staircase keeps a block at
     * (column i from the entity, row r from the top) when {@code r >= 2 && i < r};
     * everything else is carved away, producing a step every block along x with
     * two head-room rows kept open at the top. Width (z) is fixed.
     */
    public boolean isStairsAir(BlockPos pos) {
        if (area == null) area = this.getArea();

        BlockPos origin = this.getOnPos();

        // Row index along the climb: 0 at the entity's step, increasing towards the
        // far end of the staircase. STAIRS_DOWN measures downwards from the top,
        // STAIRS_UP upwards from the bottom — making UP the vertical mirror of DOWN.
        int r = (getMode() == MiningMode.STAIRS_UP)
                ? pos.getY() - (int) area.minY
                : (int) area.maxY - pos.getY();

        // Column along the staircase run. createArea() always lays the width (x size)
        // along the world X axis, so the run is measured on X from the entity outwards.
        int i = Math.abs(pos.getX() - origin.getX());

        boolean keepBlock = (r >= 2 && i < r);
        return !keepBlock;
    }

    public boolean shouldIgnore(BlockState state){
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if(id == null) return false;
        return WorkersServerConfig.MinerIgnore.get().contains(id.toString());
    }

    public void scanForOresOnWalls() {
        if (area == null) area = this.getArea();
        stackToBreak.clear();

        // Off when the "mine wall ores" toggle is disabled, and never on a staircase
        // (which is meant to stay a clean stepped tunnel).
        if (!getMineWallOres() || isStairs()) return;

        Level level = this.getCommandSenderWorld();

        getWallBlocks(area, 3).forEach(pos -> {
            BlockState state = level.getBlockState(pos);

            if (isOre(state) && hasAirNeighbor(pos, level)) {
                stackToBreak.push(pos.immutable());
            }
        });
    }

    public void scanFloorArea() {
        if (area == null) area = this.getArea();
        stackToPlace.clear();

        // STAIRS keeps its stepped profile — no floor closing or fluid filling.
        if (isStairs()) return;

        Level level = this.getCommandSenderWorld();

        boolean closeFloor = getCloseFloor();
        boolean closeFluids = getCloseFluids();
        if (!closeFloor && !closeFluids) return;

        getWallBlocks(area, 1).forEach(pos -> {
            BlockState state = level.getBlockState(pos);
            FluidState fluidState = level.getFluidState(pos);

            // Fluids touching the walls/floor are sealed when "close fluids" is on.
            if (closeFluids && !fluidState.isEmpty()) {
                stackToPlace.push(pos.immutable());
            }

            // Holes in the floor below the area are filled when "close floor" is on.
            if (pos.getY() == area.minY - 1) {
                if (closeFloor && state.isAir()) {
                    stackToPlace.push(pos.immutable());
                }
                else if (closeFluids && !fluidState.isEmpty()) {
                    stackToPlace.push(pos.immutable());
                }
            }
        });

        // Fluid sources inside the area are sealed when "close fluids" is on.
        if (closeFluids) {
            BlockPos.betweenClosedStream(area).forEach(pos -> {
                FluidState fluidState = level.getFluidState(pos);
                if (fluidState.isSource()) {
                    stackToPlace.push(pos.immutable());
                }
            });
        }
    }


    private Stream<BlockPos> getWallBlocks(AABB base, int inflate) {
        AABB expanded = base.inflate(inflate);

        return BlockPos.betweenClosedStream(expanded)
                .filter(pos -> !base.contains(Vec3.atCenterOf(pos)));
    }

    public boolean isWorkerPerfectCandidate(MinerEntity miner) {
        if (miner.getMatchingItem(stack -> stack.getItem() instanceof PickaxeItem) == ItemStack.EMPTY) {
            return false;
        }

        return true;
    }

    public boolean hasAirNeighbor(BlockPos pos, Level level){
        for(Direction direction: Direction.values()){
            BlockState state = level.getBlockState(pos.relative(direction,1));
            if(isAir(state)) return true;
        }
        return false;
    }

    public boolean isOre(BlockState state){
        return state.is(Tags.Blocks.ORES);
    }

    public boolean isAir(BlockState state){
        return state.isAir() || state.is(Blocks.AIR) || state.is(Blocks.CAVE_AIR);
    }

    public enum MiningMode {
        CUSTOM(0),
        STAIRS_DOWN(1),
        STAIRS_UP(2);

        private final int index;
        MiningMode(int index){
            this.index = index;
        }
        public int getIndex(){
            return this.index;
        }

        public String getTranslationKey(){
            return "gui.workers.mining.mode." + this.name().toLowerCase();
        }

        public static MiningMode fromIndex(int index) {
            for (MiningMode messengerState : MiningMode.values()) {
                if (messengerState.getIndex() == index) {
                    return messengerState;
                }
            }
            throw new IllegalArgumentException("Invalid State index: " + index);
        }


    }
}
