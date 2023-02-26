package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.MinerEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

import java.util.EnumSet;

public class MinerMine3x3TunnelGoal extends MinerMineGoal {
    private final double within;
    private BlockPos minePos;

    public MinerMine3x3TunnelGoal(MinerEntity miner, double v, double within) {
        this.miner = miner;
        this.within = within;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    public boolean canUse() {
        return this.miner.canWork() && miner.getMineType() == 2;
    }

    public boolean canContinueToUse() {
        return this.canUse();
    }

    @Override
    public void start() {
        super.start();
        miner.resetWorkerParameters();
    }

    @Override
    public void stop() {
        super.stop();
        resetCounts();
    }

    public void tick() {
        if (miner.getFollow() || !miner.getIsWorking()){
            resetCounts();
        }

        if (!miner.getFollow()) {
            if (miner.getMineDirection().equals(Direction.EAST)) {
                this.minePos = new BlockPos(miner.getStartPos().getX() + x, miner.getStartPos().getY() + y, miner.getStartPos().getZ() - z);

            } else if (miner.getMineDirection().equals(Direction.WEST)) {
                this.minePos = new BlockPos(miner.getStartPos().getX() - x, miner.getStartPos().getY() + y, miner.getStartPos().getZ() + z);

            } else if (miner.getMineDirection().equals(Direction.NORTH)) {
                this.minePos = new BlockPos(miner.getStartPos().getX() - z, miner.getStartPos().getY() + y, miner.getStartPos().getZ() - x);

            } else if (miner.getMineDirection().equals(Direction.SOUTH)) {
                this.minePos = new BlockPos(miner.getStartPos().getX() + z, miner.getStartPos().getY() + y, miner.getStartPos().getZ() + x);
            }

            if (!minePos.closerThan(miner.getOnPos(), 2)){
                this.miner.getNavigation().moveTo(minePos.getX(), minePos.getY(), minePos.getZ(),1);
            }

            if (minePos.closerThan(miner.getOnPos(), 4)){
                this.miner.getLookControl().setLookAt(minePos.getX(), minePos.getY() + 1, minePos.getZ(), 10.0F, (float) this.miner.getMaxHeadXRot());
            }

            BlockState blockstate = miner.level.getBlockState(minePos);
            Block block1 = blockstate.getBlock();

            if (minePos.closerThan(miner.getOnPos(), 6)) this.mineBlock(this.minePos);


            if (miner.shouldIgnoreBlock(block1)) {
                y++;
            }

            if (y == 3){
                y = 0;
                z++;
            }

            if (z == 3){
                z = 0;
                this.miner.setIsPickingUp(true);
                x++;
            }

            if (x == miner.getMineDepth()){
                miner.setIsWorking(false);
                miner.clearStartPos();
                x = 0;
            }


        }
    }

    public void resetCounts(){
        x = 0;
        y = 0;
        z = 0;
    }

}
