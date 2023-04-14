package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.AbstractAnimalFarmerEntity;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.*;

public abstract class AnimalFarmerAI extends Goal {

    public AbstractAnimalFarmerEntity animalFarmer;

    @Override
    public void tick() {
        super.tick();
        //TODO: Work Tick / Work Cooldown
        if(!animalFarmer.swinging) {
            performWork();
        }
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
