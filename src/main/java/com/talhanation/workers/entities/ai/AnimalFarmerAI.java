package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.AbstractAnimalFarmerEntity;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.*;

public abstract class AnimalFarmerAI extends Goal {

    public AbstractAnimalFarmerEntity animalFarmer;
    public State state = State.IDLE;
    @Override
    public void tick() {
        super.tick();
        //TODO: Work Tick / Work Cooldown
        switch (state){
            case IDLE ->{
                if(animalFarmer.getStartPos() != null) state = State.WORKING;
            }
            case WORKING -> {
                if(animalFarmer.getStartPos() != null && !animalFarmer.getFollow()) {
                    double distance = animalFarmer.getStartPos().distSqr(animalFarmer.getOnPos());
                    if (distance >= 80F) {
                        state = State.MOVING_TO_WORK;
                    }
                    else if(!animalFarmer.swinging) {
                            performWork();
                    }
                }
                else state = State.IDLE;
            }
            case MOVING_TO_WORK -> {
                if(animalFarmer.getStartPos() != null && !animalFarmer.getFollow()){
                    double distance = animalFarmer.getStartPos().distSqr(animalFarmer.getOnPos());
                    if(distance <= 15F) state = State.WORKING;
                    else this.animalFarmer.walkTowards(animalFarmer.getStartPos(), 1);
                }
            }
        }




    }

    private enum State {
        IDLE,
        WORKING,
        MOVING_TO_WORK
    }

    public abstract void performWork();

    public boolean hasBreedItem(Item breedItem) {
        SimpleContainer inventory = animalFarmer.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack.getItem().equals(breedItem))
                if (itemStack.getCount() >= 2)
                    return true;
        }
        return false;
    }

    public void consumeBreedItem(Item breedItem){
        SimpleContainer inventory = this.animalFarmer.getInventory();
        for(int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack.getItem().equals(breedItem)){
                itemStack.shrink(1);
                break;
            }
        }
    }

    public boolean hasMainToolInInv() {
        SimpleContainer inventory = animalFarmer.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (this.animalFarmer.isRequiredMainTool(itemStack)) return true;
        }
        return false;
    }

    public boolean hasSecondToolInInv() {
        SimpleContainer inventory = animalFarmer.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (animalFarmer.isRequiredSecondTool(itemStack)) return true;
        }
        return false;
    }
}
