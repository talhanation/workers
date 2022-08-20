package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.MinerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.ForgeEventFactory;

public abstract class MinerMineGoal extends Goal {

    public int x;
    public int y;
    public int z;
    public MinerEntity miner;
    public BlockPos minePos;
    public BlockPos startPos;
    public MinerMineGoal(){

    }

    public boolean canUse() {
        if (this.miner.getStartPos() == null) {
            return false;
        }
        else if (this.miner.getFollow()) {
            return false;
        }
        else if (!this.miner.level.isDay()) {
            return false;
        }
        else
            return this.miner.getStartPos().closerThan(miner.getOnPos(), 16D) && !this.miner.getFollow() && miner.getMineType() == 1;
    }

    @Override
    public void start() {
        this.miner.resetWorkerParameters();
        this.miner.setIsWorking(true);
        this.startPos = miner.getStartPos();
        this.minePos = miner.getStartPos();
        x = 0;
        y = 0;
        z = 0;
    }

    public void moveToPosition(){
        if (!minePos.closerThan(miner.getOnPos(), 4)){
            this.miner.getNavigation().moveTo(minePos.getX(), minePos.getY(), minePos.getZ(),1);
        }
        if (minePos.closerThan(miner.getOnPos(), 4)){
            this.miner.getLookControl().setLookAt(minePos.getX(), minePos.getY() + 1, minePos.getZ(), 10.0F, (float) this.miner.getMaxHeadXRot());
        }
    }

    public void mineBlock(BlockPos blockPos){

        if (this.miner.isAlive() && ForgeEventFactory.getMobGriefingEvent(this.miner.level, this.miner) && !miner.getFollow()) {
            BlockState blockstate = this.miner.level.getBlockState(blockPos);
            Block block = blockstate.getBlock();

            if (!miner.shouldIgnorBlock(block)) {

                if (miner.getCurrentTimeBreak() % 5 == 4) {
                    miner.level.playLocalSound(blockPos.getX(), blockPos.getY(), blockPos.getZ(), blockstate.getSoundType().getHitSound(), SoundSource.BLOCKS, 1F, 0.75F, false);
                }


                int bp = (int) (blockstate.getDestroySpeed(this.miner.level, blockPos) * 10);
                this.miner.setBreakingTime(bp);

                this.miner.setCurrentTimeBreak(this.miner.getCurrentTimeBreak() + (int) (1 * (this.miner.getUseItem().getDestroySpeed(blockstate))));
                float f = (float) this.miner.getCurrentTimeBreak() / (float) this.miner.getBreakingTime();

                int i = (int) (f * 10);

                if (i != this.miner.getPreviousTimeBreak()) {
                    this.miner.level.destroyBlockProgress(1, blockPos, i);
                    this.miner.setPreviousTimeBreak(i);
                }

                if (this.miner.getCurrentTimeBreak() == this.miner.getBreakingTime()) {
                    this.miner.level.destroyBlock(blockPos, true, this.miner);
                    this.miner.setCurrentTimeBreak(-1);
                    this.miner.setBreakingTime(0);
                }
                miner.changeTool(blockstate);
                if (this.miner.getRandom().nextInt(5) == 0) {
                    if (!this.miner.swinging) {
                        this.miner.swing(this.miner.getUsedItemHand());
                    }
                }
            }
        }
    }

    public BlockPos getBlockPositionsFromDirection(int x, int y, int z, BlockPos startPos, Direction direction){
        BlockPos pos = null;
        if (direction.equals(Direction.EAST)) {
            pos = new BlockPos(startPos.getX() + x, startPos.getY() + y, startPos.getZ() - z);

        } else if (direction.equals(Direction.WEST)) {
            pos = new BlockPos(startPos.getX() - x, startPos.getY() + y, startPos.getZ() + z);

        } else if (direction.equals(Direction.NORTH)) {
            pos = new BlockPos(startPos.getX() - z, startPos.getY() + y, startPos.getZ() - x);

        } else if (direction.equals(Direction.SOUTH)) {
            pos = new BlockPos(startPos.getX() + z, startPos.getY() + y, startPos.getZ() + x);
        }
        return pos;
    }
}
// for clean up: this is upperClass for all mine goals