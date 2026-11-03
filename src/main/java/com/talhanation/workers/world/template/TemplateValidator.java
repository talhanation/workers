package com.talhanation.workers.world.template;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

/**
 * Detects the type of a scan and checks its required content. Works on the NBT only, no world access,
 * so it runs on the server for files the server never placed.
 */
public final class TemplateValidator {

    /** Every bed needs this much living space: 3x3 floor cells with 2 blocks of headroom, under a roof. */
    public static final int FLOOR_CELLS_PER_BED = 9;
    public static final int CHUNK = 16;

    private TemplateValidator() {
    }

    /** Everything the validation needs, read once from the NBT. */
    public static final class Content {
        public int width, height, depth;
        public int beds, doors, chests, craftingTables, furnaces;
        public int floorCells;
        public final Set<String> workAreas = new HashSet<>();
    }

    public static Content read(CompoundTag root) {
        Content c = new Content();
        c.width = root.getInt("width");
        c.height = root.getInt("height");
        c.depth = root.getInt("depth");

        if (root.contains("entities", Tag.TAG_LIST)) {
            for (Tag t : root.getList("entities", Tag.TAG_COMPOUND)) {
                c.workAreas.add(((CompoundTag) t).getString("entity_type"));
            }
        }

        int w = Math.max(1, c.width), h = Math.max(1, c.height), d = Math.max(1, c.depth);
        // occupied = blocks movement (beds do not count, the space around a bed is living space)
        boolean[] occupied = new boolean[w * h * d];
        boolean[] bed = new boolean[w * h * d];

        ListTag blocks = root.getList("blocks", Tag.TAG_COMPOUND);
        for (Tag t : blocks) {
            CompoundTag b = (CompoundTag) t;
            int x = b.getInt("x"), y = b.getInt("y"), z = b.getInt("z");
            BlockState state = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), b.getCompound("state"));

            if (state.getBlock() instanceof BedBlock) {
                if (state.getValue(BlockStateProperties.BED_PART) == BedPart.FOOT) c.beds++;
            }
            else if (state.getBlock() instanceof DoorBlock) {
                if (state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER) c.doors++;
            }
            else if (state.getBlock() instanceof ChestBlock || state.getBlock() instanceof BarrelBlock) c.chests++;
            else if (state.is(Blocks.CRAFTING_TABLE)) c.craftingTables++;
            else if (state.is(Blocks.FURNACE) || state.is(Blocks.BLAST_FURNACE) || state.is(Blocks.SMOKER)) c.furnaces++;

            if (x < 0 || y < 0 || z < 0 || x >= w || y >= h || z >= d) continue;
            int i = index(x, y, z, w, h);
            if (state.getBlock() instanceof BedBlock) bed[i] = true;
            else if (blocksMotion(state)) occupied[i] = true;
        }

        c.floorCells = countFloorCells(occupied, bed, w, h, d);
        return c;
    }

    /**
     * Type of a scan: the file name decides if it has a keyword, otherwise the work area,
     * otherwise beds make it a villager house. Null = not a governor building.
     */
    @Nullable
    public static BuildingType detectType(String fileName, Content c) {
        BuildingType byName = BuildingType.fromFileName(fileName);
        if (byName != null) return byName;

        // a building with several work areas counts as its main one, the home area comes last
        BuildingType[] order = {BuildingType.WORKSHOP, BuildingType.MARKET, BuildingType.STORAGE, BuildingType.KITCHEN,
                BuildingType.FARM, BuildingType.LUMBER, BuildingType.FISHER, BuildingType.ANIMAL_PEN, BuildingType.MINE,
                BuildingType.WORKER_HOUSE};
        for (BuildingType type : order) {
            if (c.workAreas.contains(type.getWorkArea())) return type;
        }
        if (c.beds > 0) return BuildingType.VILLAGER_HOUSE;
        return null;
    }

    /** Translation key why the template does not fit its type, null = valid. */
    @Nullable
    public static String validate(BuildingType type, Content c) {
        if (type.getWorkArea() != null && !c.workAreas.contains(type.getWorkArea())) return reason("no_work_area");

        switch (type) {
            case WORKER_HOUSE -> {
                if (c.beds <= 0) return reason("no_bed");
                if (c.doors <= 0) return reason("no_door");
                if (c.chests <= 0) return reason("no_chest");
                if (c.floorCells < c.beds * FLOOR_CELLS_PER_BED) return reason("too_little_space");
            }
            case VILLAGER_HOUSE -> {
                if (c.workAreas.contains(BuildingType.WORKER_HOUSE.getWorkArea())) return reason("has_home_area");
                if (c.beds <= 0) return reason("no_bed");
                if (c.doors <= 0) return reason("no_door");
                if (c.floorCells < c.beds * FLOOR_CELLS_PER_BED) return reason("too_little_space");
            }
            case WORKSHOP -> {
                if (c.craftingTables <= 0) return reason("no_crafting_table");
                if (c.furnaces <= 0) return reason("no_furnace");
            }
            case ROAD -> {
                if (c.width != 3 || c.depth != 3) return reason("road_size");
                if (c.height > 3) return reason("road_height");
            }
            case WALL_BASIC -> {
                if (c.width != 1 || c.depth != 1) return reason("wall_basic_size");
                if (c.height < 3 || c.height > 4) return reason("wall_basic_height");
            }
            case WALL_STRAIGHT, GATE_BASIC, GATE_EXTENDED -> {
                if (Math.max(c.width, c.depth) != CHUNK || Math.min(c.width, c.depth) > CHUNK) return reason("chunk_size");
            }
            case WALL_CORNER -> {
                if (c.width != CHUNK || c.depth != CHUNK) return reason("corner_size");
            }
            default -> {
            }
        }
        return null;
    }

    private static String reason(String key) {
        return "gui.workers.template.invalid." + key;
    }

    /**
     * Floor cells with 2 blocks of headroom under a roof: free at y and y+1, a floor (or bed) below,
     * and something that blocks movement above the headroom in the same column.
     */
    private static int countFloorCells(boolean[] occupied, boolean[] bed, int w, int h, int d) {
        int count = 0;
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                // highest occupied block of the column = roof
                int roof = -1;
                for (int y = h - 1; y >= 0; y--) {
                    if (occupied[index(x, y, z, w, h)]) {
                        roof = y;
                        break;
                    }
                }
                if (roof < 3) continue;

                for (int y = 1; y + 1 < roof; y++) {
                    int here = index(x, y, z, w, h);
                    int above = index(x, y + 1, z, w, h);
                    int below = index(x, y - 1, z, w, h);
                    if (occupied[here] || occupied[above]) continue;
                    boolean floor = occupied[below] || bed[here];
                    if (floor && !bed[below]) count++;
                }
            }
        }
        return count;
    }

    @SuppressWarnings("deprecation")
    private static boolean blocksMotion(BlockState state) {
        return state.blocksMotion();
    }

    private static int index(int x, int y, int z, int w, int h) {
        return (z * h + y) * w + x;
    }
}
