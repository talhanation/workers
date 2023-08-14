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
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

import static com.talhanation.workers.entities.ai.ControlBoatAI.State.*;


public class ControlBoatAI extends Goal {

    private final AbstractWorkerEntity worker;
    private State state;
    private Path path;
    private Node node;
    //private SailorNavigator sailorNavigator;


    public ControlBoatAI(IBoatController sailor) {
        this.worker = sailor.getWorker();

        //this.sailorNavigator = new SailorNavigator(sailor, (ServerLevel) worker.getLevel());
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
        if (this.worker instanceof IBoatController sailor && !worker.getLevel().isClientSide() && worker.getNavigation() instanceof SailorPathNavigation sailorPathNavigation) {
            if (this.worker.getOwner() != null && worker.getOwner().isInWater()) {
                sailor.setSailPos(worker.getOwner().getOnPos());
                this.state = IDLE;
            }

            switch (state) {

                case IDLE -> {

                    if (sailor.getSailPos() != null) {
                        double distance = sailor.getSailPos().distToCenterSqr(worker.position());
                        Main.LOGGER.info("distance to sailpos: " + distance);
                        if (distance > 2) this.state = State.CREATING_PATH;
                    }
                }


                case CREATING_PATH -> {
                    if (sailor.getSailPos() != null) {
                        double distance = sailor.getSailPos().distToCenterSqr(worker.position());
                        Main.LOGGER.info("distance to sailpos: " + distance);
                        worker.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
                        worker.setPathfindingMalus(BlockPathTypes.WALKABLE, -1.0F);
                        worker.setPathfindingMalus(BlockPathTypes.WATER_BORDER,-1.0F);
                        worker.setPathfindingMalus(BlockPathTypes.BREACH,-1.0F);
                        this.path = sailorPathNavigation.createPath(sailor.getSailPos(), 16, false, 0);

                        if (path != null) {
                            this.node = this.path.getNextNode();

                            for(Node node : this.path.nodes) {
                                worker.level.setBlock(new BlockPos(node.x, worker.getY() + 2, node.z), Blocks.ICE.defaultBlockState(), 3);
                            }

                            state = MOVING_PATH;
                        }
                    } else
                        state = IDLE;
                }
                case MOVING_PATH -> {
                    //this.worker.moveTo();
                    double distance = getHorizontalDistance(node.asVec3(), Vec3.atCenterOf(worker.getOnPos())); //valid value example: distance = 6.5
                    if ((distance > 5F)) {
                        //this.worker.getLookControl().setLookAt(node.x,node.y, node.z);
                        updateBoatControl(node.x, node.z);
                    } else {
                        path.advance();
                        if (path.getNodeCount() == path.getNextNodeIndex() - 1) {
                            state = CREATING_PATH;
                            return;
                        }

                        if (path.getNodeCount() == path.getNextNodeIndex() - 1 || node.equals(path.getEndNode())) {
                            state = State.DONE;
                            return;
                        }
                        this.node = path.getNextNode(); //TODO: fix crash here: "Index 1 out of bounds for length 1"
                    }
                }

                case DONE -> {
                    //sailor.setSailPos(null);
                    state = IDLE;
                }
            /*
            if (sailor.getSailPos() != null) {
                Main.LOGGER.info("State: " + state);
                switch (state) {

                    case IDLE -> {
                        double distance = worker.distanceToSqr(Vec3.atCenterOf(sailor.getSailPos()));

                        if (distance > 2) {
                            this.sailorNavigator = new SailorNavigator(sailor, (ServerLevel) worker.getLevel());
                            this.state = State.CREATING_PATH;
                        }
                    }

                    case CREATING_PATH -> {
                        if (!sailorNavigator.createPath().isEmpty()) {
                            state = MOVING_PATH;
                            Main.LOGGER.info("Now Moving Path");
                        }
                    }

                    case MOVING_PATH -> {
                        sailorNavigator.tick();
                        if (sailorNavigator.isDone()) {
                            Main.LOGGER.info("Navigation Done");
                            this.state = State.DONE;
                        } else {
                            if(sailorNavigator.currentPos != null) updateBoatControl(sailorNavigator.currentPos.getX(), sailorNavigator.currentPos.getZ());
                        }
                    }

                    case DONE -> {

                    }
                }
            }

             */
            }
        }
    }

    private double getHorizontalDistance(Vec3 node, Vec3 pos) {
        double x1 = node.x;
        double z1 = node.z;

        double x2 = pos.x;
        double z2 = pos.z;

        return Math.sqrt((z2 - z1) * (z2 - z1) + (x2 - x1) * (x2 - x1));

    }

