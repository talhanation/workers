package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.FarmerEntity;
import com.talhanation.workers.entities.IWorkerController;
import com.talhanation.workers.world.CropArea;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.PlantType;

import java.util.Stack;

public class FarmerWorkController implements IWorkerController {

    public FarmerEntity farmer;
    public CropArea cropArea;
    public Stack<BlockPos> stackToPlant;
    public Stack<BlockPos> stackToBreak;
    public Stack<BlockPos> stackToPlow;
    public BlockPos blockPos;
    public int workAreaIndex;
    public int workTimePerArea;
    public boolean initialized;

    public FarmerWorkController(FarmerEntity farmer){
        this.farmer = farmer;
        this.stackToBreak = new Stack<>();
        this.stackToPlow = new Stack<>();
        this.stackToPlant = new Stack<>();
    }

    public boolean initWork(){
        if(farmer.getCropAreasTag() == null || farmer.getCropAreasTag().isEmpty()) return false;
        this.cropArea = farmer.getCropAreas().get(workAreaIndex);

        if(cropArea == null) return false;

        BlockState centerPosState = farmer.getCommandSenderWorld().getBlockState(cropArea.getCenterPos());
        if(centerPosState.isAir()){
            ItemStack itemStack = farmer.getMatchingItem(item -> farmer.isBucketWithWater(item));
            if(itemStack == null) return false;
            if(itemStack.getItem() instanceof BucketItem bucketItem){
                bucketItem.emptyContents(null,  farmer.getCommandSenderWorld(), cropArea.getCenterPos(), null);
            }
        }
        else if(centerPosState.is(Blocks.WATER)) {
            stackToBreak = new Stack<>();
            stackToPlow = new Stack<>();
            this.scanWorkArea();
            return true;
        }

        else{
            if(!stackToBreak.contains(cropArea.getCenterPos())){
                stackToBreak.push(cropArea.getCenterPos());
            }
        }
        return false;
    }

    @Override
    public void tick() {
        if(farmer == null) return;

        if(farmer.getCropAreasTag() == null || farmer.getCropAreasTag().isEmpty()) return;

        if(farmer.needsToSleep() || farmer.getFollowState() == 1 || farmer.needsToGetToChest()) return;

        if(!initialized && initWork()){
            initialized = true;
        }

        if(cropArea == null) return;

        if(moveToPosition(blockPos)) return;

        //break saved crops
        if(breakBlocks(stackToBreak)) return;

        // seed area
        if(moveToPosition(blockPos)) return;
        if(plantCrops(stackToPlant))return;

        // prepare Area
        if(moveToPosition(blockPos)) return;
        if(plowBlocks(stackToPlow)) return;

        initialized = false;
    }

    //Returns false when done
    public boolean moveToPosition(BlockPos pos){
        if(pos == null){
            return false;
        }
        else{
            if(farmer.tickCount % 20 == 0){
                farmer.setHoldPos(pos.getCenter());
                farmer.setState(3);
                double distance = pos.getCenter().distanceToSqr(farmer.position());
                if(distance < 35){
                    return false;
                }
            }
            return true;
        }
    }

    //Returns false when done
    int blockBreakTime;
    public boolean breakBlocks(Stack<BlockPos> positions){
        if(positions != null && !positions.isEmpty()){
            if(blockPos == null) blockPos = positions.pop();
            //getNextBlock
            BlockState state = farmer.getCommandSenderWorld().getBlockState(blockPos);
            if(state.isAir()){
                blockPos = positions.pop();
                blockBreakTime = 0;
            }
            else{//breakBlock
                this.farmer.mineBlock(blockPos);
            }
            return true;
        }
        return false;
    }

    public void scanWorkArea(){
        int range = 4;
        for(int i = -range; i <= range; i++){
            for(int k = -range; k <= range; k++){
                for(int j = -range; j <= range; j++){
                    BlockPos pos = cropArea.getCenterPos().offset(i, k, j);
                    BlockState state = this.farmer.getCommandSenderWorld().getBlockState(pos);

                    BlockPos above = pos.above();
                    BlockState stateAbove = this.farmer.getCommandSenderWorld().getBlockState(above);

                    boolean canSustainSeeds = FarmerEntity.TILLABLES.contains(state.getBlock());
                    boolean hasSpaceAbove = stateAbove.isAir();

                    if(state.getBlock() instanceof FarmBlock && hasSpaceAbove){
                        this.stackToPlant.push(pos.above());
                    }
                    else if (canSustainSeeds && hasSpaceAbove) {
                        this.stackToPlow.push(pos);
                        this.stackToPlant.push(pos.above());
                    }
                    else if(state.getBlock() instanceof CropBlock cropBlock){
                        int currentAge = cropBlock.getAge(state);
                        int maxAge = cropBlock.getMaxAge();

                        if(currentAge == maxAge){
                            this.stackToBreak.push(pos);
                        }
                    }
                    else if(stateAbove.getBlock() instanceof BushBlock){
                        this.stackToBreak.push(above);
                    }
                }
            }
        }
    }

    //Returns false when done
    public boolean plantCrops(Stack<BlockPos> positions){
        if(positions != null && !positions.isEmpty()){
            ItemStack seedFromInv = farmer.getMatchingItem(itemStack -> itemStack.is(cropArea.seedStack.getItem()));
            if(seedFromInv == null) return false;
            if(blockPos == null) blockPos = positions.pop();

            BlockState state = farmer.getCommandSenderWorld().getBlockState(blockPos);
            if(state.getBlock() instanceof CropBlock){
                blockPos = positions.pop();
            }
            else if (seedFromInv.getItem() instanceof BlockItem blockItem) {
                farmer.getCommandSenderWorld().setBlockAndUpdate(blockPos, blockItem.getBlock().defaultBlockState());

                farmer.getCommandSenderWorld().playSound(null, blockPos.getX(), blockPos.getY(), blockPos.getZ(), SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0F, 1.0F);
                seedFromInv.shrink(1);
            }
            else if (seedFromInv.getItem() instanceof IPlantable plantable) {
                if (plantable.getPlantType(farmer.getCommandSenderWorld(), blockPos) == PlantType.CROP) {
                    farmer.getCommandSenderWorld().setBlock(blockPos, plantable.getPlant(farmer.getCommandSenderWorld(), blockPos), 3);

                    farmer.getCommandSenderWorld().playSound(null, blockPos.getX(), blockPos.getY(), blockPos.getZ(), SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0F, 1.0F);
                    seedFromInv.shrink(1);
                }
            }
            return true;
        }
        return false;
    }

    public boolean plowBlocks(Stack<BlockPos> positions){
        if(positions != null && !positions.isEmpty()){
            if(blockPos == null) blockPos = positions.pop();
            //getNextBlock
            BlockState state = farmer.getCommandSenderWorld().getBlockState(blockPos);
            if(state.getBlock() instanceof FarmBlock){
                blockPos = positions.pop();
            }
            else{//breakBlock
                farmer.getCommandSenderWorld().setBlock(blockPos, Blocks.FARMLAND.defaultBlockState(), 3);
                farmer.getCommandSenderWorld().playSound(null, blockPos.getX(), blockPos.getY(), blockPos.getZ(), SoundEvents.HOE_TILL,
                        SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return true;
        }
        return false;
    }
}
