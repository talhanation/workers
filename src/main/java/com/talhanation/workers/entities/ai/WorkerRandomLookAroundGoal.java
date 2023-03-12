package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;


public class WorkerRandomLookAroundGoal extends RandomLookAroundGoal {

    private final AbstractWorkerEntity worker;
    public WorkerRandomLookAroundGoal(AbstractWorkerEntity worker) {
        super(worker);
        this.worker = worker;
    }

    @Override
    public boolean canUse() {
        return !worker.getIsWorking() && super.canUse();
    }
}
