package com.talhanation.workers.entities.ai;


import com.talhanation.workers.entities.AbstractWorkerEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import java.util.List;



public class WorkerPickupWantedItemGoal extends Goal{

    AbstractWorkerEntity worker;

    public WorkerPickupWantedItemGoal(AbstractWorkerEntity worker) {
        this.worker = worker;

    }

    @Override
    public boolean canUse() {
        return true;
    }

    @Override
    public void tick() {
        if (worker.getIsPickingUp()) {
            List<ItemEntity> list = worker.level.getEntitiesOfClass(ItemEntity.class, worker.getBoundingBox().inflate(8.0D, 4.0D, 8.0D), worker.getAllowedItems());
            if (!list.isEmpty()) {
                worker.getNavigation().moveTo(list.get(0), 1.15F);
            }
            else this.worker.setIsPickingUp(false);
        }
    }
}

