package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.LumberjackEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.material.Material;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ITag;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.event.ForgeEventFactory;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Random;

public class LumberjackAI extends Goal {
    private final LumberjackEntity lumber;
    private BlockPos chopPos;
    private BlockPos plantPos;
    private BlockPos startPos;
    private boolean plant;
    private boolean chop;
    private int y;

    public LumberjackAI(LumberjackEntity lumber) {
        this.lumber = lumber;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    public boolean canUse() {
        if (this.lumber.getFollow()) {
            return false;
        } else if (this.lumber.getStartPos().isPresent() && !this.lumber.getFollow())
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
        this.startPos = new BlockPos(lumber.getStartPos().get().getX(), lumber.getStartPos().get().getY(), lumber.getStartPos().get().getZ());
        lumber.resetWorkerParameters();
        plant = false;//muss true sein später
        chop = true;//muss false sein später
        y = 0;
    }


    public void tick() {
        breakLeaves();

        if (plant){
            //plant

            chop = true;
            plant = false;
        }

        if (chop){

            this.chopPos = getWoodPos();
            if (chopPos != null) {
                this.lumber.getNavigation().moveTo(chopPos.getX(), chopPos.getY(), chopPos.getZ(), 0.75);
                this.lumber.getLookControl().setLookAt(chopPos.getX(), chopPos.getY() + 1, chopPos.getZ(), 10.0F, (float) this.lumber.getMaxHeadXRot());

                if (chopPos.closerThan(lumber.position(), 9)) {
                    this.mineBlock(chopPos);
                }
            }

            //chop = false;
            //plant = true;
        }




    }

    private void breakLeaves() {
        AxisAlignedBB boundingBox = this.lumber.getBoundingBox();
        double offset = 0.25D;
        BlockPos start = new BlockPos(boundingBox.minX - offset, boundingBox.minY - offset, boundingBox.minZ - offset);
        BlockPos end = new BlockPos(boundingBox.maxX + offset, boundingBox.maxY + offset, boundingBox.maxZ + offset);
        BlockPos.Mutable pos = new BlockPos.Mutable();
        boolean hasBroken = false;
        if (this.lumber.level.hasChunksAt(start, end)) {
            for (int i = start.getX(); i <= end.getX(); ++i) {
                for (int j = start.getY(); j <= end.getY(); ++j) {
                    for (int k = start.getZ(); k <= end.getZ(); ++k) {
                        pos.set(i, j, k);
                        BlockState blockstate = this.lumber.level.getBlockState(pos);
                        if (blockstate.getBlock() instanceof LeavesBlock) {
                            this.lumber.level.destroyBlock(pos, true, this.lumber);
                            hasBroken = true;
                        }

                    }
                }
            }
        }

        if (hasBroken) {
            this.lumber.level.playSound(null, this.lumber.getX(), this.lumber.getY(), this.lumber.getZ(), SoundEvents.GRASS_BREAK, SoundCategory.BLOCKS, 1F, 0.9F + 0.2F);
            this.lumber.workerSwingArm();
        }
    }

    public BlockPos getWoodPos() {
        int range = 1;

        for (int j = 0; j < range; j++){
            for (int i = 0; i < range; i++){
                for(int k = 0; k < 6; k++){
                    BlockPos blockpos1 = this.lumber.getStartPos().get().offset(j - range / 2F, k, i - range / 2F);
                    BlockState blockState = this.lumber.level.getBlockState(blockpos1);
                    Block block = blockState.getBlock();
                    if (block == Blocks.OAK_LOG) {
                        this.chopPos = blockpos1;

                        return blockpos1;
                    }
                }
            }
            if (range <= 16) range++;
        }

        return null;
    }

    public BlockPos getPlantPos() {
        int range = 1;

        for (int j = 0; j < range; j++){
            for (int i = 0; i < range; i++){
                for(int k = 0; k < 6; k++){
                    BlockPos blockpos1 = this.lumber.getStartPos().get().offset(j - range / 2F, k, i - range / 2F);
                    BlockState blockState = this.lumber.level.getBlockState(blockpos1);
                    Block block = blockState.getBlock();
                    if (block == Blocks.OAK_LOG) {
                        this.chopPos = blockpos1;

                        return blockpos1;
                    }
                }
            }
            if (range <= 16) range++;
        }

        return null;
    }

    private boolean mineBlock(BlockPos blockPos){
        if (this.lumber.isAlive() && ForgeEventFactory.getMobGriefingEvent(this.lumber.level, this.lumber) && !lumber.getFollow()) {

            BlockState blockstate = this.lumber.level.getBlockState(blockPos);
            Block block = blockstate.getBlock();

            if (lumber.wantsToBreak(block)){
                if (lumber.getCurrentTimeBreak() % 5 == 4) {
                    lumber.level.playLocalSound(blockPos.getX(), blockPos.getY(), blockPos.getZ(), blockstate.getSoundType().getHitSound(), SoundCategory.BLOCKS, 1F, 0.75F, false);
                }

                //set max destroy speed
                int bp = (int) (blockstate.getDestroySpeed(this.lumber.level, blockPos) * 100);
                this.lumber.setBreakingTime(bp);

                //increase current
                this.lumber.setCurrentTimeBreak(this.lumber.getCurrentTimeBreak() + (int) (1 * (this.lumber.getUseItem().getDestroySpeed(blockstate))));
                float f = (float) this.lumber.getCurrentTimeBreak() / (float) this.lumber.getBreakingTime();

                int i = (int) (f * 10);

                if (i != this.lumber.getPreviousTimeBreak()) {
                    this.lumber.level.destroyBlockProgress(1, blockPos, i);
                    this.lumber.setPreviousTimeBreak(i);
                }

                if (this.lumber.getCurrentTimeBreak() == this.lumber.getBreakingTime()) {
                    this.lumber.level.destroyBlock(blockPos, true, this.lumber);
                    this.lumber.setCurrentTimeBreak(-1);
                    this.lumber.setBreakingTime(0);
                    return true;
                }
                this.lumber.workerSwingArm();
            }
        }
        return false;
    }
}
