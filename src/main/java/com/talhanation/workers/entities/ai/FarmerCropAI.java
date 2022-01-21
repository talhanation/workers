package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.FarmerEntity;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.ForgeEventFactory;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static com.talhanation.workers.entities.FarmerEntity.WANTED_SEEDS;

public class FarmerCropAI extends Goal {
    private final FarmerEntity farmer;
    private BlockPos workPos;
    private BlockPos plantPos;
    private int state;
    private int x;
    private int z;

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
    }

    @Override
    public void stop() {
        super.stop();
        state = 0;
        restetCounts();
    }

    public void tick() {
        if (z > 9 || x > 9) {
            restetCounts();
        }

        if(farmer.getOwner() != null) {
            //debugAxisPos();
        }
        /*
        state:

        0 = hoe
        1 = plant
        2 = harvest

         */


        this.workPos = new BlockPos(farmer.getStartPos().get().getX() + x, farmer.getStartPos().get().getY(), farmer.getStartPos().get().getZ() + z);
        this.plantPos = new BlockPos(this.workPos.getX(), this.workPos.getY() + 1, this.workPos.getZ());

        switch (state) {
            case 0://hoe

                BlockState blockstatehoe = farmer.level.getBlockState(workPos);
                Block block = blockstatehoe.getBlock();

                if (block != Blocks.FARMLAND) {
                    this.farmer.getNavigation().moveTo(workPos.getX(), workPos.getY(), workPos.getZ(), 0.85);
                } else
                    x++;

                if (this.workPos.closerThan(farmer.position(), 2.5)) {
                    this.prepareFarmLand(this.workPos);
                    this.farmer.workerSwingArm();
                    this.farmer.getLookControl().setLookAt(workPos.getX(), workPos.getY() + 1, workPos.getZ(), 10.0F, (float) this.farmer.getMaxHeadXRot());

                    if (block != Blocks.AIR || block == Blocks.FARMLAND) {
                        x++;
                    }
                }

                if (x >= 9) {
                    x = 0;
                    z++;
                }

                if (z >= 9) {
                    state = 1;
                    restetCounts();
                }

            break;
            case 1://plant
                if (this.hasSeedInInv()) {
                    BlockState farmlandBlockState = this.farmer.level.getBlockState(workPos);
                    BlockState cropBlockState = this.farmer.level.getBlockState(plantPos);

                    Block farmlandPos = farmlandBlockState.getBlock();
                    Block cropPos = cropBlockState.getBlock();
                    if (cropPos == Blocks.AIR) {
                        this.farmer.getNavigation().moveTo(plantPos.getX(), plantPos.getY(), plantPos.getZ(), 0.85);
                    } else x++;

                    if (plantPos.closerThan(farmer.position(), 3)) {
                        if (cropPos == Blocks.AIR && farmlandPos == Blocks.FARMLAND) { //
                            this.farmer.getLookControl().setLookAt(plantPos.getX(), plantPos.getY(), plantPos.getZ(), 10.0F, (float) this.farmer.getMaxHeadXRot());

                            this.plantSeedsFromInv();
                            this.farmer.workerSwingArm();
                        } else
                            x++;
                    }


                    if (x >= 9) {
                        x = 0;
                        z++;
                    }

                    if (z >= 9) {
                        restetCounts();
                        state = 2;
                    }
                }
                else
                    state = 2;
            break;

            case 2://harvest

                BlockState cropBlockState1 = this.farmer.level.getBlockState(plantPos);
                Block cropPos1 = cropBlockState1.getBlock();


                if (cropPos1 instanceof CropsBlock) {
                    CropsBlock crop = (CropsBlock) cropPos1;

                    if (crop.isMaxAge(cropBlockState1)) {
                        this.farmer.getNavigation().moveTo(plantPos.getX(), plantPos.getY(), plantPos.getZ(), 0.85);

                        if (plantPos.closerThan(farmer.position(), 3)) {
                            mineBlock(plantPos);
                            this.farmer.getLookControl().setLookAt(plantPos.getX(), plantPos.getY() + 1, plantPos.getZ(), 10.0F, (float) this.farmer.getMaxHeadXRot());
                        }

                    }
                    else x++;
                }
                else x++;

                if (x >= 9) {
                    x = 0;
                    z++;
                }

                if (z >= 9) {
                    restetCounts();
                    state = 0;
                }
            break;
        }
    }

    private boolean hasSeedInInv() {
        Inventory inventory = farmer.getInventory();
        return inventory.hasAnyOf(WANTED_SEEDS);
    }


    private void plantSeedsFromInv() {
        Inventory inventory = farmer.getInventory();

        for (int i = 0; i < inventory.getContainerSize(); ++i) {
            ItemStack itemstack = inventory.getItem(i);
            boolean flag = false;
            if (!itemstack.isEmpty()) {
                if (itemstack.getItem() == Items.CARROT) {
                    farmer.level.setBlock(plantPos, Blocks.CARROTS.defaultBlockState(), 3);
                    flag = true;

                } else if (itemstack.getItem() == Items.POTATO) {
                    this.farmer.level.setBlock(plantPos, Blocks.POTATOES.defaultBlockState(), 3);
                    flag = true;

                } else if (itemstack.getItem() == Items.WHEAT_SEEDS) {
                    this.farmer.level.setBlock(plantPos, Blocks.WHEAT.defaultBlockState(), 3);
                    flag = true;

                } else if (itemstack.getItem() == Items.BEETROOT_SEEDS) {
                    this.farmer.level.setBlock(plantPos, Blocks.BEETROOTS.defaultBlockState(), 3);
                    flag = true;
                }
            }

            if (flag) {
                farmer.level.playSound(null, (double) this.plantPos.getX(), (double) this.plantPos.getY(), (double) this.plantPos.getZ(), SoundEvents.GRASS_PLACE, SoundCategory.BLOCKS, 1.0F, 1.0F);
                itemstack.shrink(1);
                if (itemstack.isEmpty()) {
                    inventory.setItem(i, ItemStack.EMPTY);
                }
                break;
            }
        }

    }

    private void prepareFarmLand(BlockPos blockPos) {
        BlockState blockstatehoe = farmer.level.getBlockState(blockPos);
        Block block = blockstatehoe.getBlock();


        if (block == Blocks.GRASS_BLOCK || block == Blocks.PODZOL || block == Blocks.DIRT){
            if (x == 4 && z == 4) {
                farmer.level.setBlock(blockPos, Blocks.WATER.defaultBlockState(), 3);
                farmer.level.playSound(null, blockPos.getX(), blockPos.getY(), blockPos.getZ(), SoundEvents.WATER_AMBIENT, SoundCategory.BLOCKS, 1.0F, 1.0F);
            }
            else
                farmer.level.setBlock(blockPos, Blocks.FARMLAND.defaultBlockState(), 3);
                farmer.level.playSound(null, blockPos.getX(), blockPos.getY(), blockPos.getZ(), SoundEvents.HOE_TILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
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

    public void restetCounts(){
        x = 0;
        z = 0;
    }

    private void debugAxisPos() {
        farmer.getOwner().sendMessage(new StringTextComponent("state: " + state), farmer.getOwner().getUUID());
        farmer.getOwner().sendMessage(new StringTextComponent("x:     " + x + ""), farmer.getOwner().getUUID());
        //farmer.getOwner().sendMessage(new StringTextComponent("y: "  + ""), farmer.getOwner().getUUID());
        farmer.getOwner().sendMessage(new StringTextComponent("z:     " + z + ""), farmer.getOwner().getUUID());
    }
}


