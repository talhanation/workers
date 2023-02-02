package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.MinerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.ForgeEventFactory;

import java.util.EnumSet;

public class MinerMine8x8x1FlatGoal extends Goal {
    private final MinerEntity miner;
    private final double within;
    private BlockPos minePos;
    private int blocks;
    private int side;

    public MinerMine8x8x1FlatGoal(MinerEntity miner, double v, double within) {
        this.miner = miner;
        this.within = within;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    public boolean canUse() {
        if (this.miner.getStartPos() == null) {
            return false;
        }
        if (this.miner.getFollow()) {
            return false;

        } else if (this.miner.getStartPos().closerThan(miner.getOnPos(), within) && !this.miner.getFollow() && miner.getMineType() == 4)

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
        resetCounts();
    }

    public void tick() {
        if (miner.getFollow() || !miner.getIsWorking()){
            resetCounts();
        }

        if (!miner.getFollow()) {
            if (miner.getMineDirection().equals(Direction.EAST)) {
                this.minePos = new BlockPos(miner.getStartPos().getX() + blocks, miner.getStartPos().getY(), miner.getStartPos().getZ() - side);

            } else if (miner.getMineDirection().equals(Direction.WEST)) {
                this.minePos = new BlockPos(miner.getStartPos().getX() - blocks, miner.getStartPos().getY(), miner.getStartPos().getZ() + side);

            } else if (miner.getMineDirection().equals(Direction.NORTH)) {
                this.minePos = new BlockPos(miner.getStartPos().getX() - side, miner.getStartPos().getY(), miner.getStartPos().getZ() - blocks);

            } else if (miner.getMineDirection().equals(Direction.SOUTH)) {
                this.minePos = new BlockPos(miner.getStartPos().getX() + side, miner.getStartPos().getY(), miner.getStartPos().getZ() + blocks);
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
            //erst mienen wenn nah genug
            if (minePos.closerThan(miner.getOnPos(), 6)) this.mineBlock(this.minePos);

            if (miner.shouldIgnoreBlock(block1)) {
                blocks++;
            }

            if (blocks == 8) {
                blocks = 0;
                side++;
                this.miner.setIsPickingUp(true);
            }

            if (side == 8){
                miner.clearStartPos();
                miner.setIsWorking(false);
            }
        }
    }

    private void mineBlock(BlockPos blockPos){
        if (this.miner.isAlive() && ForgeEventFactory.getMobGriefingEvent(this.miner.level, this.miner) && !miner.getFollow()) {
            BlockState blockstate = this.miner.level.getBlockState(blockPos);
            Block block = blockstate.getBlock();

            if (!miner.shouldIgnoreBlock(block)) {

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

    public void resetCounts(){
        blocks = 0;
        side = 0;
    }

}
