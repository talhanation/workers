package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.FarmerEntity;
import net.minecraft.core.BlockPos;
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
import java.util.Random;
import static com.talhanation.workers.entities.FarmerEntity.CROP_BLOCKS;
import static com.talhanation.workers.entities.FarmerEntity.WANTED_SEEDS;

public class FarmerCropAI extends Goal {
    private final FarmerEntity farmer;
    private BlockPos workPos;
    private BlockPos waterPos;
    private enum State {
        PLOWING,
        PLANTING,
        HARVESTING
    };
    private State state;

    public FarmerCropAI(FarmerEntity farmer) {
        this.farmer = farmer;
        this.setFlags(EnumSet.of(Flag.MOVE));
        this.state = State.PLOWING;
    }

    public boolean canUse() {
        return this.farmer.canWork();
    }

    public boolean canContinueToUse() {
        return this.canUse();
    }

    @Override
    public void start() {
        super.start();

        farmer.resetWorkerParameters();
        this.waterPos = farmer.getStartPos();
    }

    @Override
    public void stop() {
        super.stop();
        state = State.PLOWING;
    }

    public void tick() {
        // TODO: Fix bug where farmer infinitely replants farmland above crops. 
            // Goes normal after manually doing this.workPos = getHoePos()
            // Apparently reproducible by setting the workPos while hovering a planted crop.
            // Ideally the farmer should tell you his workPos is not valid and setWorkPos(null).

        if (workPos != null && !workPos.closerThan(farmer.blockPosition(), 10D)) {
            this.farmer.walkTowards(workPos, 1);
        }

        switch (state) {
            case PLOWING:
                if (!startPosIsWater()) {
                    this.farmer.walkTowards(waterPos, 1.2);
                    if (waterPos.closerThan(farmer.blockPosition(), 4)) {
                        farmer.workerSwingArm();
                        this.prepareFarmLand(waterPos);
                    }
                }
                this.workPos = getHoePos();
                if (workPos != null) {
                    this.farmer.walkTowards(workPos, 1);
                    if (workPos.closerThan(farmer.blockPosition(), 3)) {
                        farmer.workerSwingArm();
                        this.prepareFarmLand(workPos);
                    }
                } else {
                    state = State.PLANTING;
                }
                break;
            case PLANTING:
                if (!hasSeedInInv()) {
                    state = State.HARVESTING;
                    break;
                }

                this.workPos = getPlantPos();
                if (this.workPos == null) {
                    state = State.HARVESTING;
                    break;
                }
                
                this.farmer.walkTowards(workPos, 0.7);
                if (workPos.closerThan(farmer.blockPosition(), 3)) {
                    farmer.workerSwingArm();
                    this.plantRandomSeed(workPos);
                }
                break;
            case HARVESTING:
                if (hasSpaceInInv())
                    this.workPos = getHarvestPos();

                if (workPos != null) {
                    this.farmer.walkTowards(workPos, 0.6);
                    if (workPos.closerThan(farmer.blockPosition(), 3)) {
                        farmer.workerSwingArm();
                        boolean blockWasMined = this.mineBlock(workPos);
                        if (blockWasMined) {
                            this.farmer.increaseFarmedItems();
                            this.farmer.consumeToolDurability();
                        }
                    }
                } else
                    state = State.PLOWING;
                break;
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

    private boolean startPosIsWater() {
        if (this.waterPos != null) {
            FluidState waterBlockState = this.farmer.level.getFluidState(this.waterPos);
            if (waterBlockState.is(Fluids.WATER) || waterBlockState.is(Fluids.FLOWING_WATER)) {
                return true;
            }
        }
        return false;
    }

    private void plantRandomSeed(BlockPos aboveFarmlandPos) {
        SimpleContainer inventory = farmer.getInventory();

        // Durstenfeld's Shuffle
        // https://en.wikipedia.org/wiki/Fisher%E2%80%93Yates_shuffle#The_modern_algorithm
        Random random = new Random();
        for (int i = inventory.getContainerSize() - 1; i > 0; i--) {
            int index = random.nextInt(i + 1);
            ItemStack foundSeed = inventory.getItem(index);
            inventory.setItem(index, inventory.getItem(i));
            inventory.setItem(i, foundSeed);

            if (foundSeed.isEmpty()) {
                continue;
            }

            BlockState foundSeedBlock = null;
            if (foundSeed.getItem() == Items.CARROT) {
                foundSeedBlock = Blocks.CARROTS.defaultBlockState();
            } else if (foundSeed.getItem() == Items.POTATO) {
                foundSeedBlock = Blocks.POTATOES.defaultBlockState();
    
            } else if (foundSeed.getItem() == Items.WHEAT_SEEDS) {
                foundSeedBlock = Blocks.WHEAT.defaultBlockState();
    
            } else if (foundSeed.getItem() == Items.BEETROOT_SEEDS) {
                foundSeedBlock = Blocks.BEETROOTS.defaultBlockState();
            }
            if (foundSeedBlock != null) {
                this.placeSeedInWorld(aboveFarmlandPos, i, foundSeed, foundSeedBlock);
                break;
            }   
        }
    }

    private void placeSeedInWorld(BlockPos blockPos, int inventoryIndex, ItemStack seed, BlockState foundCrop) {
        seed.shrink(1);
        if (seed.isEmpty()) {
            this.farmer.updateInventory(inventoryIndex, ItemStack.EMPTY);
        }
        this.farmer.level.setBlock(blockPos, foundCrop, 3);
        farmer.level.playSound(
            null, 
            (double) blockPos.getX(), 
            (double) blockPos.getY(),
            (double) blockPos.getZ(), 
            SoundEvents.GRASS_PLACE, 
            SoundSource.BLOCKS, 
            1.0F, 
            1.0F
        );
    }

    private void prepareFarmLand(BlockPos blockPos) {
        // Make sure the center block remains waterlogged.
        if (waterPos != null) {
            FluidState waterBlockState = this.farmer.level.getFluidState(waterPos);

            if (
                waterBlockState != Fluids.WATER.defaultFluidState() || 
                waterBlockState != Fluids.FLOWING_WATER.defaultFluidState()
            ) {
                farmer.level.setBlock(waterPos, Blocks.WATER.defaultBlockState(), 3);
                farmer.level.playSound(null, blockPos.getX(), blockPos.getY(), blockPos.getZ(),
                        SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            farmer.workerSwingArm();
        }

        farmer.level.setBlock(blockPos, Blocks.FARMLAND.defaultBlockState(), 3);
        farmer.level.playSound(null, blockPos.getX(), blockPos.getY(), blockPos.getZ(), SoundEvents.HOE_TILL,
                SoundSource.BLOCKS, 1.0F, 1.0F);
        BlockState blockState = this.farmer.level.getBlockState(blockPos.above());
        Block block = blockState.getBlock();
        farmer.workerSwingArm();

        if (block instanceof BushBlock || block instanceof GrowingPlantBlock) {
            farmer.level.destroyBlock(blockPos.above(), false);
            farmer.level.playSound(null, blockPos.getX(), blockPos.getY(), blockPos.getZ(), SoundEvents.GRASS_BREAK,
                    SoundSource.BLOCKS, 1.0F, 1.0F);
        }

    }
    // TODO: Selecting a growing crop as a startPos adds 1 extra y level, 1 block starting above the farmer.
    public BlockPos getHoePos() {
        // int range = 8;
        for (int j = 0; j <= 8; j++) {
            for (int i = 0; i <= 8; i++) {
                BlockPos blockPos = this.waterPos.offset(j - 4, 0, i - 4);
                BlockPos aboveBlockPos = blockPos.above();
                BlockState blockState = this.farmer.level.getBlockState(blockPos);
                BlockState aboveBlockState = this.farmer.level.getBlockState(aboveBlockPos);

                Block block = blockState.getBlock();
                Block aboveBlock = aboveBlockState.getBlock();
                boolean canSustainSeeds = block == Blocks.GRASS_BLOCK || block == Blocks.DIRT;
                boolean hasSpaceAbove = (
                    aboveBlockState.is(Blocks.AIR) || 
                    aboveBlock instanceof BushBlock || 
                    aboveBlock instanceof GrowingPlantBlock
                );
                if (canSustainSeeds && hasSpaceAbove) {
                    return blockPos;
                }
            }
        }
        return null;
    }

    public BlockPos getPlantPos() {
        // int range = 8;
        for (int j = 0; j <= 8; j++) {
            for (int i = 0; i <= 8; i++) {
                BlockPos blockPos = this.waterPos.offset(j - 4, 0, i - 4);
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
        // int range = 8;
        for (int j = 0; j <= 8; j++) {
            for (int i = 0; i <= 8; i++) {
                BlockPos blockPos = waterPos.offset(j - 4, 1, i - 4);
                BlockState blockState = this.farmer.level.getBlockState(blockPos);

                Block block = blockState.getBlock();
                if (CROP_BLOCKS.contains(block)) {
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

    private boolean mineBlock(BlockPos blockPos) {
        if (this.farmer.isAlive() && ForgeEventFactory.getMobGriefingEvent(this.farmer.level, this.farmer)
                && !farmer.getFollow()) {

            BlockState blockstate = this.farmer.level.getBlockState(blockPos);
            Block block = blockstate.getBlock();

            if ((block != Blocks.AIR)) {
                if (farmer.getCurrentTimeBreak() % 5 == 4) {
                    farmer.level.playLocalSound(blockPos.getX(), blockPos.getY(), blockPos.getZ(),
                            blockstate.getSoundType().getHitSound(), SoundSource.BLOCKS, 1F, 0.75F, false);
                }

                // set max destroy speed
                int bp = (int) (blockstate.getDestroySpeed(this.farmer.level, blockPos) * 30);
                this.farmer.setBreakingTime(bp);

                // increase current
                this.farmer.setCurrentTimeBreak(this.farmer.getCurrentTimeBreak()
                        + (int) (1 * (this.farmer.getUseItem().getDestroySpeed(blockstate))));
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
