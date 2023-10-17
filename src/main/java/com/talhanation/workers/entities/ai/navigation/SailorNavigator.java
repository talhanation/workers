package com.talhanation.workers.entities.ai.navigation;

import com.talhanation.workers.Main;
import com.talhanation.workers.entities.AbstractWorkerEntity;
import com.talhanation.workers.entities.IBoatController;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SailorNavigator {
    private List<BlockPos> list;
    private final ServerLevel level;
    private BlockPos targetPos;
    public BlockPos currentPos;
    private int currentNodeIndex;
    private static final int RANGE = 528;
    private final AbstractWorkerEntity worker;
    private final IBoatController sailor;
    private int timer;
    private boolean isDone;

    public SailorNavigator(IBoatController sailor, ServerLevel level){
       this.level = level;
       this.sailor = sailor;
       this.worker = sailor.getWorker();
   }

   public List<BlockPos> createPath(){
       this.targetPos = this.sailor.getSailPos();
       this.list = new ArrayList<>();
       this.currentPos = worker.getOnPos();
       this.currentNodeIndex = 0;
       this.isDone = false;
       this.resetTimeOut();
       if(currentPos != null){
           for(int i=0; i < RANGE; i++){
               currentPos = this.findNextValidNode(currentPos);
               if(currentPos == targetPos) break;

               if(currentPos != null) {
                   list.add(currentPos);
               }
           }
       }

       for(BlockPos pos : list) {
           this.worker.getCommandSenderWorld().setBlock(pos.above(2), Blocks.ICE.defaultBlockState(), 3);
       }

       Main.LOGGER.info("Path Created: ");
       Main.LOGGER.info("Path size: " + list.size());
       Main.LOGGER.info("Target Pos: " + this.targetPos);
       Main.LOGGER.info("Distance to target: " + this.getWorkerDistanceToTarget());

       return this.list;
   }

    private void resetTimeOut() {
       this.timer = 200;
    }

    private BlockPos findNextValidNode(BlockPos current){
       List<BlockPos> list = new ArrayList<>();
       List<BlockPos> validPos = new ArrayList<>();

       if(current != null){
           list.add(current.north(1));
           list.add(current.south(1));
           list.add(current.east(1));
           list.add(current.west(1));
           list.add(current.east(1).south(1));
           list.add(current.west(1).south(1));
           list.add(current.east(1).north(1));
           list.add(current.west(1).north(1));
       }

       for(BlockPos pos : list){
           if(isValidWaterPos(pos)) validPos.add(pos);
       }
       if(validPos.isEmpty()){
           for(BlockPos pos : list){
               if(isValidWaterPosEasy(pos)) validPos.add(pos);
           }

           validPos.sort(Comparator.comparing(pos -> getDistanceToPos(this.targetPos, pos)));

           return validPos.get(worker.getRandom().nextInt(validPos.size() -1));

       }

       validPos.sort(Comparator.comparing(pos -> getDistanceToPos(this.targetPos, pos)));

       if(validPos.isEmpty()) return null;
       else return validPos.get(0);
   }

    public double getWorkerDistanceToTarget(){
       return targetPos != null ? Vec3.atCenterOf(targetPos).distanceToSqr(worker.position()) : 0D;
    }
    public boolean isValidWaterPos(BlockPos pos){
       BlockState state = worker.getCommandSenderWorld().getBlockState(pos);

       if(state.is(Blocks.WATER)){
           BlockState stateNorth = this.worker.getCommandSenderWorld().getBlockState(pos.north());
           BlockState stateEast = this.worker.getCommandSenderWorld().getBlockState(pos.east());
           BlockState stateSouth = this.worker.getCommandSenderWorld().getBlockState(pos.south());
           BlockState stateWest = this.worker.getCommandSenderWorld().getBlockState(pos.west());

           /*
           BlockState stateNorthEast = this.mob.getCommandSenderWorld().getBlockState(pos.north().east());
           BlockState stateNorthWest = this.mob.getCommandSenderWorld().getBlockState(pos.north().west());
           BlockState stateSouthEast = this.mob.getCommandSenderWorld().getBlockState(pos.south().east());
           BlockState stateSouthWest = this.mob.getCommandSenderWorld().getBlockState(pos.south().west());

           */
           return stateNorth.is(Blocks.WATER) && stateEast.is(Blocks.WATER) && stateSouth.is(Blocks.WATER) && stateWest.is(Blocks.WATER);
           //stateNorthEast.is(Blocks.WATER) && stateNorthWest.is(Blocks.WATER) && stateSouthEast.is(Blocks.WATER) && stateSouthWest.is(Blocks.WATER);
       }
       return false;
    }

    public boolean isValidWaterPosEasy(BlockPos pos){
        BlockState state = worker.getCommandSenderWorld().getBlockState(pos);

        return state.is(Blocks.WATER);
    }

    public void tick(){
        if(!this.isDone()){
            double distance = this.getWorkerDistanceToTarget();
            if(distance <= 3) this.isDone = true;
        }
    }

    public void advance(){
        this.currentNodeIndex++;
        this.resetTimeOut();
        this.currentPos = list.get(currentNodeIndex - 1);
    }

    public void setTargetPos(BlockPos pos){
       this.targetPos = pos;
    }
    public boolean isDone(){
        return isDone;
    }

    public int getCurrentNodeIndex() {
        return currentNodeIndex;
    }

    public int getNextNodeIndex() {
       return currentNodeIndex + 1;
    }

    public double getDistanceToPos(BlockPos target, BlockPos currentPos){
        return this.getDistanceToPos(Vec3.atCenterOf(target), Vec3.atCenterOf(currentPos));
    }

    public double getDistanceToPos(Vec3 target, Vec3 currentPos){
        return currentPos.distanceToSqr(target);
    }
}