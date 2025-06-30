package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.LumberjackEntity;
import com.talhanation.workers.entities.workarea.LumberArea;
import com.talhanation.workers.world.NeededItem;
import com.talhanation.workers.world.Tree;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Stack;

public class LumberjackWorkGoal extends Goal {

    public LumberjackEntity lumberjack;
    public State state;
    public String errorMessage;
    public boolean errorMessageDone;
    public LumberArea currentLumberArea;
    public BlockPos blockPos;
    public Stack<Tree> stackOfTrees;
    public Stack<BlockPos> stackToPlant;
    public Tree currentTree;

    public LumberjackWorkGoal(LumberjackEntity lumberjack) {
        this.lumberjack = lumberjack;
        setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return !lumberjack.needsToSleep() && lumberjack.getFollowState() != 1 && !lumberjack.needsToGetToChest();
    }

    @Override
    public void start() {
        super.start();
        List<LumberArea> list = lumberjack.getCommandSenderWorld().getEntitiesOfClass(LumberArea.class, lumberjack.getBoundingBox().inflate(32));

        list.sort(Comparator.comparing(LumberArea::isBeingWorkedOn));

        list.sort(Comparator.comparing(workAreaEntity -> workAreaEntity.position().distanceToSqr(lumberjack.position())));

        list.removeIf(workAreaEntity -> !workAreaEntity.canWorkHere(this.lumberjack));
        list.removeIf(LumberArea::isDone);
        if(list.isEmpty()) return;

        this.currentLumberArea = list.get(0);

        if(currentLumberArea == null) return;

        currentLumberArea.setBeingWorkedOn(true);
        workDone = false;
        setState(State.MOVE_TO_WORK_AREA);
    }
    boolean workDone;
    @Override
    public void tick() {
        super.tick();
        if(this.lumberjack.getCommandSenderWorld().isClientSide()) return;
        if(state == null) return;
        if(blockPos != null) this.lumberjack.getLookControl().setLookAt(blockPos.getCenter());

        if(lumberjack.tickCount % 10 != 0) return;

        switch(state){
            case MOVE_TO_WORK_AREA ->{
                if(this.moveToPosition(currentLumberArea.getOnPos(), 100)) return;

                setState(State.SCAN_TREES);
            }

            case SCAN_TREES ->{
                currentLumberArea.scanForTrees();

                this.stackOfTrees = currentLumberArea.stackOfTrees;
                this.stackOfTrees.sort(Comparator.comparing(tree -> tree.getPosition().getCenter().distanceToSqr(lumberjack.position())));

                if(stackOfTrees.isEmpty()){

                    setState(State.DONE);//state = State.PREPARE_PLANT_SAPLINGS;
                    return;
                }

                setState(State.SELECT_TREE);
            }

            case SELECT_TREE -> {
                if(stackOfTrees.isEmpty()){
                    setState(State.SCAN_TREES);
                    return;
                }

                this.currentTree = this.stackOfTrees.pop();

                setState(State.MOVE_TO_TREE);
            }

            case MOVE_TO_TREE -> {
                if(this.moveToPosition(currentTree.getPosition(), 30)) return;

                setState(State.PREPARE_SHEAR_LEAVES);
            }
            case PREPARE_SHEAR_LEAVES -> {
                if(!currentLumberArea.getShearLeaves()){
                    setState(State.PREPARE_STRIP_LOGS);
                    return;
                }
                lumberjack.switchMainHandItem(itemStack -> itemStack.getItem() instanceof ShearsItem);

                boolean hasShears = lumberjack.getMainHandItem().getItem() instanceof ShearsItem;
                if(!hasShears){
                    lumberjack.addNeededItem(new NeededItem(stack -> stack.getItem() instanceof ShearsItem, 1, false));
                    this.blockPos = null;
                    return;
                }

                setState(State.SHEAR_LEAVES);
            }
            case SHEAR_LEAVES -> {
                if(currentLumberArea.getShearLeaves() && this.shearLeaves(currentTree.getStackToShear())) return;

                setState(State.PREPARE_STRIP_LOGS);
            }
            case PREPARE_STRIP_LOGS -> {
                if(!currentLumberArea.getStripLogs()){
                    setState(State.PREPARE_WOOD_CUTTING);
                    return;
                }

                lumberjack.switchMainHandItem(itemStack -> itemStack.getItem() instanceof AxeItem);

                boolean hasAxe = lumberjack.getMainHandItem().getItem() instanceof AxeItem;
                if(!hasAxe){
                    lumberjack.addNeededItem(new NeededItem(stack -> stack.getItem() instanceof AxeItem, 1, false));
                    this.blockPos = null;
                    return;
                }

                setState(State.STRIP_WOOD);
            }
            case STRIP_WOOD -> {
                if(currentLumberArea.getStripLogs() && this.stripLogs(currentTree.getStackToStrip())) return;

                setState(State.PREPARE_WOOD_CUTTING);
            }
            case PREPARE_WOOD_CUTTING -> {
                lumberjack.switchMainHandItem(itemStack -> itemStack.getItem() instanceof AxeItem);

                boolean hasAxe = lumberjack.getMainHandItem().getItem() instanceof AxeItem;
                if(!hasAxe){
                    lumberjack.addNeededItem(new NeededItem(stack -> stack.getItem() instanceof AxeItem, 1, false));
                    this.blockPos = null;
                    return;
                }

                setState(State.WOOD_CUTTING);
            }
            case WOOD_CUTTING -> {
                if(this.breakBlocks(currentTree.getStackToBreak())) return;

                setState(State.SELECT_TREE);
            }

            case PREPARE_PLANT_SAPLINGS -> {


                setState(State.PLANT_SAPLINGS);
            }

            case PLANT_SAPLINGS -> {
                if(currentLumberArea.getReplant()){

                }
                else{

                    setState(State.DONE);
                }
            }

            case DONE -> {
                if(!workDone){
                    workDone = true;
                    currentLumberArea.setDone(true);
                    currentLumberArea.setBeingWorkedOn(false);
                    currentLumberArea = null;
                    this.stop();
                }
            }

            case ERROR ->{

            }
        }
    }

