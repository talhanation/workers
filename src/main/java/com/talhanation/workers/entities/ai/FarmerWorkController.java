package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.FarmerEntity;
import com.talhanation.workers.entities.IWorkerController;
import com.talhanation.workers.entities.workarea.CropArea;
import com.talhanation.workers.world.NeededItem;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.PlantType;

import java.util.Comparator;
import java.util.List;
import java.util.Stack;

public class FarmerWorkController implements IWorkerController {

    public FarmerEntity farmer;
    public BlockPos blockPos;
    public boolean initialized;
    public CropArea currentCropArea;
    public Stack<BlockPos> stackToPlant = new Stack<>();
    public Stack<BlockPos> stackToBreak = new Stack<>();
    public Stack<BlockPos> stackToPlow = new Stack<>();
    public WorkState workState;
    public FarmerWorkController(FarmerEntity farmer){
        this.farmer = farmer;
    }

    public boolean initWork(){
        List<CropArea> list = farmer.getCommandSenderWorld().getEntitiesOfClass(CropArea.class, farmer.getBoundingBox().inflate(32));

        list.sort(Comparator.comparing(CropArea::isBeingWorkedOn));

        list.sort(Comparator.comparing(workAreaEntity -> workAreaEntity.position().distanceToSqr(farmer.position())));

        list.removeIf(workAreaEntity -> !workAreaEntity.canWorkHere(this.farmer));
        list.removeIf(CropArea::isDone);
        if(list.isEmpty()) return false;

        this.currentCropArea = list.get(0);

        if(currentCropArea == null) return false;

        setWorkState(WorkState.BREAKING_BLOCKS);

        BlockState centerPosState = farmer.getCommandSenderWorld().getBlockState(currentCropArea.getOnPos());
        if(centerPosState.isAir()){
            ItemStack itemStack = farmer.getMatchingItem(item -> farmer.isBucketWithWater(item));
            if(itemStack == null){
                farmer.addNeededItem(new NeededItem(item -> farmer.isBucketWithWater(item),  1, false));
                return false;
            }
            if(itemStack.getItem() instanceof BucketItem bucketItem){
                bucketItem.emptyContents(null,  farmer.getCommandSenderWorld(), currentCropArea.getOnPos(), null);

            }
        }
        else if(centerPosState.is(Blocks.WATER)) {
            blockPos = currentCropArea.getOnPos();
            return true;
        }

        else{
            if(!this.stackToBreak.contains(currentCropArea.getOnPos())){
                this.stackToBreak.push(currentCropArea.getOnPos());
            }
        }

        return false;
    }
    @Override
    public void tick() {
        if(farmer == null) return;

        if(farmer.needsToSleep() || farmer.getFollowState() == 1 || farmer.needsToGetToChest()) return;

        if(farmer.tickCount % 10 == 0){
            if(!initialized && initWork()){
                initialized = true;
            }

            if(currentCropArea == null) return;
            if(moveToPosition(blockPos)) return;

            switch (workState){
                case BREAKING_BLOCKS -> {
                    if(breakBlocks(this.stackToBreak)) return;
                    setWorkState(WorkState.PLOWING);
                }

                case PLOWING -> {
                    if(plowBlocks(this.stackToPlow)) return;
                    setWorkState(WorkState.PLANTING);
                }

                case PLANTING -> {
                    if(plantCrops(this.stackToPlant)) return;
                    setWorkState(WorkState.DONE);
                }

                case DONE -> {
                    currentCropArea.setDone(true);
                    currentCropArea.setBeingWorkedOn(false);
                    currentCropArea = null;

                    initialized = false;
                }

                case ERROR -> {

                }
            }
        }
    }

    @Override
    public void setInitialized(boolean b) {
        initialized = b;
    }

    //Returns false when done
    public boolean moveToPosition(BlockPos pos){
        if(pos == null){
            return false;
        }
        else{
            double distance = pos.getCenter().distanceToSqr(farmer.position());
            if(distance < 15){
                this.blockPos = null;
                return false;
            }
            else{
                farmer.getNavigation().moveTo(pos.getX(), pos.getY(), pos.getZ(), 0.8F);
                farmer.setFollowState(6); //Working
                farmer.getLookControl().setLookAt(blockPos.getCenter());
            }
            return true;
        }
    }

    //Returns false when done
    int blockBreakTime;
    public boolean breakBlocks(Stack<BlockPos> positions){
        if(positions != null && !positions.isEmpty()){
            if(blockPos == null){
                blockPos = positions.pop();
            }
            //getNextBlock
            BlockState state = farmer.getCommandSenderWorld().getBlockState(blockPos);
            if(state.isAir()){
                blockPos = positions.pop();
                blockBreakTime = 0;
            }
            else{//breakBlock
                this.farmer.mineBlock(blockPos);
                this.farmer.swing(InteractionHand.MAIN_HAND);
            }
            return true;
        }
        this.blockPos = null;
        return false;
    }

