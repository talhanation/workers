package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class WorkerFleeGoal extends Goal {

    private static final int FLEE_TIMEOUT_TICKS = 24000; // 1 in-game day max

    public final AbstractWorkerEntity worker;
    private int tickCounter;

    public WorkerFleeGoal(AbstractWorkerEntity worker) {
        this.worker = worker;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return worker.isFleeing && worker.fleeTarget != null;
    }

    @Override
    public boolean canContinueToUse() {
        return worker.isFleeing && worker.fleeTarget != null;
    }

    @Override
    public void start() {
        super.start();
        tickCounter = 0;
        worker.setFollowState(6);
    }

    @Override
    public void stop() {
        super.stop();
        worker.stopFleeing();
        worker.getNavigation().stop();
    }

    @Override
    public void tick() {
        super.tick();
        if (worker.getCommandSenderWorld().isClientSide()) return;
        if (worker.fleeTarget == null) return;

        tickCounter++;

        // Re-issue navigation every 40 ticks in case path was interrupted
        if (tickCounter % 40 == 0) {
            BlockPos target = worker.fleeTarget;
            worker.getNavigation().moveTo(target.getX(), target.getY(), target.getZ(), 1.3D);
        }

        // Reached the flee destination → stop fleeing
        if (worker.fleeTarget.distToCenterSqr(worker.position()) < 64) {
            worker.stopFleeing();
            return;
        }

        // Safety timeout — give up after 1 in-game day
        if (tickCounter > FLEE_TIMEOUT_TICKS) {
            worker.stopFleeing();
        }
    }
}
