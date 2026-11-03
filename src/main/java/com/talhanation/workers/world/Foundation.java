package com.talhanation.workers.world;

import com.talhanation.workers.entities.workarea.BuildArea;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Foundation mode of a {@link BuildArea}: levels the ground of the footprint so a building fits on uneven terrain.
 * <ul>
 *     <li><b>Cut:</b> blocks in the footprint above the bottom layer are removed, up to the terrain top
 *     (at most {@code maxCutAbove} blocks above the structure).</li>
 *     <li><b>Fill:</b> air and plants under the bottom layer are filled down to the ground
 *     (at most {@code maxDepth} blocks) with a material that fits the ground of that column.</li>
 * </ul>
 * Fluids are never cut or filled, so docks and fishing huts keep their water.
 * Everything here is computed once when the build starts, never per tick.
 */
public final class Foundation {

    private Foundation() {
    }

    /**
     * Blocks to remove before building. Only columns inside the footprint of the structure,
     * from one above the bottom layer up to the terrain top. Blocks that already match the structure stay.
     *
     * @param structure world position -> state of every block of the (rotated) structure
     */
    public static List<BlockPos> computeCut(Level level, Map<BlockPos, BlockState> structure, int maxCutAbove) {
        List<BlockPos> cut = new ArrayList<>();
        if (structure.isEmpty()) return cut;

        Bounds b = Bounds.of(structure.keySet());
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = b.minX; x <= b.maxX; x++) {
            for (int z = b.minZ; z <= b.maxZ; z++) {
                int terrainTop = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                int top = Math.min(Math.max(b.maxY, terrainTop), b.maxY + maxCutAbove);

                for (int y = b.minY + 1; y <= top; y++) {
                    pos.set(x, y, z);
                    BlockState levelState = level.getBlockState(pos);
                    if (levelState.isAir() || !isCuttable(level, pos, levelState)) continue;

                    BlockState wanted = structure.get(pos);
                    if (wanted != null && (levelState.equals(wanted) || BuildArea.canDirectlyReplace(levelState, wanted))) continue;

                    cut.add(pos.immutable());
                }
            }
        }
        return cut;
    }

    /**
     * Blocks to place under the bottom layer of the structure. Only columns that have a block in the bottom layer
     * (gardens and water of the template stay as they are). Filling stops at the first solid block or fluid.
     *
     * @param override fill block chosen by the player, null = pick from the ground of each column
     */
    public static List<BuildBlock> computeFill(Level level, Map<BlockPos, BlockState> structure, @Nullable BlockState override, int maxDepth) {
        List<BuildBlock> fill = new ArrayList<>();
        if (structure.isEmpty() || maxDepth <= 0) return fill;

        int bottomY = Integer.MAX_VALUE;
        for (BlockPos p : structure.keySet()) bottomY = Math.min(bottomY, p.getY());

        // columns of the bottom layer
        List<BlockPos> columns = new ArrayList<>();
        for (BlockPos p : structure.keySet()) {
            if (p.getY() == bottomY) columns.add(p);
        }

        // 1. find the ground and the empty blocks of every column
        Map<BlockPos, List<BlockPos>> holes = new LinkedHashMap<>();
        Map<BlockPos, BlockState> ground = new HashMap<>();
        Map<BlockState, Integer> materialCount = new HashMap<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (BlockPos column : columns) {
            List<BlockPos> empty = new ArrayList<>();
            for (int d = 1; d <= maxDepth; d++) {
                pos.set(column.getX(), bottomY - d, column.getZ());
                BlockState state = level.getBlockState(pos);
                if (isFillable(state)) {
                    empty.add(pos.immutable());
                    continue;
                }
                // solid ground, or a fluid we do not fill
                if (state.getFluidState().isEmpty()) {
                    ground.put(column, state);
                    materialCount.merge(materialFor(state), 1, Integer::sum);
                }
                break;
            }
            if (!empty.isEmpty()) holes.put(column, empty);
        }
        if (holes.isEmpty()) return fill;

        // 2. material per column: the player's choice, or what the ground below is made of
        BlockState dominant = Blocks.DIRT.defaultBlockState();
        int best = 0;
        for (Map.Entry<BlockState, Integer> entry : materialCount.entrySet()) {
            if (entry.getValue() > best) {
                best = entry.getValue();
                dominant = entry.getKey();
            }
        }

        for (Map.Entry<BlockPos, List<BlockPos>> entry : holes.entrySet()) {
            BlockState groundState = ground.get(entry.getKey());
            boolean hasGround = groundState != null;
            BlockState material;
            if (override != null) material = override;
            else material = hasGround ? materialFor(groundState) : dominant;

            // a pillar that reaches no ground must not fall down
            if (!hasGround) material = nonFalling(material);

            for (BlockPos hole : entry.getValue()) fill.add(new BuildBlock(hole, material));
        }
        return fill;
    }

    /** Fill material that fits the ground block: dirt for soil, sand for sand, cobblestone for stone. */
    public static BlockState materialFor(BlockState groundState) {
        if (groundState.is(Blocks.RED_SAND)) return Blocks.RED_SAND.defaultBlockState();
        if (groundState.is(BlockTags.SAND)) return Blocks.SAND.defaultBlockState();
        if (groundState.is(Blocks.SANDSTONE) || groundState.is(Blocks.RED_SANDSTONE)) return groundState.getBlock().defaultBlockState();
        if (groundState.is(BlockTags.TERRACOTTA)) return groundState.getBlock().defaultBlockState();
        if (groundState.is(Blocks.GRAVEL)
                || groundState.is(BlockTags.BASE_STONE_OVERWORLD)
                || groundState.is(Blocks.COBBLESTONE)
                || groundState.is(Blocks.MOSSY_COBBLESTONE)) return Blocks.COBBLESTONE.defaultBlockState();
        return Blocks.DIRT.defaultBlockState();
    }

    private static BlockState nonFalling(BlockState state) {
        if (state.is(Blocks.SAND)) return Blocks.SANDSTONE.defaultBlockState();
        if (state.is(Blocks.RED_SAND)) return Blocks.RED_SANDSTONE.defaultBlockState();
        if (state.getBlock() instanceof FallingBlock) return Blocks.COBBLESTONE.defaultBlockState();
        return state;
    }

    /** Air, grass, flowers, snow layers: everything that a block may replace, but no fluids. */
    private static boolean isFillable(BlockState state) {
        if (state.isAir()) return true;
        return state.canBeReplaced() && state.getFluidState().isEmpty();
    }

    private static boolean isCuttable(Level level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof LiquidBlock) return false;
        return state.getDestroySpeed(level, pos) >= 0;
    }

    private record Bounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        static Bounds of(Collection<BlockPos> positions) {
            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
            for (BlockPos p : positions) {
                minX = Math.min(minX, p.getX());
                minY = Math.min(minY, p.getY());
                minZ = Math.min(minZ, p.getZ());
                maxX = Math.max(maxX, p.getX());
                maxY = Math.max(maxY, p.getY());
                maxZ = Math.max(maxZ, p.getZ());
            }
            return new Bounds(minX, minY, minZ, maxX, maxY, maxZ);
        }
    }
}
