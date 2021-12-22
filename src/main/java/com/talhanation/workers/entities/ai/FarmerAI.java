package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.FarmerEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.material.Material;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.event.ForgeEventFactory;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static com.talhanation.workers.entities.FarmerEntity.WANTED_SEEDS;

public class FarmerAI extends Goal {
    private final FarmerEntity farmer;
    private BlockPos breakPos;
    private BlockPos plantPos;
    private BlockPos startPos;
    private boolean plant;
    private boolean harvest;
    private int y;
    private int x;
    private int z;

    public FarmerAI(FarmerEntity farmer) {
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
        this.startPos = new BlockPos(farmer.getStartPos().get().getX(), farmer.getStartPos().get().getY(), farmer.getStartPos().get().getZ());

        plant = true;
        harvest = false;
        innen = true;
        x = -3;
        z = -3;
    }

    public void tick() {


        if (harvest) {
            if (farmer.getFollow() || !farmer.getIsWorking()) {
                if (innen) {
                    x = -1;
                    z = -1;
                } else {
                    x = -5;
                    z = -5;
                }
            }

            this.breakPos = new BlockPos(farmer.getStartPos().get().getX() + x, farmer.getStartPos().get().getY() + y, farmer.getStartPos().get().getZ() + z);

            BlockState blockstate = farmer.level.getBlockState(breakPos);
            Material blockmat = blockstate.getMaterial();
            if (blockmat == Material.WOOD) {
                this.farmer.getNavigation().moveTo(breakPos.getX(), breakPos.getY(), breakPos.getZ(), 0.5);
            }

            if (blockmat == Material.WOOD && breakPos.closerThan(farmer.position(), 9)) {
                this.breakBlock(breakPos);
                this.farmer.getLookControl().setLookAt(breakPos.getX(), breakPos.getY() + 1, breakPos.getZ(), 10.0F, (float) this.farmer.getMaxHeadXRot());
            }
            calculateHarvestArea(blockmat);
        }

        if (plant && hasSeedInInv()) {

            if (farmer.getFollow() || !farmer.getIsWorking()) {
                    x = -5;
                    z = -5;
                }
            }

            this.plantPos = new BlockPos(this.startPos.getX() + x, this.startPos.getY() + y, this.startPos.getZ() + z);
            BlockState blockstate = farmer.level.getBlockState(plantPos);
            BlockState blockstate2 = this.farmer.level.getBlockState(plantPos.below());
            Block plant = blockstate.getBlock();
            Block plantPosBlock = blockstate2.getBlock();

            this.farmer.getNavigation().moveTo(plantPos.getX(), plantPos.getY(), plantPos.getZ(), 0.65);

            if (plant == Blocks.AIR && (plantPosBlock == Blocks.GRASS_BLOCK || plantPosBlock == Blocks.PODZOL || plantPosBlock == Blocks.DIRT) && plantPos.closerThan(farmer.position(), 9)) {
                this.farmer.getLookControl().setLookAt(plantPos.getX(), plantPos.getY() + 1, plantPos.getZ(), 10.0F, (float) this.farmer.getMaxHeadXRot());
                //plantSaplingFromInv();
                this.farmer.level.playSound(null, this.farmer.getX(), this.farmer.getY(), this.farmer.getZ(), SoundEvents.GRASS_PLACE, SoundCategory.BLOCKS, 1F, 0.9F + 0.2F);
                this.farmer.workerSwingArm();
            }
            else
                y++;

            if (blocks == 8) {
                blocks = 0;
                side++;
                this.miner.setIsPickingUp(true);
            }

            if (side == 8){
                miner.setStartPos(Optional.empty());
                miner.setIsWorking(false);
            }
        }

        if (plant && !hasSeedInInv()) {
            plant = false;
            harvest = true;
        }
    }

    private boolean hasSeedInInv(){
        Inventory inventory = farmer.getInventory();
        return inventory.hasAnyOf(WANTED_SEEDS);
    }


    private void plantSeedsFromInv() {
        if (hasSeedInInv()) {
            Inventory inventory = farmer.getInventory();

            for (int i = 0; i < inventory.getContainerSize(); ++i) {
                ItemStack itemstack = inventory.getItem(i);
                boolean flag = false;
                if (!itemstack.isEmpty()) {
                    if (itemstack.getItem() == Items.WHEAT_SEEDS) {
                        farmer.level.setBlock(this.plantPos, Blocks.SPRUCE_SAPLING.defaultBlockState(), 3);
                        flag = true;

                    } else if (itemstack.getItem() == Items.BEETROOT_SEEDS) {
                        this.farmer.level.setBlock(plantPos, Blocks.OAK_SAPLING.defaultBlockState(),3);
                        flag = true;

                    } else if (itemstack.getItem() == Items.CARROT) {
                        this.farmer.level.setBlock(plantPos, Blocks.BIRCH_SAPLING.defaultBlockState(), 3);
                        flag = true;

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
    }

    private void calculatePlantArea(){

    }

    private void calculateHarvestArea(Material blockmat){

    }

    private void breakBlock(BlockPos blockPos){
        if (this.farmer.isAlive() && ForgeEventFactory.getMobGriefingEvent(this.farmer.level, this.farmer) && !farmer.getFollow()) {

            BlockState blockstate = this.farmer.level.getBlockState(blockPos);
            Block block = blockstate.getBlock();

            if (block == Blocks.OAK_LOG || block == Blocks.SPRUCE_LOG) {

                if (farmer.getCurrentTimeBreak() % 5 == 4) {
                    farmer.level.playLocalSound(blockPos.getX(), blockPos.getY(), blockPos.getZ(), blockstate.getSoundType().getHitSound(), SoundCategory.BLOCKS, 1F, 0.75F, false);
                }

                //set max destroy speed
                int bp = (int) (blockstate.getDestroySpeed(this.farmer.level, blockPos) * 100);
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
                }
                this.farmer.workerSwingArm();
            }
        }
    }

    private void debugAxisPos(){
        farmer.getOwner().sendMessage(new StringTextComponent("x: " + x + ""), farmer.getOwner().getUUID());
        farmer.getOwner().sendMessage(new StringTextComponent("y: " + y + ""), farmer.getOwner().getUUID());
        farmer.getOwner().sendMessage(new StringTextComponent("z: " + z + ""), farmer.getOwner().getUUID());
    }

}


