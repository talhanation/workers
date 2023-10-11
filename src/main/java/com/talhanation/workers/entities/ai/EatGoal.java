package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.AbstractWorkerEntity;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Objects;

public class EatGoal extends Goal{

    public AbstractWorkerEntity worker;
    public ItemStack foodStack;
    public ItemStack beforeItem;
    public int slotID;

    public EatGoal(AbstractWorkerEntity worker) {
        this.worker = worker;
    }

    @Override
    public boolean canUse() {
        worker.updateHunger();
        return hasFoodInInv() && worker.needsToEat() && !worker.isUsingItem();
    }

    @Override
    public boolean canContinueToUse() {
        return worker.isUsingItem();
    }

    public boolean isInterruptable() {
        return false;
    }

    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        slotID = 0;
        beforeItem = worker.getOffhandItem().copy();
        this.foodStack = getAndRemoveFoodInInv().copy();
        /*
        Main.LOGGER.debug("Start--------------: ");
        Main.LOGGER.debug("beforeFoodItem: " + beforeFoodItem.copy());
        Main.LOGGER.debug("isEating: " + recruit.getIsEating());
        Main.LOGGER.debug("foodStack: " + foodStack.copy());
        Main.LOGGER.debug("Start--------------:");
        */

        worker.heal(Objects.requireNonNull(foodStack.getItem().getFoodProperties(foodStack, worker)).getSaturationModifier() * 1);
        if (!worker.isSaturated())
            worker.setHunger(worker.getHunger() + Objects.requireNonNull(foodStack.getItem().getFoodProperties(foodStack, worker)).getSaturationModifier() * 20);

        worker.setItemInHand(InteractionHand.OFF_HAND, foodStack);
        worker.startUsingItem(InteractionHand.OFF_HAND);
    }

    @Override
    public void stop() {
        worker.stopUsingItem();
        resetItemInHand();
        /*
        Main.LOGGER.debug("Stop--------------: ");
        Main.LOGGER.debug("beforeFoodItem: " + beforeFoodItem);
        Main.LOGGER.debug("isEating: " + recruit.getIsEating());
        Main.LOGGER.debug("foodStack: " + foodStack.copy());
        Main.LOGGER.debug("Stop--------------:");
         */
    }

    public void resetItemInHand() {
        worker.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        worker.getInventory().setItem(4, ItemStack.EMPTY);

        worker.setItemInHand(InteractionHand.OFF_HAND, this.beforeItem.copy());
        if(worker.getInventory().getItem(slotID).isEmpty()){
            worker.getInventory().setItem(slotID, foodStack.copy());
        }
        else{
            for(int i = 0; i < worker.getInventory().getContainerSize(); i++){
                if(worker.getInventory().getItem(i).isEmpty()){
                    worker.getInventory().setItem(i, foodStack.copy());
                    return;
                }
            }
            //if the method is not returned there is no space in inv, therefor spawn it in world
            worker.spawnAtLocation(foodStack.copy());
        }

    }

    private boolean hasFoodInInv(){
        return worker.getInventory().items
                .stream()
                .filter(itemStack -> !itemStack.is(Items.PUFFERFISH))
                .filter(itemStack -> itemStack.isEdible() && itemStack.getFoodProperties(this.worker).getNutrition() > 4)
                .anyMatch(ItemStack::isEdible);
    }

    private ItemStack getAndRemoveFoodInInv(){
        ItemStack itemStack = null;
        for(int i = 0; i < worker.getInventory().getContainerSize(); i++){
            ItemStack stackInSlot = worker.getInventory().getItem(i).copy();
            if(stackInSlot.isEdible() && !stackInSlot.is(Items.PUFFERFISH) && stackInSlot.getFoodProperties(this.worker).getNutrition() > 4){
                itemStack = stackInSlot.copy();
                this.slotID = i;
                worker.getInventory().removeItemNoUpdate(i); //removing item in slot
                break;
            }
        }
        return itemStack;
    }
}

