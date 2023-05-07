package com.talhanation.workers.entities.ai;

import com.talhanation.workers.Main;
import com.talhanation.workers.entities.MerchantEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;
import java.util.List;

import static com.talhanation.workers.entities.MerchantEntity.State.*;

public class MerchantAI extends Goal {

    private final MerchantEntity merchant;
    private MerchantEntity.State state;

    private Boat boat;

    public MerchantAI(MerchantEntity merchant) {
        this.merchant = merchant;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    public boolean canUse() {
        return merchant.getIsWorking();
    }

    @Override
    public void start() {
        this.setWorkState(IDLE);
    }

    @Override
    public void tick() {
        Main.LOGGER.info("State: " + state);

        if(state == null)
            state = MerchantEntity.State.fromIndex(merchant.getState());

        int indexChange = this.merchant.getReturning() ? -1 : 1;
        boolean condition = this.merchant.getReturning() ? (merchant.getCurrentWayPointIndex() == 0) :(merchant.getCurrentWayPointIndex() == merchant.WAYPOINTS.size() -1);
        MerchantEntity.State nextState = this.merchant.getReturning() ? HOME : ARRIVED;

        switch (state){
            case IDLE -> {
                if(merchant.getTraveling()){
                    int index = this.merchant.getCurrentWayPointIndex();

                    if (index >= 0 && index < this.merchant.WAYPOINTS.size()) {
                        this.merchant.setCurrentWayPoint(this.merchant.WAYPOINTS.get(index));
                    }
                    this.changeTravelType();
                }
            }
            case MOVE_TO_BOAT -> {
                this.searchForBoat();
                if(boat != null) {
                    this.moveToPos(boat.getOnPos());

                    if (boat.getOnPos().closerThan(merchant.getOnPos(), 4F)) {
                        merchant.startRiding(boat);
                    }

                    if (boat.getFirstPassenger() != null && this.merchant.equals(boat.getFirstPassenger())) {
                        this.setWorkState(SAILING);
                    }
                }
                else {
                    this.setWorkState(TRAVELING_GROUND);
                }
            }

            case SAILING -> {
                moveToWayPoint(indexChange, nextState, merchant.getCurrentWayPointIndex() == merchant.WAYPOINTS.size() -1, true);
            }

            case TRAVELING_GROUND -> {
                if(merchant.getVehicle() instanceof Boat) merchant.stopRiding();
                moveToWayPoint(indexChange, nextState, condition, false);
            }

            case ARRIVED -> {
                if(!merchant.level.isDay()){//Returning time elapsed
                    merchant.setReturning(true);
                    this.setWorkState(IDLE);
                }
            }

            case HOME -> {
                merchant.setTraveling(false);
                merchant.setReturning(false);
                this.setWorkState(IDLE);
            }
        }
    }

    private void setWorkState(MerchantEntity.State state) {
        this.state = state;
        this.merchant.setState(state.getIndex());

    }

    private void moveToWayPoint(int indexChange, MerchantEntity.State nextState, boolean condition, boolean isSailing) {
        int index = merchant.getCurrentWayPointIndex();
        if(index >= this.merchant.WAYPOINTS.size()) index = this.merchant.WAYPOINTS.size() - 1;

        if (index < this.merchant.WAYPOINTS.size() && index >= 0 ) {// do not simplify
            this.merchant.setCurrentWayPoint(this.merchant.WAYPOINTS.get(index));
            BlockPos pos = merchant.getCurrentWayPoint();

            if(isSailing){
                merchant.setSailPos(pos);
            } else {
                moveToPos(pos);
            }

            if (merchant.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < 4.5F) {
                if (condition) {
                    this.setWorkState(nextState);
                } else{
                    if(index <= this.merchant.WAYPOINTS.size() - 1) {
                        merchant.setCurrentWayPointIndex(index + indexChange);
                    }
                }
            }
        }

        this.changeTravelType();
    }
    private void searchForBoat() {
        List<Boat> list = merchant.level.getEntitiesOfClass(Boat.class, merchant.getBoundingBox().inflate(16D));
        if (!list.isEmpty()) {
            this.boat = list.get(0);
        }
    }

    private void moveToPos(BlockPos pos) {
        if(pos != null) {
            //Move to Pos -> normal movement
            if (!pos.closerThan(merchant.getOnPos(), 4F)) {
                this.merchant.walkTowards(pos, 1F);
            }
            //Near Pos -> precise movement
            if (!pos.closerThan(merchant.getOnPos(), 1F)) {
                this.merchant.getMoveControl().setWantedPosition(pos.getX(), pos.getY(), pos.getZ(), 1F);
            }
        }
    }

    private boolean isWaterBlockPos(BlockPos pos){
        for(int i = 0; i < 5; i++){
            BlockPos pos1 = pos.below(i);
            BlockState state = this.merchant.level.getBlockState(pos1);
            if(state.is(Blocks.WATER)){
                return true;
            }
        }
        return false;
    }

    private void changeTravelType(){
        if(isWaterBlockPos(merchant.getCurrentWayPoint())){
            if(boat != null && boat.getFirstPassenger() != null && this.merchant.equals(boat.getFirstPassenger())){
                this.setWorkState(SAILING);
            }
            else
                this.setWorkState(MOVE_TO_BOAT);
        }
        else if (state != ARRIVED && state != HOME)
            this.setWorkState(TRAVELING_GROUND);
    }
}
