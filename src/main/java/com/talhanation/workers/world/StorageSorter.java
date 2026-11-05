package com.talhanation.workers.world;

import com.talhanation.workers.entities.workarea.StorageArea;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.registries.BuiltInRegistries;

import javax.annotation.Nullable;
import java.util.*;

/**
 * The order of a storage, used by the storekeeper. No settings: every chest gets a label, either a category
 * ("cat:FOOD") or one item ("item:minecraft:cobblestone"). An item with a lot of stacks gets its own chests,
 * everything else shares the chests of its category.
 * <p>
 * {@link #plan} gives every label as many chests as its stacks need, keeps chests that already hold the right items
 * and frees chests that are not needed. The rest only answers questions on that plan: where does a stack belong,
 * which chest holds the most stacks that are wrong. Verified with a simulation: converges, nothing moves back and forth.
 */
public final class StorageSorter {
    /** An item with this many stacks gets its own chests. */
    public static final int DEDICATE_STACKS = 9;
    /** An item with its own chests goes back to the category chests below this many stacks. */
    public static final int RELEASE_STACKS = 3;
    /** A label gives up a chest only if its other chests still have this many slots left on top. */
    public static final int RELEASE_SLACK = 9;

    private static final String CATEGORY_LABEL = "cat:";
    private static final String ITEM_LABEL = "item:";

    private StorageSorter() {
    }

    // ── Labels ────────────────────────────────────────────────────────────────

    public static String targetLabel(StorageArea area, ItemStack stack) {
        if (area.getDedicatedItems().contains(stack.getItem())) return itemLabel(stack.getItem());
        return CATEGORY_LABEL + StorageCategory.of(stack).name();
    }

    public static boolean isMisplaced(StorageArea area, BlockPos pos, ItemStack stack) {
        return !targetLabel(area, stack).equals(area.getChestLabel(pos));
    }

    private static String itemLabel(Item item) {
        return ITEM_LABEL + BuiltInRegistries.ITEM.getKey(item);
    }

    // ── Plan ──────────────────────────────────────────────────────────────────

    /**
     * Updates which items get their own chests and which chest holds which label.
     * {@code storageMap} must be scanned before. {@code carried} are the items the storekeeper holds, they count as stored.
     */
    public static void plan(StorageArea area, List<ItemStack> carried) {
        Map<BlockPos, String> labels = area.getChestLabels();
        labels.keySet().removeIf(pos -> !area.storageMap.containsKey(pos));

        // amount of every item, one stack of each as example for its category
        Map<Item, Integer> amounts = new HashMap<>();
        Map<Item, ItemStack> examples = new HashMap<>();
        for (Container container : area.storageMap.values()) {
            for (int i = 0; i < container.getContainerSize(); i++) {
                count(container.getItem(i), amounts, examples);
            }
        }
        for (ItemStack stack : carried) count(stack, amounts, examples);

        Map<Item, Integer> stacks = new HashMap<>();
        for (Map.Entry<Item, Integer> entry : amounts.entrySet()) {
            int max = Math.max(1, examples.get(entry.getKey()).getMaxStackSize());
            stacks.put(entry.getKey(), (entry.getValue() + max - 1) / max);
        }

        // items with their own chests: many stacks, or already own chests and not too few stacks left
        Set<String> hadOwnChest = new HashSet<>();
        for (String label : labels.values()) if (label.startsWith(ITEM_LABEL)) hadOwnChest.add(label);
        Set<Item> dedicated = area.getDedicatedItems();
        dedicated.clear();
        for (Map.Entry<Item, Integer> entry : stacks.entrySet()) {
            Item item = entry.getKey();
            if (examples.get(item).getMaxStackSize() <= 1) continue;
            int count = entry.getValue();
            if (count >= DEDICATE_STACKS || (count >= RELEASE_STACKS && hadOwnChest.contains(itemLabel(item)))) dedicated.add(item);
        }

        // slots every label needs, and the category of every label
        Map<String, Integer> need = new HashMap<>();
        Map<String, StorageCategory> labelCategory = new HashMap<>();
        for (Map.Entry<Item, Integer> entry : stacks.entrySet()) {
            ItemStack example = examples.get(entry.getKey());
            String label = targetLabel(area, example);
            need.merge(label, entry.getValue(), Integer::sum);
            labelCategory.putIfAbsent(label, StorageCategory.of(example));
        }

        // stacks per chest and label, so a chest that already holds the right items keeps its label
        Map<BlockPos, Map<String, Integer>> matching = new HashMap<>();
        for (Map.Entry<BlockPos, Container> entry : area.storageMap.entrySet()) {
            Map<String, Integer> tally = new HashMap<>();
            Container container = entry.getValue();
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack stack = container.getItem(i);
                if (!stack.isEmpty()) tally.merge(targetLabel(area, stack), 1, Integer::sum);
            }
            matching.put(entry.getKey(), tally);
        }

