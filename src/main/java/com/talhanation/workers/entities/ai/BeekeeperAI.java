package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.BeekeeperEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;
import java.util.Optional;

import static com.google.common.base.Predicates.not;

public class BeekeeperAI extends Goal {
    private Optional<Bee> bee;
    private final BeekeeperEntity beekeeper;
    private boolean breeding;
    private BlockPos workPos;


    public BeekeeperAI(BeekeeperEntity worker) {
        this.beekeeper = worker;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!this.beekeeper.level.isDay()) {
            return false;
        }
        else return beekeeper.getIsWorking() && !beekeeper.getFollow();
    }

    @Override
    public void start() {
        super.start();
        this.workPos = beekeeper.getStartPos();
        this.breeding = true;
    }

    private void consumeFlowers(){
        SimpleContainer inventory = beekeeper.getInventory();
        for(int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack itemStack = inventory.getItem(i);

            if (itemStack.is(ItemTags.FLOWERS)){
                itemStack.shrink(1);
                break;
            }
        }
    }

    private Optional<Bee> findBeeBreeding() {
        return  beekeeper.level.getEntitiesOfClass(Bee.class, beekeeper.getBoundingBox()
                        .inflate(8D), Bee::isAlive)
                .stream()
                .filter(not(Bee::isBaby))
                .filter(not(Bee::isInLove))
                .findAny();
    }

    private boolean hasFlowers() {
        SimpleContainer inventory = beekeeper.getInventory();
        for(int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack.is(ItemTags.FLOWERS)){
                if (itemStack.getCount() >= 2)
                    return true;
            }
        }
        return false;
    }

    public BlockPos getPlantPos() {
        //int range = 8;
        for (int j = 0; j <= 8; j++){
            for (int i = 0; i <= 8; i++){
                for (int k = -3; k <= 3; k++) {
                    BlockPos blockPos = this.beekeeper.getStartPos().offset(j - 4, k, i - 4);
                    BlockPos aboveBlockPos = blockPos.above();
                    BlockState blockState = this.beekeeper.level.getBlockState(blockPos);
                    BlockState aboveBlockState = this.beekeeper.level.getBlockState(aboveBlockPos);

                    Block block = blockState.getBlock();
                    Block aboveBlock = aboveBlockState.getBlock();
                    if (block == Blocks.GRASS_BLOCK && aboveBlock == Blocks.AIR) {
                        return aboveBlockPos;
                    }
                }
            }
        }
        return null;
    }

    public BlockPos getBeePos() {
        //int range = 8;
        for (int j = 0; j <= 8; j++){
            for (int i = 0; i <= 8; i++){
                for (int k = -3; k <= 3; k++) {
                    BlockPos blockPos = this.beekeeper.getStartPos().offset(j - 4, k, i - 4);
                    BlockState blockState = this.beekeeper.level.getBlockState(blockPos);

                    Block block = blockState.getBlock();
                    if (block == Blocks.BEE_NEST || block == Blocks.BEEHIVE) {
                        return blockPos;
                    }
                }
            }
        }
        return null;
    }

    private void plantFlowerFromInv(BlockPos blockPos) {
        if (hasFlowers()) {
            SimpleContainer inventory = beekeeper.getInventory();

            for (int i = 0; i < inventory.getContainerSize(); ++i) {
                ItemStack itemstack = inventory.getItem(i);
                boolean flag = false;
                if (!itemstack.isEmpty()) {
                    if (itemstack.getItem() == Items.DANDELION) {
                        beekeeper.level.setBlock(blockPos, Blocks.DANDELION.defaultBlockState(), 3);
                        flag = true;
                    }
                    else if (itemstack.getItem() == Items.POPPY) {
                        this.beekeeper.level.setBlock(blockPos, Blocks.POPPY.defaultBlockState(),3);
                        flag = true;

                    }
                    else if (itemstack.getItem() == Items.BLUE_ORCHID) {
                        this.beekeeper.level.setBlock(blockPos, Blocks.BLUE_ORCHID.defaultBlockState(),3);
                        flag = true;

                    }
                    else if (itemstack.getItem() == Items.ALLIUM) {
                        this.beekeeper.level.setBlock(blockPos, Blocks.ALLIUM.defaultBlockState(), 3);
                        flag = true;

                    }
                    else if (itemstack.getItem() == Items.AZURE_BLUET) {
                        this.beekeeper.level.setBlock(blockPos, Blocks.AZURE_BLUET.defaultBlockState(), 3);
                        flag = true;

                    }
                    else if (itemstack.getItem() == Items.RED_TULIP) {
                        this.beekeeper.level.setBlock(blockPos, Blocks.RED_TULIP.defaultBlockState(), 3);
                        flag = true;
                    }
                    else if (itemstack.getItem() == Items.ORANGE_TULIP) {
                        this.beekeeper.level.setBlock(blockPos, Blocks.ORANGE_TULIP.defaultBlockState(), 3);
                        flag = true;
                    }
                    else if (itemstack.getItem() == Items.WHITE_TULIP) {
                        this.beekeeper.level.setBlock(blockPos, Blocks.WHITE_TULIP.defaultBlockState(), 3);
                        flag = true;
                    }
                    else if (itemstack.getItem() == Items.PINK_TULIP) {
                        this.beekeeper.level.setBlock(blockPos, Blocks.PINK_TULIP.defaultBlockState(), 3);
                        flag = true;
                    }
                    else if (itemstack.getItem() == Items.OXEYE_DAISY) {
                        this.beekeeper.level.setBlock(blockPos, Blocks.OXEYE_DAISY.defaultBlockState(), 3);
                        flag = true;
                    }
                    else if (itemstack.getItem() == Items.LILY_OF_THE_VALLEY) {
                        this.beekeeper.level.setBlock(blockPos, Blocks.LILY_OF_THE_VALLEY.defaultBlockState(), 3);
                        flag = true;
                    }
                    else if (itemstack.getItem() == Items.SUNFLOWER) {
                        this.beekeeper.level.setBlock(blockPos, Blocks.SUNFLOWER.defaultBlockState(), 3);
                        flag = true;
                    }
                    else if (itemstack.getItem() == Items.LILAC) {
                        this.beekeeper.level.setBlock(blockPos, Blocks.LILAC.defaultBlockState(), 3);
                        flag = true;
                    }
                    else if (itemstack.getItem() == Items.ROSE_BUSH) {
                        this.beekeeper.level.setBlock(blockPos, Blocks.ROSE_BUSH.defaultBlockState(), 3);
                        flag = true;
                    }
                    else if (itemstack.getItem() == Items.PEONY) {
                        this.beekeeper.level.setBlock(blockPos, Blocks.PEONY.defaultBlockState(), 3);
                        flag = true;
                    }
                }

                if (flag) {
                    beekeeper.level.playSound(null, (double) blockPos.getX(), (double) blockPos.getY(), (double) blockPos.getZ(), SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                    itemstack.shrink(1);
                    if (itemstack.isEmpty()) {
                        inventory.setItem(i, ItemStack.EMPTY);
                    }
                    break;
                }
            }

        }
    }
}
