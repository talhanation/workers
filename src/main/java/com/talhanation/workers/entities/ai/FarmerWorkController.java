package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.FarmerEntity;
import com.talhanation.workers.entities.IWorkerController;
import com.talhanation.workers.entities.WorkAreaEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
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
    public WorkAreaEntity currentWorkArea;

    public FarmerWorkController(FarmerEntity farmer){
        this.farmer = farmer;
    }

    public boolean initWork(){
        List<WorkAreaEntity> list = farmer.getCommandSenderWorld().getEntitiesOfClass(WorkAreaEntity.class, farmer.getBoundingBox().inflate(32));

        list.sort(Comparator.comparing(workAreaEntity -> workAreaEntity.position().distanceToSqr(farmer.position())));
        list.removeIf(workAreaEntity -> !workAreaEntity.canWorkHere(this.farmer));
        //list.removeIf(workAreaEntity -> !workAreaEntity.hasWorkOpen());
        if(list.isEmpty()) return false;

        this.currentWorkArea = list.get(0);

        if(currentWorkArea == null) return false;

        currentWorkArea.scanArea();

        BlockState centerPosState = farmer.getCommandSenderWorld().getBlockState(currentWorkArea.getOnPos());
        if(centerPosState.isAir()){
            ItemStack itemStack = farmer.getMatchingItem(item -> farmer.isBucketWithWater(item));
            if(itemStack == null) return false;
            if(itemStack.getItem() instanceof BucketItem bucketItem){
                bucketItem.emptyContents(null,  farmer.getCommandSenderWorld(), currentWorkArea.getOnPos(), null);
            }
        }
        else if(centerPosState.is(Blocks.WATER)) {
            return true;
        }

        else{
            if(!currentWorkArea.stackToBreak.contains(currentWorkArea.getOnPos())){
                currentWorkArea.stackToBreak.push(currentWorkArea.getOnPos());
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

            if(currentWorkArea == null) return;

            if(moveToPosition(blockPos)) return;
            if(breakBlocks(currentWorkArea.stackToBreak) || plowBlocks(currentWorkArea.stackToPlow) || plantCrops(currentWorkArea.stackToPlant)) return;

            initialized = false;
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
            farmer.setHoldPos(pos.getCenter());
            farmer.setState(3);
            farmer.getLookControl().setLookAt(blockPos.getCenter());

            double distance = pos.getCenter().distanceToSqr(farmer.position());
            if(distance < 35){
                this.blockPos = null;
                return false;
            }
            return true;
        }
    }

    //Returns false when done
    int blockBreakTime;
    public boolean breakBlocks(Stack<BlockPos> positions){
        if(positions != null && !positions.isEmpty()){
            if(blockPos == null){
                //this.cropArea.stackToBreak.sort(Comparator.comparingDouble(pos -> pos.getCenter().distanceToSqr(this.farmer.position())));
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

    private boolean isFarmland(BlockState state){
        return state.getBlock() instanceof FarmBlock;
    }
    private boolean isTillAble(BlockState state){
        return FarmerEntity.TILLABLES.contains(state.getBlock());
    }

    private boolean isBush(BlockState state){
        return state.getBlock() instanceof BushBlock;
    }

    private boolean isCrop(BlockState state){
        return state.getBlock() instanceof CropBlock;
    }

    private boolean isCropDone(BlockState state){
        return state.getBlock() instanceof CropBlock cropBlock && cropBlock.getAge(state) == cropBlock.getMaxAge();
    }

    private boolean isAir(BlockState state){
        return state.isAir();
    }

    //Returns false when done
    public boolean plantCrops(Stack<BlockPos> positions){
        if(positions != null && !positions.isEmpty()){
            ItemStack seedFromInv = farmer.getMatchingItem(itemStack -> itemStack.is(currentWorkArea.seedStack.getItem()));
            if(seedFromInv == null){
                this.blockPos = null;
                return false;
            }
            if(blockPos == null){
                //this.cropArea.stackToPlant.sort(Comparator.comparingDouble(pos -> pos.getCenter().distanceToSqr(this.farmer.position())));
                blockPos = positions.pop();
            }

            BlockState state = farmer.getCommandSenderWorld().getBlockState(blockPos);
            if(state.getBlock() instanceof CropBlock){
                blockPos = positions.pop();
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
            if(blockPos == null){
                //this.cropArea.stackToPlow.sort(Comparator.comparingDouble(pos -> pos.getCenter().distanceToSqr(this.farmer.position())));
                blockPos = positions.pop();
            }
            //getNextBlock
            BlockState state = farmer.getCommandSenderWorld().getBlockState(blockPos);
            if(state.getBlock() instanceof FarmBlock){
                blockPos = positions.pop();
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
}
