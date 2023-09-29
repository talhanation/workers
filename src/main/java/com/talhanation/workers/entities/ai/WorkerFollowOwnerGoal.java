package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import com.talhanation.workers.entities.IBoatController;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.vehicle.Boat;

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
        LivingEntity owner =this.worker.getOwner();
        if (owner != null){
            if(owner.distanceToSqr(worker) > within) {
                if(worker instanceof IBoatController sailor && worker.getVehicle() instanceof Boat){
                    sailor.setSailPos(owner.getOnPos());
                }
                else {

                    this.worker.getNavigation().moveTo(owner.getX(), owner.getY(), owner.getZ(), this.speedModifier);
                }
                this.worker.getLookControl().setLookAt(owner);
            }
        }
    }
}