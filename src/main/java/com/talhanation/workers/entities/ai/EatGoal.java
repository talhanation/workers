package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class EatGoal extends Goal {

    AbstractWorkerEntity worker;
    ItemStack foodStack;

    public EatGoal(AbstractWorkerEntity worker) {
        this.worker = worker;
    }

    @Override
    public boolean canUse() {
        //if (worker.beforeFoodItem != null) return false;
        return hasFoodInInv() && worker.needsToEat() && !worker.isUsingItem();
    }

    @Override
    public boolean canContinueToUse() {
//        return hasFoodInInv() && !worker.isSaturated() && !worker.isUsingItem(); // This doesnt work
        return false;
    }

    @Override
    public void start() {
        worker.beforeFoodItem = worker.getItemInHand(InteractionHand.OFF_HAND);
        foodStack = getFoodInInv();
        worker.setItemInHand(InteractionHand.OFF_HAND, foodStack);
        worker.startUsingItem(InteractionHand.OFF_HAND);

        worker.heal(foodStack.getItem().getFoodProperties(foodStack,worker).getSaturationModifier() * 10);
        worker.setHunger(worker.getHunger() + foodStack.getItem().getFoodProperties(foodStack,worker).getSaturationModifier() * 10);

//        worker.eat(worker.level, foodStack); // startUsingItem() is doing this already
//        shrinkItemInInv(foodStack.getItem(), 1); // startUsingItem() is doing this already
    }

    private boolean hasFoodInInv(){
        return worker.getInventory().items.stream().anyMatch(ItemStack::isEdible);
    }

    private ItemStack getFoodInInv(){
        Optional<ItemStack> itemStack = worker.getInventory().items.stream().filter(ItemStack::isEdible).findAny();
        assert itemStack.isPresent();
        return itemStack.get();
    }

    public void shrinkItemInInv(Item item, int count){
        worker.getInventory().items.stream().filter(stack -> stack.getItem() == item).findAny().ifPresentOrElse(stack -> stack.shrink(1), () -> {});
    }

    @Override
    public void tick() {
        super.tick();
    }
}