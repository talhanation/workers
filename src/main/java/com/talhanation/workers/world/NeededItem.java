package com.talhanation.workers.world;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Predicate;

public class NeededItem {
    public final Predicate<ItemStack> matcher;
    public int count;
    public final boolean required;

    public NeededItem(Predicate<ItemStack> matcher, int count, boolean required) {
        this.matcher = matcher;
        this.count = count;
        this.required = required;
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


