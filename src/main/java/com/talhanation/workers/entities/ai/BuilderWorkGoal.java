package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.BuilderEntity;
import com.talhanation.workers.entities.workarea.BuildArea;
import com.talhanation.workers.world.BuildBlock;
import com.talhanation.workers.world.BuildBlockParse;
import com.talhanation.workers.world.NeededItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class BuilderWorkGoal extends Goal {

    public BuilderEntity builderEntity;
    public State state;
    public String errorMessage;
    public boolean errorMessageDone;
    public BlockPos blockPos;
    public Stack<BlockPos> stackToBreak;
    public Stack<BlockPos> stackToPlace;
    public int minBuildHeight;

    public BuilderWorkGoal(BuilderEntity builderEntity) {
        this.builderEntity = builderEntity;
        setFlags(EnumSet.of(Flag.LOOK, Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return !builderEntity.needsToSleep() && builderEntity.getFollowState() != 1 && !builderEntity.needsToGetToChest() && this.isBuildingAreaAvailable();
    }

    @Override
    public void start() {
        super.start();
        if(this.builderEntity.getCommandSenderWorld().isClientSide()) return;


        setState(State.SELECT_WORK_AREA);
    }
    boolean workDone;
    @Override
    public void tick() {
        super.tick();
        if(this.builderEntity.getCommandSenderWorld().isClientSide()) return;
        if(state == null) return;
        if(blockPos != null) this.builderEntity.getLookControl().setLookAt(blockPos.getCenter());

        if(!isBuildingAreaAvailable()) return;

        if(state != State.SELECT_WORK_AREA && this.builderEntity.currentBuildArea == null){
            this.blockPos = null;
            setState(State.SELECT_WORK_AREA);
            return;
        }

        if(builderEntity.tickCount % 20 == 0){
            if(blockPos != null && moveToPosition(blockPos, 40)) return;


            if(state == State.BREAK_BLOCKS){
                if(this.mineBlocks(this.stackToBreak)) return;

                setState(State.PREPARE_PLACE_BLOCKS);
            }
        }

        if(builderEntity.tickCount % 5 != 0) return;
        switch(state){
            case SELECT_WORK_AREA ->{
                if(builderEntity.currentBuildArea != null) setState(State.MOVE_TO_WORK_AREA);

                List<BuildArea> areas = getAvailableWorkAreasByPriority((ServerLevel) builderEntity.getCommandSenderWorld(), builderEntity, builderEntity.currentBuildArea);

                if (!areas.isEmpty()) {
                    builderEntity.currentBuildArea = areas.get(0);
                }

                if(builderEntity.currentBuildArea == null) return;

                builderEntity.currentBuildArea.setBeingWorkedOn(true);
                this.builderEntity.currentBuildArea.setTime(0);
                workDone = false;
                setState(State.MOVE_TO_WORK_AREA);
            }

            case MOVE_TO_WORK_AREA ->{
                if(this.moveToPosition(builderEntity.currentBuildArea.getOnPos(), 70)) return;
                this.blockPos = null;
                setState(State.PREPARE_BREAK_BLOCKS);
            }

            case PREPARE_BREAK_BLOCKS -> {
                this.builderEntity.currentBuildArea.scanBreakArea();//SOMETHING WRONG I CAN FEEL IT

                this.stackToBreak = this.builderEntity.currentBuildArea.stackToBreak;

                if(stackToBreak.isEmpty()){
                    setState(State.PREPARE_PLACE_BLOCKS);
                    return;
                }

                builderEntity.switchMainHandItem(itemStack -> itemStack.getItem() instanceof PickaxeItem);

                boolean hasAxe = builderEntity.getMainHandItem().getItem() instanceof PickaxeItem;
                if(!hasAxe){
                    builderEntity.addNeededItem(new NeededItem(stack -> stack.getItem() instanceof PickaxeItem, 1, false));
                    this.blockPos = null;
                    return;
                }
                setState(State.BREAK_BLOCKS);
            }

            case PREPARE_PLACE_BLOCKS -> {
                if (builderEntity.currentBuildArea.stackToPlace.isEmpty()) {
                    setState(State.DONE);
                    return;
                }

                minBuildHeight = (int) builderEntity.currentBuildArea.getArea().maxY;

                for (BuildBlock bb : builderEntity.currentBuildArea.stackToPlace) {
                    int y = bb.getPos().getY();
                    if (y < minBuildHeight) {
                        minBuildHeight = y;
                    }
                }


                this.stackToPlace = new Stack<>();
                for (BuildBlock buildBlock : builderEntity.currentBuildArea.stackToPlace) {
                    BlockPos pos = buildBlock.getPos();
                    if (pos.getY() != minBuildHeight) continue;

                    Item item = BuildBlockParse.parseBlock(buildBlock.getState().getBlock()).getItem();
                    if(item == null){
                        if(builderEntity.getOwner() != null) builderEntity.getOwner().sendSystemMessage(Component.literal("Could not found item for " + buildBlock.getState().getBlock().getName() + " i, will skip this block." ));
                        builderEntity.currentBuildArea.removeBuildBlockToPlace(pos);
                        return;
                    }
                    else if (builderEntity.getInventory().hasAnyMatching(itemStack -> itemStack.is(item))) {
                        stackToPlace.push(pos);
                    }
                }

                if (stackToPlace.isEmpty()) {
                    List<ItemStack> neededItems = builderEntity.currentBuildArea.getRequiredMaterials();
                    neededItems.sort(Comparator.comparingInt(ItemStack::getCount).reversed());

                    Set<Item> allowedItems = builderEntity.currentBuildArea.stackToPlace.stream()
                            .filter(bb -> bb.getPos().getY() == minBuildHeight)
                            .map(bb -> BuildBlockParse.parseBlock(bb.getState().getBlock()).getItem())
                            .collect(Collectors.toSet());

                    neededItems.removeIf(stack -> !allowedItems.contains(stack.getItem()));

                    if (!neededItems.isEmpty()) {
                        ItemStack neededItem = neededItems.get(0);
                        int amount = Math.min(64, neededItem.getCount());

                        builderEntity.addNeededItem(new NeededItem(
                                itemStack -> itemStack.is(neededItem.getItem()), amount, false
                        ));
                    }
                }

                setState(State.PLACE_BLOCKS);
            }


            case PLACE_BLOCKS -> {
                if(this.placeBlocks(this.stackToPlace)) return;

                setState(State.PREPARE_PLACE_BLOCKS);
            }

            case DONE -> {
                if(!workDone){
                    workDone = true;
                    builderEntity.currentBuildArea.setBeingWorkedOn(false);

                    //ONLY FOR BUILDING AREA WILL REMOVE IT
                    this.builderEntity.currentBuildArea.setDone(true);

                    blockPos = null;
                    builderEntity.currentBuildArea = null;
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

    private boolean isBuildingAreaAvailable() {
        if(builderEntity.currentBuildArea == null || !builderEntity.currentBuildArea.isRemoved()) return true;
        else {
            builderEntity.currentBuildArea = null;
        }
        return false;
    }

    public void setState(State state) {
        //if(builderEntity.getOwner() != null) builderEntity.getOwner().sendSystemMessage(Component.literal(state.toString()));
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

            BlockState state = builderEntity.getCommandSenderWorld().getBlockState(blockPos);
            if(state.isAir() || builderEntity.shouldIgnoreBlock(state)){
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
                this.builderEntity.changeTool(state);

                this.builderEntity.mineBlock(blockPos);
                this.builderEntity.swing(InteractionHand.MAIN_HAND);
            }
            return true;
        }
        return false;
    }

    private BlockPos getNewMiningPosition(Stack<BlockPos> positions) {
        positions.sort(Comparator.comparingDouble(
                pos -> builderEntity.position().distanceToSqr(pos.getCenter())
        ));
        positions.sort(Comparator.reverseOrder());

        BlockPos newPosition = null;

        if(blockPos == null){
            positions.removeIf(pos ->
                    !canSeeBlock(builderEntity.getCommandSenderWorld(), builderEntity.position().add(0, 1, 0), pos)
            );

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
    public boolean placeBlocks(Stack<BlockPos> positions){
        if(positions != null){
            if(blockPos == null){
                if(!positions.isEmpty()){
                    blockPos = positions.pop();
                }
                else{
                    return false;
                }
            }

            BlockState buildingState = builderEntity.currentBuildArea.getStateFromPos(blockPos);
            BlockState levelState = builderEntity.getCommandSenderWorld().getBlockState(blockPos);

            if(builderEntity.currentBuildArea.statesMatch(levelState, buildingState)){
                if(!positions.isEmpty()){
                    blockPos = positions.pop();
                }
                else{
                    return false;
                }
            }
            else if(buildingState != null) {
                BuildBlockParse blockParse = BuildBlockParse.parseBlock(buildingState.getBlock());
                ItemStack buildingItem = builderEntity.getMatchingItem(itemStack -> itemStack.is(blockParse.getItem()));
                if(buildingItem != null){
                    if(!builderEntity.getMainHandItem().is(buildingItem.getItem())){
                        builderEntity.switchMainHandItem(itemStack -> itemStack.is(buildingItem.getItem()));
                    }
                    //CHECK IF IT WAS PARSED TO KEEP THE BLOCK-ROTATIONS OF NOT EFFECTED ONES
                    if(blockParse.wasParsed() && buildingItem.getItem() instanceof BlockItem blockItem){
                        buildingState = blockItem.getBlock().defaultBlockState();
                    }

                    builderEntity.getCommandSenderWorld().setBlockAndUpdate(blockPos, buildingState);
                    builderEntity.getCommandSenderWorld().playSound(null, blockPos.getX(), blockPos.getY(), blockPos.getZ(), buildingState.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 1.0F);

                    this.builderEntity.swing(InteractionHand.MAIN_HAND);

                    buildingItem.shrink(1);

                    builderEntity.currentBuildArea.removeBuildBlockToPlace(blockPos);
                    //this.blockPos = null;
                }
                else{
                    return false;
                }
            }
            return true;
        }
        this.blockPos = null;
        return false;
    }


    //PERFORMANCE HEAVY DO NOT USE FREQUENTLY
    private boolean canSeeBlock(Level level, Vec3 start, BlockPos target) {
        Vec3 targetCenter = target.getCenter();
        ClipContext ctx = new ClipContext(start, targetCenter, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, builderEntity);
        BlockPos ctxPos = level.clip(ctx).getBlockPos();
        return ctxPos.equals(target);
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

    public static List<BuildArea> getAvailableWorkAreasByPriority(ServerLevel level, BuilderEntity builderEntity, @Nullable BuildArea currentArea) {
        List<BuildArea> list = level.getEntitiesOfClass(BuildArea.class, builderEntity.getBoundingBox().inflate(64));

        Map<BuildArea, Integer> priorityMap = new HashMap<>();

        for (BuildArea area : list) {
            if (area == null || area == currentArea || !area.canWorkHere(builderEntity)) continue;

            if(area.isDone() || area.stackToPlace.isEmpty()) continue;

            int priority = 0;

            boolean perfectCandidate = true;//area.isWorkerPerfectCandidate(builderEntity);

            if (perfectCandidate) {
                priority += 10;
            } else {
                priority += 1;
            }

            if (!area.isBeingWorkedOn()) {
                priority += 3;
            }

            priority += area.time;

            //double dist = area.position().distanceToSqr(builderEntity.position());
            //priority -= dist / 10.0;

            priorityMap.put(area, priority);
        }


        List<BuildArea> sorted = new ArrayList<>(priorityMap.keySet());
        sorted.sort((a, b) -> Integer.compare(priorityMap.get(b), priorityMap.get(a)));

        return sorted;
    }



    public boolean moveToPosition(BlockPos pos, int threshold){
        if(pos == null){
            return false;
        }
        else{
            double distance = builderEntity.getHorizontalDistanceTo(pos.getCenter());
            if(distance < threshold){
                builderEntity.getNavigation().stop();
                return false;
            }
            else{

                builderEntity.setFollowState(6); //Working
                builderEntity.getNavigation().moveTo(pos.getX(), pos.getY(), pos.getZ(), 0.8F);
                builderEntity.getLookControl().setLookAt(pos.getCenter());
            }
            return true;
        }
    }

    public enum State{
        SELECT_WORK_AREA,
        MOVE_TO_WORK_AREA,
        PREPARE_BREAK_BLOCKS,
        BREAK_BLOCKS,
        PREPARE_PLACE_BLOCKS,
        PLACE_BLOCKS,
        DONE,
        ERROR

    }
}
