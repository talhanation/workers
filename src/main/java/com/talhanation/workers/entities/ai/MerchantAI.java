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
        this.state = IDLE;
    }

    @Override
    public void tick() {
        Main.LOGGER.info("State: " + state);
        Main.LOGGER.info("Index: " + merchant.getCurrentWayPointIndex());

        switch (state){
            case IDLE -> {
                if(merchant.getTraveling()){
                    this.merchant.setCurrentWayPoint(this.merchant.WAYPOINTS.get(merchant.getCurrentWayPointIndex()));
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
                        state = SAILING;
                    }
                }
                else {
                    state = TRAVELING_GROUND;
                }
            }

            case SAILING -> {
                moveToWayPoint(1, ARRIVED, merchant.getCurrentWayPointIndex() == merchant.WAYPOINTS.size(), true);
            }

            case TRAVELING_GROUND -> {
                if(merchant.getVehicle() instanceof Boat) merchant.stopRiding();

                moveToWayPoint(1, ARRIVED, merchant.getCurrentWayPointIndex() == merchant.WAYPOINTS.size(), false);
            }

            case ARRIVED -> {
                if(!merchant.level.isDay()){
                    this.state = RETURNING;
                }
            }

            case RETURNING -> {
                moveToWayPoint(-1, HOME, merchant.getCurrentWayPointIndex() == 0, false);
            }

            case HOME -> {
                merchant.setTraveling(false);
            }
        }
    }

    private void moveToWayPoint(int indexChange, MerchantEntity.State nextState, boolean condition, boolean isSailing) {
        this.merchant.setCurrentWayPoint(this.merchant.WAYPOINTS.get(merchant.getCurrentWayPointIndex()));
        BlockPos pos = merchant.getCurrentWayPoint();

        if(isSailing){
            merchant.setSailPos(pos);
        }
        else
            moveToPos(pos);

        if (merchant.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < 4.5F) {

            if (condition) {
                this.state = nextState;
            } else
                merchant.setCurrentWayPointIndex(merchant.getCurrentWayPointIndex() + indexChange);
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
            //Move to minePos -> normal movement
            if (!pos.closerThan(merchant.getOnPos(), 12F)) {
                this.merchant.walkTowards(pos, 1F);
            }
            //Near Mine Pos -> precise movement
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
                state = SAILING;
            }
            else
                state = MOVE_TO_BOAT;
        }
        else
            state = TRAVELING_GROUND;
    }
}
