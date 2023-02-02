package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.MinerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;

public class MinerMine8x8RoomGoal extends MinerMineGoal {
    private final double within;
    private BlockPos minePos;

    public MinerMine8x8RoomGoal(MinerEntity miner, double within) {
        this.miner = miner;
        this.within = within;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    public boolean canUse() {
        if (this.miner.getStartPos() == null) {
            return false;
        }
        if (this.miner.getFollow()) {
            return false;

        } else return this.miner.getStartPos().closerThan(miner.getOnPos(), within) && !this.miner.getFollow() && miner.getMineType() == 5;
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
        miner.resetWorkerParameters();
    }

    public void tick() {
        if (miner.getFollow() || !miner.getIsWorking()){
            resetCounts();
        }

        if (!miner.getFollow()) {
            if (miner.getMineDirection().equals(Direction.EAST)) {
                this.minePos = new BlockPos(miner.getStartPos().getX() + x, miner.getStartPos().getY() + y, miner.getStartPos().getZ() - z);
                //this.standPos = new BlockPos(minePos.getX() + 2, minePos.getY(), minePos.getZ());

            } else if (miner.getMineDirection().equals(Direction.WEST)) {
                this.minePos = new BlockPos(miner.getStartPos().getX() - x, miner.getStartPos().getY() + y, miner.getStartPos().getZ() + z);
                //this.standPos = new BlockPos(minePos.getX() - 2, minePos.getY(), minePos.getZ());

            } else if (miner.getMineDirection().equals(Direction.NORTH)) {
                this.minePos = new BlockPos(miner.getStartPos().getX() - z, miner.getStartPos().getY() + y, miner.getStartPos().getZ() - x);
                //this.standPos = new BlockPos(minePos.getX(), minePos.getY(), minePos.getZ() - 2);

            } else if (miner.getMineDirection().equals(Direction.SOUTH)) {
                this.minePos = new BlockPos(miner.getStartPos().getX() + z, miner.getStartPos().getY() + y, miner.getStartPos().getZ() + x);
                //this.standPos = new BlockPos(minePos.getX(), minePos.getY(), minePos.getZ() + 2);
            }

            if (!minePos.closerThan(miner.getOnPos(), 2) && !miner.getIsPickingUp()){
                this.miner.getNavigation().moveTo(minePos.getX(), minePos.getY(), minePos.getZ(),1);
            }

            if (minePos.closerThan(miner.getOnPos(), 4)){
                this.miner.getLookControl().setLookAt(minePos.getX(), minePos.getY() + 1, minePos.getZ(), 10.0F, (float) this.miner.getMaxHeadXRot());
            }
            BlockState blockstate = miner.level.getBlockState(minePos);
            Block block1 = blockstate.getBlock();
            AttributeInstance movSpeed = this.miner.getAttribute(Attributes.MOVEMENT_SPEED);
            if (movSpeed != null) movSpeed.setBaseValue(0.3D);

            if (minePos.closerThan(miner.getOnPos(), 6)) this.mineBlock(this.minePos);

            if (miner.shouldIgnoreBlock(block1)) {
                y++;
            }

            if (y == 3){
                y = 0;
                z++;
            }

            if (z == 8){
                z = 0;
                this.miner.setIsPickingUp(true);
                x++;
            }

            if (x == 8){
                miner.setIsWorking(false);
                miner.clearStartPos();
                x = 0;
            }
        }
    }
    public void resetCounts(){
        x = 0;
        z = 0;
        y = 0;
    }
}
