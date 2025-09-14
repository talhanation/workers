package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.MinerEntity;
import com.talhanation.workers.entities.workarea.MiningArea;
import com.talhanation.workers.world.NeededItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

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

    public MinerWorkGoal(MinerEntity minerEntity) {
        this.minerEntity = minerEntity;
        setFlags(EnumSet.of(Flag.LOOK, Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return !minerEntity.needsToSleep() && minerEntity.getFollowState() != 1 && !minerEntity.needsToGetToChest();
    }

    @Override
    public void start() {
        super.start();
        if(this.minerEntity.getCommandSenderWorld().isClientSide()) return;


        setState(State.SELECT_WORK_AREA);
    }
    boolean workDone;
    @Override
    public void tick() {
        super.tick();
        if(this.minerEntity.getCommandSenderWorld().isClientSide()) return;
        if(state == null) return;
        if(blockPos != null) this.minerEntity.getLookControl().setLookAt(blockPos.getCenter());


        //if(!minerEntity.isWorkAreaNotRemoved()) return;


        //if(minerEntity.tickCount % 5 != 0) return;
        if(blockPos != null && moveToPosition(blockPos, 30)) return;

        if(state == State.MINING){
            if(this.mineBlocks(this.stackToBreak)) return;

            setState(State.PREPARE_CLOSE_HOLES);
        }

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
                if(this.moveToPosition(minerEntity.currentMiningArea.getOnPos(), 100)) return;
                this.blockPos = null;
                setState(State.PREPARE_MINING);
            }

            case PREPARE_MINING -> {
                this.minerEntity.currentMiningArea.scanBreakArea();

                this.stackToBreak = this.minerEntity.currentMiningArea.stackToBreak;


                if(stackToBreak.isEmpty()){
                    setState(State.PREPARE_MINE_WALLS);
                    return;
                }

                minerEntity.switchMainHandItem(itemStack -> itemStack.getItem() instanceof PickaxeItem);

                setState(State.MINING);
            }

            case MINING -> {
                //IS HANDLED ABOVE
            }

            case PREPARE_MINE_WALLS -> {
                this.minerEntity.currentMiningArea.scanForOresOnWalls();

                this.stackToBreak = minerEntity.currentMiningArea.stackToBreak;

                if(stackToBreak.isEmpty()){
                    setState(State.PREPARE_CLOSE_HOLES);
                    return;
                }

                minerEntity.switchMainHandItem(itemStack -> itemStack.getItem() instanceof PickaxeItem);

                boolean hasAxe = minerEntity.getMainHandItem().getItem() instanceof PickaxeItem;
                if(!hasAxe){
                    minerEntity.addNeededItem(new NeededItem(stack -> stack.getItem() instanceof PickaxeItem, 1, false));
                    this.blockPos = null;
                    return;
                }

                setState(State.MINING);
            }


            case PREPARE_CLOSE_HOLES -> {
                this.minerEntity.currentMiningArea.scanClosingArea();
                this.stackToPlace = minerEntity.currentMiningArea.stackToPlace;

                if(stackToPlace.isEmpty()){
                    setState(State.DONE);
                    return;
                }

                setState(State.CLOSE_HOLES);
            }

            case CLOSE_HOLES -> {
                if(this.closeHoles(this.stackToPlace)) return;

                setState(State.DONE);
            }

            case DONE -> {
                if(!workDone){
                    workDone = true;
                    minerEntity.currentMiningArea.setBeingWorkedOn(false);
                    blockPos = null;
                    minerEntity.currentMiningArea = null;
                    this.start();
                }
            }

            case ERROR ->{
                if(!errorMessageDone){
                    errorMessageDone = true;
                }
            }
        }
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
            if(state.isAir()){
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
                this.minerEntity.mineBlock(blockPos);
                this.minerEntity.swing(InteractionHand.MAIN_HAND);
            }
            return true;
        }
        return false;
    }

    private BlockPos getNewMiningPosition(Stack<BlockPos> positions) {
        positions.sort(Comparator.comparing(pos -> minerEntity.position().distanceToSqr(pos.getCenter())));
        positions.sort(Comparator.reverseOrder());
        BlockPos newPosition;

        if(positions.contains(blockPos.above())){
            newPosition = blockPos.above();
        }
        else if(positions.contains(blockPos.below())){
            newPosition = blockPos.below();
        }
        else{
            newPosition = positions.pop();
        }

        return newPosition;
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
                if(!positions.isEmpty()) blockPos = positions.pop();
                return blockPos != null;
            }

            BlockState state = minerEntity.getCommandSenderWorld().getBlockState(blockPos);
            if(!state.isAir()){
                if(!positions.isEmpty()){
                    blockPos = positions.pop();
                }
                else{
                    this.blockPos = null;
                    return false;
                }
            }
            else if(cobbleBlockFromInv.getItem() instanceof BlockItem blockItem) {
                minerEntity.getCommandSenderWorld().setBlockAndUpdate(blockPos, blockItem.getBlock().defaultBlockState());
                minerEntity.getCommandSenderWorld().playSound(null, blockPos.getX(), blockPos.getY(), blockPos.getZ(), SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0F, 1.0F);
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
        return false;
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
                return false;
            }
            else{
                //minerEntity.getNavigation().stop();
                minerEntity.getNavigation().moveTo(pos.getX(), pos.getY(), pos.getZ(), 0.8F);
                minerEntity.setFollowState(6); //Working
                minerEntity.getLookControl().setLookAt(pos.getCenter());
            }
            return true;
        }
    }

    public enum State{
        SELECT_WORK_AREA,
        MOVE_TO_WORK_AREA,
        SCAN_BLOCKS,
        PREPARE_MINE_WALLS,
        MINE_WALLS,
        PREPARE_CLOSE_HOLES,
        CLOSE_HOLES,
        PREPARE_MINING,
        MINING,
        DONE,
        ERROR

    }
}
