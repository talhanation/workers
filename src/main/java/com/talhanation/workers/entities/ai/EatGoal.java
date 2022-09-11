package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;
import java.util.Optional;
public class EatGoal extends Goal {

    public AbstractWorkerEntity worker;
    public ItemStack foodStack;
    public ItemStack beforeFoodItem;

    public EatGoal(AbstractWorkerEntity recruit) {
        this.worker = recruit;
    }

    @Override
    public boolean canUse() {
        return hasFoodInInv() && worker.needsToEat() && !worker.getIsEating() && !worker.isSleeping();
    }

    @Override
    public boolean canContinueToUse() {
        return worker.getIsEating() && hasFoodInInv() && worker.needsToEat();
    }

    public boolean isInterruptable() {
        return false;
    }

    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        beforeFoodItem = worker.getItemInHand(InteractionHand.MAIN_HAND);
        worker.setIsEating(true);
        this.foodStack = getFoodInInv();

        worker.heal(Objects.requireNonNull(foodStack.getItem().getFoodProperties(foodStack, worker)).getSaturationModifier() * 1);
        if (!worker.isSaturated())
            worker.setHunger(worker.getHunger() + Objects.requireNonNull(foodStack.getItem().getFoodProperties(foodStack, worker)).getSaturationModifier() * 100);


        worker.setItemInHand(InteractionHand.MAIN_HAND, foodStack);
        worker.startUsingItem(InteractionHand.MAIN_HAND);
    }


    private boolean hasFoodInInv(){
        return worker.getInventory().items
                .stream()
                .anyMatch(ItemStack::isEdible);
    }

    private ItemStack getFoodInInv(){
        Optional<ItemStack> itemStack = worker.getInventory().items
                .stream()
                .filter(ItemStack::isEdible)
                .findAny();

        assert itemStack.isPresent();
        return itemStack.get();
    }

    @Override
    public void tick() {
        super.tick();

        if(!worker.isUsingItem() && worker.getIsEating() && beforeFoodItem != null) stop();
    }

    @Override
    public void stop() {
        worker.setIsEating(false);
        worker.stopUsingItem();

        resetItemInHand();
    }

    public void resetItemInHand() {
        worker.setItemInHand(InteractionHand.MAIN_HAND, this.beforeFoodItem);
    }
}