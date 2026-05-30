package com.talhanation.workers.world;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public class NeededItem {
    public final Predicate<ItemStack> matcher;
    public int count;
    public final boolean required;

    /**
     * Identifies which source (typically a work area's UUID) pushed this need.
     * Two NeededItems with the same matchKey but different sourceKeys are kept
     * separate, so e.g. two different crop fields each needing bone meal don't
     * merge their counts together. May be null if the caller doesn't know or
     * doesn't care about source-based separation.
     */
    @Nullable public final Object sourceKey;

    /**
     * Cached "what does this matcher accept" identifier, used by isSameRequest.
     * Either set explicitly via the 5-arg constructor or lazily derived from
     * the matcher by sampling the item registry. Lambdas can't be reliably
     * compared, so we use this derived key for equality checks instead.
     */
    @Nullable private Object cachedMatchKey;
    private boolean matchKeyResolved;

    public NeededItem(Predicate<ItemStack> matcher, int count, boolean required) {
        this(matcher, count, required, null, null);
    }

    public NeededItem(Predicate<ItemStack> matcher, int count, boolean required, @Nullable Object sourceKey) {
        this(matcher, count, required, null, sourceKey);
    }

    public NeededItem(Predicate<ItemStack> matcher, int count, boolean required, @Nullable Object matchKey, @Nullable Object sourceKey) {
        this.matcher = matcher;
        this.count = count;
        this.required = required;
        this.sourceKey = sourceKey;
        if (matchKey != null) {
            this.cachedMatchKey = matchKey;
            this.matchKeyResolved = true;
        }
    }

    /**
     * Returns a key that identifies what kind of item this matcher accepts.
     * Explicit if provided to the constructor, otherwise lazily derived by
     * finding the first registry item the matcher accepts. Cached so the
     * registry scan only runs once per NeededItem.
     */
    @Nullable
    public Object getMatchKey() {
        if (!matchKeyResolved) {
            cachedMatchKey = tryExtractSingleItemFromMatcher();
            matchKeyResolved = true;
        }
        return cachedMatchKey;
    }

    /**
     * Two NeededItems are "the same request" if they match the same kind of
     * item AND come from the same source. If either matchKey can't be derived
     * we treat them as different (safer to keep both than to silently merge).
     */
    public boolean isSameRequest(NeededItem other) {
        Object myKey = this.getMatchKey();
        Object otherKey = other.getMatchKey();
        if (myKey == null || otherKey == null) return false;
        if (!Objects.equals(myKey, otherKey)) return false;
        return Objects.equals(this.sourceKey, other.sourceKey);
    }

    public boolean matches(ItemStack stack) {
        if(stack == null || stack.isEmpty()) return false;
        return matcher.test(stack);
    }

    @Nullable
    private Item tryExtractSingleItemFromMatcher() {
        for (Item item : BuiltInRegistries.ITEM) {
            if (matcher.test(new ItemStack(item))) {
                return item;
            }
        }

        return null;
    }

    @Override
    public String toString() {
        Item displayItem = tryExtractSingleItemFromMatcher();

        String name = displayItem != null ? new ItemStack(displayItem).getHoverName().getString() : "Unknown Item";

        return count + "x " + name;
    }

    public static void applyToNeededItems(ItemStack stack, List<NeededItem> neededItems) {
        if (stack.isEmpty()) return;

        for (int i = neededItems.size() - 1; i >= 0; i--) {
            NeededItem needed = neededItems.get(i);

            if (needed.matches(stack)) {
                int toUse = Math.min(stack.getCount(), needed.count);
                stack.shrink(toUse);

                if (toUse == needed.count) {
                    neededItems.remove(i);
                } else {
                    needed.count -= toUse;
                }

                return;
            }
        }

    }
}