package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class EatGoal extends Goal {

    AbstractWorkerEntity worker;
    ItemStack foodStack;

    public EatGoal(AbstractWorkerEntity worker) {
        this.worker = worker;
    }

    public boolean isInterruptable() {
        return true;
    }
    public boolean requiresUpdateEveryTick() {
        return true;
    }
    @Override
    public boolean canUse() {
        //if (worker.beforeFoodItem != null) return false;
        if (hasFoodInInv() && (worker.needsToEat() || !worker.isSaturated())) return true;
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
            SimpleContainer inventory = worker.getInventory();

            worker.beforeFoodItem = worker.getItemInHand(InteractionHand.OFF_HAND);

            worker.setItemInHand(InteractionHand.OFF_HAND, foodStack);
            worker.startUsingItem(InteractionHand.OFF_HAND);

            worker.heal(foodStack.getItem().getFoodProperties(foodStack,worker).getSaturationModifier() * 10);
            worker.setHunger(worker.getHunger() + foodStack.getItem().getFoodProperties(foodStack,worker).getSaturationModifier() * 100);

            worker.eat(worker.level, foodStack);
            shrinkItemInInv(inventory, foodStack.getItem(), 1);
            inventory.setChanged();
        }
    }

    private boolean hasFoodInInv(){
        SimpleContainer inventory = worker.getInventory();

        for(int i = 0; i < inventory.getContainerSize(); i++){
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack.isEdible()){
                this.foodStack = itemStack.copy();

                return true;
            }
        }
        return false;
    }

    public void shrinkItemInInv(SimpleContainer inventory, Item item, int count){
        for (int i = 0; i < inventory.getContainerSize(); i++){
            ItemStack itemStackInSlot = inventory.getItem(i);
            Item itemInSlot = itemStackInSlot.getItem();
            if (itemInSlot == item){
                if(stack)
                itemStackInSlot.shrink(count);
                break;
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
    }
}