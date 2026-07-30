package com.talhanation.workers.world;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerPotBlock;

import java.util.HashMap;
import java.util.Map;

public class BuildBlockParse {
    private final Item item;
    private final boolean wasParsed;
    // true  -> the parsed item should also be PLACED as its block (grass -> dirt).
    // false -> the parsed item is only the MATERIAL to consume; the original target
    //          block is still placed (oak_stairs consumes oak_planks but places stairs).
    private final boolean placeAsBase;

    // Caches the resolved base item per block so we don't scan the recipe manager
    // every time. null value = "resolved to no simpler base, use block's own item".
    private static final Map<Block, Item> BASE_ITEM_CACHE = new HashMap<>();

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
     * "base material" so the builder consumes the cheapest ingredient instead of
     * the finished block (e.g. oak_stairs -> oak_planks). This is done through the
     * crafting recipe manager, so it works for modded blocks too, not just a hard
     * coded vanilla list. If no unambiguous base is found, the block's own item is
     * used as the fallback (same result as {@link #parseBlock(Block)}).
     */
    public static BuildBlockParse parseBlock(Block block, Level level) {
        // Fast special-cases first (grass/dirt, flower pot) keep their existing behaviour.
        BuildBlockParse special = parseBlock(block);
        if (special.wasParsed()) {
            return special;
        }

        Item baseItem = resolveBaseItem(block, level);
        if (baseItem != null && baseItem != special.getItem()) {
            // Material-only substitution: consume the base item, still place the block.
            return new BuildBlockParse(baseItem, true, false);
        }
        return special;
    }

    /**
     * Finds the base crafting material for a block by scanning crafting recipes
     * whose result is the block's item and that use a SINGLE ingredient item type
     * (typical for stairs/slabs/fences/walls/doors). Returns that ingredient's
     * item, or null if none/ambiguous. Cached per block.
     */
    private static Item resolveBaseItem(Block block, Level level) {
        if (level == null) return null;

        Item ownItem = Item.BY_BLOCK.get(block);
        if (ownItem == null || ownItem == net.minecraft.world.item.Items.AIR) return null;

        if (BASE_ITEM_CACHE.containsKey(block)) {
            return BASE_ITEM_CACHE.get(block);
        }

        Item resolved = null;
        try {
            for (net.minecraft.world.item.crafting.RecipeHolder<CraftingRecipe> holder
                    : level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
                CraftingRecipe recipe = holder.value();
                ItemStack result = recipe.getResultItem(level.registryAccess());
                if (result.isEmpty() || result.getItem() != ownItem) continue;

                Item single = singleIngredientItem(recipe);
                // Guard against self-referential/looping results (e.g. planks<->block).
                if (single != null && single != ownItem) {
                    resolved = single;
                    break;
                }
            }
        } catch (Exception ignored) {
            resolved = null;
        }

        BASE_ITEM_CACHE.put(block, resolved);
        return resolved;
    }

    /**
     * If every non-empty ingredient of the recipe resolves to the same single
     * item, returns that item; otherwise null (ambiguous / multi-material recipe).
     */
    private static Item singleIngredientItem(CraftingRecipe recipe) {
        Item found = null;
        for (Ingredient ingredient : recipe.getIngredients()) {
            if (ingredient.isEmpty()) continue;

            ItemStack[] matching = ingredient.getItems();
            if (matching.length == 0) return null;

            // Use the first matching item of the ingredient as its representative.
            Item candidate = matching[0].getItem();
            if (found == null) {
                found = candidate;
            }
            else if (found != candidate) {
                return null; // more than one distinct base material -> ambiguous
            }
        }
        return found;
    }
}
