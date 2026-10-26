package com.talhanation.workers.world;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.crafting.IShapedRecipe;

import javax.annotation.Nullable;
import java.util.*;

/**
 * One entry of the workshop order list. The recipe is set up by the player in a 3x3 grid (crafting)
 * or a single slot (smelting); the grid holds ghost items with count 1.
 * Orders live on the {@link com.talhanation.workers.entities.workarea.WorkshopArea}, not on the craftsman.
 */
public class CraftOrder {

    public static final int GRID_SIZE = 9;
    public static final int MIN_PRIORITY = 1;
    public static final int MAX_PRIORITY = 9;
    public static final int MAX_AMOUNT = 9999;

    public UUID uuid;
    public final ItemStack[] grid = new ItemStack[GRID_SIZE];
    public OrderType type;
    @Nullable public ResourceLocation recipeId;
    public ItemStack result;
    public boolean allowVariants;
    public int amount;
    public int produced;
    public Mode mode;
    public int priority;
    public boolean enabled;
    public Source source;
    @Nullable public String sourceKey;

    // Runtime state, synced to the client for the order list
    public Status status;
    /** STOCK: how many of the result are in the workshop chests (last check). */
    public int stock;
    public final List<ItemStack> missing = new ArrayList<>();

    public CraftOrder() {
        this(UUID.randomUUID());
    }

    private CraftOrder(UUID uuid) {
        this.uuid = uuid;
        Arrays.fill(grid, ItemStack.EMPTY);
        this.type = OrderType.CRAFTING;
        this.recipeId = null;
        this.result = ItemStack.EMPTY;
        this.allowVariants = false;
        this.amount = 16;
        this.produced = 0;
        this.mode = Mode.STOCK;
        this.priority = 1;
        this.enabled = true;
        this.source = Source.PLAYER;
        this.sourceKey = null;
        this.status = Status.OK;
    }

    // ── State helpers ─────────────────────────────────────────────────────────

    /** Only ONCE orders are ever finished, STOCK orders refill whenever the stock drops. */
    public boolean isTargetReached() {
        return mode == Mode.ONCE && amount > 0 && produced >= amount;
    }

    /** How many items are still wanted: ONCE counts the produced items, STOCK the items in the chests. */
    public int getRemaining() {
        if (mode == Mode.STOCK) return Math.max(0, amount - stock);
        return Math.max(0, amount - produced);
    }

    public ItemStack getSmeltInput() {
        return grid[0];
    }

    public boolean isGovernorOrder() {
        return source == Source.GOVERNOR;
    }

    public boolean hasGrid() {
        for (ItemStack stack : grid) {
            if (!stack.isEmpty()) return true;
        }
        return false;
    }

    public void clampValues() {
        this.amount = Math.max(1, Math.min(MAX_AMOUNT, amount));
        this.priority = Math.max(MIN_PRIORITY, Math.min(MAX_PRIORITY, priority));
        this.produced = Math.max(0, produced);
        for (int i = 0; i < GRID_SIZE; i++) {
            if (grid[i] == null) grid[i] = ItemStack.EMPTY;
            else if (!grid[i].isEmpty() && grid[i].getCount() != 1) grid[i] = grid[i].copyWithCount(1);
        }
        if (type == OrderType.SMELTING) {
            for (int i = 1; i < GRID_SIZE; i++) grid[i] = ItemStack.EMPTY;
        }
    }

    public CraftOrder copy() {
        return fromNbt(this.toNbt());
    }

    // ── Recipe helpers ────────────────────────────────────────────────────────

    /** A crafting container that is not bound to any open menu. */
    public static CraftingContainer createCraftingContainer() {
        AbstractContainerMenu dummy = new AbstractContainerMenu(null, -1) {
            @Override
            public ItemStack quickMoveStack(Player player, int index) {
                return ItemStack.EMPTY;
            }

            @Override
            public boolean stillValid(Player player) {
                return false;
            }
        };
        return new TransientCraftingContainer(dummy, 3, 3);
    }

    public static CraftingContainer createCraftingContainer(ItemStack[] grid) {
        CraftingContainer container = createCraftingContainer();
        for (int i = 0; i < GRID_SIZE; i++) {
            container.setItem(i, grid[i] == null ? ItemStack.EMPTY : grid[i].copy());
        }
        return container;
    }

