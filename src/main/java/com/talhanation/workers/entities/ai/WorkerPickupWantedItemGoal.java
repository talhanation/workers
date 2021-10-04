package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import net.minecraft.entity.ai.goal.Goal;

public class WorkerPickupWantedItemGoal extends Goal {

    AbstractWorkerEntity worker;

    public WorkerPickupWantedItemGoal(AbstractWorkerEntity worker) {
        this.worker = worker;
    }

    @Override
    public boolean canUse() {
        return this.worker.getInventory().;
    }


    public void tick() {
    }
}
