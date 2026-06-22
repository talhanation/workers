package com.talhanation.workers.entities.ai;

import com.google.common.collect.ImmutableSet;
import com.talhanation.workers.entities.MinerEntity;
import com.talhanation.workers.entities.workarea.MiningArea;
import com.talhanation.workers.world.NeededItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.*;

public class MinerWorkGoal extends Goal {

    public MinerEntity minerEntity;
    public State state;
    public String errorMessage;
    public boolean errorMessageDone;
    public BlockPos blockPos;
    public Stack<BlockPos> stackToBreak;
    public Stack<BlockPos> stackToPlace;
    private boolean needToSeeBlock;

    public MinerWorkGoal(MinerEntity minerEntity) {
        this.minerEntity = minerEntity;
        setFlags(EnumSet.of(Flag.LOOK, Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return !minerEntity.needsToSleep() && minerEntity.shouldWork() && !minerEntity.needsToGetToChest() && this.isMiningAreaAvailable();
    }

    @Override
    public void start() {
        super.start();
        if(this.minerEntity.getCommandSenderWorld().isClientSide()) return;
        if(minerEntity.getFollowState() == 0) minerEntity.setFollowState(6); //Working

        setState(State.SELECT_WORK_AREA);
    }
    boolean workDone;
    @Override
    public void tick() {
        super.tick();
        if(this.minerEntity.getCommandSenderWorld().isClientSide()) return;
        if(state == null) return;
        if(blockPos != null) this.minerEntity.getLookControl().setLookAt(blockPos.getCenter());

        if(!isMiningAreaAvailable()) return;

        if(checkPlaceTorch()) return;

        if(state != State.SELECT_WORK_AREA && this.minerEntity.currentMiningArea == null){
            this.blockPos = null;
            setState(State.SELECT_WORK_AREA);
            return;
        }

        if(minerEntity.tickCount % 20 == 0){
            if(blockPos != null && moveToPosition(blockPos, 20)) return;
        }

        if(state == State.MINING){
            if(this.mineBlocks(this.stackToBreak)) return;

            setState(State.PREPARE_MINE_WALLS);
        }

        if(state == State.MINE_WALLS){
            if(this.mineBlocks(this.stackToBreak)) return;

            setState(State.CHECK);
        }

        if(minerEntity.tickCount % 5 != 0) return;
        switch(state){
            case SELECT_WORK_AREA ->{
                if(minerEntity.currentMiningArea != null) setState(State.MOVE_TO_WORK_AREA);

                List<MiningArea> areas = getAvailableWorkAreasByPriority((ServerLevel) minerEntity.getCommandSenderWorld(), minerEntity, minerEntity.currentMiningArea);

                if (!areas.isEmpty()) {
                    minerEntity.currentMiningArea = areas.get(0);
                }

                if(minerEntity.currentMiningArea == null) return;

                minerEntity.currentMiningArea.setBeingWorkedOn(true);
                this.minerEntity.currentMiningArea.setTime(0);
                workDone = false;
                setState(State.MOVE_TO_WORK_AREA);
            }

            case MOVE_TO_WORK_AREA ->{
                this.blockPos = null;
                if(this.moveToPosition(minerEntity.currentMiningArea.getOnPos(), 10)) return;

                setState(State.PREPARE_CLOSE_FLOOR);
            }

            case PREPARE_MINING -> {
                this.blockPos = null;

                if(this.minerEntity.currentMiningArea.stackToBreak.isEmpty()){
                    this.minerEntity.currentMiningArea.scanBreakArea();
                }

                this.stackToBreak = this.minerEntity.currentMiningArea.stackToBreak;

                if(stackToBreak.isEmpty()){
                    setState(State.PREPARE_MINE_WALLS);
                    return;
                }

                minerEntity.switchMainHandItem(itemStack -> itemStack.getItem() instanceof PickaxeItem);

                boolean hasAxe = minerEntity.getMainHandItem().getItem() instanceof PickaxeItem;
                if(!hasAxe){
                    minerEntity.addNeededItem(new NeededItem(stack -> stack.getItem() instanceof PickaxeItem, 1, true));
                    this.blockPos = null;
                    return;
                }

                boolean areaHasShovelBlocks = stackToBreak.stream().anyMatch(pos ->
                        minerEntity.getCommandSenderWorld().getBlockState(pos).is(BlockTags.MINEABLE_WITH_SHOVEL));
                boolean hasShovel = minerEntity.getInventory().hasAnyMatching(itemStack -> itemStack.getItem() instanceof ShovelItem);
                if(areaHasShovelBlocks && !hasShovel){
                    minerEntity.addNeededItem(new NeededItem(stack -> stack.getItem() instanceof ShovelItem, 1, true));
                    this.blockPos = null;
                    return;
                }

                needToSeeBlock = true;
                setState(State.MINING);
            }

            case PREPARE_MINE_WALLS -> {
                this.minerEntity.currentMiningArea.scanForOresOnWalls();

                this.stackToBreak = minerEntity.currentMiningArea.stackToBreak;
                if(stackToBreak.isEmpty()){
                    setState(State.CHECK);
                    return;
                }

                minerEntity.switchMainHandItem(itemStack -> itemStack.getItem() instanceof PickaxeItem);

                boolean hasAxe = minerEntity.getMainHandItem().getItem() instanceof PickaxeItem;
                if(!hasAxe){
                    minerEntity.addNeededItem(new NeededItem(stack -> stack.getItem() instanceof PickaxeItem, 1, true));
                    this.blockPos = null;
                    return;
                }
                needToSeeBlock = false;
                setState(State.MINE_WALLS);
            }

            case PREPARE_CLOSE_FLOOR -> {
                if(!this.minerEntity.currentMiningArea.getCloseFloor() && !this.minerEntity.currentMiningArea.getCloseFluids()){
                    setState(State.PREPARE_MINING);
                    return;
                }

                if(minerEntity.currentMiningArea.stackToPlace.isEmpty()){
                    this.minerEntity.currentMiningArea.scanFloorArea();
                }

                this.stackToPlace = minerEntity.currentMiningArea.stackToPlace;

                if(stackToPlace.isEmpty()){
                    setState(State.PREPARE_MINING);
                    return;
                }

                setState(State.CLOSE_FLOOR);
            }

            case CLOSE_FLOOR -> {
                if(!this.minerEntity.currentMiningArea.getCloseFloor() && !this.minerEntity.currentMiningArea.getCloseFluids()){
                    setState(State.PREPARE_MINING);
                    return;
                }

                minerEntity.switchMainHandItem(itemStack -> itemStack.is(Items.COBBLESTONE));

                if(this.closeHoles(this.stackToPlace)) return;

                setState(State.PREPARE_MINING);
            }

            case CHECK -> {
                this.minerEntity.currentMiningArea.scanBreakArea();
                this.stackToBreak = this.minerEntity.currentMiningArea.stackToBreak;

                if(stackToBreak.isEmpty()){
                    setState(State.DONE);
                }
                else{
                    setState(State.PREPARE_CLOSE_FLOOR);
                }
            }

            case DONE -> {
                this.minerEntity.currentMiningArea.setDone(true);
                if(this.minerEntity.getFollowState() == 6) this.minerEntity.setFollowState(0);//Wander
                blockPos = null;
                minerEntity.currentMiningArea = null;
                this.start();

                this.minerEntity.forcedDeposit = true;
            }

            case ERROR ->{
                if(!errorMessageDone){
                    errorMessageDone = true;
                }
            }
        }
    }

    private boolean checkPlaceTorch() {
        if(minerEntity.getCommandSenderWorld().getRawBrightness(minerEntity.getOnPos().above(), 0) <= 7){
            BlockState onPosState = minerEntity.getCommandSenderWorld().getBlockState(minerEntity.getOnPos());
            BlockState stateAbove = minerEntity.getCommandSenderWorld().getBlockState(minerEntity.getOnPos().above());
            if(stateAbove.isAir() && !onPosState.isAir()){
                placeTorch();
                return true;
            }

        }
        return false;
    }

    public void placeTorch(){
        if (minerEntity.getInventory().hasAnyOf(ImmutableSet.of(Items.TORCH))){
            minerEntity.switchMainHandItem(itemStack -> itemStack.is(Items.TORCH));

            minerEntity.getCommandSenderWorld().setBlock(minerEntity.getOnPos().above(), Blocks.TORCH.defaultBlockState(), 3);
            minerEntity.getCommandSenderWorld().playSound(null, this.minerEntity.getX(), this.minerEntity.getY(), this.minerEntity.getZ(), SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);

            for (int i = 0; i < minerEntity.getInventory().getContainerSize(); ++i) {
                ItemStack itemstack = minerEntity.getInventory().getItem(i);
                if(itemstack.is(Items.TORCH)) itemstack.shrink(1);
            }
        }
        else{
            NeededItem torch = new NeededItem(itemStack -> itemStack.is(Items.TORCH), 16, true);
            if(!minerEntity.neededItems.contains(torch)) minerEntity.addNeededItem(torch);
        }
    }

    private boolean isMiningAreaAvailable() {
        if(minerEntity.currentMiningArea == null || !minerEntity.currentMiningArea.isRemoved()) return true;
        else {
            minerEntity.currentMiningArea = null;
        }
        return false;
    }

    public void setState(State state) {
        if(minerEntity.getOwner() != null) minerEntity.getOwner().sendSystemMessage(Component.literal(state.toString()));
        this.state = state;
    }

    int blockBreakTime;
    public boolean mineBlocks(Stack<BlockPos> positions){
        if(positions != null){
            if(blockPos == null){
                if(!positions.isEmpty()){
                    blockPos = this.getNewMiningPosition(positions);
                }
                return blockPos != null;
            }

            BlockState state = minerEntity.getCommandSenderWorld().getBlockState(blockPos);
            if(state.isAir() || minerEntity.shouldIgnoreBlock(state)){
                if(!positions.isEmpty()){
                    blockPos = this.getNewMiningPosition(positions);
                }
                else{
                    this.blockPos = null;
                    return false;
                }
                blockBreakTime = 0;
            }
            else{
                this.minerEntity.changeTool(state);

                this.minerEntity.mineBlock(blockPos);
                this.minerEntity.swing(InteractionHand.MAIN_HAND);
            }
            return true;
        }
        return false;
    }

    private BlockPos getNewMiningPosition(Stack<BlockPos> positions) {
        positions.sort(Comparator.comparingDouble(
                pos -> minerEntity.position().distanceToSqr(pos.getCenter())
        ));
        positions.sort(Comparator.reverseOrder());

        BlockPos newPosition = null;

        if(blockPos == null){
            if(this.needToSeeBlock){
                positions.removeIf(pos ->
                        !canSeeBlock(minerEntity.getCommandSenderWorld(), minerEntity.position().add(0, 1, 0), pos)
                );

            }

            if(positions.isEmpty()){
                setState(State.MOVE_TO_WORK_AREA);
                return null;
            }

            newPosition = positions.pop();
        }
        else if(positions.contains(blockPos.above())){
            newPosition = blockPos.above();
        }
        else if(positions.contains(blockPos.below())){
            newPosition = blockPos.below();
        }
        else{
            newPosition = positions.pop();
        }
        positions.remove(newPosition);
        return newPosition;
    }

    //PERFORMANCE HEAVY DO NOT USE FREQUENTLY
    private boolean canSeeBlock(Level level, Vec3 start, BlockPos target) {
        Vec3 targetCenter = target.getCenter();
        ClipContext ctx = new ClipContext(start, targetCenter, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, minerEntity);
        BlockPos ctxPos = level.clip(ctx).getBlockPos();
        return ctxPos.equals(target);
    }

    public boolean closeHoles(Stack<BlockPos> positions){
        if(positions != null){
            ItemStack cobbleBlockFromInv;

            cobbleBlockFromInv = minerEntity.getMatchingItem(itemStack -> itemStack.getItem() instanceof BlockItem blockItem && blockItem.getBlock().defaultBlockState().is(Blocks.COBBLESTONE));
            if(cobbleBlockFromInv == null){
                minerEntity.addNeededItem(new NeededItem(itemStack -> itemStack.getItem() instanceof BlockItem blockItem && blockItem.getBlock().defaultBlockState().is(Blocks.COBBLESTONE),  16, true));
                this.blockPos = null;
                return false;
            }

            if(blockPos == null){
                if(!positions.isEmpty()){
                    blockPos = positions.pop();
                }
                else{
                    return false;
                }
            }

            BlockState state = minerEntity.getCommandSenderWorld().getBlockState(blockPos);
            FluidState fluid = minerEntity.getCommandSenderWorld().getFluidState(blockPos);
            if(!state.isAir() && fluid.isEmpty()){
                if(!positions.isEmpty()){
                    blockPos = positions.pop();
                }
                else{
                    return false;
                }
            }
            else if(cobbleBlockFromInv.getItem() instanceof BlockItem blockItem) {
                minerEntity.getCommandSenderWorld().setBlockAndUpdate(blockPos, blockItem.getBlock().defaultBlockState());
                minerEntity.getCommandSenderWorld().playSound(null, blockPos.getX(), blockPos.getY(), blockPos.getZ(), SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                cobbleBlockFromInv.shrink(1);
                this.minerEntity.swing(InteractionHand.MAIN_HAND);
            }
            return true;
        }
        this.blockPos = null;
        return false;
    }
    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public boolean isInterruptable() {
        return true;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    public static List<MiningArea> getAvailableWorkAreasByPriority(ServerLevel level, MinerEntity minerEntity, @Nullable MiningArea currentArea) {
        List<MiningArea> list = level.getEntitiesOfClass(MiningArea.class, minerEntity.getBoundingBox().inflate(64));

        Map<MiningArea, Integer> priorityMap = new HashMap<>();

        for (MiningArea area : list) {
            if (area == null || area == currentArea || !area.canWorkHere(minerEntity)) continue;

            if(area.isDone()) continue;

            int priority = 0;

            boolean perfectCandidate = area.isWorkerPerfectCandidate(minerEntity);

            if (perfectCandidate) {
                priority += 10;
            } else {
                priority += 1;
            }

            if (!area.isBeingWorkedOn()) {
                priority += 3;
            }

            priority += area.time;

            //double dist = area.position().distanceToSqr(minerEntity.position());
            //priority -= dist / 10.0;

            priorityMap.put(area, priority);
        }


        List<MiningArea> sorted = new ArrayList<>(priorityMap.keySet());
        sorted.sort((a, b) -> Integer.compare(priorityMap.get(b), priorityMap.get(a)));

        return sorted;
    }



    public boolean moveToPosition(BlockPos pos, int threshold){
        if(pos == null){
            return false;
        }
        else{
            double distance = minerEntity.getHorizontalDistanceTo(pos.getCenter());
            if(distance < threshold){
                minerEntity.getNavigation().stop();
                return false;
            }
            else{

                // start() already claimed the working state; calling it here every
                // tick would override owner commands and pull the worker back to work.
                minerEntity.getNavigation().moveTo(pos.getX(), pos.getY(), pos.getZ(), 0.8F);
                minerEntity.getLookControl().setLookAt(pos.getCenter());
            }
            return true;
        }
    }

    public enum State{
        SELECT_WORK_AREA,
        MOVE_TO_WORK_AREA,
        PREPARE_MINE_WALLS,
        MINE_WALLS,
        PREPARE_CLOSE_FLOOR,
        CLOSE_FLOOR,
        PREPARE_MINING,
        MINING,
        CHECK,
        DONE,
        ERROR

    }
}
