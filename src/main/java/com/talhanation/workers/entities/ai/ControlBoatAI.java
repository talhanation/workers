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
import net.minecraft.world.phys.Vec3;

import static com.talhanation.workers.entities.ai.ControlBoatAI.State.*;


public class ControlBoatAI extends Goal {

    private final AbstractWorkerEntity worker;
    private State state;
    private Path path;
    private Node node;

    public ControlBoatAI(IBoatController sailor) {
        this.worker = sailor.getWorker();

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
/*
            if (this.worker.getOwner() != null && worker.getOwner().isInWater()) {
                sailor.setSailPos(worker.getOwner().getOnPos());
                this.state = IDLE;
            }
*/


            switch (state) {

                case IDLE -> {

                    if (sailor.getSailPos() != null) {
                        double distance = sailor.getSailPos().distToCenterSqr(worker.position());
                        if (distance > 2) this.state = State.CREATING_PATH;
                    }
                }


                case CREATING_PATH -> {
                    if (sailor.getSailPos() != null) {
                        this.path = sailorPathNavigation.createPath(sailor.getSailPos(), 16, false, 0);

                        if (path != null) {
                            this.node = this.path.getNextNode();
                        /*
                            for(Node node : this.path.nodes) {
                                worker.level.setBlock(new BlockPos(node.x, worker.getY() + 2, node.z), Blocks.ICE.defaultBlockState(), 3);
                            }
                         */


                            state = MOVING_PATH;
                        }
                    } else
                        state = IDLE;
                }
                case MOVING_PATH -> {
                    double speedFactor = 1.25F;
                    double turnFactor = 0.8F;
                    double precision = 7.0F;

                    if(isSensitiveNeeded(path.getEndNode())){
                        speedFactor = 0.8F;
                        turnFactor = 1.75F;
                        precision = 3.5F;
                    }

                    double distance = getHorizontalDistance(node.asVec3(), Vec3.atCenterOf(worker.getOnPos())); //valid value example: distance = 6.5
                    if ((distance > 1.5F)) {
                        updateBoatControl(node.x, node.z, speedFactor, turnFactor);
                    }

                    if(distance <= precision){// default = 4.5F
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
            }
        }
    }

    private boolean isSensitiveNeeded(Node node) {
        BlockPos pos = new BlockPos(node.x, this.worker.getY(), node.z);

        BlockState stateBelow = this.worker.getLevel().getBlockState(pos.below());

        BlockState stateNorth = this.worker.getLevel().getBlockState(pos.north());
        BlockState stateEast = this.worker.getLevel().getBlockState(pos.east());
        BlockState stateSouth = this.worker.getLevel().getBlockState(pos.south());
        BlockState stateWest = this.worker.getLevel().getBlockState(pos.west());

        BlockState stateNorthEast = this.worker.getLevel().getBlockState(pos.north().east());
        BlockState stateNorthWest = this.worker.getLevel().getBlockState(pos.north().west());
        BlockState stateSouthEast = this.worker.getLevel().getBlockState(pos.south().east());
        BlockState stateSouthWest = this.worker.getLevel().getBlockState(pos.south().west());

        return !(
                stateBelow.is(Blocks.WATER) &&
                stateNorth.is(Blocks.WATER) &&
                stateEast.is(Blocks.WATER) &&
                stateSouth.is(Blocks.WATER) &&
                stateWest.is(Blocks.WATER) &&
                stateNorthEast.is(Blocks.WATER) &&
                stateNorthWest.is(Blocks.WATER) &&
                stateSouthEast.is(Blocks.WATER) &&
                stateSouthWest.is(Blocks.WATER));
    }


    private double getHorizontalDistance(Vec3 node, Vec3 pos) {
        double x1 = node.x;
        double z1 = node.z;

        double x2 = pos.x;
        double z2 = pos.z;

        return Math.sqrt((z2 - z1) * (z2 - z1) + (x2 - x1) * (x2 - x1));

    }

    private void updateBoatControl(double posX, double posZ, double speedFactor, double turnFactor) {
        if(this.worker.getVehicle() instanceof Boat boat && boat.getPassengers().get(0).equals(this.worker)) {
            double dx = posX - this.worker.getX();
            double dz = posZ - this.worker.getZ();

            float angle = Mth.wrapDegrees((float) (Mth.atan2(dz, dx) * 180.0D / 3.14D) - 90.0F);
            float drot = angle - Mth.wrapDegrees(boat.getYRot());

            boolean inputLeft = (drot < 0.0F && Math.abs(drot) >= 2F);
            boolean inputRight = (drot > 0.0F && Math.abs(drot) >= 2F);
            boolean inputUp = (Math.abs(drot) < 20.0F);

            float f = 0.0F;

            if (inputLeft) {
                boat.setYRot((float) (boat.getYRot() - 2.5F * turnFactor));
            }

            if (inputRight) {
                boat.setYRot((float) (boat.getYRot() + 2.5F * turnFactor));
            }


            if (inputRight != inputLeft && !inputUp) {
                f += 0.005F * speedFactor;
            }

            if (inputUp) {
                f += 0.02F * speedFactor;
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
    }
}
