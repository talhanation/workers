package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.LumberjackEntity;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.event.ForgeEventFactory;

import java.util.*;

import static com.talhanation.workers.entities.LumberjackEntity.WANTED_SAPLINGS;

public class LumberjackAI extends Goal {
    private final LumberjackEntity lumber;
    private BlockPos minePos;
    private BlockPos plantPos;
    private BlockPos startPos;
    private boolean innen;
    private boolean plant;
    private boolean chop;
    private int y;
    private int x;
    private int z;

    public LumberjackAI(LumberjackEntity lumber) {
        this.lumber = lumber;
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
        this.startPos = new BlockPos(lumber.getStartPos().get().getX(), lumber.getStartPos().get().getY(), lumber.getStartPos().get().getZ());

        plant = true;
        chop = false;
        innen = true;
        x = -3;
        z = -3;
    }

    public void tick() {
        breakLeaves();
        debugAxisPos();

        if (chop) {
            if (lumber.getFollow() || !lumber.getIsWorking()) {
                if (innen) {
                    x = -3;
                    z = -3;
                } else {
                    x = -9;
                    z = -9;
                }
            }

            this.minePos = new BlockPos(lumber.getStartPos().get().getX() + x, lumber.getStartPos().get().getY() + y, lumber.getStartPos().get().getZ() + z);

            BlockState blockstate = lumber.level.getBlockState(minePos);
            Material blockmat = blockstate.getMaterial();
            if (blockmat == Material.WOOD) {
                this.lumber.getNavigation().moveTo(minePos.getX(), minePos.getY(), minePos.getZ(), 0.5);
            }

            if (blockmat == Material.WOOD && minePos.closerThan(lumber.position(), 9)) {
                this.mineBlock(minePos);
                this.lumber.getLookControl().setLookAt(minePos.getX(), minePos.getY() + 1, minePos.getZ(), 10.0F, (float) this.lumber.getMaxHeadXRot());
            }
            calculateChopArea(blockmat);
        }

        if (plant && hasPlantInInv()) {

            if (lumber.getFollow() || !lumber.getIsWorking()) {
                if (innen) {
                    x = -3;
                    z = -3;
                } else {
                    x = -9;
                    z = -9;
                }
            }

            this.plantPos = new BlockPos(this.startPos.getX() + x, this.startPos.getY() + y, this.startPos.getZ() + z);
            BlockState blockstate = lumber.level.getBlockState(plantPos);
            BlockState blockstate2 = this.lumber.level.getBlockState(plantPos.below());
            Block plant = blockstate.getBlock();
            Block plantPosBlock = blockstate2.getBlock();

            this.lumber.getNavigation().moveTo(plantPos.getX(), plantPos.getY(), plantPos.getZ(), 0.65);

            if (plant == Blocks.AIR && (plantPosBlock == Blocks.GRASS_BLOCK || plantPosBlock == Blocks.PODZOL || plantPosBlock == Blocks.DIRT) && plantPos.closerThan(lumber.position(), 9)) {
                this.lumber.getLookControl().setLookAt(plantPos.getX(), plantPos.getY() + 1, plantPos.getZ(), 10.0F, (float) this.lumber.getMaxHeadXRot());
                plantSaplingFromInv();
                this.lumber.level.playSound(null, this.lumber.getX(), this.lumber.getY(), this.lumber.getZ(), SoundEvents.GRASS_PLACE, SoundCategory.BLOCKS, 1F, 0.9F + 0.2F);
                this.lumber.workerSwingArm();
            }
            else
                y++;

            calculatePlantArea();
        }

        if (plant && !hasPlantInInv()) {
            plant = false;
            chop = true;
        }
    }

    private boolean hasPlantInInv(){
        Inventory inventory = lumber.getInventory();
        return inventory.hasAnyOf(WANTED_SAPLINGS);
    }