    //Returns false when done
    public boolean plantCrops(Stack<BlockPos> positions){
        if(positions != null && !positions.isEmpty()){
            ItemStack seedFromInv = farmer.getMatchingItem(itemStack -> itemStack.is(currentCropArea.getSeedStack().getItem()));
            if(seedFromInv == null){
                farmer.addNeededItem(new NeededItem(itemStack -> ItemStack.isSameItemSameTags(itemStack, currentCropArea.getSeedStack()),  16, true));
                this.blockPos = null;
                return false;
            }
            if(blockPos == null){
                if(!positions.isEmpty()) blockPos = positions.pop();
            }

            BlockState state = farmer.getCommandSenderWorld().getBlockState(blockPos);
            if(state.getBlock() instanceof CropBlock){
                if(!positions.isEmpty()){
                    blockPos = positions.pop();
                }
            }
            else if (seedFromInv.getItem() instanceof BlockItem blockItem) {
                farmer.getCommandSenderWorld().setBlockAndUpdate(blockPos, blockItem.getBlock().defaultBlockState());

                farmer.getCommandSenderWorld().playSound(null, blockPos.getX(), blockPos.getY(), blockPos.getZ(), SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0F, 1.0F);
                seedFromInv.shrink(1);
                this.farmer.swing(InteractionHand.MAIN_HAND);
            }
            else if (seedFromInv.getItem() instanceof IPlantable plantable) {
                if (plantable.getPlantType(farmer.getCommandSenderWorld(), blockPos) == PlantType.CROP) {
                    farmer.getCommandSenderWorld().setBlock(blockPos, plantable.getPlant(farmer.getCommandSenderWorld(), blockPos), 3);

                    farmer.getCommandSenderWorld().playSound(null, blockPos.getX(), blockPos.getY(), blockPos.getZ(), SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0F, 1.0F);
                    seedFromInv.shrink(1);
                    this.farmer.swing(InteractionHand.MAIN_HAND);
                }
            }
            return true;
        }
        this.blockPos = null;
        return false;
    }

    public boolean plowBlocks(Stack<BlockPos> positions){
        if(positions != null && !positions.isEmpty()){
            boolean hasHoe = farmer.getMainHandItem().getItem() instanceof HoeItem;
            if(!hasHoe){
                farmer.addNeededItem(new NeededItem(stack -> stack.getItem() instanceof HoeItem, 1, false));
                this.blockPos = null;
                return true;
            }

            if(blockPos == null){
                blockPos = positions.pop();
            }
            //getNextBlock
            BlockState state = farmer.getCommandSenderWorld().getBlockState(blockPos);
            if(state.getBlock() instanceof FarmBlock){
                if(!positions.isEmpty()) blockPos = positions.pop();
            }
            else{//plowBlock
                this.farmer.swing(InteractionHand.MAIN_HAND);
                farmer.getCommandSenderWorld().setBlock(blockPos, Blocks.FARMLAND.defaultBlockState(), 3);
                farmer.getCommandSenderWorld().playSound(null, blockPos.getX(), blockPos.getY(), blockPos.getZ(), SoundEvents.HOE_TILL,
                        SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return true;
        }
        blockPos = null;
        return false;
    }

    public void setWorkState(WorkState newState){
        if(newState == this.workState) return;

        switch (newState){
            case BREAKING_BLOCKS -> {
                this.currentCropArea.scanBreakArea();

                this.stackToBreak = currentCropArea.stackToBreak;

                this.stackToBreak.sort(Comparator.comparing(pos -> pos.getCenter().distanceToSqr(farmer.position())));
                this.stackToBreak.sort(Comparator.reverseOrder());
            }

            case PLOWING -> {
                this.currentCropArea.scanPlowArea();

                this.stackToPlow = currentCropArea.stackToPlow;

                this.stackToPlow.sort(Comparator.comparing(pos -> pos.getCenter().distanceToSqr(farmer.position())));
                this.stackToPlow.sort(Comparator.reverseOrder());
            }

            case PLANTING ->  {
                this.currentCropArea.scanPlantArea();

                this.stackToPlant = currentCropArea.stackToPlant;

                this.stackToPlant.sort(Comparator.comparing(pos -> pos.getCenter().distanceToSqr(farmer.position())));
                this.stackToPlow.sort(Comparator.reverseOrder());
            }
            default -> {}
        }
        this.workState = newState;
    }

    public enum WorkState{
        BREAKING_BLOCKS,
        PLOWING,
        PLANTING,
        DONE,
        ERROR
    }
}


