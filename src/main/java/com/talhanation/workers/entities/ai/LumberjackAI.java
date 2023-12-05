package com.talhanation.workers.entities.ai;

import com.talhanation.workers.Main;
import com.talhanation.workers.entities.LumberjackEntity;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;
import net.minecraftforge.event.ForgeEventFactory;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Random;

import static com.talhanation.workers.entities.LumberjackEntity.State.*;

public class LumberjackAI extends Goal {
    private final LumberjackEntity lumber;
    private BlockPos workPos;
    private LumberjackEntity.State state;

    public LumberjackAI(LumberjackEntity lumber) {
        this.lumber = lumber;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    public boolean canUse() {
        // Start AI if there are trees near the work place.
        return lumber.isTame() && lumber.getStartPos() != null;
    }

    public boolean canContinueToUse() {
        return this.canUse();
    }

    @Override
    public void start() {
        super.start();
        this.workPos = this.lumber.getStartPos();
        this.lumber.resetWorkerParameters();
        this.state = LumberjackEntity.State.fromIndex(lumber.getState());
    }

    public void tick() {
        this.breakLeaves();
        //TODO: Add memories of initial saplings/trees around the work position.
        //TODO: Replant if the blocks are AIR.
        //Main.LOGGER.info("LumberState: " +state);
        // Go back to assigned work position.
        switch (state){
            case IDLE -> {
                if(lumber.getStartPos() != null && lumber.canWork()){
                    setWorkState(CALC_WORK_POS);
                }
            }

            case CALC_WORK_POS ->  {
                if(!lumber.canWork()) this.setWorkState(STOP);
                if(lumber.getVehicle() != null) lumber.stopRiding();

                workPos = lumber.getStartPos();
                if (workPos != null){
                    setWorkState(WORKING);
                }
                else setWorkState(STOP);;

            }

            case WORKING -> {
                if(!lumber.canWork()) this.setWorkState(STOP);
                if(lumber.getVehicle() != null) lumber.stopRiding();

                if(isInSapling()) {
                    lumber.walkTowards(lumber.getStartPos(), 1F);
                }

                if (workPos != null){
                    //Move to minePos -> normal movement
                    if(!workPos.closerThan(lumber.getOnPos(), 12)){
                        this.lumber.walkTowards(workPos, 1F);
                    }
                    //Near Mine Pos -> presice movement
                    if (!workPos.closerThan(lumber.getOnPos(), 3.5F)) {
                        this.lumber.getMoveControl().setWantedPosition(workPos.getX(), lumber.getStartPos().getY(), workPos.getZ(), 1);
                    }
                    else
                        lumber.getNavigation().stop();
                }
                else setWorkState(CALC_WORK_POS);
                // If near wood position, start chopping.
                BlockPos chopPos = getWoodPos();
                if (chopPos == null) return;

                BlockPos lumberPos = lumber.blockPosition();
                //this.lumber.walkTowards(chopPos, 1);
                this.lumber.getMoveControl().setWantedPosition(chopPos.getX(), chopPos.getY(), chopPos.getZ(), 1);
                boolean standingBelowChopPos = (
                        lumberPos.getX() == chopPos.getX() &&
                                lumberPos.getZ() == chopPos.getZ() &&
                                lumberPos.getY() < chopPos.getY());

                if (chopPos.closerThan(lumber.blockPosition(), 9) || standingBelowChopPos) {
                    if (this.mineBlock(chopPos))
                        this.lumber.increaseFarmedItems();

                    if(lumber.getReplantSaplings() && lumber.getCommandSenderWorld().getBlockState(chopPos.below()).is(Blocks.DIRT) && this.lumber.getCommandSenderWorld().isEmptyBlock(chopPos)) {
                        plantSaplingFromInv(chopPos);
                    }
                }
            }

            case STOP -> {
                lumber.stopRiding();
                if(lumber.needsToSleep()){
                    setWorkState(SLEEP);
                }

                else if(lumber.needsToDeposit()){
                    setWorkState(DEPOSIT);
                }

                else if(lumber.needsToGetFood()){
                    setWorkState(UPKEEP);
                }
                else{
                    this.lumber.walkTowards(workPos, 1);

                    double distance = lumber.distanceToSqr(lumber.getX(), lumber.getY(), lumber.getZ());
                    if(distance < 5.5F) { //valid value example: distance = 3.2
                        stop();
                    }
                }
            }

            case DEPOSIT -> {
                //Separate AI doing stuff
                lumber.stopRiding();
                if(!lumber.needsToDeposit()){
                    setWorkState(IDLE);
                }
            }

            case UPKEEP -> {
                //Separate AI doing stuff
                lumber.stopRiding();
                if(!lumber.needsToGetFood()){
                    setWorkState(IDLE);
                }
            }

            case SLEEP -> {
                //Separate AI doing stuff
                lumber.stopRiding();
                if(!lumber.needsToSleep()){
                    setWorkState(IDLE);
                }
            }
        }
    }

    public boolean isInSapling() {
        for(int i = -1; i <= 1; i++){
            BlockPos blockPos = lumber.getOnPos();

            if(this.lumber.getCommandSenderWorld().getBlockState(blockPos.above()).is(BlockTags.SAPLINGS)){
                return true;
            }
        }

        return false;
    }

    @Override
    public void stop() {
        this.setWorkState(IDLE);
        super.stop();
    }

    private void setWorkState(LumberjackEntity.@NotNull State state) {
        this.state = state;
        this.lumber.setState(state.getIndex());
    }

    @SuppressWarnings("deprecation")
    private void breakLeaves() {
        AABB boundingBox = this.lumber.getBoundingBox();
        double offset = 0.25D;
        BlockPos start = new BlockPos((int) (boundingBox.minX - offset), (int) (boundingBox.minY - offset), (int) (boundingBox.minZ - offset));
        BlockPos end = new BlockPos((int) (boundingBox.maxX + offset), (int) (boundingBox.maxY + offset), (int) (boundingBox.maxZ + offset));
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean hasBroken = false;
        if (this.lumber.getCommandSenderWorld().hasChunksAt(start, end)) {
            for (int i = start.getX(); i <= end.getX(); ++i) {
                for (int j = start.getY(); j <= end.getY(); ++j) {
                    for (int k = start.getZ(); k <= end.getZ(); ++k) {
                        pos.set(i, j, k);
                        BlockState blockstate = this.lumber.getCommandSenderWorld().getBlockState(pos);
                        if (blockstate.is(BlockTags.LEAVES)) {
                            this.lumber.getCommandSenderWorld().destroyBlock(pos, true, this.lumber);
                            hasBroken = true;
                        }
                    }
                }
            }
        }

        if (hasBroken) {
            this.lumber.getCommandSenderWorld().playSound(null, this.lumber.getX(), this.lumber.getY(), this.lumber.getZ(), SoundEvents.GRASS_BREAK, SoundSource.BLOCKS, 1F, 0.9F + 0.2F);
            this.lumber.workerSwingArm();
        }
    }

    public BlockPos getWoodPos() {
        int range = 16;

        for (int j = 0; j < range; j++) {
            for (int i = 0; i < range; i++) {
                for(int k = 0; k < range * 2; k++){
                    BlockPos blockPos = workPos.offset((int) (j - range / 2F), k, (int) (i - range / 2F));

                    BlockState blockState = this.lumber.getCommandSenderWorld().getBlockState(blockPos);
                    if (this.lumber.wantsToBreak(blockState.getBlock())) {
                        return blockPos;
                    }
                }
            }
        }
        return null;
    }
/*
    public BlockPos getPlantPos() {
        int range = 16;
        Random random = new Random();
        for (int j = 0; j < range; j++) {
            BlockPos blockPos = this.lumber.getStartPos().offset(random.nextInt(10) - range / 2F, 1, random.nextInt(10) - range / 2F);


            if (this.lumber.getCommandSenderWorld().isEmptyBlock(blockPos) && lumber.getCommandSenderWorld().getBlockState(blockPos.below()).is(Blocks.GRASS_BLOCK)) {
                return blockPos;
            }
        }
        return null;
    }
*/

    private void plantSaplingFromInv(BlockPos blockPos) {
        SimpleContainer inventory = lumber.getInventory();

        for (int i = 0; i < inventory.getContainerSize(); ++i) {
            ItemStack itemstack = inventory.getItem(i);
            if (!itemstack.isEmpty() && itemstack.is(ItemTags.SAPLINGS)) {
                BlockState placedSaplingBlock = Block.byItem(itemstack.getItem()).defaultBlockState();
                this.lumber.getCommandSenderWorld().setBlock(blockPos, placedSaplingBlock, 3);
                lumber.getCommandSenderWorld().playSound(null, blockPos.getX(), blockPos.getY(), blockPos.getZ(), SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                itemstack.shrink(1);
                if (itemstack.isEmpty()) {
                    inventory.setItem(i, ItemStack.EMPTY);
                }
                break;
            }
        }
    }

    private boolean mineBlock(BlockPos blockPos){
        if (this.lumber.isAlive() && ForgeEventFactory.getMobGriefingEvent(this.lumber.getCommandSenderWorld(), this.lumber) && !lumber.getFollow()) {

            BlockState blockstate = this.lumber.getCommandSenderWorld().getBlockState(blockPos);
            Block block = blockstate.getBlock();
            ItemStack heldItem = this.lumber.getItemInHand(InteractionHand.MAIN_HAND);

            if (lumber.wantsToBreak(block)){
                if (lumber.getCurrentTimeBreak() % 5 == 4) {
                    lumber.getCommandSenderWorld().playLocalSound(blockPos.getX(), blockPos.getY(), blockPos.getZ(), blockstate.getSoundType().getHitSound(), SoundSource.BLOCKS, 1F, 0.75F, false);
                }

                //set max destroy speed
                int bp = (int) (blockstate.getDestroySpeed(this.lumber.getCommandSenderWorld(), blockPos) * 30);
                this.lumber.setBreakingTime(bp);

                //increase current
                this.lumber.setCurrentTimeBreak(this.lumber.getCurrentTimeBreak() + (int) (1 * (heldItem.getDestroySpeed(blockstate))));
                float f = (float) this.lumber.getCurrentTimeBreak() / (float) this.lumber.getBreakingTime();

                int i = (int) (f * 10);

                if (i != this.lumber.getPreviousTimeBreak()) {
                    this.lumber.getCommandSenderWorld().destroyBlockProgress(1, blockPos, i);
                    this.lumber.setPreviousTimeBreak(i);
                }

                if (this.lumber.getCurrentTimeBreak() >= this.lumber.getBreakingTime()) {
                    // Break the target block
                    this.lumber.getCommandSenderWorld().destroyBlock(blockPos, true, this.lumber);
                    this.lumber.setCurrentTimeBreak(-1);
                    this.lumber.setBreakingTime(0);
                    this.lumber.consumeToolDurability();
                    return true;
                }
                this.lumber.workerSwingArm();
            }
        }
        return false;
    }
}

