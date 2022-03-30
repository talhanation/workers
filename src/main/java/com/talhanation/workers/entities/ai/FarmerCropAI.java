package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.FarmerEntity;
import net.minecraft.block.*;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.ForgeEventFactory;

import java.util.EnumSet;

import static com.talhanation.workers.entities.FarmerEntity.WANTED_SEEDS;

public class FarmerCropAI extends Goal {
    private final FarmerEntity farmer;
    private BlockPos workPos;
    private BlockPos waterPos;
    private int state;

    public FarmerCropAI(FarmerEntity farmer) {
        this.farmer = farmer;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    public boolean canUse() {
        if (!this.farmer.getStartPos().isPresent()) {
            return false;
        }
        if (this.farmer.getFollow()) {
            return false;
        } else if (farmer.getIsWorking() && !this.farmer.getFollow())
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
        farmer.resetWorkerParameters();
        this.waterPos = farmer.getStartPos().get();
    }

    @Override
    public void stop() {
        super.stop();
        state = 0;
    }

    public void tick() {
        if(farmer.getOwner() != null) {
            //debugAxisPos();
        }
        /*
        state:
        0 = hoe
        1 = plant
        2 = harvest
         */

        switch (state) {
            case 0://hoe

                this.workPos = getHoePos();
                if (workPos != null) {
                    this.farmer.getNavigation().moveTo(workPos.getX(), workPos.getY(), workPos.getZ(), 1);
                    this.farmer.getLookControl().setLookAt(workPos.getX(), workPos.getY() + 1, workPos.getZ(), 10.0F, (float) this.farmer.getMaxHeadXRot());

                    if (workPos.closerThan(farmer.position(), 2.5)) {
                        this.prepareFarmLand(workPos);
                        farmer.workerSwingArm();
                    }
                }
            break;
            case 1://plant

            break;

            case 2://harvest
            break;
        }
    }

    private boolean hasSeedInInv() {
        Inventory inventory = farmer.getInventory();
        return inventory.hasAnyOf(WANTED_SEEDS);
    }

    private boolean hasWaterInInv() {
        Inventory inventory = farmer.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); ++i) {
            ItemStack itemstack = inventory.getItem(i);

            if (!itemstack.isEmpty() && itemstack.getItem() == Items.WATER_BUCKET) {
                return true;
            }
        }
         return false;
    }

    private void plantSeedsFromInv(BlockPos blockPos) {
        Inventory inventory = farmer.getInventory();

        for (int i = 0; i < inventory.getContainerSize(); ++i) {
            ItemStack itemstack = inventory.getItem(i);
            boolean flag = false;
            if (!itemstack.isEmpty()) {
                if (itemstack.getItem() == Items.CARROT) {
                    farmer.level.setBlock(blockPos, Blocks.CARROTS.defaultBlockState(), 3);
                    flag = true;

                } else if (itemstack.getItem() == Items.POTATO) {
                    this.farmer.level.setBlock(blockPos, Blocks.POTATOES.defaultBlockState(), 3);
                    flag = true;

                } else if (itemstack.getItem() == Items.WHEAT_SEEDS) {
                    this.farmer.level.setBlock(blockPos, Blocks.WHEAT.defaultBlockState(), 3);
                    flag = true;

                } else if (itemstack.getItem() == Items.BEETROOT_SEEDS) {
                    this.farmer.level.setBlock(blockPos, Blocks.BEETROOTS.defaultBlockState(), 3);
                    flag = true;
                }
            }

            if (flag) {
                farmer.level.playSound(null, (double) blockPos.getX(), (double) blockPos.getY(), (double) blockPos.getZ(), SoundEvents.GRASS_PLACE, SoundCategory.BLOCKS, 1.0F, 1.0F);
                itemstack.shrink(1);
                if (itemstack.isEmpty()) {
                    inventory.setItem(i, ItemStack.EMPTY);
                }
                break;
            }
        }

    }

    private void prepareFarmLand(BlockPos blockPos) {
        farmer.level.setBlock(blockPos, Blocks.FARMLAND.defaultBlockState(), 3);
        farmer.level.playSound(null, blockPos.getX(), blockPos.getY(), blockPos.getZ(), SoundEvents.HOE_TILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
        BlockState blockState = this.farmer.level.getBlockState(blockPos.above());
        Block block = blockState.getBlock();

        if (block instanceof BushBlock || block instanceof AbstractPlantBlock) {

            farmer.level.destroyBlock(blockPos.above(), false);
            farmer.level.playSound(null, blockPos.getX(), blockPos.getY(), blockPos.getZ(), SoundEvents.GRASS_BREAK, SoundCategory.BLOCKS, 1.0F, 1.0F);
        }

        if (waterPos != null) {
            FluidState waterBlockState = this.farmer.level.getFluidState(waterPos);

            if (waterBlockState != Fluids.WATER.defaultFluidState() || waterBlockState != Fluids.FLOWING_WATER.defaultFluidState()){
                farmer.level.setBlock(waterPos, Blocks.WATER.defaultBlockState(), 3);
                farmer.level.playSound(null, blockPos.getX(), blockPos.getY(), blockPos.getZ(), SoundEvents.BUCKET_EMPTY, SoundCategory.BLOCKS, 1.0F, 1.0F);
                this.waterPos = null;
            }
        }
    }

    private void mineBlock(BlockPos blockPos){

        if (this.farmer.isAlive() && ForgeEventFactory.getMobGriefingEvent(this.farmer.level, this.farmer) && !farmer.getFollow()) {
            BlockState blockstate = this.farmer.level.getBlockState(blockPos);
            Block block = blockstate.getBlock();

            if (block != Blocks.AIR) {

                if (farmer.getCurrentTimeBreak() % 5 == 4) {
                    farmer.level.playLocalSound(blockPos.getX(), blockPos.getY(), blockPos.getZ(), blockstate.getSoundType().getHitSound(), SoundCategory.BLOCKS, 1F, 0.75F, false);
                }

                int bp = (int) (blockstate.getDestroySpeed(this.farmer.level, blockPos) * 10);
                this.farmer.setBreakingTime(bp);

                this.farmer.setCurrentTimeBreak(this.farmer.getCurrentTimeBreak() + (int) (1 * (this.farmer.getUseItem().getDestroySpeed(blockstate))));
                float f = (float) this.farmer.getCurrentTimeBreak() / (float) this.farmer.getBreakingTime();

                int i = (int) (f * 10);

                if (i != this.farmer.getPreviousTimeBreak()) {
                    this.farmer.level.destroyBlockProgress(1, blockPos, i);
                    this.farmer.setPreviousTimeBreak(i);
                }

                if (this.farmer.getCurrentTimeBreak() == this.farmer.getBreakingTime()) {
                    this.farmer.level.destroyBlock(blockPos, true, this.farmer);
                    this.farmer.setCurrentTimeBreak(-1);
                    this.farmer.setBreakingTime(0);
                }
                //farmer.changeTool(blockstate);
                if (this.farmer.getRandom().nextInt(5) == 0) {
                    if (!this.farmer.swinging) {
                        this.farmer.swing(this.farmer.getUsedItemHand());
                    }
                }
            }
        }
    }


    public BlockPos getHoePos() {
        //int range = 8;
        for (int j = 0; j <= 8; j++){
            for (int i = 0; i <= 8; i++){
                BlockPos blockPos = this.farmer.getStartPos().get().offset(j - 4, 0, i - 4);

                BlockState blockState = this.farmer.level.getBlockState(blockPos);

                Block block = blockState.getBlock();
                if (block == Blocks.GRASS_BLOCK || block == Blocks.DIRT) {
                    return blockPos;
                }
            }
        }
        return null;
    }
}


