package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.MinerEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.inventory.Inventory;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.ForgeEventFactory;

import java.util.EnumSet;
import java.util.Optional;

public class MinerMine8x8PitGoal extends Goal {
    private final MinerEntity miner;
    private final double speedModifier;
    private final double within;
    private BlockPos minePos;
    private BlockPos standPos;
    private int blocks;
    private int side;
    private int depth;

    public MinerMine8x8PitGoal(MinerEntity miner, double v, double within) {
        this.miner = miner;
        this.speedModifier = v;
        this.within = within;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    public boolean canUse() {
        if (!this.miner.getStartPos().isPresent()) {
            return false;
        }
        if (this.miner.getFollow()) {
            return false;

        } else if (this.miner.getStartPos().get().closerThan(miner.position(), within) && !this.miner.getFollow() && miner.getMineType() == 3)

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
        miner.resetWorkerParameters();
    }

    @Override
    public void stop() {
        super.stop();
        restetCounts();
    }

    public void tick() {
        if (miner.getFollow() || !miner.getIsWorking()){
            restetCounts();
        }

        if (!miner.getFollow()) {
            if (miner.getMineDirection().equals(Direction.EAST)) {
                this.minePos = new BlockPos(miner.getStartPos().get().getX() + blocks, miner.getStartPos().get().getY() - depth, miner.getStartPos().get().getZ() - side);
                this.standPos = new BlockPos(minePos.getX() + 2, minePos.getY(), minePos.getZ());

            } else if (miner.getMineDirection().equals(Direction.WEST)) {
                this.minePos = new BlockPos(miner.getStartPos().get().getX() - blocks, miner.getStartPos().get().getY() - depth, miner.getStartPos().get().getZ() + side);
                this.standPos = new BlockPos(minePos.getX() - 2, minePos.getY(), minePos.getZ());

            } else if (miner.getMineDirection().equals(Direction.NORTH)) {
                this.minePos = new BlockPos(miner.getStartPos().get().getX() - side, miner.getStartPos().get().getY() - depth, miner.getStartPos().get().getZ() - blocks);
                this.standPos = new BlockPos(minePos.getX(), minePos.getY(), minePos.getZ() - 2);

            } else if (miner.getMineDirection().equals(Direction.SOUTH)) {
                this.minePos = new BlockPos(miner.getStartPos().get().getX() + side, miner.getStartPos().get().getY() - depth, miner.getStartPos().get().getZ() + blocks);
                this.standPos = new BlockPos(minePos.getX(), minePos.getY(), minePos.getZ() + 2);
            }

            if (!minePos.closerThan(miner.position(), 2) && !miner.getIsPickingUp()){
                this.miner.getNavigation().moveTo(minePos.getX(), minePos.getY(), minePos.getZ(),1);
            }

            if (minePos.closerThan(miner.position(), 4)){
                this.miner.getLookControl().setLookAt(minePos.getX(), minePos.getY() + 1, minePos.getZ(), 10.0F, (float) this.miner.getMaxHeadXRot());
            }

            BlockState blockstate = miner.level.getBlockState(minePos);
            Block block1 = blockstate.getBlock();
            /*
            BlockState blockstate2 = miner.level.getBlockState(minePos.above());
            Block block2 = blockstate2.getBlock();
             */
            this.miner.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3D);
            //erst mienen wenn nah genug
            if (minePos.closerThan(miner.position(), 6)) this.mineBlock(this.minePos);
            //if (miner.getOwner() !=null)
            //miner.getOwner().sendMessage(new StringTextComponent("Blocks: " + blocks +"   Side: " + side + "   Depth: " + depth), miner.getOwner().getUUID());

            if (miner.shouldIgnorBlock(block1) || block1 == Blocks.OAK_PLANKS) {
                blocks++;
                if (block1 != Blocks.OAK_PLANKS) placePlanks();

            }

            if (blocks == 8) {
                blocks = 0;
                side++;
                this.miner.setIsPickingUp(true);
            }

            if (side == 8){
                side = 0;
                depth++;
            }

            if (depth == 8){
                miner.setStartPos(Optional.empty());
                miner.setIsWorking(false);
            }
        }
    }

    private void mineBlock(BlockPos blockPos){
        if (this.miner.isAlive() && ForgeEventFactory.getMobGriefingEvent(this.miner.level, this.miner) && !miner.getFollow()) {
            BlockState blockstate = this.miner.level.getBlockState(blockPos);
            Block block = blockstate.getBlock();

            if (!miner.shouldIgnorBlock(block) && block != Blocks.OAK_PLANKS) {

                if (miner.getCurrentTimeBreak() % 5 == 4) {
                    miner.level.playLocalSound(blockPos.getX(), blockPos.getY(), blockPos.getZ(), blockstate.getSoundType().getHitSound(), SoundCategory.BLOCKS, 1F, 0.75F, false);
                }

                int bp = (int) (blockstate.getDestroySpeed(this.miner.level, blockPos) * 30);
                this.miner.setBreakingTime(bp);

                this.miner.setCurrentTimeBreak(this.miner.getCurrentTimeBreak() + (int) (1 * (this.miner.getUseItem().getDestroySpeed(blockstate))));
                float f = (float) this.miner.getCurrentTimeBreak() / (float) this.miner.getBreakingTime();

                int i = (int) (f * 10);

                if (i != this.miner.getPreviousTimeBreak()) {
                    this.miner.level.destroyBlockProgress(1, blockPos, i);
                    this.miner.setPreviousTimeBreak(i);
                }

                if (this.miner.getCurrentTimeBreak() == this.miner.getBreakingTime()) {
                    this.miner.level.destroyBlock(blockPos, true, this.miner);
                    this.miner.setCurrentTimeBreak(-1);
                    this.miner.setBreakingTime(0);
                }
                miner.changeTool(blockstate);
                if (this.miner.getRandom().nextInt(5) == 0) {
                    if (!this.miner.swinging) {
                        this.miner.swing(this.miner.getUsedItemHand());
                    }
                }
            }
        }

    }

    public void restetCounts(){
        blocks = 0;
        side = 0;
        depth = 0;
    }

    public boolean shouldPlacePlanks(){
        if (side == 0) {
            if (blocks == depth) return true;

        }
        return false;
    }

    public void placePlanks(){
        if (shouldPlacePlanks() && hasPlanksInInv()){
            miner.level.setBlock(this.minePos, Blocks.OAK_PLANKS.defaultBlockState(), 3);
            miner.level.playSound(null, (double) this.minePos.getX(), (double) this.minePos.getY(), (double) this.minePos.getZ(), SoundEvents.WOOD_PLACE, SoundCategory.BLOCKS, 1.0F, 1.0F);
        }
    }

    private boolean hasPlanksInInv(){
        Inventory inventory = miner.getInventory();
        return true;
    }
}
