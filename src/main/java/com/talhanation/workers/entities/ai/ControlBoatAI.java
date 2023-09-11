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
    private byte timer;

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
        if (this.worker instanceof IBoatController sailor && !worker.getLevel().isClientSide() && worker.getNavigation() instanceof SailorPathNavigation sailorPathNavigation && worker.getIsWorking()) {

            if (this.worker.getOwner() != null && worker.getOwner().isInWater()) {
                sailor.setSailPos(worker.getOwner().getOnPos());
                this.state = IDLE;
            }

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

                            for(Node node : this.path.nodes) {
                                worker.level.setBlock(new BlockPos(node.x, worker.getY() + 3, node.z), Blocks.ICE.defaultBlockState(), 3);
                            }

                            state = MOVING_PATH;
                        }
                    } else
                        state = IDLE;
                }
                case MOVING_PATH -> {
                    double speedFactor = 0.9F;
                    double turnFactor = 1.0F;



                    if(sailor.getBoatControlSensitiveMode() || isSensitiveNeeded(path.getEndNode())){
                        speedFactor = 0.7F;
                        turnFactor = 2.00F;
                    }
                    double precision = 40F;
                    if(!isFreeWater(node)) precision = 15F;

                    double distance = this.worker.distanceToSqr(node.x, node.y, node.z);
                    if ((distance > 5F)) {
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
                    else if (++timer > 100){
                        state = CREATING_PATH;
                        this.path = null;
                        this.node = null;
                        this.timer = 0;
                    }
                }

                case DONE -> {
                    //sailor.setSailPos(null);
                    state = IDLE;
                }
            }
        }
    }

    private boolean isFreeWater(Node node){
        for(int i = -1; i <= 1; i++) {
            for (int k = -1; k <= 1; k++) {
                BlockPos pos = new BlockPos(node.x, node.y, node.z).offset(i, 0, k);
                BlockState state = this.worker.level.getBlockState(pos);

                if(!state.is(Blocks.WATER)) return false;
            }
        }
        return true;
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

    private void updateBoatControl(double posX, double posZ, double speedFactor, double turnFactor) {
        if(this.worker.getVehicle() instanceof Boat boat && boat.getPassengers().get(0).equals(this.worker)) {
            double dx = posX - this.worker.getX();
            double dz = posZ - this.worker.getZ();

            float angle = Mth.wrapDegrees((float) (Mth.atan2(dz, dx) * 180.0D / 3.14D) - 90.0F);
            float drot = angle - Mth.wrapDegrees(boat.getYRot());

            boolean inputLeft = (drot < 0.0F && Math.abs(drot) >= 1F);
            boolean inputRight = (drot > 0.0F && Math.abs(drot) >= 1F);
            boolean inputUp = (Math.abs(drot) < 15.0F);

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
