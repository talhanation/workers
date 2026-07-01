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
    public static final EntityDataAccessor<ItemStack> FILL_ITEM = SynchedEntityData.defineId(MiningArea.class, EntityDataSerializers.ITEM_STACK);
    public static final EntityDataAccessor<Integer> MODE = SynchedEntityData.defineId(MiningArea.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Boolean> KEEP_ON = SynchedEntityData.defineId(MiningArea.class, EntityDataSerializers.BOOLEAN);
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
        this.entityData.define(FILL_ITEM, new ItemStack(Blocks.COBBLESTONE));
        this.entityData.define(MODE, MiningMode.CUSTOM.getIndex());
        this.entityData.define(KEEP_ON, false);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setCloseFloor(tag.getBoolean("closeFloor"));
        this.setCloseFluids(tag.getBoolean("closeFluids"));
        this.setMineWallOres(tag.getBoolean("mineWallOres"));
        if (tag.contains("fillItem")) {
            this.setFillItem(ItemStack.of(tag.getCompound("fillItem")));
        }
        this.setMode(tag.getInt("miningMode"));
        this.setKeepOn(tag.getBoolean("keepOn"));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("closeFloor", this.getCloseFloor());
        tag.putBoolean("closeFluids", this.getCloseFluids());
        tag.putBoolean("mineWallOres", this.getMineWallOres());
        tag.put("fillItem", this.getFillItem().save(new CompoundTag()));
        tag.putInt("miningMode", this.getMode().getIndex());
        tag.putBoolean("keepOn", this.getKeepOn());
    }

    @Override
    public void tick() {
        super.tick();
        if(this.isDone() && !this.getKeepOn()) this.remove(RemovalReason.DISCARDED);
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

        // Unified, facing-relative axes for BOTH custom and stairs so switching mode
        // never rotates the area: width (length) runs along the facing direction and
        // depth (breadth) runs to the right of it. fwd/side are unit offsets per facing.
        int fwdX, fwdZ, sideX, sideZ;
        switch (facing) {
            case NORTH -> { fwdX = 0;  fwdZ = -1; sideX = +1; sideZ = 0;  }
            case SOUTH -> { fwdX = 0;  fwdZ = +1; sideX = -1; sideZ = 0;  }
            case WEST  -> { fwdX = -1; fwdZ = 0;  sideX = 0;  sideZ = -1; }
            default    -> { fwdX = +1; fwdZ = 0;  sideX = 0;  sideZ = +1; }//EAST
        }

        if (isStairs()) {
            int yReach = (getMode() == MiningMode.STAIRS_UP) ? (height - 1) : -(height - 1);
            BlockPos start = (getMode() == MiningMode.STAIRS_UP) ? getOnPos().above() : getOnPos().above(3);
            BlockPos end = start.offset(fwdX * width + sideX * depth, yReach, fwdZ * width + sideZ * depth);
            return new AABB(start, end);
        }

        BlockPos start = this.getOnPos().offset(0, this.getHeightOffset(), 0);
        BlockPos end = start.offset(fwdX * width + sideX * depth, height, fwdZ * width + sideZ * depth);
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

    public ItemStack getFillItem() {
        ItemStack stack = this.entityData.get(FILL_ITEM);
        // Guard against an empty/invalid selection so closing always has a block.
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) {
            return new ItemStack(Blocks.COBBLESTONE);
        }
        return stack;
    }
    public void setFillItem(ItemStack stack) {
        this.entityData.set(FILL_ITEM, stack.isEmpty() ? new ItemStack(Blocks.COBBLESTONE) : stack);
    }

    public MiningMode getMode() {
        return MiningMode.fromIndex(this.entityData.get(MODE));
    }
    public void setMode(int index) {
        this.entityData.set(MODE, index);
    }

    public boolean getKeepOn() {
        return this.entityData.get(KEEP_ON);
    }
    public void setKeepOn(boolean keepOn) {
        this.entityData.set(KEEP_ON, keepOn);
    }


    /**
     * Drops any cached work plan so the next scan rebuilds from the CURRENT
     * geometry. Must be called whenever the area definition changes (size,
     * depth/width, height offset, facing/rotation, mode); otherwise the miner
     * keeps consuming the old stairs/box layout — even across goal restarts,
     * because PREPARE_MINING only rescans when the break stack is empty.
     */
    public void resetWork() {
        this.stackToBreak.clear();
        this.stackToPlace.clear();
        this.setDone(false);
        this.setTime(0);
    }

    public void scanBreakArea() {
        // Always recompute: facing/size/mode/position can all change at runtime,
        // and a cached box would leave stairs (and custom) scanning the old shape.
        AABB area = this.getArea();
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
        // Always recompute: facing/size/mode/position can all change at runtime,
        // and a cached box would leave stairs (and custom) scanning the old shape.
        AABB area = this.getArea();

        BlockPos origin = this.getOnPos();

        // Row index measured from the top of the box for BOTH modes. DOWN has its
        // entrance at the top and digs down; UP has its entrance at the bottom and
        // digs up, but the carved profile is identical — only the box grows the
        // other way (see createArea) and the entity sits at the opposite corner.
        int r = (int) area.maxY - pos.getY();

        // Column along the staircase run. The run follows the facing direction:
        // X for EAST/WEST, Z for NORTH/SOUTH (matching createArea's axis layout).
        Direction facing = getFacing();
        int i = (facing == Direction.NORTH || facing == Direction.SOUTH)
                ? Math.abs(pos.getZ() - origin.getZ())
                : Math.abs(pos.getX() - origin.getX());

        // STAIRS_UP enters at the bottom of the run, so mirror the distance along the
        // run: the entity stands at the foot (in air) and the steps rise away from it.
        if (getMode() == MiningMode.STAIRS_UP) {
            i = (getWidthSize() - 1) - i;
        }

        // Two diagonals leave stone standing: the rising floor (i < r-1) and the
        // ceiling (i >= r+4). Between them is a constant 4-block tall walkable
        // corridor — the floor-to-ceiling clearance the staircase keeps open.
        boolean keepBlock = (i < (r - 1)) || (i >= (r + 4));
        return !keepBlock;
    }

    public boolean shouldIgnore(BlockState state){
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if(id == null) return false;
        return WorkersServerConfig.MinerIgnore.get().contains(id.toString());
    }

    public void scanForOresOnWalls() {
        // Always recompute: facing/size/mode/position can all change at runtime,
        // and a cached box would leave stairs (and custom) scanning the old shape.
        AABB area = this.getArea();
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
        // Always recompute: facing/size/mode/position can all change at runtime,
        // and a cached box would leave stairs (and custom) scanning the old shape.
        AABB area = this.getArea();
        stackToPlace.clear();

        Level level = this.getCommandSenderWorld();

        // STAIRS: when "close floor" is on, fill only the actual STAIR TREADS that
        // are missing in the world (e.g. where the staircase crosses a cave). A tread
        // is a profile-stone cell with profile-air directly above it (the walkable
        // surface). The ceiling and the deeper stone mass are left alone, and
        // profile-air cells are never filled — so this stays disjoint from the
        // carving in scanBreakArea and no miner places what another mines away.
        if (isStairs()) {
            if (!getCloseFloor()) return;

            BlockPos.betweenClosedStream(area).forEach(pos -> {
                if (isStairsAir(pos)) return;          // profile wants air → never fill
                if (!isStairsAir(pos.above())) return; // not a tread (mass/ceiling) → skip

                BlockState state = level.getBlockState(pos);
                FluidState fluidState = level.getFluidState(pos);
                if (state.isAir() && fluidState.isEmpty()) {
                    stackToPlace.push(pos.immutable());
                }
            });
            return;
        }

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

    @Override
    public void setDone(boolean b) {
        if(this.getKeepOn()) return;

        super.setDone(b);
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