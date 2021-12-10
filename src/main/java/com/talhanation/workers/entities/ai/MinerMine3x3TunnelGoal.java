package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.MinerEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ai.attributes.Attributes;
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
    private BlockPos standPos;
    private int blocks;
    private int side;

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
                this.minePos = new BlockPos(miner.getStartPos().get().getX() + blocks, miner.getStartPos().get().getY(), miner.getStartPos().get().getZ() - side);
                this.standPos = new BlockPos(minePos.getX() + 2, minePos.getY(), minePos.getZ());

            } else if (miner.getMineDirection().equals(Direction.WEST)) {
                this.minePos = new BlockPos(miner.getStartPos().get().getX() - blocks, miner.getStartPos().get().getY(), miner.getStartPos().get().getZ() + side);
                this.standPos = new BlockPos(minePos.getX() - 2, minePos.getY(), minePos.getZ());

            } else if (miner.getMineDirection().equals(Direction.NORTH)) {
                this.minePos = new BlockPos(miner.getStartPos().get().getX() - side, miner.getStartPos().get().getY(), miner.getStartPos().get().getZ() - blocks);
                this.standPos = new BlockPos(minePos.getX(), minePos.getY(), minePos.getZ() - 2);

            } else if (miner.getMineDirection().equals(Direction.SOUTH)) {
                this.minePos = new BlockPos(miner.getStartPos().get().getX() + side, miner.getStartPos().get().getY(), miner.getStartPos().get().getZ() + blocks);
                this.standPos = new BlockPos(minePos.getX(), minePos.getY(), minePos.getZ() + 2);
            }

            if (!minePos.closerThan(miner.position(), 2)){
                this.miner.getNavigation().moveTo(minePos.getX(), minePos.getY(), minePos.getZ(),1);
            }


            if (minePos.closerThan(miner.position(), 4)){
                this.miner.getLookControl().setLookAt(minePos.getX(), minePos.getY() + 1, minePos.getZ(), 10.0F, (float) this.miner.getMaxHeadXRot());
            }

            BlockState blockstate = miner.level.getBlockState(minePos);
            Block block1 = blockstate.getBlock();
            BlockState blockstate2 = miner.level.getBlockState(minePos.above());
            Block block2 = blockstate2.getBlock();
            BlockState blockstate3 = miner.level.getBlockState(minePos.above().above());
            Block block3 = blockstate3.getBlock();

            this.miner.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3D);

            if (minePos.closerThan(miner.position(), 6)) this.mineBlock(this.minePos);
            //miner.getOwner().sendMessage(new StringTextComponent("" + blocks + ""), miner.getOwner().getUUID());

            if (block1 == Blocks.AIR && block2 == Blocks.AIR && block3 == Blocks.AIR) {
                side++;
            }

            if (blocks == miner.getMineDepth()){
                miner.setIsWorking(false);
                miner.setStartPos(Optional.empty());
                blocks = 0;
            }

            if (side == 3){
                side = 0;
                this.miner.setIsPickingUp(true);
                blocks++;
            }

        }
    }

    private void mineBlock(BlockPos blockPos){
        if (this.miner.isAlive() && ForgeEventFactory.getMobGriefingEvent(this.miner.level, this.miner) && !miner.getFollow()) {
            BlockPos blockpos2 = blockPos.above();
            BlockPos blockpos3 = blockPos.above().above();
            BlockState blockstate = this.miner.level.getBlockState(blockPos);
            Block block = blockstate.getBlock();

            BlockState blockstate2 = this.miner.level.getBlockState(blockPos.above());
            Block block2 = blockstate2.getBlock();

            BlockState blockstate3 = this.miner.level.getBlockState(blockPos.above(2));
            Block block3 = blockstate3.getBlock();


            if (block != Blocks.AIR) {

                if (miner.getCurrentTimeBreak() % 5 == 4) {
                    miner.level.playLocalSound(blockPos.getX(), blockPos.getY(), blockPos.getZ(), blockstate.getSoundType().getHitSound(), SoundCategory.BLOCKS, 1F, 0.75F, false);
                }


                int bp = (int) (blockstate.getDestroySpeed(this.miner.level, blockPos) * 100);
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
            } else if (block2 != Blocks.AIR) {

                if (this.miner.getCurrentTimeBreak() % 5 == 4) {
                    miner.level.playLocalSound(blockpos2.getX(), blockpos2.getY(), blockpos2.getZ(), blockstate2.getSoundType().getHitSound(), SoundCategory.BLOCKS, 1F, 0.75F, false);
                }

                //set max destroy speed
                int bp = (int) (blockstate2.getDestroySpeed(this.miner.level, blockpos2.above()) * 100);
                this.miner.setBreakingTime(bp);

                //increase current
                this.miner.setCurrentTimeBreak(this.miner.getCurrentTimeBreak() + (int) (1 * (this.miner.getUseItem().getDestroySpeed(blockstate2))));
                float f = (float) this.miner.getCurrentTimeBreak() / (float) this.miner.getBreakingTime();

                int i = (int) (f * 10);

                if (i != this.miner.getPreviousTimeBreak()) {
                    this.miner.level.destroyBlockProgress(1, blockpos2, i);
                    this.miner.setPreviousTimeBreak(i);
                }

                if (this.miner.getCurrentTimeBreak() == this.miner.getBreakingTime()) {
                    this.miner.level.destroyBlock(blockpos2, true, this.miner);
                    this.miner.setCurrentTimeBreak(-1);
                    this.miner.setBreakingTime(0);
                }
                miner.changeTool(blockstate2);
                if (this.miner.getRandom().nextInt(5) == 0) {
                    if (!this.miner.swinging) {
                        this.miner.swing(this.miner.getUsedItemHand());
                    }
                }

            }else if (block3 != Blocks.AIR) {

                if (this.miner.getCurrentTimeBreak() % 5 == 4) {
                    miner.level.playLocalSound(blockpos3.getX(), blockpos3.getY(), blockpos3.getZ(), blockstate3.getSoundType().getHitSound(), SoundCategory.BLOCKS, 1F, 0.75F, false);
                }

                //set max destroy speed
                int bp = (int) (blockstate3.getDestroySpeed(this.miner.level, blockpos2.above()) * 100);
                this.miner.setBreakingTime(bp);

                //increase current
                this.miner.setCurrentTimeBreak(this.miner.getCurrentTimeBreak() + (int) (1 * (this.miner.getUseItem().getDestroySpeed(blockstate3))));
                float f = (float) this.miner.getCurrentTimeBreak() / (float) this.miner.getBreakingTime();

                int i = (int) (f * 10);

                if (i != this.miner.getPreviousTimeBreak()) {
                    this.miner.level.destroyBlockProgress(1, blockpos3, i);
                    this.miner.setPreviousTimeBreak(i);
                }

                if (this.miner.getCurrentTimeBreak() == this.miner.getBreakingTime()) {
                    this.miner.level.destroyBlock(blockpos3, true, this.miner);
                    this.miner.setCurrentTimeBreak(-1);
                    this.miner.setBreakingTime(0);
                }
                miner.changeTool(blockstate3);
                if (this.miner.getRandom().nextInt(5) == 0) {
                    if (!this.miner.swinging) {
                        this.miner.swing(this.miner.getUsedItemHand());
                    }
                }
            }
        }
    }

    public void restetCounts(){
        blocks = 0;
        side = 0;
    }

}