    private void updateBoatControl(double posX, double posZ) {
        if(this.worker.getVehicle() instanceof Boat boat && boat.getPassengers().get(0).equals(this.worker)) {
            double dx = posX - this.worker.getX();
            double dz = posZ - this.worker.getZ();

            float angle = Mth.wrapDegrees((float) (Mth.atan2(dz, dx) * 180.0D / 3.14D) - 90.0F);
            float drot = angle - Mth.wrapDegrees(boat.getYRot());

            boolean inputLeft = (drot < 0.0F && Math.abs(drot) >= 4F);
            boolean inputRight = (drot > 0.0F && Math.abs(drot) >= 4F);
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
                f += 0.02F;
            }

            boat.setDeltaMovement(boat.getDeltaMovement().add((double)(Mth.sin(-boat.getYRot() * ((float)Math.PI / 180F)) * f), 0.0D, (double)(Mth.cos(boat.getYRot() * ((float)Math.PI / 180F)) * f)));
            boat.setPaddleState(inputRight || inputUp, inputLeft || inputUp);
        }
    }

    enum State{
        IDLE,
        CREATING_PATH,
        MOVING_PATH,
        DONE,
        MANEUVER
        //MOVING_TO_SAIL_POS,
        //AVOIDING,
        //MOVING_TO_WATER_POS,
    }


    /*
    switch (state) {

                    case IDLE -> {

                        if (sailor.getSailPos() != null) {
                            double distance = sailor.getSailPos().distToCenterSqr(worker.position());
                            Main.LOGGER.info("distance to sailpos: " + distance);
                            if(distance > 2) this.state = State.CREATING_PATH;
                        }
                    }


                    case CREATING_PATH -> {
                        if (sailor.getSailPos() != null) {
                            double distance = sailor.getSailPos().distToCenterSqr(worker.position());
                            Main.LOGGER.info("distance to sailpos: " + distance);

                            this.path = sailorPathNavigation.createPath(sailor.getSailPos(), 16, false, 0);

                            if (path != null && path.getNodeCount() > 1) {
                                this.node = this.path.getNextNode();
                                state = MOVING_PATH;
                            } else {
                                Main.LOGGER.info("Path null or has 1 node");
                                state = MANEUVER;
                            }
                        }
                        else
                            state = IDLE;
                    }
                    case MOVING_PATH -> {
                        //this.worker.moveTo();
                        double distance = node.distanceTo(worker.getOnPos()); //valid value example: distance = 6.5
                        if ((distance > 1.5)) {
                            //this.worker.getLookControl().setLookAt(node.x,node.y, node.z);
                            updateBoatControl(node.x, node.z);
                        } else {
                            path.advance();
                            if(path.getNodeCount() == path.getNextNodeIndex() - 1){
                                state = CREATING_PATH;
                                return;
                            }

                            if (path.getNodeCount() == path.getNextNodeIndex() - 1 || node.equals(path.getEndNode())) {
                                state = State.DONE;
                                return;
                            }
                            this.node = path.getNextNode(); //TODO: fix crash here: "Index 1 out of bounds for length 1"
                        }
                    }

                    case DONE -> {
                        //sailor.setSailPos(null);
                        state = IDLE;
                    }

                    case MANEUVER -> {
                        //TODO: leichter schubs in richtung wasser weg vom coast

                        //if(!calculateManeuverDone){
                            BlockPos pos = worker.getOnPos();
                            Main.LOGGER.info("worker on Pos: " + pos);
                            int range = 2;
                            BlockPos north = worker.getOnPos().above(1).north(1);
                            BlockPos east = worker.getOnPos().above(1).east(1);
                            BlockPos south = worker.getOnPos().above(1).south(1);
                            BlockPos west = worker.getOnPos().above(1).west(1);

                            BlockPos northeast = worker.getOnPos().above(1).north(1).east(1);
                            BlockPos northwest = worker.getOnPos().above(1).north(1).west(1);
                            BlockPos southeast = worker.getOnPos().above(1).south(1).east(1);
                            BlockPos southwest = worker.getOnPos().above(1).south(1).west(1);


                            if(!worker.level.getBlockState(northeast).is(Blocks.WATER)){
                                maneuverPos = northeast.south(range).west(range);
                            }
                            else if(!worker.level.getBlockState(northwest).is(Blocks.WATER)){
                                maneuverPos = northwest.south(range).east(range);
                            }
                            else if(!worker.level.getBlockState(southeast).is(Blocks.WATER)){
                                maneuverPos = southeast.north(range).west(range);
                            }
                            else if(!worker.level.getBlockState(southwest).is(Blocks.WATER)){
                                maneuverPos = southwest.north(range).east(range);
                            }
                            else if(!worker.level.getBlockState(north).is(Blocks.WATER)){
                                maneuverPos = north.south(range);
                            }
                            else if(!worker.level.getBlockState(east).is(Blocks.WATER)){
                                maneuverPos = east.west(range);
                            }
                            else if(!worker.level.getBlockState(south).is(Blocks.WATER)){
                                maneuverPos = south.north(range);
                            }
                            else if(!worker.level.getBlockState(west).is(Blocks.WATER)){
                                maneuverPos = west.east(range);
                            }
                        //}

                        if(maneuverPos != null){
                            calculateManeuverDone = true;
                            updateBoatControl(maneuverPos.getX(), maneuverPos.getZ());
                            double distance = worker.distanceToSqr(maneuverPos.getX(), maneuverPos.getY(), maneuverPos.getZ());

                            if(distance < 2.75){ //valid value example: distance = 3.2
                                calculateManeuverDone = false;
                                state = CREATING_PATH;
                            }
                            else{

                            }
                        }
                    }
                }
     */

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
                case MANEUVER -> {
                        if(!calculateManeuverDone){
                            maneuverPos = findValidWaterBlock(sailor);
                            calculateManeuverDone = true;
                        }
                        if(maneuverPos != null){
                            updateBoatControl(maneuverPos.getX(), maneuverPos.getZ());

                            if(worker.distanceToSqr(maneuverPos.getX(), maneuverPos.getY(), maneuverPos.getZ()) <= 3){
                                state = CREATING_PATH;
                            }
                        }
                        else
                            calculateManeuverDone = false;
                    }
            }
    */
}