    private void plantSaplingFromInv() {
        if (hasPlantInInv()) {
            Inventory inventory = lumber.getInventory();

            for (int i = 0; i < inventory.getContainerSize(); ++i) {
                ItemStack itemstack = inventory.getItem(i);
                boolean flag = false;
                if (!itemstack.isEmpty()) {
                    if (itemstack.getItem() == Items.SPRUCE_SAPLING) {
                        lumber.level.setBlock(this.plantPos, Blocks.SPRUCE_SAPLING.defaultBlockState(), 3);
                        flag = true;

                    } else if (itemstack.getItem() == Items.OAK_SAPLING) {
                        this.lumber.level.setBlock(plantPos, Blocks.OAK_SAPLING.defaultBlockState(),3);
                        flag = true;

                    } else if (itemstack.getItem() == Items.DARK_OAK_SAPLING) {
                        this.lumber.level.setBlock(plantPos, Blocks.DARK_OAK_SAPLING.defaultBlockState(),3);
                        flag = true;

                    } else if (itemstack.getItem() == Items.BIRCH_SAPLING) {
                        this.lumber.level.setBlock(plantPos, Blocks.BIRCH_SAPLING.defaultBlockState(), 3);
                        flag = true;

                    } else if (itemstack.getItem() == Items.SPRUCE_SAPLING) {
                        this.lumber.level.setBlock(plantPos, Blocks.SPRUCE_SAPLING.defaultBlockState(), 3);
                        flag = true;

                    } else if (itemstack.getItem() == Items.JUNGLE_SAPLING) {
                        this.lumber.level.setBlock(plantPos, Blocks.JUNGLE_SAPLING.defaultBlockState(), 3);
                        flag = true;
                    }
                }

                if (flag) {
                    lumber.level.playSound(null, (double) this.plantPos.getX(), (double) this.plantPos.getY(), (double) this.plantPos.getZ(), SoundEvents.GRASS_PLACE, SoundCategory.BLOCKS, 1.0F, 1.0F);
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
        if (y == 5) {
            y = -2;
            x = x + 3 + lumber.getRandom().nextInt(3);
        }

        if (x >= 9) {
            x = -9;
            z = z + 3 + lumber.getRandom().nextInt(3);
        }
        if (z >= 9) {
            z = -9;
            this.plant = true;
            this.innen = true;
        }

    }

    private void calculateChopArea(Material blockmat){
        if (blockmat != Material.WOOD) {
            y++;
        }

        if (y == 9) {
            y = -2;
            x++;
        }

        if (innen){
            if (x == 3) {
                x = -3;
                z++;
            }
            if (z == 3) {
                x = -9;
                z = -9;
                this.innen = false;
            }
        }
        else {
            if (x == 9) {
                x = -9;
                z++;
            }
            if (z == 9) {
                z = -9;
                this.plant = true;
                this.innen = true;
            }
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
                            this.lumber.workerSwingArm();
                        }

                    }
                }
            }
        }

        if (hasBroken) {
            this.lumber.level.playSound(null, this.lumber.getX(), this.lumber.getY(), this.lumber.getZ(), SoundEvents.GRASS_BREAK, SoundCategory.BLOCKS, 1F, 0.9F + 0.2F);
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
                this.lumber.workerSwingArm();
            }
        }
    }

    private void debugAxisPos(){
        lumber.getOwner().sendMessage(new StringTextComponent("x: " + x + ""), lumber.getOwner().getUUID());
        lumber.getOwner().sendMessage(new StringTextComponent("y: " + y + ""), lumber.getOwner().getUUID());
        lumber.getOwner().sendMessage(new StringTextComponent("z: " + z + ""), lumber.getOwner().getUUID());
    }


    public void palceGoodsFromInventory() {
        Inventory inventory = lumber.getInventory();
        List<ItemStack> items = new ArrayList();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            /*
            if (chestLooter.shouldLootItem(stack)) {
                items.add(stack);
            }
             */
        }
    }
}


