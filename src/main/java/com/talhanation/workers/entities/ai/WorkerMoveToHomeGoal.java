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
        if(this.worker.getBedPos() != null && !worker.getFollow() && !worker.getIsWorking() && !worker.isSleeping()){
            this.home = this.worker.getBedPos();
            return this.home.closerThan(worker.getOnPos(), 100F);
        }
        else
            return false;
    }

    public boolean canContinueToUse() {
        return this.canUse();
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
