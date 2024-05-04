package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

public class WorkerFollowOwnerGoal extends Goal {
    private final AbstractWorkerEntity worker;

    private LivingEntity owner;
    private final double speedModifier;
    private int timeToRecalcPath;
    private final float stopDistance;
    private final float startDistance;
    private long lastCanUseCheck;

    public WorkerFollowOwnerGoal(AbstractWorkerEntity worker, double speedModifier) {
        this.worker = worker;
        this.speedModifier = speedModifier;
        this.startDistance = 25;
        this.stopDistance = 5;
    }

    public boolean canUse() {
        long i = this.worker.getCommandSenderWorld().getGameTime();
        if (i - this.lastCanUseCheck >= 20L) {
            this.lastCanUseCheck = i;
            LivingEntity livingentity = this.worker.getOwner();
            if (livingentity == null) {
                return false;
            }
            else if (livingentity.isSpectator()) {
                return false;
            }
            else if (this.worker.distanceToSqr(livingentity) < startDistance) {
                return false;
            }
            else {
                this.owner = livingentity;
                return worker.getStatus() == AbstractWorkerEntity.Status.FOLLOW;
            }
        }
        return false;

    }

    public boolean canContinueToUse() {
        if (this.worker.getNavigation().isDone()) {
            return false;
        }
        return !(this.worker.distanceToSqr(this.owner) <= this.stopDistance) && worker.getStatus() == AbstractWorkerEntity.Status.FOLLOW;
    }

    public void start() {
        this.timeToRecalcPath = 0;
    }

    public void stop() {
        this.owner = null;
        this.worker.getNavigation().stop();
    }

    public void tick() {

        if (--this.timeToRecalcPath <= 0) {
            this.timeToRecalcPath = this.worker.getVehicle() != null ? this.adjustedTickDelay(5) : this.adjustedTickDelay(10);

            this.worker.getLookControl().setLookAt(this.owner, 10.0F, (float)this.worker.getMaxHeadXRot());
            this.worker.getNavigation().moveTo(this.owner, this.speedModifier);
        }
    }
}