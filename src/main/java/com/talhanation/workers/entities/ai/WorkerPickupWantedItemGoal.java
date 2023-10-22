package com.talhanation.workers.entities.ai;


import com.talhanation.workers.entities.AbstractWorkerEntity;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import java.util.List;


public class WorkerPickupWantedItemGoal extends Goal{
    AbstractWorkerEntity worker;
    List<ItemEntity> itemsToPickup;
    public WorkerPickupWantedItemGoal(AbstractWorkerEntity worker) {
        this.worker = worker;
    }

    @Override
    public boolean canUse() {
        if (
            this.worker.getStartPos() == null ||
            this.worker.getFollow() ||
            this.worker.isSleeping()
        ) {
            return false;
        };
        this.itemsToPickup = this.findItemsNearby();
        return !this.itemsToPickup.isEmpty();
    }

    private List<ItemEntity> findItemsNearby() {
        return this.worker.level.getEntitiesOfClass(ItemEntity.class, this.worker.getBoundingBox().inflate(8.0D, 4.0D, 8.0D), this.worker.getAllowedItems());
    }

    @Override
    public void tick() {
        this.itemsToPickup = this.findItemsNearby();
        if (!this.itemsToPickup.isEmpty()) {
            if(worker.distanceToSqr(this.itemsToPickup.get(0).position()) < 10F){
                worker.isPickingUp = true;
                worker.getNavigation().moveTo(this.itemsToPickup.get(0), 1.15F);
            }
            else worker.isPickingUp = false;

        }
    }
}

