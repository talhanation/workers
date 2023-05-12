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

import static com.talhanation.workers.entities.ai.ControlBoatAI.State.*;


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
        state = State.IDLE;
    }

    public void stop(){
    }

    public boolean requiresUpdateEveryTick() {
        return true;
    }

    public void tick() {
        if(this.worker instanceof IBoatController sailor && this.worker.getNavigation() instanceof SailorPathNavigation sailorPathNavigation) {
            if (sailor.getSailPos() != null) {
                Main.LOGGER.info("Sate: " + state);
                switch (state) {

                    case IDLE -> {
                        if (sailor.getSailPos() != null) {
                            this.state = State.CREATING_PATH;
                        }
                    }

                    case CREATING_PATH -> {
                        if (sailor.getSailPos() != null) {
                            worker.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
                            this.path = sailorPathNavigation.createPath(sailor.getSailPos(), 8, false, 1);

                            if (path != null && path.getNodeCount() > 1) {
                                this.node = this.path.getNextNode();
                                state = MOVING_PATH;
                            } else {
                                Main.LOGGER.info("Path null or has 1 node");
                                worker.setStartPos(null);
                                state = IDLE;
                            }
                            

                        }
                        else
                            state = IDLE;
                    }

                    case MOVING_PATH -> {

                        if (!(node.distanceToSqr(worker.getOnPos()) < 6F)) {
                            this.worker.getLookControl().setLookAt(node.x,node.y, node.z);
                            updateBoatControl(node.x, node.z);
                        } else {
                            path.advance();
                            if (path.getNodeCount() == path.getNextNodeIndex() - 1 || node.equals(path.getEndNode())) {
                                state = State.DONE;
                                return;
                            }
                            this.node = path.getNextNode(); //TODO: fix crash here: "Index 1 out of bounds for length 1"
                        }
                    }

                    case DONE -> {
                        sailor.setSailPos(null);
                        state = IDLE;
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




    enum State{
        IDLE,
        CREATING_PATH,
        MOVING_PATH,
        DONE
        //MOVING_TO_SAIL_POS,
        //AVOIDING,
        //MOVING_TO_WATER_POS,
    }
}
