package com.talhanation.workers.entities.ai;

import com.google.common.collect.ImmutableSet;
import com.talhanation.workers.entities.FarmerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.ForgeEventFactory;

import java.util.EnumSet;
import java.util.Random;
import java.util.Set;

import static com.talhanation.workers.entities.FarmerEntity.CROP_BLOCKS;
import static com.talhanation.workers.entities.FarmerEntity.WANTED_SEEDS;

public class FarmerAI extends Goal {
    private final FarmerEntity farmer;
    private BlockPos workPos;
    private BlockPos waterPos;
    private enum State {
        PLOWING,
        PLANTING,
        FERTILIZING,
        HARVESTING
    };
    private State state;

    public FarmerAI(FarmerEntity farmer) {
        this.farmer = farmer;
        this.setFlags(EnumSet.of(Flag.MOVE));
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
        this.state = State.PLOWING;
        farmer.resetWorkerParameters();
    }

    @Override
    public void stop() {
        super.stop();
        state = State.PLOWING;
    }

    public void tick() {
        if (!this.farmer.swinging) {
            this.waterPos = this.farmer.getStartPos();

            if (workPos != null && !workPos.closerThan(farmer.blockPosition(), 10D)) {
                this.farmer.walkTowards(workPos, 1);
            }

            switch (state) {
                case PLOWING -> {
                    if (!startPosIsWater()) {
                        this.farmer.walkTowards(waterPos, 1.2);
                        if (waterPos.closerThan(farmer.blockPosition(), 4)) {
                            farmer.workerSwingArm();
                            this.prepareWaterPos();
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
                }
                case PLANTING -> {
                    if (!hasSeedInInv()) {
                        state = State.FERTILIZING;
                        break;
                    }
                    this.workPos = getPlantPos();
                    if (this.workPos == null) {
                        state = State.FERTILIZING;
                        break;
                    }
                    this.farmer.walkTowards(workPos, 0.7);
                    if (workPos.closerThan(farmer.blockPosition(), 3)) {
                        farmer.workerSwingArm();
                        this.plantSeedsFromInv(workPos);
                    }
                }
                case FERTILIZING -> {
                    if(!hasBone()){
                        state = State.HARVESTING;
                        break;
                    }
                    this.workPos = getFertilizePos();
                    if (this.workPos == null) {
                        state = State.HARVESTING;
                        break;
                    }
                    this.farmer.walkTowards(workPos, 0.7);
                    if (workPos.closerThan(farmer.blockPosition(), 3)) {
                        farmer.workerSwingArm();
                        this.fertilizeSeeds(workPos);
                    }
                }

                case HARVESTING -> {
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
                }
            }
        }
    }

    private boolean hasBone() {
        SimpleContainer inventory = farmer.getInventory();
        return inventory.hasAnyOf(ImmutableSet.of(Items.BONE_MEAL));
    }

    private void fertilizeSeeds(BlockPos workPos) {
        BlockState state = farmer.getLevel().getBlockState(workPos);
        Block block = state.getBlock();
        if(block instanceof BonemealableBlock crop && !this.farmer.getLevel().isClientSide()){
            crop.performBonemeal((ServerLevel) farmer.getLevel(), farmer.getRandom(), workPos, state);
            if(crop.isBonemealSuccess((ServerLevel) farmer.getLevel(), farmer.getRandom(), workPos, state)){
                for (int i = 0; i < farmer.getInventory().getContainerSize(); ++i) {
                    ItemStack itemstack = farmer.getInventory().getItem(i);
                    if(itemstack.getItem() instanceof BoneMealItem) itemstack.shrink(1);
                }
            }
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
            return waterBlockState.is(Fluids.WATER) || waterBlockState.is(Fluids.FLOWING_WATER);
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
    private void prepareWaterPos(){
        if (waterPos != null) {
            BlockState blockState = this.farmer.getCommandSenderWorld().getBlockState(waterPos);
            FluidState waterBlockState = this.farmer.getCommandSenderWorld().getFluidState(waterPos);

            if (waterBlockState != Fluids.WATER.defaultFluidState() || waterBlockState != Fluids.FLOWING_WATER.defaultFluidState()){
                if(FarmerEntity.TILLABLES.contains(blockState.getBlock())){
                    farmer.getCommandSenderWorld().setBlock(waterPos, Blocks.WATER.defaultBlockState(), 3);
                    farmer.getCommandSenderWorld().playSound(null, waterPos.getX(), waterPos.getY(), waterPos.getZ(),
                            SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                }
                else return;
            }
            farmer.workerSwingArm();
        }
    }
    private void prepareFarmLand(BlockPos blockPos) {
        // Make sure the center block remains waterlogged.
        BlockState blockState = this.farmer.getCommandSenderWorld().getBlockState(blockPos);
        Block block = blockState.getBlock();
        if(blockPos != waterPos && FarmerEntity.TILLABLES.contains(block)) {
            farmer.getCommandSenderWorld().setBlock(blockPos, Blocks.FARMLAND.defaultBlockState(), 3);
            farmer.getCommandSenderWorld().playSound(null, blockPos.getX(), blockPos.getY(), blockPos.getZ(), SoundEvents.HOE_TILL,
                    SoundSource.BLOCKS, 1.0F, 1.0F);
            BlockState blockStateAbove = this.farmer.getCommandSenderWorld().getBlockState(blockPos.above());
            Block blockAbove = blockStateAbove.getBlock();
            farmer.workerSwingArm();

            if (blockAbove instanceof BushBlock || blockAbove instanceof GrowingPlantBlock) {
                farmer.getCommandSenderWorld().destroyBlock(blockPos.above(), false);
                farmer.getCommandSenderWorld().playSound(null, blockPos.getX(), blockPos.getY(), blockPos.getZ(), SoundEvents.GRASS_BREAK,
                        SoundSource.BLOCKS, 1.0F, 1.0F);
            }
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

                boolean canSustainSeeds = FarmerEntity.TILLABLES.contains(block);
                boolean hasSpaceAbove = (aboveBlockState.is(Blocks.AIR) || aboveBlockState.is(BlockTags.REPLACEABLE));

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
                    BlockPos belowBlockPos = blockPos.below();
                    BlockState belowBlockState = this.farmer.level.getBlockState(belowBlockPos);

                    if (block instanceof CropBlock crop && belowBlockState.is(Blocks.FARMLAND)) {

                        if (crop.isMaxAge(blockState)) {
                            return blockPos;
                        }
                    }
                }
            }
        }
        return null;
    }

    public BlockPos getFertilizePos() {
        // int range = 8;
        for (int j = 0; j <= 8; j++) {
            for (int i = 0; i <= 8; i++) {
                BlockPos blockPos = waterPos.offset(j - 4, 1, i - 4);
                BlockState blockState = this.farmer.level.getBlockState(blockPos);
                Block block = blockState.getBlock();

                if (CROP_BLOCKS.contains(block)) {
                    BlockPos belowBlockPos = blockPos.below();
                    BlockState belowBlockState = this.farmer.level.getBlockState(belowBlockPos);

                    if (block instanceof CropBlock crop && belowBlockState.is(Blocks.FARMLAND)) {

                        if (!crop.isMaxAge(blockState)) {
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
