package com.talhanation.workers.entities.ai;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;

public class VillagerPickupFoodGoal extends Goal {

    private static final double SCAN_RADIUS   = 8.0;
    private static final double PICKUP_RADIUS = 1.2;

    private final Villager villager;
    @Nullable private ItemEntity targetItem;

    public VillagerPickupFoodGoal(Villager villager) {
        this.villager = villager;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (villager.level().isClientSide()) return false;
        if (villager.isSleeping() || villager.isTrading()) return false;
        targetItem = findNearbyFoodItem();
        return targetItem != null;
    }

    @Override
    public boolean canContinueToUse() {
        return targetItem != null
                && !targetItem.isRemoved()
                && !targetItem.hasPickUpDelay()
                && villager.distanceTo(targetItem) < SCAN_RADIUS;
    }

    @Override
    public void stop() {
        targetItem = null;
        villager.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (targetItem == null || targetItem.isRemoved()) {
            stop();
            return;
        }

        if (villager.distanceTo(targetItem) <= PICKUP_RADIUS) {
            pickupItem(targetItem);
            stop();
        }
        else {
            villager.getNavigation().moveTo(targetItem, 0.7);
        }
    }

    private void pickupItem(ItemEntity itemEntity) {
        ItemStack stack = itemEntity.getItem();
        if (stack.isEmpty()) return;

        // Only discard the item entity if the full stack was accepted
        ItemStack remaining = villager.getInventory().addItem(stack.copy());
        if (remaining.isEmpty()) {
            itemEntity.discard();
        }
    }

    @Nullable
    private ItemEntity findNearbyFoodItem() {
        AABB searchBox = villager.getBoundingBox().inflate(SCAN_RADIUS);

        List<ItemEntity> items = villager.level().getEntitiesOfClass(
                ItemEntity.class, searchBox,
                item -> !item.hasPickUpDelay()
                        && !item.isRemoved()
                        && isFood(item.getItem()));

        if (items.isEmpty()) return null;

        ItemEntity closest      = null;
        double     closestDistSq = Double.MAX_VALUE;

        for (ItemEntity item : items) {
            double d = villager.distanceToSqr(item);
            if (d < closestDistSq) {
                closestDistSq = d;
                closest       = item;
            }
        }

        return closest;
    }

    private static boolean isFood(ItemStack stack) {
        if (stack.isEmpty()) return false;
        FoodProperties food = stack.getItem().getFoodProperties();
        return food != null && food.getNutrition() > 0;
    }
}