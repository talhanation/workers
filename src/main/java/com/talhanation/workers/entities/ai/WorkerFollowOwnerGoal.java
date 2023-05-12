package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import com.talhanation.workers.entities.IBoatController;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import java.util.EnumSet;

public class WorkerFollowOwnerGoal extends Goal {
    private final AbstractWorkerEntity workerEntity;
    private LivingEntity owner;
    private final double speedModifier;
    private final PathNavigation navigation;
    private int timeToRecalcPath;
    private final float stopDistance;
    private final float startDistance;
    private float oldWaterCost;



    public WorkerFollowOwnerGoal(AbstractWorkerEntity abstractworkerEntity, double v, float startDistance, float stopDistance) {
        this.workerEntity = abstractworkerEntity;
        this.speedModifier = v;
        this.navigation = abstractworkerEntity.getNavigation();
        this.startDistance = startDistance;
        this.stopDistance = stopDistance;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    public boolean canUse() {
        LivingEntity owner = this.workerEntity.getOwner();
        if (
            owner == null ||
            owner.isSpectator() ||
            this.workerEntity.isSleeping() ||
            this.workerEntity.isOrderedToSit() ||
            this.workerEntity.distanceToSqr(owner) < (double)(this.startDistance * this.startDistance)
        ) {
            return false;
        }
        this.owner = owner;
        return this.workerEntity.getFollow();
    }

    public boolean canContinueToUse() {
        if (
            this.navigation.isDone() ||
            !this.workerEntity.isSleeping() ||
            this.workerEntity.isOrderedToSit() ||
            !this.workerEntity.getFollow()
         ) {
            return false;
        }
        return this.workerEntity.distanceTo(this.owner) <= this.stopDistance;
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
        this.workerEntity.getLookControl().setLookAt(this.owner, 10.0F, (float)this.workerEntity.getMaxHeadXRot());
        if (--this.timeToRecalcPath <= 0) {
            this.timeToRecalcPath = 10;
            if (!this.workerEntity.isLeashed() && !this.workerEntity.isPassenger()) {
                this.navigation.moveTo(this.owner, this.speedModifier);
            }

        }
        /*
        if(this.workerEntity instanceof IBoatController sailor){
            sailor.setSailPos(owner.getOnPos());
        }
         */
    }
}