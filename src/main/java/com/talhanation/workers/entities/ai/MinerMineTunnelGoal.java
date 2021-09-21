package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.MinerEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
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
    private BlockPos minePos;
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

        else if (this.miner.getDestPos().get().closerThan(miner.position(), within) && !this.miner.getFollow())
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
        //miner.setNextStep(true);
    }

    public void tick() {
        if (!miner.getFollow()) {
            this.minePos = new BlockPos(miner.getStartPos().get().getX() + blocks, miner.getStartPos().get().getY(), miner.getStartPos().get().getZ());
            this.miner.getNavigation().moveTo(minePos.getX(), minePos.getY(), minePos.getZ(), this.speedModifier);
            BlockState blockstate = miner.level.getBlockState(minePos);
            Block block1 = blockstate.getBlock();
            BlockState blockstate2 = miner.level.getBlockState(minePos.above());
            Block block2 = blockstate2.getBlock();

            this.miner.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.4D);
            this.miner.mineBlock(this.minePos);

            if (block1 == Blocks.AIR && block2 == Blocks.AIR) {
                blocks++;
            }
        }

        if (miner.getFollow()){
            miner.setStartPos(Optional.empty());
        }
    }
}