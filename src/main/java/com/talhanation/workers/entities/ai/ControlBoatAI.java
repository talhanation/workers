package com.talhanation.workers.entities.ai;

import com.talhanation.workers.Main;
import com.talhanation.workers.entities.AbstractWorkerEntity;
import com.talhanation.workers.entities.IBoatController;
import com.talhanation.workers.entities.ai.navigation.SailorPathNavigation;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;

import java.util.*;



public class ControlBoatAI extends Goal {

    private final AbstractWorkerEntity worker;
    private BlockPos waterPos;
    private BlockPos avoidPos;
    private BlockPos toSailPos;
    private State state;
    private Path path;
    private Node node;

    public ControlBoatAI(IBoatController worker) {
        this.worker = worker.getWorker();
    }

    @Override
    public boolean canUse() {
        return  this.worker.getVehicle() instanceof Boat boat && boat.getPassengers().get(0).equals(this.worker) && !worker.getFollow();
    }

    public boolean canContinueToUse() {
        return true;
    }

    public boolean isInterruptable() {
        return true;
    }

    public void start(){
        state = State.CREATING_PATH;
    }

    public void stop(){
    }

    public boolean requiresUpdateEveryTick() {
        return true;
    }

    public void tick() {
        if(this.worker instanceof IBoatController sailor){
            if(sailor.getSailPos() != null){
                Main.LOGGER.info("Sate: " + state);
                switch (state) {

                    case CREATING_PATH -> {
                        if(this.worker.getNavigation() instanceof SailorPathNavigation waterNavigation && worker.getStartPos() != null) {
                            worker.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
                            this.path = waterNavigation.createPath(worker.getStartPos(), 1);

                            if (path != null) {
                                this.node = this.path.getNextNode();
                                state = State.MOVING_PATH;
                            }
                            else
                                Main.LOGGER.info("path null");
                        }
                    }

                    case MOVING_PATH -> {

                        if (!(node.distanceToSqr(worker.getOnPos()) < 5F)){
                            updateBoatControl(node.x, node.z);
                        }
                        else {
                            path.advance();
                            if(path.getNextNode() != null) this.node = path.getNextNode();
                        }


                        if(node == path.getEndNode()){
                            state = State.CREATING_PATH;
                        }
                    }
                }

            }

            /*
            switch (state){

                case MOVING_TO_SAIL_POS -> {

                    }




                    if(!sailor.getSailPos().closerThan(this.worker.getOnPos(), sailor.getControlAccuracy()))
                        //updateBoatControl(posX,posZ);

                    if(obstacleDetected()){
                        this.waterPos = getWaterPos(new BlockPos(posX, this.worker.getOnPos().getY(), posZ), avoidPos);
                        this.state = MOVING_TO_WATER_POS;
                    }

                }


                case MOVING_TO_WATER_POS -> {
                    if(waterPos != null) {
                        double posX = waterPos.getX();
                        double posZ = waterPos.getZ();
                        Main.LOGGER.info("WaterPos: " + waterPos);


                        updateBoatControl(posX, posZ);
                        if (waterPos.closerThan(this.worker.getOnPos(), sailor.getControlAccuracy() * 1.5)) {
                            state = State.MOVING_TO_SAIL_POS;
                        }
                    }
                }


            }
    */
        }
    }

    private void updateBoatControl(double posX, double posZ) {
        if(this.worker.getVehicle() instanceof Boat boat && boat.getPassengers().get(0).equals(this.worker)) {
            double dx = posX - this.worker.getX();
            double dz = posZ - this.worker.getZ();

            float angle = Mth.wrapDegrees((float) (Mth.atan2(dz, dx) * 180.0D / 3.14D) - 90.0F);
            float drot = angle - Mth.wrapDegrees(boat.getYRot());

            boolean inputLeft = (drot < 0.0F && Math.abs(drot) >= 5.0F);
            boolean inputRight = (drot > 0.0F && Math.abs(drot) >= 5.0F);
            boolean inputUp = (Math.abs(drot) < 20.0F);

            float f = 0.0F;

            if (inputLeft) {
                boat.setYRot(boat.getYRot() - 2.5F);
            }

            if (inputRight) {
                boat.setYRot(boat.getYRot() + 2.5F);
            }


            if (inputRight != inputLeft && !inputUp) {
                f += 0.005F;
            }

            if (inputUp) {
                f += 0.04F;
            }

            boat.setDeltaMovement(boat.getDeltaMovement().add((double)(Mth.sin(-boat.getYRot() * ((float)Math.PI / 180F)) * f), 0.0D, (double)(Mth.cos(boat.getYRot() * ((float)Math.PI / 180F)) * f)));
            boat.setPaddleState(inputRight || inputUp, inputLeft || inputUp);
        }
    }

    private boolean obstacleDetected() {
        BlockPos boatPos = this.worker.getOnPos();
        for (BlockPos pos : BlockPos.betweenClosed(boatPos.offset(-2, -0, -2), boatPos.offset(2, 0, 2))) {
            BlockState state = worker.level.getBlockState(pos);
            if (!state.is(Blocks.WATER)) {
                this.avoidPos = pos;
                return true;
            }
        }
        return false;
    }

    public BlockPos getWaterPos(BlockPos targetPos, BlockPos avoidPos){
        List<BlockPos> waterBlockPos = new ArrayList<>();
        int range = 15;
        BlockPos workerPos = this.worker.getOnPos();
        for(int i = -range; i < range; i++){
            for(int j = -range; j < range; j++){
                BlockPos pos = new BlockPos(workerPos.getX() + i, workerPos.getY(), workerPos.getZ() + j);
                BlockState state = this.worker.getLevel().getBlockState(pos);

                if(state.is(Blocks.WATER)){
                    BlockState stateNorth = this.worker.getLevel().getBlockState(pos.north());
                    BlockState stateEast = this.worker.getLevel().getBlockState(pos.east());
                    BlockState stateSouth = this.worker.getLevel().getBlockState(pos.south());
                    BlockState stateWest = this.worker.getLevel().getBlockState(pos.west());

                    if(stateNorth.is(Blocks.WATER) && stateEast.is(Blocks.WATER) && stateSouth.is(Blocks.WATER) && stateWest.is(Blocks.WATER))
                        waterBlockPos.add(pos);
                }
            }
        }

        waterBlockPos.sort(Comparator.comparingDouble(pos -> pos.distSqr(targetPos) + 1.5 * pos.distSqr(avoidPos)));
        List<Double> distanceList = new ArrayList<>();
        for(BlockPos pos : waterBlockPos){
            distanceList.add(pos.distSqr(targetPos));
        }

        Main.LOGGER.info("onPos: " + this.worker.getOnPos());
        Main.LOGGER.info("WaterPosList: " + waterBlockPos);
        Main.LOGGER.info("distanceList: " + distanceList);
        return !waterBlockPos.isEmpty() ? waterBlockPos.get(0) : null;
    }


    enum State{
        CREATING_PATH,
        MOVING_PATH
        //MOVING_TO_SAIL_POS,
        //AVOIDING,
        //MOVING_TO_WATER_POS,
    }
}
