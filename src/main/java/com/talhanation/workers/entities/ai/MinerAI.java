package com.talhanation.workers.entities.ai;
import com.talhanation.workers.Translatable;
import com.talhanation.workers.entities.MinerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.ForgeEventFactory;

import java.util.EnumSet;

public class MinerAI extends Goal {

    private final MinerEntity miner;
    private BlockPos minePos;
    private MineType mineType;
    private WorkState workState;
    private boolean messageNoPickaxe;
    public MinerAI(MinerEntity miner) {
        this.miner = miner;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    public boolean canUse() {
        return this.miner.canWork() && miner.getMineType() != 0;
    }

    public boolean canContinueToUse() {
        return this.canUse();
    }

    @Override
    public void start() {
        super.start();
        this.miner.resetWorkerParameters();
        this.miner.resetCounts();
        this.mineType = getMineType();
        this.workState = WorkState.IDLE;
        this.messageNoPickaxe = false;
    }

    public void resetGoal(){
        miner.resetCounts();
        miner.setIsWorking(false);
        this.miner.setIsPickingUp(false);

        this.miner.setChecked(false);
        this.stop();
    }

    private enum WorkState {
        IDLE,
        TO_WORK_POS,
        WORKING,
        DONE,
    }
    private enum MineType {
        TUNNEL_1X2,
        TUNNEL_3X3,
        PIT_8X8X8,
        FLAT_8X8X1,
        ROOM_8X8X3
    }

    public void tick() {
        switch (workState){
            case IDLE -> {
                if(miner.getStartPos() != null) {
                    workState = WorkState.TO_WORK_POS;
                }
            }

            case TO_WORK_POS -> {
                miner.getNavigation().moveTo(miner.getStartPos().getX(), miner.getStartPos().getY(), miner.getStartPos().getZ(), 1.0F);

                if (miner.getStartPos().closerThan(miner.getOnPos(), 3)) {
                    workState = WorkState.WORKING;
                }
            }

            case WORKING -> {
                this.working();
            }

            case DONE -> {
                if(!this.miner.getChecked()){
                    miner.resetCounts();
                    this.miner.setChecked(true);
                    workState = WorkState.WORKING;
                }
                else {
                    miner.getNavigation().moveTo(miner.getStartPos().getX(), miner.getStartPos().getY(), miner.getStartPos().getZ(), 1.0F);

                    if (miner.getStartPos().closerThan(miner.getOnPos(), 3)) {
                        resetGoal();
                        workState = WorkState.IDLE;
                    }
                }
            }
        }
    }

    private void working(){
        //Handle Direction and assign minePos
        if (miner.getMineDirection().equals(Direction.EAST)) {
            this.minePos = new BlockPos(miner.getStartPos().getX() + miner.blocks, miner.getStartPos().getY() - miner.depth, miner.getStartPos().getZ() - miner.side);

        } else if (miner.getMineDirection().equals(Direction.WEST)) {
            this.minePos = new BlockPos(miner.getStartPos().getX() - miner.blocks, miner.getStartPos().getY() - miner.depth, miner.getStartPos().getZ() + miner.side);

        } else if (miner.getMineDirection().equals(Direction.NORTH)) {
            this.minePos = new BlockPos(miner.getStartPos().getX() - miner.side, miner.getStartPos().getY() - miner.depth, miner.getStartPos().getZ() - miner.blocks);

        } else if (miner.getMineDirection().equals(Direction.SOUTH)) {
            this.minePos = new BlockPos(miner.getStartPos().getX() + miner.side, miner.getStartPos().getY() - miner.depth, miner.getStartPos().getZ() + miner.blocks);
        }

        //Move to minePos -> normal movement
        if(!minePos.closerThan(miner.getOnPos(), 12)){
            this.miner.walkTowards(minePos, 1F);
        }
        //Near Mine Pos -> presice movement
        if (!minePos.closerThan(miner.getOnPos(), 3.5F)) {
            this.miner.getMoveControl().setWantedPosition(minePos.getX(), miner.getStartPos().getY(), minePos.getZ(), 1);
        }
        else
            miner.getNavigation().stop();

        if (minePos.closerThan(miner.getOnPos(), 3)){
            this.miner.getLookControl().setLookAt(minePos.getX(), minePos.getY() + 1, minePos.getZ(), 10.0F, (float) this.miner.getMaxHeadXRot());
        }
        BlockState blockstate = miner.level.getBlockState(minePos);
        Block block1 = blockstate.getBlock();
        AttributeInstance movSpeed = this.miner.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movSpeed != null) movSpeed.setBaseValue(0.3D);

        //break block if close enough
        if (minePos.closerThan(miner.getOnPos(), 6)){
            boolean needsPickaxe = blockstate.is(BlockTags.MINEABLE_WITH_PICKAXE);

            if(needsPickaxe && !miner.hasMainToolInInv()) {
                if(!messageNoPickaxe && this.miner.getOwner() != null){
                    this.miner.tellPlayer(miner.getOwner(), Translatable.TEXT_NO_PICKAXE);
                    messageNoPickaxe = true;
                }
                return;
            }

            if (this.mineBlock(minePos)) this.miner.increaseFarmedItems();
        }

        switch (mineType) {
            case PIT_8X8X8 -> {
                if (miner.shouldIgnoreBlock(block1) || block1 == Blocks.OAK_PLANKS) {
                    miner.blocks++;
                    if (block1 != Blocks.OAK_PLANKS) placePlanks();
                }

                if (miner.blocks == 8) {
                    miner.blocks = 0;
                    miner.side++;
                }

                if (miner.side == 8) {
                    miner.side = 0;
                    miner.depth++;
                }

                if (miner.depth >= 8) {
                    this.workState = WorkState.DONE;
                }
            }

            case ROOM_8X8X3 -> {
                if (miner.shouldIgnoreBlock(block1)) {
                    miner.depth--;
                }

                if (miner.depth == -3) {
                    miner.depth = 0;
                    miner.side++;
                }

                if (miner.side == 8) {
                    miner.side = 0;
                    miner.blocks++;
                }

                if (miner.blocks == 8) {
                    this.workState = WorkState.DONE;
                }
            }

            case FLAT_8X8X1 -> {
                if (miner.shouldIgnoreBlock(block1)) {
                    miner.blocks++;
                }

                if (miner.blocks == 8) {
                    miner.blocks = 0;
                    miner.side++;
                }

                if (miner.side == 8) {
                    this.workState = WorkState.DONE;
                }
            }

            case TUNNEL_3X3 -> {
                if (miner.shouldIgnoreBlock(block1)) {
                    miner.depth--;
                }

                if (miner.depth == -3) {
                    miner.depth = 0;
                    miner.side++;
                }

                if (miner.side == 3) {
                    miner.side = 0;
                    miner.blocks++;
                }

                if (miner.blocks == miner.getMineDepth()) {
                    this.workState = WorkState.DONE;
                }
            }

            case TUNNEL_1X2 -> {
                if (miner.shouldIgnoreBlock(block1)) {
                    miner.depth--;
                }

                if (miner.depth == -2) {
                    miner.depth = 0;
                    miner.blocks++;
                }

                if (miner.blocks == miner.getMineDepth()) {
                    this.workState = WorkState.DONE;
                }
            }
        }
    }

