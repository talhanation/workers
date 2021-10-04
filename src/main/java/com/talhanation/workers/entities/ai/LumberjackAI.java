package com.talhanation.workers.entities.ai;

import com.talhanation.workers.CommandEvents;
import com.talhanation.workers.entities.AbstractWorkerEntity;
import com.talhanation.workers.entities.LumberjackEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.Items;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.event.ForgeEventFactory;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

public class LumberjackAI extends Goal {
    private final LumberjackEntity lumber;
    private final double speedModifier;
    private final double within;
    private BlockPos minePos;
    private int y;
    private int x;
    private int z;

    public LumberjackAI(LumberjackEntity lumber, double v, double within) {
        this.lumber = lumber;
        this.speedModifier = v;
        this.within = within;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    public boolean canUse() {
        if (!this.lumber.getStartPos().isPresent()) {
            return false;
        }
        if (this.lumber.getFollow()) {
            return false;
        } else if (lumber.getIsWorking() && !this.lumber.getFollow())
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
        x = -9;
        z = -9;
    }

    public void tick() {
        lumber.getOwner().sendMessage(new StringTextComponent("x: " + x + ""), lumber.getOwner().getUUID());
        lumber.getOwner().sendMessage(new StringTextComponent("y: " + y + ""), lumber.getOwner().getUUID());
        lumber.getOwner().sendMessage(new StringTextComponent("z: " + z + ""), lumber.getOwner().getUUID());
        if (lumber.getFollow() || !lumber.getIsWorking()) {
            x = -9;
            z = -9;
        }

        this.minePos = new BlockPos(lumber.getStartPos().get().getX() + x, lumber.getStartPos().get().getY() + y, lumber.getStartPos().get().getZ() + z);

        BlockState blockstate = lumber.level.getBlockState(minePos);
        Block block = blockstate.getBlock();
        if (block == Blocks.SPRUCE_LOG) {
            this.lumber.getNavigation().moveTo(minePos.getX(), minePos.getY(), minePos.getZ(),0.5);
        }

        if (block == Blocks.SPRUCE_LOG && minePos.closerThan(lumber.position(), 9)){
            this.mineBlock(minePos);
            this.lumber.getLookControl().setLookAt(minePos.getX(), minePos.getY() + 1, minePos.getZ(), 10.0F, (float) this.lumber.getMaxHeadXRot());


        }

        if (block != Blocks.SPRUCE_LOG) {
            List<ItemEntity> list = lumber.level.getEntitiesOfClass(ItemEntity.class, this.lumber.getBoundingBox().inflate(16.0D));
            for (ItemEntity items : list) {
                if (items.getItem() == Items.SPRUCE_LOG.getDefaultInstance())
                this.lumber.getNavigation().moveTo(items.position().x, items.position().y, items.position().z,1);
            }
            y++;
        }

        if (y == 9){
            y = -2;
            x++;
        }

        if (x == 9){
            x = -9;
            z++;
        }

        if (z == 9){
            z = -9;
           lumber.setIsWorking(false);
        }
    }
/*
    public void checkForWood(){
        BlockPos woodPos = new BlockPos(standPos.getX() + blocks -8, standPos.getY() + 1, standPos.getZ() -8 + side);

        BlockState blockstate = lumber.level.getBlockState(minePos);
        Material blockstateMaterial = blockstate.getMaterial();

        if (blockstateMaterial != Material.WOOD) {
            blocks++;
        }
        else this.minePos = woodPos;

        if (blocks == 16){
            blocks = 0;
            side++;
        }

        if (side == 16){
            side = 0;
            blocks++;
        }
    }

    private void chopWood(BlockPos blockPos){
        if (this.lumber.isAlive() && ForgeEventFactory.getMobGriefingEvent(this.lumber.level, this.lumber) && !lumber.getFollow()) {
            BlockPos blockpos2 = blockPos.above();
            BlockPos blockpos3 = blockPos.above().above();
            BlockState blockstate = this.lumber.level.getBlockState(blockPos);
            BlockState blockstate2 = this.lumber.level.getBlockState(blockPos.above());
            BlockState blockstate3 = this.lumber.level.getBlockState(blockPos.above(2));

            Material material1 = blockstate.getMaterial();
            Material material2 = blockstate2.getMaterial();
            Material material3 = blockstate3.getMaterial();

            if (material1 == Material.WOOD) {

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
                }
                if (this.lumber.getRandom().nextInt(5) == 0) {
                    if (!this.lumber.swinging) {
                        this.lumber.swing(this.lumber.getUsedItemHand());
                    }
                }
            }
        }
    }
    */

    private void mineBlock(BlockPos blockPos){
        if (this.lumber.isAlive() && ForgeEventFactory.getMobGriefingEvent(this.lumber.level, this.lumber) && !lumber.getFollow()) {

            BlockState blockstate = this.lumber.level.getBlockState(blockPos);
            Block block = blockstate.getBlock();

            if (block == Blocks.OAK_LOG || block == Blocks.SPRUCE_LOG) {

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
                }
                if (this.lumber.getRandom().nextInt(5) == 0) {
                    if (!this.lumber.swinging) {
                        this.lumber.swing(this.lumber.getUsedItemHand());
                    }
                }
            }
        }
    }

}


