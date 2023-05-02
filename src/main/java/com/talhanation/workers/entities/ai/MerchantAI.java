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
        Main.LOGGER.info("Index: " + merchant.getCurrentWayPointIndex());

        switch (state){
            case IDLE -> {
                if(merchant.getTraveling()) state = TRAVELING;
            }

            case TRAVELING -> {

                moveToWayPoint(1, ARRIVED, merchant.getCurrentWayPointIndex() == merchant.WAYPOINTS.size());

            }

            case ARRIVED -> {
                if(!merchant.level.isDay()){
                    this.state = RETURNING;
                }
            }

            case RETURNING -> {
                moveToWayPoint(-1, HOME, merchant.getCurrentWayPointIndex() == 0);
            }

            case HOME -> {

            }
        }
    }

    private void moveToWayPoint(int indexChange, MerchantEntity.State nextState, boolean condition) {
        this.merchant.setCurrentWayPoint(this.merchant.WAYPOINTS.get(merchant.getCurrentWayPointIndex()));


        BlockPos pos = merchant.getCurrentWayPoint();

        this.merchant.walkTowards(pos, 1F);

        if (merchant.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < 4.5F) {

            if (condition) {
                this.state = nextState;
            } else
                merchant.setCurrentWayPointIndex(merchant.getCurrentWayPointIndex() + indexChange);
        }
    }
}
