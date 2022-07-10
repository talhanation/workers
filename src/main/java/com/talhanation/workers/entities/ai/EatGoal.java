package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public class EatGoal extends Goal {

    AbstractWorkerEntity worker;
    ItemStack foodStack;

    public EatGoal(AbstractWorkerEntity worker) {
        this.worker = worker;
    }

    @Override
    public boolean canUse() {
        return hasFoodInInv() && worker.needsToEat() && !worker.isUsingItem();
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }

    @Override
    public void start() {
        worker.beforeFoodItem = worker.getItemInHand(InteractionHand.MAIN_HAND);
        foodStack = getFoodInInv();
        worker.setIsEating(true);
        worker.setItemInHand(InteractionHand.MAIN_HAND, foodStack);
        worker.startUsingItem(InteractionHand.MAIN_HAND);

        worker.heal(foodStack.getItem().getFoodProperties(foodStack,worker).getSaturationModifier() * 10);
        worker.setHunger(worker.getHunger() + foodStack.getItem().getFoodProperties(foodStack,worker).getSaturationModifier() * 25);
        if(foodStack.getCount() == 1)foodStack.shrink(1);//fix infinite food?
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
    }
}