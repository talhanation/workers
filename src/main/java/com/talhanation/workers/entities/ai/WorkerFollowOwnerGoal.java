package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.pathfinding.PathNavigator;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.world.IWorldReader;

import java.util.EnumSet;

public class WorkerFollowOwnerGoal extends Goal {
    private final AbstractWorkerEntity workerEntity;
    private LivingEntity owner;
    private final IWorldReader level;
    private final double speedModifier;
    private final PathNavigator navigation;
    private int timeToRecalcPath;
    private final float stopDistance;
    private final float startDistance;
    private float oldWaterCost;



    public WorkerFollowOwnerGoal(AbstractWorkerEntity abstractworkerEntity, double v, float startDistance, float stopDistance) {
        this.workerEntity = abstractworkerEntity;
        this.level = abstractworkerEntity.level;
        this.speedModifier = v;
        this.navigation = abstractworkerEntity.getNavigation();
        this.startDistance = startDistance;
        this.stopDistance = stopDistance;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    public boolean canUse() {
        LivingEntity owner = this.workerEntity.getOwner();
        if (owner == null) {
            return false;
        } else if (owner.isSpectator()) {
            return false;
        } else if (!this.workerEntity.getFollow()) {
            return false;
        } else if (this.workerEntity.isOrderedToSit()) {
            return false;
        } else if (this.workerEntity.distanceToSqr(owner) < (double)(this.startDistance * this.startDistance)) {
            return false;
        }
        else {
            this.owner = owner;
            return workerEntity.getFollow();
        }
    }

    public boolean canContinueToUse() {
        if (this.navigation.isDone()) {
            return false;
        } else if (this.workerEntity.isOrderedToSit()) {
            return false;
        } else if (!this.workerEntity.getFollow()) {
            return false;
        }
        else {
            return !(this.workerEntity.distanceToSqr(this.owner) <= (double)(this.stopDistance * this.stopDistance));
        }

    }

    public void start() {
        this.timeToRecalcPath = 0;
        this.oldWaterCost = this.workerEntity.getPathfindingMalus(PathNodeType.WATER);
        this.workerEntity.setPathfindingMalus(PathNodeType.WATER, 0.0F);
    }

    public void stop() {
        this.owner = null;
        this.navigation.stop();
        this.workerEntity.setPathfindingMalus(PathNodeType.WATER, this.oldWaterCost);
    }

    public void tick() {
        this.workerEntity.getLookControl().setLookAt(this.owner, 10.0F, (float)this.workerEntity.getMaxHeadXRot());
        if (--this.timeToRecalcPath <= 0) {
            this.timeToRecalcPath = 10;
            if (!this.workerEntity.isLeashed() && !this.workerEntity.isPassenger()) {
                this.navigation.moveTo(this.owner, this.speedModifier);
            }

        }
    }
}