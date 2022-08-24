package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.LevelReader;

import java.util.EnumSet;

public class WorkerFollowOwnerGoal extends Goal {
    private final AbstractWorkerEntity workerEntity;
    private LivingEntity owner;
    private final LevelReader level;
    private final double speedModifier;
    private final PathNavigation navigation;
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
        } else if (this.workerEntity.isSleeping()) {
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
        } else if (!this.workerEntity.isSleeping()) {
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
        this.oldWaterCost = this.workerEntity.getPathfindingMalus(BlockPathTypes.WATER);
        this.workerEntity.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
    }

    public void stop() {
        this.owner = null;
        this.navigation.stop();
        this.workerEntity.setPathfindingMalus(BlockPathTypes.WATER, this.oldWaterCost);
    }

    public void tick() {
        this.navigation.getNodeEvaluator().setCanPassDoors(true);
        this.navigation.getNodeEvaluator().setCanOpenDoors(true);

        this.workerEntity.getLookControl().setLookAt(this.owner, 10.0F, (float)this.workerEntity.getMaxHeadXRot());
        if (--this.timeToRecalcPath <= 0) {
            this.timeToRecalcPath = 10;
            if (!this.workerEntity.isLeashed() && !this.workerEntity.isPassenger()) {
                this.navigation.moveTo(this.owner, this.speedModifier);
            }

        }
    }
}