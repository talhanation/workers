package com.talhanation.workers.world;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerPotBlock;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BuildBlockParse {
    private final Item item;
    private final boolean wasParsed;
    // true  -> the parsed item should also be PLACED as its block (grass -> dirt).
    // false -> the parsed item is only the MATERIAL to consume; the original target
    //          block is still placed (oak_stairs consumes oak_planks but places stairs).
    private final boolean placeAsBase;

    // Caches the resolved base item per block so we don't recompute every time.
    // Items.AIR value = "no simpler base, use block's own item".
    // Concurrent because the BuildAreaScreen (client thread) and the BuilderWorkGoal
    // (server thread) both resolve materials at the same time in singleplayer.
    private static final Map<Block, Item> BASE_ITEM_CACHE = new ConcurrentHashMap<>();

    // Suffixes whose family collapses to <base>_planks (wooden building blocks).
    // Order matters: longer/more specific suffixes must come before shorter ones
    // (e.g. _fence_gate before _fence, _wall_sign before _sign).
    private static final String[] WOOD_SUFFIXES = {
            "_pressure_plate",
            "_trapdoor",
            "_fence_gate",
            "_wall_hanging_sign",
            "_wall_sign",
            "_hanging_sign",
            "_stairs",
            "_slab",
            "_fence",
            "_button",
            "_door",
            "_sign"
    };

    // Suffixes whose base is the full/plural block itself (stone-like families):
    // <base>, <base>s or <base>_block (stone_brick_stairs -> stone_bricks,
    // brick_wall -> bricks, quartz_stairs -> quartz_block, stone_button -> stone).
    private static final String[] GENERIC_SUFFIXES = {
            "_pressure_plate",
            "_stairs",
            "_slab",
            "_button",
            "_fence",
            "_wall"
    };

    private static final String CARPET_SUFFIX = "_carpet";

    public BuildBlockParse(Item item, boolean wasParsed) {
        this(item, wasParsed, wasParsed);
    }

    public BuildBlockParse(Item item, boolean wasParsed, boolean placeAsBase) {
        this.item = item;
        this.wasParsed = wasParsed;
        this.placeAsBase = placeAsBase;
    }

    public Item getItem() {
        return item;
    }

    public boolean wasParsed() {
        return wasParsed;
    }

    public boolean placeAsBase() {
        return placeAsBase;
    }

    public static BuildBlockParse parseBlock(Block block) {
        if (block == Blocks.GRASS_BLOCK
                || block == Blocks.MYCELIUM
                || block == Blocks.PODZOL
                || block == Blocks.ROOTED_DIRT
                || block == Blocks.FARMLAND) {
            return new BuildBlockParse(Item.BY_BLOCK.get(Blocks.DIRT), true);
        }
        else if (block instanceof FlowerPotBlock) {
            return new BuildBlockParse(Item.BY_BLOCK.get(Blocks.FLOWER_POT), true);
        }

        return new BuildBlockParse(Item.BY_BLOCK.get(block), false);
    }

    /**
     * Like {@link #parseBlock(Block)}, but additionally resolves the block to its
     * "base material" so the builder consumes the base resource instead of the
     * finished block (e.g. cobblestone_stairs -> cobblestone, oak_door -> oak_planks).
     * Resolution is name-based (see {@link #resolveByName(Block)}), so it works for
     * modded blocks that follow the vanilla naming scheme too. 1 block = 1 base item.
     * If no base is found, the block's own item is used as the fallback (same result
     * as {@link #parseBlock(Block)}).
     */
    public static BuildBlockParse parseBlock(Block block, Level level) {
        // Fast special-cases first (grass/dirt, flower pot) keep their existing behaviour.
        BuildBlockParse special = parseBlock(block);
        if (special.wasParsed()) {
            return special;
        }

        Item baseItem = resolveBaseItem(block);
        if (baseItem != null && baseItem != special.getItem()) {
            // Material-only substitution: consume the base item, still place the block.
            return new BuildBlockParse(baseItem, true, false);
        }
        return special;
    }

    /**
     * Finds the base crafting material for a block from its RESOURCE NAME, staying
     * inside the block's own namespace. This is deterministic and mod-safe: the
     * recipe scan could pick the wrong wood (e.g. AppleWood for oak_planks) when a
     * mod defines recipes via wood tags, but name-based resolution keeps oak -> oak.
     *
     * Examples: oak_stairs/oak_slab/oak_fence -> oak_planks; stone_brick_stairs ->
     * stone_bricks; cobblestone_wall -> cobblestone. Returns null if the name has no
     * known building suffix or the derived base block does not exist, in which case
     * the block's own item is used. Cached per block.
     */
    private static Item resolveBaseItem(Block block) {
        Item ownItem = Item.BY_BLOCK.get(block);
        if (ownItem == null || ownItem == Items.AIR) return null;

        Item cached = BASE_ITEM_CACHE.get(block);
        if (cached != null) {
            return cached == Items.AIR ? null : cached;
        }

        Item resolved = resolveByName(block);
        // Never resolve to the block's own item (no-op) and never to air.
        if (resolved == null || resolved == ownItem || resolved == Items.AIR) {
            resolved = null;
        }

        BASE_ITEM_CACHE.put(block, resolved == null ? Items.AIR : resolved);
        return resolved;
    }

    private static Item resolveByName(Block block) {
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
        if (key == null) return null;

        String namespace = key.getNamespace();
        String path = key.getPath();

        // Build candidate base names in priority order. Wood suffixes prefer
        // <base>_planks; generic (stone-like) suffixes prefer <base> then <base>s.
        // A block like stone_brick_stairs matches both a wood suffix (_stairs) and a
        // generic one, so we try _planks first and, if that block doesn't exist,
        // fall through to stone_bricks — hence we collect ALL candidates and return
        // the first that actually exists rather than committing to the first suffix.
        java.util.List<String> candidates = new java.util.ArrayList<>();

        for (String suffix : WOOD_SUFFIXES) {
            if (path.endsWith(suffix)) {
                String base = path.substring(0, path.length() - suffix.length());
                candidates.add(base + "_planks");
                break;
            }
        }

        for (String suffix : GENERIC_SUFFIXES) {
            if (path.endsWith(suffix)) {
                String base = path.substring(0, path.length() - suffix.length());
                candidates.add(base);            // stone_brick_stairs -> stone_brick (miss) ...
                candidates.add(base + "s");      // ... -> stone_bricks
                candidates.add(base + "_block"); // quartz_stairs -> quartz_block, purpur_slab -> purpur_block
                break;
            }
        }

        // Carpets collapse to their wool (white_carpet -> white_wool, moss_carpet -> moss_block).
        if (path.endsWith(CARPET_SUFFIX)) {
            String base = path.substring(0, path.length() - CARPET_SUFFIX.length());
            candidates.add(base + "_wool");
            candidates.add(base + "_block");
        }

        for (String candidate : candidates) {
            Item item = itemFromName(namespace, candidate);
            if (item != null) return item;
        }

        return null;
    }

    /**
     * Looks up a block by "namespace:path" and returns its item, or null if the
     * block does not exist or has no item.
     */
    private static Item itemFromName(String namespace, String path) {
        ResourceLocation id = new ResourceLocation(namespace, path);
        if (!BuiltInRegistries.BLOCK.containsKey(id)) return null;
        Block block = BuiltInRegistries.BLOCK.get(id);
        if (block == Blocks.AIR) return null;
        Item item = Item.BY_BLOCK.get(block);
        return (item == null || item == Items.AIR) ? null : item;
    }
}