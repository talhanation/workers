package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.MinerEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

import java.util.EnumSet;

public class MinerMineTunnelGoal extends MinerMineGoal {
    private BlockPos minePos;

    public MinerMineTunnelGoal(MinerEntity miner, double v) {
        this.miner = miner;

        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return super.canUse();
    }

    public boolean canContinueToUse() {
        return this.canUse();
    }

    @Override
    public void start() {
        super.start();
    }

    public void tick() {


        if (miner.getFollow() || !miner.getIsWorking()){
            this.start();
        }

        if (!miner.getFollow() && miner.getIsWorking()){
            if (miner.getMineDirection().equals(Direction.EAST)) {
                this.minePos = new BlockPos(this.startPos.getX() + x, this.startPos.getY() + y, this.startPos.getZ() - z);

            } else if (miner.getMineDirection().equals(Direction.WEST)) {
                this.minePos = new BlockPos(this.startPos.getX() - x, this.startPos.getY() + y, this.startPos.getZ() + z);

            } else if (miner.getMineDirection().equals(Direction.NORTH)) {
                this.minePos = new BlockPos(this.startPos.getX() - z, this.startPos.getY() + y, this.startPos.getZ() - x);

            } else if (miner.getMineDirection().equals(Direction.SOUTH)) {
                this.minePos = new BlockPos(this.startPos.getX() + z, this.startPos.getY() + y, this.startPos.getZ() + x);
            }

            BlockState blockstate = miner.level.getBlockState(minePos);
            Block block = blockstate.getBlock();

            this.moveToPosition();

            if (minePos.closerThan(miner.getOnPos(), 6)) {
                this.mineBlock(this.minePos);

            }

            if (miner.shouldIgnoreBlock(block)) {
                y++;
            }

            if (y == 2){
                y = 0;
                x ++;
            }

            if (x == miner.getMineDepth()){
                this.start();
            }
        }
    }
}