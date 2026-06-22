package com.talhanation.workers.entities.workarea;

import com.talhanation.workers.client.gui.MiningAreaScreen;
import com.talhanation.workers.config.WorkersServerConfig;
import com.talhanation.workers.entities.MinerEntity;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
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

        // SPIRAL_STAIRCASE: square shaft centred on the entity (the central column),
        // odd width N clamped 5..15, free height. DOWN carves below, UP above.
        if (isSpiral()) {
            int n = getSpiralWidth();
            int radius = (n - 1) / 2;
            int h = Math.max(1, height) - 1;
            int yReach = (getMode() == MiningMode.SPIRAL_STAIRCASE_UP) ? h : -h;

            BlockPos center = this.getOnPos();
            BlockPos start = center.offset(-radius, 0, -radius);
            BlockPos end = center.offset(radius, yReach, radius);
            return new AABB(start, end);
        }

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

    public boolean isSpiral() {
        MiningMode mode = getMode();
        return mode == MiningMode.SPIRAL_STAIRCASE_DOWN || mode == MiningMode.SPIRAL_STAIRCASE_UP;
    }

    /** Spiral shaft edge length: the width clamped to an odd value between 5 and 15
     *  so there is always a single 1-block central column. */
    public int getSpiralWidth() {
        int n = getWidthSize() | 1; // round up to the next odd number
        return Mth.clamp(n, 5, 15);
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
        boolean spiral = isSpiral();

        BlockPos.betweenClosedStream(area).forEach(pos -> {
            BlockState state = level.getBlockState(pos);
            FluidState fluidState = level.getFluidState(pos);

            // SPIRAL: carve the shaft, keeping the central column and the rotating
            // tread arm. Same "only mine the air cells" approach as the stairs.
            if (spiral) {
                if (isSpiralAir(pos) && !state.isAir() && fluidState.isEmpty()
                        && !isAir(state) && !shouldIgnore(state)) {
                    stackToBreak.push(pos.immutable());
                }
                return;
            }

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

    /**
     * Spiral staircase profile test. Returns true if the position is an "air" cell
     * (should be mined out). Kept standing are the central column and, on each level,
     * one full radial arm (column → outer wall) that rotates 90° clockwise per level
     * (N→E→S→W). One block of height per quarter turn leaves three open blocks above
     * each tread (head room). Width (z) is coupled to the odd shaft width (x).
     */
    public boolean isSpiralAir(BlockPos pos) {
        if (area == null) area = this.getArea();

        BlockPos center = this.getOnPos();

        // Central column always stays.
        if (pos.getX() == center.getX() && pos.getZ() == center.getZ()) return false;

        // Step index along the climb: 0 at the entity's level, increasing as we climb.
        int step = (getMode() == MiningMode.SPIRAL_STAIRCASE_UP)
                ? pos.getY() - (int) area.minY
                : (int) area.maxY - pos.getY();

        int side = ((step % 4) + 4) % 4; // 0=N(-z) 1=E(+x) 2=S(+z) 3=W(-x)

        int dx = pos.getX() - center.getX();
        int dz = pos.getZ() - center.getZ();

        boolean onArm = switch (side) {
            case 0 -> dx == 0 && dz < 0;  // North arm
            case 1 -> dz == 0 && dx > 0;  // East arm
            case 2 -> dx == 0 && dz > 0;  // South arm
            default -> dz == 0 && dx < 0; // West arm
        };

        return !onArm;
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
        if (!getMineWallOres() || isStairs() || isSpiral()) return;

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

        // STAIRS / SPIRAL keep their carved profile — no floor closing or fluid filling.
        if (isStairs() || isSpiral()) return;

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
        STAIRS_UP(2),
        SPIRAL_STAIRCASE_DOWN(3),
        SPIRAL_STAIRCASE_UP(4);

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
