package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.MinerEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.Goal;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

import java.util.EnumSet;
import java.util.Optional;

public class MinerMineTunnelGoal extends Goal {
    private final MinerEntity miner;
    private final double speedModifier;
    private final double within;
    private int blocks = 1;

    public MinerMineTunnelGoal(MinerEntity miner, double v, double within) {
        this.miner = miner;
        this.speedModifier = v;
        this.within = within;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }
    public boolean canUse() {
        if (!this.miner.getStartPos().isPresent()) {
            return false;
        }
        if (this.miner.getFollow()) {
            return false;
        }

        else if (this.miner.getDestPos().get().closerThan(miner.position(), within))
            return true;

        else
            return false;
    }

    public boolean canContinueToUse() {
        return this.canUse();
    }

    @Override
    public void start() {
        super.start();
    }

    public void tick() {

        this.miner.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.4D);
        BlockPos Pos = new BlockPos(miner.getStartPos().get().getX() + blocks, miner.getStartPos().get().getY(), miner.getStartPos().get().getZ());
        this.miner.getNavigation().moveTo(Pos.getX(), Pos.getY(), Pos.getZ(), this.speedModifier);
        this.miner.mineBlock(Pos);
        //mehr blöcke in schleife ohne for
    }
}