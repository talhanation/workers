package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.core.BlockPos;

public class WorkerMoveToHomeGoal<T extends LivingEntity> extends Goal {
    private final AbstractWorkerEntity worker;
    private final double within;

    public WorkerMoveToHomeGoal(AbstractWorkerEntity worker, double within) {
        this.worker = worker;
        this.within = within;
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
    public void tick() {
        super.tick();
        BlockPos blockpos = this.worker.getHomePos();
        if (blockpos != null && canUse()) {
            if (!blockpos.closerThan(worker.getOnPos(), within)) {
                this.worker.getNavigation().moveTo(
                    blockpos.getX(), 
                    blockpos.getY(), 
                    blockpos.getZ(), 
                    1
                );
            }
        }
    }
}
