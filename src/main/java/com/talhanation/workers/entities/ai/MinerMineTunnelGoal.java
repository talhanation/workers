package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.MinerEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

import java.util.EnumSet;
import java.util.Optional;

public class MinerMineTunnelGoal extends Goal {
    private final MinerEntity miner;
    private final double speedModifier;
    private final double within;
    private BlockPos minePos;
    private int blocks = 0;

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
        } else if (this.miner.getStartPos().get().closerThan(miner.position(), within) && !this.miner.getFollow())
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
        if (!miner.getFollow()) {
            if (miner.getMineDirectrion().equals(Direction.EAST)) {

                this.minePos = new BlockPos(miner.getStartPos().get().getX() + blocks, miner.getStartPos().get().getY(), miner.getStartPos().get().getZ());
            } else if (miner.getMineDirectrion().equals(Direction.WEST)) {
                this.minePos = new BlockPos(miner.getStartPos().get().getX() - blocks, miner.getStartPos().get().getY(), miner.getStartPos().get().getZ());
            } else if (miner.getMineDirectrion().equals(Direction.NORTH)) {
                this.minePos = new BlockPos(miner.getStartPos().get().getX(), miner.getStartPos().get().getY(), miner.getStartPos().get().getZ() - blocks);
            } else if (miner.getMineDirectrion().equals(Direction.SOUTH)) {
                this.minePos = new BlockPos(miner.getStartPos().get().getX(), miner.getStartPos().get().getY(), miner.getStartPos().get().getZ() + blocks);
            }


            this.miner.getNavigation().moveTo(minePos.getX(), minePos.getY(), minePos.getZ(), this.speedModifier);

            if (minePos.closerThan(miner.position(), 2.5)){
                miner.getNavigation().stop();
                this.miner.getLookControl().setLookAt(minePos.getX(), minePos.getY(), minePos.getZ(), 10.0F, (float) this.miner.getMaxHeadXRot());
            }

            BlockState blockstate = miner.level.getBlockState(minePos);
            Block block1 = blockstate.getBlock();
            BlockState blockstate2 = miner.level.getBlockState(minePos.above());
            Block block2 = blockstate2.getBlock();

            this.miner.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3D);
            //erst mienen wenn nah genug
            if (minePos.closerThan(miner.position(), 5)) this.miner.mineBlock(this.minePos);
            //miner.getOwner().sendMessage(new StringTextComponent("" + blocks + ""), miner.getOwner().getUUID());

            if (block1 == Blocks.AIR && block2 == Blocks.AIR) {

                blocks++;
            }

            if (blocks == 9){
                miner.setIsWorking(false);
                miner.setStartPos(Optional.empty());
                blocks = 0;
            }

        }
    }
}