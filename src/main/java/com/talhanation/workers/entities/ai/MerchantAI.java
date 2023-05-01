package com.talhanation.workers.entities.ai;

import com.talhanation.workers.Main;
import com.talhanation.workers.entities.MerchantEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

import static com.talhanation.workers.entities.MerchantEntity.State.*;

public class MerchantAI extends Goal {

    private final MerchantEntity merchant;
    private MerchantEntity.State state;

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

        switch (state){
            case IDLE -> {
                if(merchant.getTraveling()) state = TRAVELING;
            }

            case TRAVELING -> {
                this.merchant.setCurrentWayPoint(this.merchant.WAYPOINTS.get(merchant.getCurrentWayPointIndex()));
                BlockPos pos = merchant.getCurrentWayPoint();

                this.moveTo(pos);

                if(merchant.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < 4F){

                    if(merchant.getCurrentWayPointIndex() == merchant.WAYPOINTS.size()){
                        this.state = ARRIVED;
                    }
                    else
                        merchant.setCurrentWayPointIndex(merchant.getCurrentWayPointIndex() + 1);
                }
            }

            case ARRIVED -> {
                if(!merchant.level.isDay()){
                    this.state = RETURNING;
                }
            }

            case RETURNING -> {
                this.merchant.setCurrentWayPoint(this.merchant.WAYPOINTS.get(merchant.getCurrentWayPointIndex()));
                BlockPos pos = merchant.getCurrentWayPoint();

                this.moveTo(pos);

                if(merchant.getCurrentWayPointIndex() == 0){
                    this.state = HOME;
                }
                else
                    merchant.setCurrentWayPointIndex(merchant.getCurrentWayPointIndex() - 1);
            }

            case HOME -> {

            }
        }
    }

    private void moveToWayPoint(int indexChange, int maxIndex, MerchantEntity.State nextState){

    }

    private void moveTo(BlockPos pos){
        if (pos != null) {
            //Move to minePos -> normal movement
            if (!pos.closerThan(merchant.getOnPos(), 12)) {
                this.merchant.walkTowards(pos, 1F);
            }
            //Near Mine Pos -> presice movement
            if (!pos.closerThan(merchant.getOnPos(), 2F)) {
                this.merchant.getMoveControl().setWantedPosition(pos.getX(), pos.getY(), pos.getZ(), 1);
            } else
                merchant.getNavigation().stop();
        }
    }
}
