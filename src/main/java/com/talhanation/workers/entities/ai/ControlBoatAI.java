package com.talhanation.workers.entities.ai;

import com.talhanation.workers.Main;
import com.talhanation.workers.entities.AbstractWorkerEntity;
import com.talhanation.workers.entities.IBoatController;
import com.talhanation.workers.entities.ai.navigation.SailorPathNavigation;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.behavior.RandomSwim;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraftforge.common.Tags;

import static com.talhanation.workers.entities.ai.ControlBoatAI.State.*;


public class ControlBoatAI extends Goal {

    private final AbstractWorkerEntity worker;
    private State state;
    private Path path;
    private Node node;
    private int timer;
    private float precision;
    private final boolean DEBUG = false;

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
        return false;
    }

    public void start(){
        state = State.IDLE;
        precision = 100F;
    }

    public void stop(){
    }

    public boolean requiresUpdateEveryTick() {
        return true;
    }

    public void tick() {
        if (this.worker instanceof IBoatController sailor && !worker.getLevel().isClientSide() && worker.getNavigation() instanceof SailorPathNavigation sailorPathNavigation && worker.getIsWorking()) {
            if(DEBUG) {
                if (this.worker.getOwner() != null && worker.getOwner().isInWater()) {
                    sailor.setSailPos(worker.getOwner().getOnPos());
                    this.state = IDLE;
                }
            }

            switch (state) {

                case IDLE -> {

                    if (sailor.getSailPos() != null) {
                        this.state = State.CREATING_PATH;
                    }
                }

                case CREATING_PATH -> {
                    if (sailor.getSailPos() != null) {
                        this.path = sailorPathNavigation.createPath(sailor.getSailPos(), 16, false, 0);

                        if (path != null) {
                            this.node = this.path.getNextNode();

                            if(DEBUG){
                                for(Node node : this.path.nodes) {
                                    worker.level.setBlock(new BlockPos(node.x, worker.getY() + 4, node.z), Blocks.ICE.defaultBlockState(), 3);
                                }
                            }


                            state = MOVING_PATH;
                        }
                    } else
                        state = IDLE;
                }
                case MOVING_PATH -> {
                    if(getWaterDepth(worker.getOnPos()) >= 7 && path.getEndNode() != null) {
                        this.node = path.getEndNode();
                    }

                    double distance = this.worker.distanceToSqr(node.x, node.y, node.z);
                    if(DEBUG) {
                        Main.LOGGER.info("################################");
                        Main.LOGGER.info("State: " + this.state);
                        Main.LOGGER.info("Precision: " + precision);
                        Main.LOGGER.info("distance to node: " + distance);
                        Main.LOGGER.info("################################");
                    }


                    if(distance >= 5F){
                        sailor.updateBoatControl(node.x, node.z, 0.9F, 1.1F, node);
                    }

                    if(distance <= precision){// default = 4.5F
                        path.advance();
                        if(!isFreeWater(node)){
                            precision = sailor.getPrecisionMin();
                        }
                        else precision = sailor.getPrecisionMax();

                        this.timer = 0;
                        if (path.getNodeCount() == path.getNextNodeIndex() - 1) {
                            state = CREATING_PATH;
                        }

                        if (path.getNodeCount() == path.getNextNodeIndex() - 1 || node.equals(path.getEndNode())) {
                            node = null;
                            state = State.DONE;
                        }

                        try {
                            this.node = path.getNextNode();// FIX for "IndexOutOfBoundsException: Index 23 out of bounds for length 23" or "Index 1 out of bounds for length 1"

                        } catch (IndexOutOfBoundsException e) {
                            this.node = path.nodes.get(path.nodes.size() - 1);
                        }
                    }
                    else if (++timer > 50){
                        if(precision < 300) precision += 25;
                        else{
                            precision = 50F;
                            state = CREATING_PATH;
                        }
                        this.timer = 0;
                    }
                }

                case DONE -> {
                    sailor.setSailPos(null);
                    state = IDLE;
                }
            }
        }
    }

    private int getWaterDepth(BlockPos pos){
        int depth = 0;
        for(int i = 0; i < 10; i++){
            BlockState state = worker.level.getBlockState(pos.below(i));
            if(state.is(Blocks.WATER)){
                depth++;
            }
            else break;
        }
        return depth;
    }

    private boolean isFreeWater(Node node){
        for(int i = -2; i <= 2; i++) {
            for (int k = -2; k <= 2; k++) {
                BlockPos pos = new BlockPos(node.x, this.worker.getY(), node.z).offset(i, 0, k);
                BlockState state = this.worker.level.getBlockState(pos);

                if(!state.is(Blocks.WATER) || (!state.is(Blocks.KELP_PLANT) || !state.is(Blocks.KELP)))
                    return false;
            }
        }
        return true;
    }
/*
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
*/

    enum State{
        IDLE,
        CREATING_PATH,
        MOVING_PATH,
        DONE,
    }
}