    public void setState(State state) {
        if(lumberjack.getOwner() != null) lumberjack.getOwner().sendSystemMessage(Component.literal(state.toString()));
        this.state = state;
    }

    int blockBreakTime;
    public boolean breakBlocks(Stack<BlockPos> positions){
        if(positions != null){
            if(blockPos == null){
                if(!positions.isEmpty()) blockPos = positions.pop();
                return blockPos != null;
            }

            BlockState state = lumberjack.getCommandSenderWorld().getBlockState(blockPos);
            if(state.isAir()){
                if(!positions.isEmpty()){
                    blockPos = positions.pop();
                }
                else{
                    this.blockPos = null;
                    return false;
                }
                blockBreakTime = 0;

            }
            else{
                this.lumberjack.mineBlock(blockPos);
                this.lumberjack.swing(InteractionHand.MAIN_HAND);
            }
            return true;
        }
        return false;
    }

    public boolean stripLogs(Stack<BlockPos> positions){
        if(positions != null){
            if(blockPos == null){
                if(!positions.isEmpty()) blockPos = positions.pop();
                return blockPos != null;
            }

            BlockState state = lumberjack.getCommandSenderWorld().getBlockState(blockPos);
            if(AxeItem.STRIPPABLES.containsValue(state.getBlock())){
                if(!positions.isEmpty()){
                    blockPos = positions.pop();
                }
                else{
                    this.blockPos = null;
                    return false;
                }
                blockBreakTime = 0;
            }
            else{
                Block strippedBlock = AxeItem.STRIPPABLES.get(state.getBlock());

                if(strippedBlock == null){
                    this.blockPos = null;
                    return false;
                }

                this.lumberjack.getCommandSenderWorld().setBlock(blockPos, strippedBlock.defaultBlockState(), 3);
                this.lumberjack.getCommandSenderWorld().playSound(null, blockPos, SoundEvents.AXE_STRIP, SoundSource.BLOCKS, 1.0F, 1.0F);

                this.lumberjack.swing(InteractionHand.MAIN_HAND);
            }
            return true;
        }
        return false;
    }

    public boolean shearLeaves(Stack<BlockPos> positions){
        if(positions != null){
            if(blockPos == null){
                if(!positions.isEmpty()) blockPos = positions.pop();
                return blockPos != null;
            }

            BlockState state = lumberjack.getCommandSenderWorld().getBlockState(blockPos);
            if(state.isAir()){
                if(!positions.isEmpty()){
                    blockPos = positions.pop();
                }
                else{
                    this.blockPos = null;
                    return false;
                }
                blockBreakTime = 0;

            }
            else{
                this.lumberjack.mineBlock(blockPos);
                this.lumberjack.swing(InteractionHand.MAIN_HAND);
            }
            return true;
        }
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

    public boolean moveToPosition(BlockPos pos, int threshold){
        if(pos == null){
            return false;
        }
        else{
            double distance = pos.getCenter().distanceToSqr(lumberjack.position());
            if(distance < threshold){
                return false;
            }
            else{
                lumberjack.getNavigation().moveTo(pos.getX(), pos.getY(), pos.getZ(), 0.8F);
                lumberjack.setFollowState(6); //Working
                lumberjack.getLookControl().setLookAt(pos.getCenter());
            }
            return true;
        }
    }

    public enum State{
        MOVE_TO_WORK_AREA,
        SCAN_TREES,
        SELECT_TREE,
        MOVE_TO_TREE,
        PREPARE_SHEAR_LEAVES,
        SHEAR_LEAVES,
        PREPARE_STRIP_LOGS,
        STRIP_WOOD,
        PREPARE_WOOD_CUTTING,
        WOOD_CUTTING,
        PREPARE_PLANT_SAPLINGS,
        PLANT_SAPLINGS,
        DONE,
        ERROR

    }
}
