package com.talhanation.workers.entities.ai;

import com.talhanation.workers.WorkersMain;
import com.talhanation.workers.entities.LumberjackEntity;
import com.talhanation.workers.entities.workarea.LumberArea;
import com.talhanation.workers.world.NeededItem;
import com.talhanation.workers.world.Tree;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import com.talhanation.workers.WorkersMain;
import com.talhanation.workers.compat.DynamicTrees;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.*;

public class LumberjackWorkGoal extends Goal {

    public LumberjackEntity lumberjack;
    public State state;
    public String errorMessage;
    public boolean errorMessageDone;
    public BlockPos blockPos;
    public Stack<Tree> stackOfTrees;
    public Stack<BlockPos> stackToPlant;
    public Tree currentTree;
    public List<NeededItem> neededItems = new ArrayList<>();

    public LumberjackWorkGoal(LumberjackEntity lumberjack) {
        this.lumberjack = lumberjack;
        setFlags(EnumSet.of(Flag.LOOK, Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return !lumberjack.needsToSleep() && lumberjack.shouldWork() && !lumberjack.needsToGetToChest() && this.isAreaNotRemoved();
    }

    private boolean isAreaNotRemoved() {
        if(lumberjack.currentLumberArea == null || !lumberjack.currentLumberArea.isRemoved()) return true;
        else {
            lumberjack.currentLumberArea = null;
        }
        return false;
    }

    @Override
    public void start() {
        super.start();
        if(this.lumberjack.getCommandSenderWorld().isClientSide()) return;
        lumberjack.setFollowState(6); //Working
        setState(State.SELECT_WORK_AREA);
    }

    boolean workDone;

    @Override
    public void tick() {
        super.tick();
        if(this.lumberjack.getCommandSenderWorld().isClientSide()) return;
        if(state == null) return;
        if(blockPos != null) this.lumberjack.getLookControl().setLookAt(blockPos.getCenter());

        if(state == State.WOOD_CUTTING){
            if(this.breakBlocks(currentTree.getStackToBreak())) return;

            currentTree.setInWork(false);
            setState(State.SELECT_TREE);
        }

        if(lumberjack.tickCount % 10 != 0) return;

        switch(state){
            case SELECT_WORK_AREA ->{
                if(lumberjack.currentLumberArea != null) setState(State.MOVE_TO_WORK_AREA);

                List<LumberArea> areas = getAvailableWorkAreasByPriority((ServerLevel) lumberjack.getCommandSenderWorld(), lumberjack, lumberjack.currentLumberArea);

                if (!areas.isEmpty()) {
                    lumberjack.currentLumberArea = areas.get(0);
                }

                if(lumberjack.currentLumberArea == null) return;

                lumberjack.currentLumberArea.setBeingWorkedOn(true);
                this.lumberjack.currentLumberArea.setTime(0);
                workDone = false;
                setState(State.MOVE_TO_WORK_AREA);
            }

            case MOVE_TO_WORK_AREA ->{
                this.blockPos = null;
                if(this.moveToPosition(lumberjack.currentLumberArea.getOnPos(), 100)) return;

                setState(State.SCAN_TREES);
            }

            case SCAN_TREES ->{
                if(lumberjack.currentLumberArea.stackOfTrees.isEmpty()){
                    lumberjack.currentLumberArea.scanForTrees();
                }

                this.stackOfTrees = lumberjack.currentLumberArea.stackOfTrees;
                this.stackOfTrees.sort(Comparator.comparing(tree -> tree.getPosition().getCenter().distanceToSqr(lumberjack.position())));

                if(stackOfTrees.isEmpty()){
                    setState(State.PREPARE_PLANT_SAPLINGS);
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
                this.currentTree.setInWork(true);
                setState(State.MOVE_TO_TREE);
            }

            case MOVE_TO_TREE -> {
                if(this.moveToPosition(currentTree.getPosition(), 30)) return;

                setState(State.PREPARE_SHEAR_LEAVES);
            }

            case PREPARE_SHEAR_LEAVES -> {
                if(!lumberjack.currentLumberArea.getShearLeaves()){
                    setState(State.PREPARE_STRIP_LOGS);
                    return;
                }
                lumberjack.switchMainHandItem(itemStack -> itemStack.getItem() instanceof ShearsItem);

                boolean hasShears = lumberjack.getMainHandItem().getItem() instanceof ShearsItem;
                if(!hasShears){
                    this.neededItems.add(new NeededItem(stack -> stack.getItem() instanceof ShearsItem, 1, true));
                    this.blockPos = null;
                    return;
                }

                setState(State.SHEAR_LEAVES);
            }

            case SHEAR_LEAVES -> {
                if(lumberjack.currentLumberArea.getShearLeaves() && this.shearLeaves(currentTree.getStackToShear())) return;

                setState(State.PREPARE_STRIP_LOGS);
            }

            case PREPARE_STRIP_LOGS -> {
                if(!lumberjack.currentLumberArea.getStripLogs()){
                    setState(State.PREPARE_WOOD_CUTTING);
                    return;
                }

                lumberjack.switchMainHandItem(itemStack -> itemStack.getItem() instanceof AxeItem);

                boolean hasAxe = lumberjack.getMainHandItem().getItem() instanceof AxeItem;
                if(!hasAxe){
                    this.neededItems.add(new NeededItem(stack -> stack.getItem() instanceof AxeItem, 1, true));
                    this.blockPos = null;
                    return;
                }

                setState(State.STRIP_WOOD);
            }

            case STRIP_WOOD -> {
                if(lumberjack.currentLumberArea.getStripLogs() && this.stripLogs(currentTree.getStackToStrip())) return;

                setState(State.PREPARE_WOOD_CUTTING);
            }

            case PREPARE_WOOD_CUTTING -> {
                lumberjack.switchMainHandItem(itemStack -> itemStack.getItem() instanceof AxeItem);

                boolean hasAxe = lumberjack.getMainHandItem().getItem() instanceof AxeItem;
                if(!hasAxe){
                    this.neededItems.add(new NeededItem(stack -> stack.getItem() instanceof AxeItem, 1, true));
                    this.blockPos = null;
                    return;
                }

                setState(State.WOOD_CUTTING);
            }

            case WOOD_CUTTING -> {

            }

            case PREPARE_PLANT_SAPLINGS -> {
                if(!lumberjack.currentLumberArea.getReplant()){
                    setState(State.DONE);
                    return;
                }

                this.lumberjack.currentLumberArea.scanPlantArea();
                this.stackToPlant = lumberjack.currentLumberArea.stackToPlant;

                if(stackToPlant.isEmpty()){
                    setState(State.DONE);
                    return;
                }

                setState(State.PLANT_SAPLINGS);
            }

            case PLANT_SAPLINGS -> {
                if(lumberjack.currentLumberArea.getReplant() && this.plantSaplings(lumberjack.currentLumberArea.getStackToPlant())) return;

                setState(State.DONE);
            }

            case DONE -> {
                if(!workDone){
                    workDone = true;
                    lumberjack.currentLumberArea.setBeingWorkedOn(false);
                    lumberjack.setFollowState(0); //Wandering
                    blockPos = null;
                    lumberjack.currentLumberArea = null;

                    if(!this.neededItems.isEmpty()){
                        for(NeededItem neededItem : neededItems){
                            this.lumberjack.addNeededItem(neededItem);
                        }
                        this.neededItems.clear();
                    }

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
        //if(lumberjack.getOwner() != null) lumberjack.getOwner().sendSystemMessage(Component.literal(state.toString()));
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
            Block strippedBlock = LumberArea.getStrippedBlock(state);

            if(strippedBlock != null){
                this.lumberjack.getCommandSenderWorld().setBlock(blockPos, strippedBlock.defaultBlockState(), 3);
                this.lumberjack.getCommandSenderWorld().playSound(null, blockPos, SoundEvents.AXE_STRIP, SoundSource.BLOCKS, 1.0F, 1.0F);

                this.lumberjack.swing(InteractionHand.MAIN_HAND);

                if(!positions.isEmpty()){
                    blockPos = positions.pop();
                }
                else{
                    this.blockPos = null;
                    return false;
                }
            }
            else{
                this.blockPos = null;
                return false;
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

    public boolean plantSaplings(Stack<BlockPos> positions){
        if(positions != null){
            ItemStack saplingFromInv;

            if(lumberjack.currentLumberArea.getSaplingStack().isEmpty()){
                saplingFromInv = lumberjack.getMatchingItem(itemStack ->
                        (itemStack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof SaplingBlock)
                                || (WorkersMain.isDynamicTreesInstalled && DynamicTrees.isDynamicTreesSeed(itemStack))
                );
                if(saplingFromInv == null){
                    this.neededItems.add(new NeededItem(itemStack ->
                            (itemStack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof SaplingBlock)
                                    || (WorkersMain.isDynamicTreesInstalled && DynamicTrees.isDynamicTreesSeed(itemStack)),
                            8, true));
                    this.blockPos = null;
                    return false;
                }

            }
            else{
                saplingFromInv = lumberjack.getMatchingItem(itemStack -> itemStack.is(lumberjack.currentLumberArea.getSaplingStack().getItem())
                        || (WorkersMain.isDynamicTreesInstalled && DynamicTrees.isDynamicTreesSeed(itemStack)));
                if(saplingFromInv == null){
                    this.neededItems.add(new NeededItem(itemStack ->
                            (itemStack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof SaplingBlock)
                                    || (WorkersMain.isDynamicTreesInstalled && DynamicTrees.isDynamicTreesSeed(itemStack)),
                            8, true));
                    this.blockPos = null;
                    return false;
                }
            }

            if(blockPos == null){
                if(!positions.isEmpty()) blockPos = positions.pop();
                return blockPos != null;
            }

            BlockState state = lumberjack.getCommandSenderWorld().getBlockState(blockPos);
            if(!state.isAir()){
                if(!positions.isEmpty()){
                    blockPos = positions.pop();
                }
                else{
                    this.blockPos = null;
                    return false;
                }
            }

            if(this.moveToPosition(blockPos, 60)) return true;

            if (WorkersMain.isDynamicTreesInstalled && DynamicTrees.isDynamicTreesSeed(saplingFromInv)) {
                if (DynamicTrees.plantSeed(lumberjack.getCommandSenderWorld(), blockPos, saplingFromInv)) {
                    lumberjack.getCommandSenderWorld().playSound(null, blockPos.getX(), blockPos.getY(), blockPos.getZ(), SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0F, 1.0F);
                    saplingFromInv.shrink(1);
                    this.lumberjack.swing(InteractionHand.MAIN_HAND);
                }
            }
            else if (saplingFromInv.getItem() instanceof BlockItem blockItem) {
                lumberjack.getCommandSenderWorld().setBlockAndUpdate(blockPos, blockItem.getBlock().defaultBlockState());

                lumberjack.getCommandSenderWorld().playSound(null, blockPos.getX(), blockPos.getY(), blockPos.getZ(), SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0F, 1.0F);
                saplingFromInv.shrink(1);
                this.lumberjack.swing(InteractionHand.MAIN_HAND);
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

    public static List<LumberArea> getAvailableWorkAreasByPriority(ServerLevel level, LumberjackEntity lumberjack, @Nullable LumberArea currentArea) {
        List<LumberArea> list = level.getEntitiesOfClass(LumberArea.class, lumberjack.getBoundingBox().inflate(64));

        Map<LumberArea, Integer> priorityMap = new HashMap<>();

        for (LumberArea area : list) {
            if (area == null || area == currentArea || !area.canWorkHere(lumberjack)) continue;

            int priority = 0;

            boolean perfectCandidate = area.isWorkerPerfectCandidate(lumberjack);

            if (perfectCandidate) {
                priority += 10;
            } else {
                priority += 1;
            }

            if (!area.isBeingWorkedOn()) {
                priority += 3;
            }

            priority += area.time;

            priorityMap.put(area, priority);
        }

        List<LumberArea> sorted = new ArrayList<>(priorityMap.keySet());
        sorted.sort((a, b) -> Integer.compare(priorityMap.get(b), priorityMap.get(a)));

        return sorted;
    }



    public boolean moveToPosition(BlockPos pos, int threshold){
        if(pos == null){
            return false;
        }
        else{
            double distance = lumberjack.getHorizontalDistanceTo(pos.getCenter());
            if(distance < threshold){
                return false;
            }
            else{
                lumberjack.getNavigation().moveTo(pos.getX(), pos.getY(), pos.getZ(), 0.8F);
                lumberjack.getLookControl().setLookAt(pos.getCenter());
            }
            return true;
        }
    }

    public enum State{
        SELECT_WORK_AREA,
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