    public static Optional<CraftingRecipe> findCraftingRecipe(Level level, ItemStack[] grid) {
        CraftingContainer container = createCraftingContainer(grid);
        if (container.isEmpty()) return Optional.empty();
        return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, container, level)
                .filter(recipe -> !recipe.isSpecial());
    }

    public static Optional<SmeltingRecipe> findSmeltingRecipe(Level level, ItemStack input) {
        if (input == null || input.isEmpty()) return Optional.empty();
        return level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SimpleContainer(input.copyWithCount(1)), level);
    }

    /** Result of the current grid, used for the live preview in the edit screen. */
    public static ItemStack previewResult(Level level, OrderType type, ItemStack[] grid) {
        RegistryAccess access = level.registryAccess();
        if (type == OrderType.SMELTING) {
            return findSmeltingRecipe(level, grid[0]).map(r -> r.getResultItem(access).copy()).orElse(ItemStack.EMPTY);
        }
        CraftingContainer container = createCraftingContainer(grid);
        return findCraftingRecipe(level, grid).map(r -> r.assemble(container, access)).orElse(ItemStack.EMPTY);
    }

    /**
     * Looks up the recipe of the grid and stores its id and result.
     * Returns false if the grid has no valid (non-special) recipe.
     */
    public boolean resolveRecipe(Level level) {
        RegistryAccess access = level.registryAccess();
        if (type == OrderType.SMELTING) {
            Optional<SmeltingRecipe> recipe = findSmeltingRecipe(level, grid[0]);
            if (recipe.isEmpty()) return false;
            this.recipeId = recipe.get().getId();
            this.result = recipe.get().getResultItem(access).copy();
        }
        else {
            Optional<CraftingRecipe> recipe = findCraftingRecipe(level, grid);
            if (recipe.isEmpty()) return false;
            this.recipeId = recipe.get().getId();
            this.result = recipe.get().assemble(createCraftingContainer(grid), access);
        }
        return !result.isEmpty();
    }

    @Nullable
    public Recipe<?> getRecipe(Level level) {
        if (recipeId == null) return null;
        Recipe<?> recipe = level.getRecipeManager().byKey(recipeId).orElse(null);
        if (recipe == null) return null;
        if (type == OrderType.SMELTING) return recipe instanceof SmeltingRecipe ? recipe : null;
        return recipe instanceof CraftingRecipe ? recipe : null;
    }

    /**
     * One matcher per grid slot (null for empty slots). Without variants every slot accepts only its ghost item,
     * with variants every slot accepts what the recipe ingredient of that slot accepts.
     */
    public SlotMatcher[] buildSlotMatchers(Level level) {
        SlotMatcher[] matchers = new SlotMatcher[GRID_SIZE];
        for (int i = 0; i < GRID_SIZE; i++) {
            if (!grid[i].isEmpty()) matchers[i] = SlotMatcher.exact(grid[i]);
        }
        if (!allowVariants) return matchers;

        Recipe<?> recipe = getRecipe(level);
        if (recipe == null) return matchers;

        Ingredient[] ingredients = mapIngredients(recipe);
        for (int i = 0; i < GRID_SIZE; i++) {
            if (matchers[i] != null && ingredients[i] != null && !ingredients[i].isEmpty()) {
                matchers[i] = SlotMatcher.ingredient(ingredients[i], grid[i]);
            }
        }
        return matchers;
    }

    /** Maps the recipe ingredients onto the grid slots. Unmapped slots stay null. */
    private Ingredient[] mapIngredients(Recipe<?> recipe) {
        Ingredient[] mapped = new Ingredient[GRID_SIZE];
        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        if (ingredients.isEmpty()) return mapped;

        if (type == OrderType.SMELTING) {
            mapped[0] = ingredients.get(0);
            return mapped;
        }

        if (recipe instanceof IShapedRecipe<?> shaped) {
            int w = shaped.getRecipeWidth();
            int h = shaped.getRecipeHeight();
            int minRow = 3, minCol = 3;
            for (int i = 0; i < GRID_SIZE; i++) {
                if (grid[i].isEmpty()) continue;
                minRow = Math.min(minRow, i / 3);
                minCol = Math.min(minCol, i % 3);
            }
            for (boolean mirrored : new boolean[]{false, true}) {
                Ingredient[] attempt = new Ingredient[GRID_SIZE];
                boolean fits = true;
                for (int i = 0; i < GRID_SIZE && fits; i++) {
                    if (grid[i].isEmpty()) continue;
                    int row = i / 3 - minRow;
                    int col = i % 3 - minCol;
                    if (row >= h || col >= w) { fits = false; break; }
                    int index = row * w + (mirrored ? w - 1 - col : col);
                    if (index < 0 || index >= ingredients.size() || !ingredients.get(index).test(grid[i])) { fits = false; break; }
                    attempt[i] = ingredients.get(index);
                }
                if (fits) return attempt;
            }
            return mapped;
        }

        // shapeless: assign every grid item to the first free ingredient that accepts it
        boolean[] used = new boolean[ingredients.size()];
        for (int i = 0; i < GRID_SIZE; i++) {
            if (grid[i].isEmpty()) continue;
            for (int j = 0; j < ingredients.size(); j++) {
                if (!used[j] && ingredients.get(j).test(grid[i])) {
                    used[j] = true;
                    mapped[i] = ingredients.get(j);
                    break;
                }
            }
        }
        return mapped;
    }

    /**
     * Builds an order from a recipe (e.g. for the Governor, who only knows the item he wants).
     * The grid takes the first item of every ingredient and variants are allowed.
     */
    public static CraftOrder fromRecipe(Recipe<?> recipe, RegistryAccess access) {
        CraftOrder order = new CraftOrder();
        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        order.allowVariants = true;
        order.recipeId = recipe.getId();
        order.result = recipe.getResultItem(access).copy();

        if (recipe instanceof SmeltingRecipe) {
            order.type = OrderType.SMELTING;
            if (!ingredients.isEmpty()) order.grid[0] = firstItem(ingredients.get(0));
            return order;
        }

        order.type = OrderType.CRAFTING;
        if (recipe instanceof IShapedRecipe<?> shaped) {
            int w = shaped.getRecipeWidth();
            for (int i = 0; i < ingredients.size(); i++) {
                int row = i / w;
                int col = i % w;
                if (row < 3 && col < 3) order.grid[row * 3 + col] = firstItem(ingredients.get(i));
            }
        }
        else {
            for (int i = 0; i < ingredients.size() && i < GRID_SIZE; i++) {
                order.grid[i] = firstItem(ingredients.get(i));
            }
        }
        return order;
    }

    private static ItemStack firstItem(Ingredient ingredient) {
        ItemStack[] items = ingredient.getItems();
        return items.length == 0 ? ItemStack.EMPTY : items[0].copyWithCount(1);
    }

    // ── NBT ───────────────────────────────────────────────────────────────────

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("uuid", uuid);
        ListTag gridList = new ListTag();
        for (ItemStack stack : grid) {
            gridList.add(stack.serializeNBT());
        }
        tag.put("grid", gridList);
        tag.putString("type", type.name());
        if (recipeId != null) tag.putString("recipeId", recipeId.toString());
        tag.put("result", result.serializeNBT());
        tag.putBoolean("allowVariants", allowVariants);
        tag.putInt("amount", amount);
        tag.putInt("produced", produced);
        tag.putString("mode", mode.name());
        tag.putInt("priority", priority);
        tag.putInt("stock", stock);
        tag.putBoolean("enabled", enabled);
        tag.putString("source", source.name());
        if (sourceKey != null) tag.putString("sourceKey", sourceKey);
        tag.putString("status", status.name());

        ListTag missingList = new ListTag();
        for (ItemStack stack : missing) {
            CompoundTag entry = new CompoundTag();
            entry.put("item", stack.copyWithCount(1).serializeNBT());
            entry.putInt("count", stack.getCount());
            missingList.add(entry);
        }
        tag.put("missing", missingList);
        return tag;
    }

    public static CraftOrder fromNbt(CompoundTag tag) {
        CraftOrder order = new CraftOrder(tag.hasUUID("uuid") ? tag.getUUID("uuid") : UUID.randomUUID());
        ListTag gridList = tag.getList("grid", Tag.TAG_COMPOUND);
        for (int i = 0; i < GRID_SIZE && i < gridList.size(); i++) {
            order.grid[i] = ItemStack.of(gridList.getCompound(i));
        }
        order.type = OrderType.fromString(tag.getString("type"));
        order.recipeId = tag.contains("recipeId") ? ResourceLocation.tryParse(tag.getString("recipeId")) : null;
        order.result = ItemStack.of(tag.getCompound("result"));
        order.allowVariants = tag.getBoolean("allowVariants");
        order.amount = tag.getInt("amount");
        order.produced = tag.getInt("produced");
        order.mode = Mode.fromString(tag.getString("mode"));
        // old saves: DAILY became STOCK, its max stock is the stock amount now
        if ("DAILY".equals(tag.getString("mode")) && tag.getInt("maxStock") > 0) order.amount = tag.getInt("maxStock");
        order.stock = tag.getInt("stock");
        order.priority = tag.contains("priority") ? tag.getInt("priority") : 1;
        order.enabled = !tag.contains("enabled") || tag.getBoolean("enabled");
        order.source = Source.fromString(tag.getString("source"));
        order.sourceKey = tag.contains("sourceKey") ? tag.getString("sourceKey") : null;
        order.status = Status.fromString(tag.getString("status"));

        ListTag missingList = tag.getList("missing", Tag.TAG_COMPOUND);
        for (int i = 0; i < missingList.size(); i++) {
            CompoundTag entry = missingList.getCompound(i);
            ItemStack stack = ItemStack.of(entry.getCompound("item"));
            if (stack.isEmpty()) continue;
            stack.setCount(entry.getInt("count"));
            order.missing.add(stack);
        }
        return order;
    }

    public static CompoundTag listToNbt(List<CraftOrder> orders) {
        CompoundTag compound = new CompoundTag();
        ListTag list = new ListTag();
        if (orders != null) {
            for (CraftOrder order : orders) {
                list.add(order.toNbt());
            }
        }
        compound.put("Orders", list);
        return compound;
    }

    public static List<CraftOrder> listFromNbt(CompoundTag compound) {
        List<CraftOrder> out = new ArrayList<>();
        if (compound == null || !compound.contains("Orders", Tag.TAG_LIST)) return out;

        ListTag list = compound.getList("Orders", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            out.add(CraftOrder.fromNbt(list.getCompound(i)));
        }
        return out;
    }

    // ── Types ─────────────────────────────────────────────────────────────────

    /** What a grid slot accepts: exactly its ghost item, or anything the recipe ingredient accepts. */
    public static class SlotMatcher {
        public final ItemStack display;
        @Nullable private final ItemStack exact;
        @Nullable private final Ingredient ingredient;

        private SlotMatcher(ItemStack display, @Nullable ItemStack exact, @Nullable Ingredient ingredient) {
            this.display = display;
            this.exact = exact;
            this.ingredient = ingredient;
        }

        public static SlotMatcher exact(ItemStack stack) {
            ItemStack copy = stack.copyWithCount(1);
            return new SlotMatcher(copy, copy, null);
        }

        public static SlotMatcher ingredient(Ingredient ingredient, ItemStack display) {
            return new SlotMatcher(display.copyWithCount(1), null, ingredient);
        }

        public boolean isExact() {
            return exact != null;
        }

        public boolean test(ItemStack stack) {
            if (stack == null || stack.isEmpty()) return false;
            if (exact != null) return ItemStack.isSameItemSameTags(exact, stack);
            return ingredient != null && ingredient.test(stack);
        }

        public boolean isSame(SlotMatcher other) {
            if (other == null) return false;
            if (exact != null && other.exact != null) return ItemStack.isSameItemSameTags(exact, other.exact);
            return ingredient != null && ingredient == other.ingredient;
        }
    }

    public enum OrderType {
        CRAFTING,
        SMELTING;

        public static OrderType fromString(String s) {
            try { return valueOf(s); } catch (Exception e) { return CRAFTING; }
        }
    }

    /** STOCK: keep this many in the workshop chests. ONCE: produce this many one time. */
    public enum Mode {
        STOCK,
        ONCE;

        public static Mode fromString(String s) {
            try { return valueOf(s); } catch (Exception e) { return STOCK; }
        }
    }

    public enum Source {
        PLAYER,
        GOVERNOR;

        public static Source fromString(String s) {
            try { return valueOf(s); } catch (Exception e) { return PLAYER; }
        }
    }

    public enum Status {
        OK,
        MISSING_ITEMS,
        STOCK_FULL,
        NO_SPACE,
        DONE,
        INVALID,
        DISABLED;

        public static Status fromString(String s) {
            try { return valueOf(s); } catch (Exception e) { return OK; }
        }

        /** Orders with these states are skipped by the craftsman. */
        public boolean isBlocking() {
            return this != OK;
        }
    }
}
