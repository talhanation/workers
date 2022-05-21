package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.FarmerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.event.ForgeEventFactory;

import java.util.EnumSet;
import java.util.Optional;

import static com.talhanation.workers.entities.FarmerEntity.CROP_BLOCKS;
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

        //this.debug();

        /*
        state:
        0 = hoe
        1 = plant
        2 = harvest
         */

        switch (state) {
            case 0://hoe
                if ((hasWaterInInv() || startPosIsWater()))
                    this.workPos = getHoePos();

                if (workPos != null) {
                    this.farmer.getNavigation().moveTo(workPos.getX(), workPos.getY(), workPos.getZ(), 0.7);
                    this.farmer.getLookControl().setLookAt(workPos.getX(), workPos.getY() + 1, workPos.getZ(), 10.0F, (float) this.farmer.getMaxHeadXRot());

                    if (workPos.closerThan(farmer.position(), 1.75)) {
                        farmer.workerSwingArm();
                        this.prepareFarmLand(workPos);
                    }
                }

                else
                state = 1;
            break;
            case 1://plant
                if (hasSeedInInv())
                    this.workPos = getPlantPos();

                if (workPos != null) {
                    this.farmer.getNavigation().moveTo(workPos.getX(), workPos.getY(), workPos.getZ(), 0.7);
                    this.farmer.getLookControl().setLookAt(workPos.getX(), workPos.getY(), workPos.getZ(), 10.0F, (float) this.farmer.getMaxHeadXRot());

                    if (workPos.closerThan(farmer.position(), 1.75)) {
                        farmer.workerSwingArm();
                        this.plantSeedsFromInv(workPos);
                    }
                }

                else
                state = 2;
            break;

            case 2://harvest
                if (hasSpaceInInv())
                    this.workPos = getHarvestPos();

                if (workPos != null) {
                    this.farmer.getNavigation().moveTo(workPos.getX(), workPos.getY(), workPos.getZ(), 0.6);
                    this.farmer.getLookControl().setLookAt(workPos.getX(), workPos.getY(), workPos.getZ(), 10.0F, (float) this.farmer.getMaxHeadXRot());

                    if (workPos.closerThan(farmer.position(), 1.75)) {
                        farmer.workerSwingArm();
                        this.mineBlock(workPos);
                    }
                }

                else
                state = 0;
            break;
        }
    }

    private void debug() {
        if(farmer.getOwner() != null) {
            this.farmer.getOwner().sendMessage(new TextComponent("State: " + state), farmer.getOwnerUUID());
            this.farmer.getOwner().sendMessage(new TextComponent("WorkPos: " + workPos), farmer.getOwnerUUID());
            this.farmer.getOwner().sendMessage(new TextComponent("StartPos: " + farmer.getStartPos()), farmer.getOwnerUUID());
        }
    }

    private boolean hasSeedInInv() {
        SimpleContainer inventory = farmer.getInventory();
        return inventory.hasAnyOf(WANTED_SEEDS);
    }

    private boolean hasSpaceInInv() {
        SimpleContainer inventory = farmer.getInventory();
        return inventory.canAddItem(farmer.WANTED_ITEMS.stream().findAny().get().getDefaultInstance());
    }

    private boolean hasWaterInInv() {
        SimpleContainer inventory = farmer.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); ++i) {
            ItemStack itemstack = inventory.getItem(i);

            if (!itemstack.isEmpty() && itemstack.getItem() == Items.WATER_BUCKET) {
                return true;
            }
        }
         return false;
    }

    private boolean startPosIsWater(){
        Optional<BlockPos> blockPos = farmer.getStartPos();
        if (blockPos.isPresent()) {
            FluidState waterBlockState = this.farmer.level.getFluidState(blockPos.get());

            if (waterBlockState == Fluids.WATER.defaultFluidState() || waterBlockState == Fluids.FLOWING_WATER.defaultFluidState()) {
                return true;
            }
        }
        return false;
    }

    private void plantSeedsFromInv(BlockPos blockPos) {
        SimpleContainer inventory = farmer.getInventory();

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
                farmer.level.playSound(null, (double) blockPos.getX(), (double) blockPos.getY(), (double) blockPos.getZ(), SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
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
        farmer.level.playSound(null, blockPos.getX(), blockPos.getY(), blockPos.getZ(), SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0F, 1.0F);
        BlockState blockState = this.farmer.level.getBlockState(blockPos.above());
        Block block = blockState.getBlock();

        if (block instanceof BushBlock || block instanceof GrowingPlantBlock) {

            farmer.level.destroyBlock(blockPos.above(), false);
            farmer.level.playSound(null, blockPos.getX(), blockPos.getY(), blockPos.getZ(), SoundEvents.GRASS_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
        }

        if (waterPos != null) {
            FluidState waterBlockState = this.farmer.level.getFluidState(waterPos);

            if (waterBlockState != Fluids.WATER.defaultFluidState() || waterBlockState != Fluids.FLOWING_WATER.defaultFluidState()){
                farmer.level.setBlock(waterPos, Blocks.WATER.defaultBlockState(), 3);
                farmer.level.playSound(null, blockPos.getX(), blockPos.getY(), blockPos.getZ(), SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                this.waterPos = null;
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

    public BlockPos getPlantPos() {
        //int range = 8;
        for (int j = 0; j <= 8; j++){
            for (int i = 0; i <= 8; i++){
                BlockPos blockPos = this.farmer.getStartPos().get().offset(j - 4, 0, i - 4);
                BlockPos aboveBlockPos = blockPos.above();
                BlockState blockState = this.farmer.level.getBlockState(blockPos);
                BlockState aboveBlockState = this.farmer.level.getBlockState(aboveBlockPos);

                Block block = blockState.getBlock();
                Block aboveBlock = aboveBlockState.getBlock();
                if (block == Blocks.FARMLAND && aboveBlock == Blocks.AIR) {
                    return aboveBlockPos;
                }
            }
        }
        return null;
    }


    public BlockPos getHarvestPos() {
        //int range = 8;
        for (int j = 0; j <= 8; j++){
            for (int i = 0; i <= 8; i++){
                BlockPos blockPos = this.farmer.getStartPos().get().offset(j - 4, 1, i - 4);
                BlockState blockState = this.farmer.level.getBlockState(blockPos);

                Block block = blockState.getBlock();
                if (CROP_BLOCKS.contains(block)){
                    if (block instanceof CropBlock) {
                        CropBlock crop = (CropBlock) block;

                        if (crop.isMaxAge(blockState)) {
                            return blockPos;
                        }
                    }
                }
            }
        }
        return null;
    }

    private boolean mineBlock(BlockPos blockPos){
        if (this.farmer.isAlive() && ForgeEventFactory.getMobGriefingEvent(this.farmer.level, this.farmer) && !farmer.getFollow()) {

            BlockState blockstate = this.farmer.level.getBlockState(blockPos);
            Block block = blockstate.getBlock();

            if ((block != Blocks.AIR)){
                if (farmer.getCurrentTimeBreak() % 5 == 4) {
                    farmer.level.playLocalSound(blockPos.getX(), blockPos.getY(), blockPos.getZ(), blockstate.getSoundType().getHitSound(), SoundSource.BLOCKS, 1F, 0.75F, false);
                }

                //set max destroy speed
                int bp = (int) (blockstate.getDestroySpeed(this.farmer.level, blockPos) * 30);
                this.farmer.setBreakingTime(bp);

                //increase current
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
                    return true;
                }
                this.farmer.workerSwingArm();
            }
        }
        return false;
    }
}


