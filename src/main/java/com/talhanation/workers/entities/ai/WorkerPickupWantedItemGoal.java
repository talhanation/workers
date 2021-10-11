package com.talhanation.workers.entities.ai;


import com.talhanation.workers.entities.AbstractWorkerEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.item.ItemEntity;

import java.util.List;
import java.util.function.Predicate;


public class WorkerPickupWantedItemGoal extends Goal{

    AbstractWorkerEntity worker;
    Predicate<ItemEntity> allowedItems;

    public WorkerPickupWantedItemGoal(AbstractWorkerEntity worker, Predicate<ItemEntity> allowedItems) {
        this.worker = worker;
        this.allowedItems = allowedItems;
    }

    @Override
    public boolean canUse() {
        return true;
    }

    @Override
    public void tick() {
        List<ItemEntity> list = worker.level.getEntitiesOfClass(ItemEntity.class, worker.getBoundingBox().inflate(16.0D, 8.0D, 16.0D), allowedItems);
        if (!list.isEmpty()) {
            worker.getNavigation().moveTo(list.get(0), 1.15F);
        }
    }
}