        Vec3 center = area.getArea().getCenter();

        // 1. every label keeps its best chests until it has enough room, the others are freed
        Map<String, List<BlockPos>> byLabel = new HashMap<>();
        for (Map.Entry<BlockPos, String> entry : labels.entrySet()) {
            byLabel.computeIfAbsent(entry.getValue(), l -> new ArrayList<>()).add(entry.getKey());
        }
        for (Map.Entry<String, List<BlockPos>> entry : byLabel.entrySet()) {
            String label = entry.getKey();
            List<BlockPos> own = entry.getValue();
            own.sort(Comparator.comparingInt((BlockPos pos) -> -matching.get(pos).getOrDefault(label, 0))
                    .thenComparingDouble(pos -> pos.getCenter().distanceToSqr(center)));
            int capacity = 0;
            for (BlockPos pos : own) {
                if (!need.containsKey(label) || capacity >= need.get(label) + RELEASE_SLACK) labels.remove(pos);
                else capacity += area.storageMap.get(pos).getContainerSize();
            }
        }

        // 2. labels without enough room get the free chest that holds most of their items, otherwise an empty one near their kind
        List<String> order = new ArrayList<>(need.keySet());
        order.sort(Comparator.comparingInt((String label) -> -need.get(label)).thenComparing(label -> label));
        for (String label : order) {
            int capacity = 0;
            for (Map.Entry<BlockPos, String> entry : labels.entrySet()) {
                if (entry.getValue().equals(label)) capacity += area.storageMap.get(entry.getKey()).getContainerSize();
            }
            while (capacity < need.get(label)) {
                BlockPos chosen = chooseFreeChest(area, labels, matching, label, labelCategory, center);
                if (chosen == null) break;
                labels.put(chosen, label);
                capacity += area.storageMap.get(chosen).getContainerSize();
            }
        }
    }

    private static void count(ItemStack stack, Map<Item, Integer> amounts, Map<Item, ItemStack> examples) {
        if (stack.isEmpty()) return;
        amounts.merge(stack.getItem(), stack.getCount(), Integer::sum);
        examples.putIfAbsent(stack.getItem(), stack);
    }

    @Nullable
    private static BlockPos chooseFreeChest(StorageArea area, Map<BlockPos, String> labels, Map<BlockPos, Map<String, Integer>> matching,
                                            String label, Map<String, StorageCategory> labelCategory, Vec3 center) {
        // chests of the same kind, the new chest should be next to them
        StorageCategory category = labelCategory.get(label);
        double x = 0, y = 0, z = 0;
        int same = 0;
        for (Map.Entry<BlockPos, String> entry : labels.entrySet()) {
            if (labelCategory.get(entry.getValue()) != category) continue;
            x += entry.getKey().getX() + 0.5D;
            y += entry.getKey().getY() + 0.5D;
            z += entry.getKey().getZ() + 0.5D;
            same++;
        }
        Vec3 near = same > 0 ? new Vec3(x / same, y / same, z / same) : center;

        BlockPos bestMatching = null;
        int bestCount = 0;
        BlockPos bestEmpty = null;
        BlockPos bestFull = null;
        for (Map.Entry<BlockPos, Container> entry : area.storageMap.entrySet()) {
            BlockPos pos = entry.getKey();
            if (labels.containsKey(pos)) continue;
            int count = matching.get(pos).getOrDefault(label, 0);
            if (count > bestCount || (count == bestCount && count > 0 && closer(pos, bestMatching, near))) {
                bestMatching = pos;
                bestCount = count;
            }
            if (entry.getValue().isEmpty()) {
                if (closer(pos, bestEmpty, near)) bestEmpty = pos;
            }
            else if (closer(pos, bestFull, near)) bestFull = pos;
        }
        if (bestMatching != null) return bestMatching;
        if (bestEmpty != null) return bestEmpty;
        return bestFull;
    }

    private static boolean closer(BlockPos pos, @Nullable BlockPos best, Vec3 near) {
        return best == null || pos.getCenter().distanceToSqr(near) < best.getCenter().distanceToSqr(near);
    }

    // ── Questions on the plan ─────────────────────────────────────────────────

    /** The nearest chest with the label of the stack that still has room for it, or null. Chests in {@code skip} are left out. */
    @Nullable
    public static BlockPos findTarget(StorageArea area, ItemStack stack, Vec3 near, @Nullable BlockPos exclude, Set<BlockPos> skip) {
        String label = targetLabel(area, stack);
        BlockPos best = null;
        for (Map.Entry<BlockPos, String> entry : area.getChestLabels().entrySet()) {
            BlockPos pos = entry.getKey();
            if (pos.equals(exclude) || skip.contains(pos) || !entry.getValue().equals(label)) continue;
            Container container = area.storageMap.get(pos);
            if (container == null || !hasRoom(container, stack)) continue;
            if (closer(pos, best, near)) best = pos;
        }
        return best;
    }

    /** Any chest with room, for when the right chests are full and no chest is free. */
    @Nullable
    public static BlockPos findAnySpace(StorageArea area, ItemStack stack, Vec3 near, Set<BlockPos> skip) {
        BlockPos best = null;
        for (Map.Entry<BlockPos, Container> entry : area.storageMap.entrySet()) {
            if (skip.contains(entry.getKey()) || !hasRoom(entry.getValue(), stack)) continue;
            if (closer(entry.getKey(), best, near)) best = entry.getKey();
        }
        return best;
    }

    /** The chest with the most stacks that are in the wrong chest and have a better place. Null = everything is in order. */
    @Nullable
    public static BlockPos findSource(StorageArea area, Vec3 near, Set<BlockPos> skip) {
        BlockPos best = null;
        int bestCount = 0;
        for (Map.Entry<BlockPos, Container> entry : area.storageMap.entrySet()) {
            BlockPos pos = entry.getKey();
            if (skip.contains(pos)) continue;
            int count = 0;
            Container container = entry.getValue();
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack stack = container.getItem(i);
                if (canMove(area, pos, stack, skip)) count++;
            }
            if (count > bestCount || (count == bestCount && count > 0 && closer(pos, best, near))) {
                best = pos;
                bestCount = count;
            }
        }
        return best;
    }

    /** The stack lies in the wrong chest and its chest has room for it. */
    public static boolean canMove(StorageArea area, BlockPos pos, ItemStack stack, Set<BlockPos> skip) {
        return !stack.isEmpty() && isMisplaced(area, pos, stack) && findTarget(area, stack, pos.getCenter(), pos, skip) != null;
    }

    // ── Container helpers ─────────────────────────────────────────────────────

    public static boolean hasRoom(Container container, ItemStack stack) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack slot = container.getItem(i);
            if (slot.isEmpty()) return true;
            if (ItemStack.isSameItemSameComponents(slot, stack) && slot.getCount() < Math.min(slot.getMaxStackSize(), container.getMaxStackSize())) return true;
        }
        return false;
    }

    /** Puts the stack into the container, first onto stacks of the same item, then into empty slots. Returns what did not fit. */
    public static ItemStack insert(Container container, ItemStack stack) {
        ItemStack rest = stack.copy();
        for (int i = 0; i < container.getContainerSize() && !rest.isEmpty(); i++) {
            ItemStack slot = container.getItem(i);
            if (slot.isEmpty() || !ItemStack.isSameItemSameComponents(slot, rest)) continue;
            int max = Math.min(slot.getMaxStackSize(), container.getMaxStackSize());
            int move = Math.min(rest.getCount(), max - slot.getCount());
            if (move <= 0) continue;
            slot.grow(move);
            rest.shrink(move);
            container.setItem(i, slot);
        }
        for (int i = 0; i < container.getContainerSize() && !rest.isEmpty(); i++) {
            if (!container.getItem(i).isEmpty()) continue;
            int move = Math.min(rest.getCount(), Math.min(rest.getMaxStackSize(), container.getMaxStackSize()));
            container.setItem(i, rest.split(move));
        }
        container.setChanged();
        return rest;
    }

    /** Merges stacks of the same item inside one container, so half stacks do not take up slots. */
    public static void compact(Container container) {
        boolean changed = false;
        for (int i = container.getContainerSize() - 1; i > 0; i--) {
            ItemStack from = container.getItem(i);
            if (from.isEmpty()) continue;
            boolean moved = false;
            for (int j = 0; j < i && !from.isEmpty(); j++) {
                ItemStack to = container.getItem(j);
                if (to.isEmpty() || !ItemStack.isSameItemSameComponents(to, from)) continue;
                int max = Math.min(to.getMaxStackSize(), container.getMaxStackSize());
                int move = Math.min(from.getCount(), max - to.getCount());
                if (move <= 0) continue;
                to.grow(move);
                from.shrink(move);
                container.setItem(j, to);
                moved = true;
            }
            if (moved) {
                container.setItem(i, from.isEmpty() ? ItemStack.EMPTY : from);
                changed = true;
            }
        }
        if (changed) container.setChanged();
    }
}
