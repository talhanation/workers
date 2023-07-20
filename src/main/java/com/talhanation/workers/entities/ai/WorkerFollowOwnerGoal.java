package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import net.minecraft.world.entity.ai.goal.Goal;

public class WorkerFollowOwnerGoal extends Goal {
    private final AbstractWorkerEntity worker;

    private final double speedModifier;
    private final double within;

    public WorkerFollowOwnerGoal(AbstractWorkerEntity worker, double v, double within) {
        this.worker = worker;
        this.speedModifier = v;
        this.within = within;
    }

    public boolean canUse() {
        if (this.worker.getOwner() == null) {
            return false;
        }
        else
            return this.worker.getFollow() && !worker.needsToGetFood();
    }

    public boolean canContinueToUse() {
        return canUse();
    }

    public void tick() {
        if (this.worker.getOwner() != null){
            if(worker.getOwner().distanceToSqr(worker) > within) {
                this.worker.getLookControl().setLookAt(worker.getOwner());
                this.worker.getNavigation().moveTo(worker.getOwner().getX(), worker.getOwner().getY(), worker.getOwner().getZ(), this.speedModifier);
            }
        }
    }
}