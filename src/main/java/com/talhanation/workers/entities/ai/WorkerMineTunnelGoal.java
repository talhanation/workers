package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.Goal;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

import java.util.EnumSet;
import java.util.Optional;

public class WorkerMineTunnelGoal extends Goal {
    private final AbstractWorkerEntity worker;
    private final double speedModifier;
    private final double within;

    public WorkerMineTunnelGoal(AbstractWorkerEntity worker, double v, double within) {
        this.worker = worker;
        this.speedModifier = v;
        this.within = within;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }
    public boolean canUse() {
        if (!this.worker.getStartPos().isPresent()) {
            return false;
        }
        if (this.worker.getFollow()) {
            return false;
        }
        else if (this.worker.getDestPos().get().closerThan(worker.position(), within))

            return true;
        else
            return false;
    }

    public boolean canContinueToUse() {
        return this.canUse();
    }

    public void tick() {
        Optional<BlockPos> DestPos = this.worker.getDestPos();
        Optional<BlockPos> StartPos = this.worker.getStartPos();

        DestPos.ifPresent(blockPos -> this.worker.getNavigation().moveTo(blockPos.getX(), blockPos.getY(), blockPos.getZ(), this.speedModifier));

        this.worker.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.1D);

    }
}