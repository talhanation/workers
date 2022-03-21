package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.LumberjackEntity;
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
import net.minecraftforge.event.ForgeEventFactory;

import java.util.EnumSet;
import java.util.Random;

import static com.talhanation.workers.entities.LumberjackEntity.WANTED_SAPLINGS;

public class LumberjackAI extends Goal {
    private final LumberjackEntity lumber;
    private BlockPos chopPos;

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
        lumber.resetWorkerParameters();
    }

    public void tick() {
        breakLeaves();

        this.chopPos = getWoodPos();
        if (chopPos != null) {
            this.lumber.getNavigation().moveTo(chopPos.getX(), chopPos.getY(), chopPos.getZ(), 1);
            this.lumber.getLookControl().setLookAt(chopPos.getX(), chopPos.getY() + 1, chopPos.getZ(), 10.0F, (float) this.lumber.getMaxHeadXRot());

            if (chopPos.closerThan(lumber.position(), 9)) {
                this.mineBlock(chopPos);

                if(lumber.level.getBlockState(chopPos.below()).is(Blocks.DIRT) && this.lumber.level.isEmptyBlock(chopPos)){
                    plantSaplingFromInv(chopPos);
                }
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
        int range = 16;

        for (int j = 0; j < range; j++){
            for (int i = 0; i < range; i++){
                for(int k = 0; k < 8; k++){
                    BlockPos blockPos = this.lumber.getStartPos().get().offset(j - range / 2F, k, i - range / 2F);
                   //this.lumber.level.setBlock(blockPos, Blocks.COBWEB.defaultBlockState(), 3);
                    BlockState blockState = this.lumber.level.getBlockState(blockPos);

                    Material blockStateMaterial = blockState.getMaterial();
                    if (blockStateMaterial == Material.WOOD) {
                         return blockPos;
                    }
                }
            }
        }
        return null;
    }

    public BlockPos getPlantPos() {
        int range = 16;
        Random random = new Random();
        for (int j = 0; j < range; j++) {
            BlockPos blockPos = this.lumber.getStartPos().get().offset(random.nextInt(10) - range / 2F, 1, random.nextInt(10) - range / 2F);


            if (this.lumber.level.isEmptyBlock(blockPos) && lumber.level.getBlockState(blockPos.below()).is(Blocks.GRASS_BLOCK)) {
                return blockPos;
            }
        }
        return null;
    }

    private boolean hasPlantInInv(){
        Inventory inventory = lumber.getInventory();
        return inventory.hasAnyOf(WANTED_SAPLINGS);
    }


    private void plantSaplingFromInv(BlockPos blockPos) {
        if (hasPlantInInv()) {
            Inventory inventory = lumber.getInventory();

            for (int i = 0; i < inventory.getContainerSize(); ++i) {
                ItemStack itemstack = inventory.getItem(i);
                boolean flag = false;
                if (!itemstack.isEmpty()) {
                    if (itemstack.getItem() == Items.SPRUCE_SAPLING) {
                        lumber.level.setBlock(blockPos, Blocks.SPRUCE_SAPLING.defaultBlockState(), 3);
                        flag = true;

                    } else if (itemstack.getItem() == Items.OAK_SAPLING) {
                        this.lumber.level.setBlock(blockPos, Blocks.OAK_SAPLING.defaultBlockState(),3);
                        flag = true;

                    } else if (itemstack.getItem() == Items.DARK_OAK_SAPLING) {
                        this.lumber.level.setBlock(blockPos, Blocks.DARK_OAK_SAPLING.defaultBlockState(),3);
                        flag = true;

                    } else if (itemstack.getItem() == Items.BIRCH_SAPLING) {
                        this.lumber.level.setBlock(blockPos, Blocks.BIRCH_SAPLING.defaultBlockState(), 3);
                        flag = true;

                    } else if (itemstack.getItem() == Items.SPRUCE_SAPLING) {
                        this.lumber.level.setBlock(blockPos, Blocks.SPRUCE_SAPLING.defaultBlockState(), 3);
                        flag = true;

                    } else if (itemstack.getItem() == Items.JUNGLE_SAPLING) {
                        this.lumber.level.setBlock(blockPos, Blocks.JUNGLE_SAPLING.defaultBlockState(), 3);
                        flag = true;
                    }
                }

                if (flag) {
                    lumber.level.playSound(null, (double) blockPos.getX(), (double) blockPos.getY(), (double) blockPos.getZ(), SoundEvents.GRASS_PLACE, SoundCategory.BLOCKS, 1.0F, 1.0F);
                    itemstack.shrink(1);
                    if (itemstack.isEmpty()) {
                        inventory.setItem(i, ItemStack.EMPTY);
                    }
                    break;
                }
            }

        }
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
                int bp = (int) (blockstate.getDestroySpeed(this.lumber.level, blockPos) * 30);
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
