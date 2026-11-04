package com.talhanation.workers.world;

import com.mojang.datafixers.util.Pair;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.Tags;

/**
 * What kind of item a stack is, so the storekeeper knows where it belongs.
 * Decided by tags first (modded items with forge tags land right on their own) and by the item class second.
 * The order matters: the first match wins, e.g. carrots are seeds for the farmer before they are food.
 */
public enum StorageCategory {
    ARMOR,
    TOOLS,
    MOB_DROPS,
    FARMING,
    FOOD,
    ORES,
    WOOD,
    BUILDING,
    MISC;

    public static StorageCategory of(ItemStack stack) {
        Item item = stack.getItem();

        if (item instanceof ArmorItem || item instanceof HorseArmorItem || stack.is(Tags.Items.ARMORS)) return ARMOR;

        if (item instanceof TieredItem || item instanceof ProjectileWeaponItem || item instanceof ShieldItem || item instanceof TridentItem
                || stack.is(Tags.Items.TOOLS) || stack.is(ItemTags.ARROWS)) return TOOLS;

        if (isBadFood(stack) || stack.is(Tags.Items.BONES) || stack.is(Tags.Items.STRING) || stack.is(Tags.Items.LEATHER)
                || stack.is(Tags.Items.FEATHERS) || stack.is(Tags.Items.GUNPOWDER) || stack.is(Tags.Items.SLIMEBALLS)
                || stack.is(Tags.Items.ENDER_PEARLS) || stack.is(Tags.Items.RODS_BLAZE) || stack.is(Tags.Items.HEADS)) return MOB_DROPS;

        if (stack.is(Tags.Items.SEEDS) || stack.is(Tags.Items.CROPS) || stack.is(Tags.Items.MUSHROOMS) || stack.is(Tags.Items.EGGS)
                || stack.is(ItemTags.SAPLINGS) || stack.is(ItemTags.FLOWERS) || item == Items.BONE_MEAL
                || item == Items.PUMPKIN || item == Items.MELON || isPlantable(item)) return FARMING;

        if (stack.isEdible()) return FOOD;

        if (stack.is(Tags.Items.INGOTS_BRICK) || stack.is(Tags.Items.INGOTS_NETHER_BRICK)) return BUILDING;

        if (stack.is(Tags.Items.ORES) || stack.is(Tags.Items.RAW_MATERIALS) || stack.is(Tags.Items.INGOTS) || stack.is(Tags.Items.NUGGETS)
                || stack.is(Tags.Items.GEMS) || stack.is(Tags.Items.DUSTS) || stack.is(Tags.Items.STORAGE_BLOCKS) || stack.is(ItemTags.COALS)) return ORES;

        if (stack.is(ItemTags.LOGS) || stack.is(ItemTags.PLANKS) || stack.is(ItemTags.WOODEN_SLABS) || stack.is(ItemTags.WOODEN_STAIRS)
                || stack.is(Tags.Items.RODS_WOODEN)) return WOOD;

        if (item instanceof BlockItem) return BUILDING;

        return MISC;
    }

    /** Food that mostly makes you sick (rotten flesh, spider eye) is a mob drop, not a meal. Raw chicken (30% hunger) stays food. */
    private static boolean isBadFood(ItemStack stack) {
        if (!stack.isEdible()) return false;
        FoodProperties food = stack.getFoodProperties(null);
        if (food == null) return false;
        for (Pair<MobEffectInstance, Float> effect : food.getEffects()) {
            if (effect.getFirst().getEffect().getCategory() == MobEffectCategory.HARMFUL && effect.getSecond() >= 0.5F) return true;
        }
        return false;
    }

    /** Saplings, seeds and plants of other mods that have no tag: their block can be planted. */
    private static boolean isPlantable(Item item) {
        return item instanceof BlockItem blockItem && blockItem.getBlock() instanceof IPlantable;
    }
}
