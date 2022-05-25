package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class EatGoal extends Goal {

    AbstractWorkerEntity worker;
    ItemStack foodItem;

    public EatGoal(AbstractWorkerEntity worker) {
        this.worker = worker;
    }

    @Override
    public boolean canUse() {
        //if (worker.beforeFoodItem != null) return false;
        if (worker.needsToEat() && hasFoodInInv() && !worker.getIsEating()) return true;
        else
            return false;
    }

    @Override
    public boolean canContinueToUse() {
        return hasFoodInInv() && !worker.isSaturated();
    }

    @Override
    public void start() {
        if (hasFoodInInv()) {
            //worker.beforeFoodItem = worker.getItemInHand(InteractionHand.OFF_HAND);

            worker.setItemInHand(InteractionHand.OFF_HAND, foodItem);
            worker.startUsingItem(InteractionHand.OFF_HAND);

            worker.heal(foodItem.getItem().getFoodProperties(foodItem,worker).getSaturationModifier() * 10);
            worker.setHunger(worker.getHunger() + foodItem.getItem().getFoodProperties(foodItem,worker).getSaturationModifier() * 100);
            worker.setIsEating(true);
            worker.eat(worker.level, foodItem);
        }
    }

    @Override
    public void stop() {
        super.stop();
        consumeFoodItem();
        worker.setIsEating(false);
    }

    private boolean hasFoodInInv(){
        SimpleContainer inventory = worker.getInventory();

        for(int i = 0; i < inventory.getContainerSize(); i++){
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack.isEdible()){
                setFoodItem(itemStack);
                return true;
            }
        }
        return false;
    }


    private void setFoodItem(ItemStack itemStack){
        this.foodItem = itemStack.copy();
    }

    private void consumeFoodItem(){
        this.foodItem.shrink(1);
    }
}