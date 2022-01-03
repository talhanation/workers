package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.MinerEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.ForgeEventFactory;

import java.util.EnumSet;
import java.util.Optional;

public class MinerMine3x3TunnelGoal extends Goal {
    private final MinerEntity miner;
    private final double speedModifier;
    private final double within;
    private BlockPos minePos;
    private int x;
    private int y;
    private int z;

    public MinerMine3x3TunnelGoal(MinerEntity miner, double v, double within) {
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
       } else if (this.miner.getStartPos().get().closerThan(miner.position(), within) && !this.miner.getFollow() && miner.getMineType() == 2)

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
        miner.resetWorkerParameters();
    }

    @Override
    public void stop() {
        super.stop();
        restetCounts();
    }

    public void tick() {
        if (miner.getFollow() || !miner.getIsWorking()){
            restetCounts();
        }

        if (!miner.getFollow()) {
            if (miner.getMineDirection().equals(Direction.EAST)) {
                this.minePos = new BlockPos(miner.getStartPos().get().getX() + x, miner.getStartPos().get().getY() + y, miner.getStartPos().get().getZ() - z);

            } else if (miner.getMineDirection().equals(Direction.WEST)) {
                this.minePos = new BlockPos(miner.getStartPos().get().getX() - x, miner.getStartPos().get().getY() + y, miner.getStartPos().get().getZ() + z);

            } else if (miner.getMineDirection().equals(Direction.NORTH)) {
                this.minePos = new BlockPos(miner.getStartPos().get().getX() - z, miner.getStartPos().get().getY() + y, miner.getStartPos().get().getZ() - x);

            } else if (miner.getMineDirection().equals(Direction.SOUTH)) {
                this.minePos = new BlockPos(miner.getStartPos().get().getX() + z, miner.getStartPos().get().getY() + y, miner.getStartPos().get().getZ() + x);
            }

            if (!minePos.closerThan(miner.position(), 2)){
                this.miner.getNavigation().moveTo(minePos.getX(), minePos.getY(), minePos.getZ(),1);
            }

            if (minePos.closerThan(miner.position(), 4)){
                this.miner.getLookControl().setLookAt(minePos.getX(), minePos.getY() + 1, minePos.getZ(), 10.0F, (float) this.miner.getMaxHeadXRot());
            }

            BlockState blockstate = miner.level.getBlockState(minePos);
            Block block1 = blockstate.getBlock();

            if (minePos.closerThan(miner.position(), 6)) this.mineBlock(this.minePos);


            if (miner.shouldIgnorBlock(block1)) {
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
                miner.setStartPos(Optional.empty());
                x = 0;
            }


        }
    }

    private void mineBlock(BlockPos blockPos){

        if (this.miner.isAlive() && ForgeEventFactory.getMobGriefingEvent(this.miner.level, this.miner) && !miner.getFollow()) {
            BlockState blockstate = this.miner.level.getBlockState(blockPos);
            Block block = blockstate.getBlock();

            if (!miner.shouldIgnorBlock(block)) {

                if (miner.getCurrentTimeBreak() % 5 == 4) {
                    miner.level.playLocalSound(blockPos.getX(), blockPos.getY(), blockPos.getZ(), blockstate.getSoundType().getHitSound(), SoundCategory.BLOCKS, 1F, 0.75F, false);
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

    public void restetCounts(){
        x = 0;
        y = 0;
        z = 0;
    }

}
