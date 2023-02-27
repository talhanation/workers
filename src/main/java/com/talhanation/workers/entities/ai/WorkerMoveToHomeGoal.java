package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.core.BlockPos;

public class WorkerMoveToHomeGoal<T extends LivingEntity> extends Goal {
    private final AbstractWorkerEntity worker;
    private BlockPos home;

    public WorkerMoveToHomeGoal(AbstractWorkerEntity worker) {
        this.worker = worker;
    }

    public boolean canUse() {
        return (
            this.worker.getBedPos() != null &&
            !worker.getFollow() && 
            !worker.getIsWorking() &&
            !worker.getIsEating() && 
            !worker.isSleeping()
        );
    }

    public boolean canContinueToUse() {
        return this.canUse();
    }

    @Override
    public void start() {
        super.start();
        this.home = this.worker.getBedPos();
    }

    @Override
    public void tick() {
        super.tick();
        if (home != null) {
            if (!home.closerThan(worker.getOnPos(), 16)) {
                this.worker.walkTowards(home, 1);
            }
        }
    }
}
