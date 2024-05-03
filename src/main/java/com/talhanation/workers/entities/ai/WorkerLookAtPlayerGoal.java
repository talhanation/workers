package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;

public class WorkerLookAtPlayerGoal extends LookAtPlayerGoal {
    private final AbstractWorkerEntity worker;
    public WorkerLookAtPlayerGoal(AbstractWorkerEntity worker, Class<? extends LivingEntity> living, float f) {
        super(worker, living, f);
        this.worker = worker;
    }

    @Override
    public boolean canUse() {
        return worker.getStatus() != AbstractWorkerEntity.Status.WORK
                && worker.getStatus() != AbstractWorkerEntity.Status.SLEEP
                && super.canUse();
    }
}