    private MineType getMineType() {
        return switch (miner.getMineType()) {
            case 1 -> MineType.TUNNEL_1X2;
            case 2 -> MineType.TUNNEL_3X3;
            case 3 -> MineType.PIT_8X8X8;
            case 4 -> MineType.FLAT_8X8X1;
            case 5 -> MineType.ROOM_8X8X3;
            default -> throw new IllegalStateException("Unexpected value: " + miner.getMineType());
        };
    }

    private boolean mineBlock(BlockPos blockPos){
        if (this.miner.isAlive() && ForgeEventFactory.getMobGriefingEvent(this.miner.level, this.miner) && !miner.getFollow()) {
            BlockState blockstate = this.miner.level.getBlockState(blockPos);
            Block block = blockstate.getBlock();

            this.miner.changeTool(blockstate);

            ItemStack heldItem = this.miner.getItemInHand(InteractionHand.MAIN_HAND);

            if (!miner.shouldIgnoreBlock(block)){
                if (miner.getCurrentTimeBreak() % 5 == 4) {
                    miner.level.playLocalSound(blockPos.getX(), blockPos.getY(), blockPos.getZ(), blockstate.getSoundType().getHitSound(), SoundSource.BLOCKS, 1F, 0.75F, false);
                }

                //set max destroy speed
                int bp = (int) (blockstate.getDestroySpeed(this.miner.level, blockPos) * 30);
                this.miner.setBreakingTime(bp);

                //increase current
                this.miner.setCurrentTimeBreak(this.miner.getCurrentTimeBreak() + (int) (1 * (heldItem.getDestroySpeed(blockstate))));
                float f = (float) this.miner.getCurrentTimeBreak() / (float) this.miner.getBreakingTime();

                int i = (int) (f * 10);

                if (i != this.miner.getPreviousTimeBreak()) {
                    this.miner.level.destroyBlockProgress(1, blockPos, i);
                    this.miner.setPreviousTimeBreak(i);
                }

                if (this.miner.getCurrentTimeBreak() >= this.miner.getBreakingTime()) {
                    // Break the target block
                    this.miner.level.destroyBlock(blockPos, true, this.miner);
                    this.miner.setCurrentTimeBreak(-1);
                    this.miner.setBreakingTime(0);
                    this.miner.consumeToolDurability();
                    return true;
                }
                this.miner.workerSwingArm();
            }
        }
        return false;
    }
    public boolean shouldPlacePlanks(){
        if (miner.side == 0) {
            return (miner.blocks -1) == miner.depth;

        }
        return false;
    }

    public void placePlanks(){
        if (shouldPlacePlanks()) {// && hasPlanksInInv()){
            miner.level.setBlock(this.minePos, Blocks.OAK_PLANKS.defaultBlockState(), 3);
            miner.level.playSound(null, this.minePos.getX(), this.minePos.getY(), this.minePos.getZ(), SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }
}
