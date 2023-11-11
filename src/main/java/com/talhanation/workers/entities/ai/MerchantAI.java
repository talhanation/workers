package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.MerchantEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.vehicle.Boat;

import java.util.*;

import static com.talhanation.workers.entities.MerchantEntity.State.*;

public class MerchantAI extends Goal {

    private final MerchantEntity merchant;
    private MerchantEntity.State state;

    private boolean arrivedMessage;
    private boolean homeMessage;
    private boolean returningMessage;
    private float precision;
    private int timer;
    private Boat boat;
    private List<Entity> prevPassengers;

    public MerchantAI(MerchantEntity merchant) {
        this.merchant = merchant;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    public boolean canUse() {
        return merchant.getIsWorking() && !merchant.getFollow();
    }

    @Override
    public void start() {
        this.setWorkState(IDLE);
    }

    @Override
    public void tick() {
        //Main.LOGGER.info("State: " + state);

        if(state == null)
            state = MerchantEntity.State.fromIndex(merchant.getState());

        int indexChange = this.merchant.getReturning() ? -1 : 1;
        boolean condition = this.merchant.getReturning() ? (merchant.getCurrentWayPointIndex() == 0) :(merchant.getCurrentWayPointIndex() == merchant.WAYPOINTS.size() -1);
        MerchantEntity.State nextState = this.merchant.getReturning() ? HOME : ARRIVED;

        switch (state){
            case IDLE -> {
                if(merchant.getTraveling()){
                    returningMessage = false;
                    homeMessage = false;
                    arrivedMessage = false;
                    precision = 10F;
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
                    this.moveToPos(boat.getOnPos(), 1F);

                    if (boat.getOnPos().closerThan(merchant.getOnPos(), 4F)) {
                        boardBoat(boat);
                    }

                    if (boat.getFirstPassenger() != null && this.merchant.equals(boat.getFirstPassenger())) {

                        if(prevPassengers != null && !prevPassengers.isEmpty()){
                            for (Entity passenger : prevPassengers){
                                passenger.startRiding(boat, true);
                            }
                            prevPassengers = null;
                        }

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
                if(!arrivedMessage && merchant.getOwner() != null && merchant.getSendInfo()){
                    merchant.tellPlayer(merchant.getOwner(), Component.literal("I've arrived at my last waypoint."));
                    arrivedMessage = true;
                }
                if(merchant.isReturnTimeElapsed() || merchant.getReturning()){
                    merchant.setReturning(true);
                    merchant.setCurrentReturningTime(0);
                    if(!returningMessage && merchant.getOwner() != null && merchant.getSendInfo()){
                        merchant.tellPlayer(merchant.getOwner(), Component.literal("I'm returning now."));
                        returningMessage = true;
                    }
                    this.setWorkState(IDLE);
                }
            }

            case HOME -> {
                if(!homeMessage && merchant.getOwner() != null && merchant.getSendInfo()){
                    merchant.tellPlayer(merchant.getOwner(), Component.literal("I've arrived where i've started."));
                    homeMessage = true;
                }
                merchant.setTraveling(false);
                merchant.setReturning(false);

                if(merchant.getAutoStartTravel()){
                    if(merchant.isReturnTimeElapsed()){
                        merchant.setCurrentReturningTime(0);
                        merchant.setTraveling(true);
                        this.setWorkState(IDLE);
                    }
                }
                else
                    this.setWorkState(IDLE);
            }
        }
    }

    private void setWorkState(MerchantEntity.State state) {
        this.state = state;
        this.merchant.setState(state.getIndex());
    }
    @SuppressWarnings("all")
    private void moveToWayPoint(int indexChange, MerchantEntity.State nextState, boolean condition, boolean isSailing) {
        int index = merchant.getCurrentWayPointIndex();
        if(index >= this.merchant.WAYPOINTS.size()) index = this.merchant.WAYPOINTS.size() - 1;

        if (index < this.merchant.WAYPOINTS.size() && index >= 0 ) {// do not simplify
            this.merchant.setCurrentWayPoint(this.merchant.WAYPOINTS.get(index));
            BlockPos pos = merchant.getCurrentWayPoint();

            if(isSailing){
                //BlockPos pos1 = this.getCoastPos(pos);
                merchant.setSailPos(pos);
            } else {
                float speed = 0F;
                switch (merchant.getTravelSpeedState()){
                    default -> speed = 0.8F;
                    case 0 -> speed = 0.5F;
                    case 2 -> speed = 1.1F;
                }

                moveToPos(pos, speed);
            }

            double distance = merchant.distanceToSqr(pos.getX(), pos.getY(), pos.getZ());
            //Main.LOGGER.info("distance to waypoint: " + distance);

            if (distance <= precision) {
                if (condition) {
                    this.setWorkState(nextState);
                } else{
                    if(index <= this.merchant.WAYPOINTS.size() - 1) {
                        merchant.setCurrentWayPointIndex(index + indexChange);

                        if(!merchant.isFreeWater(pos.getX(), pos.getY(), pos.getZ()))
                            precision = 10F;
                        else
                            precision = 75F;


                    }
                }
            }
            else if (++timer > 100 && isSailing){
                if(precision < 250) precision += 25;

                this.timer = 0;
            }
        }

        this.changeTravelType();
    }
    private void searchForBoat() {
        List<Boat> list = merchant.getCommandSenderWorld().getEntitiesOfClass(Boat.class, merchant.getBoundingBox().inflate(16D));
        //list.removeIf(boat -> !boat.getPassengers().isEmpty());
        list.sort(Comparator.comparing(boatInList -> boatInList.distanceTo(merchant)));
        boolean getClosest = false;
        if(merchant.getBoatUUID() != null){
            if(list.isEmpty()){
                getClosest = true;
            }
            else {
                for (Boat boat : list){
                    if(merchant.getBoatUUID() != null && boat.getUUID().equals(merchant.getBoatUUID())){
                        this.boat = boat;
                    }
                }
                if(boat == null){
                    getClosest = true;
                }
            }
        }
        else getClosest = true;

        if(!list.isEmpty() && getClosest){
            merchant.setBoatUUID(Optional.of(list.get(0).getUUID()));
            this.boat = list.get(0);
        }
    }

    private void boardBoat(Boat boat){
        this.prevPassengers = new ArrayList<>();
        if(!boat.getPassengers().isEmpty()){
            prevPassengers = boat.getPassengers();
            for (Entity passenger : prevPassengers){
                passenger.stopRiding();
            }
        }

        merchant.startRiding(boat);
        this.boat = boat;
    }

    private void moveToPos(BlockPos pos, float speed) {
        //Move to pos -> normal movement
        BlockPos workerPos = merchant.getOnPos();

        int heightDiff = Math.abs(workerPos.getY() - pos.getY());
        double distance = merchant.distanceToSqr(pos.getX(), pos.getY(), pos.getZ());

        this.merchant.walkTowards(pos, speed);
        if(heightDiff >= 4){

        }
        /*
        else if(distance > 6.0F) {
            this.merchant.getMoveControl().setWantedPosition(pos.getX(), merchant.getOnPos().getY(), pos.getZ(), speed);
        }
         */
    }
    private void changeTravelType(){
        if(merchant.getCurrentWayPoint() != null && merchant.isWaterBlockPos(merchant.getCurrentWayPoint())){
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
