package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.core.BlockPos;

public class WorkerMoveToCampGoal<T extends LivingEntity> extends Goal {

    private final AbstractWorkerEntity worker;
    private final double within;

    public WorkerMoveToCampGoal(AbstractWorkerEntity worker, double within) {
        this.worker = worker;
        this.within = within;

    }

    public boolean canUse() {
        return !worker.getFollow() && !worker.getIsWorking();
    }

    public boolean canContinueToUse() {
        return this.canUse();
    }

    @Override
    public void tick() {
        super.tick();

        BlockPos blockpos = this.worker.getCampPos();
        if (this.worker.getCampPos() != null && canUse()) {
            if (!blockpos.closerThan(worker.position(), within))
            this.worker.getNavigation().moveTo(blockpos.getX(), blockpos.getY(), blockpos.getZ(), 1);
        }
    }
}