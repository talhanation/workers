package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.MerchantEntity;
import com.talhanation.workers.entities.workarea.MarketArea;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.*;

public class MerchantWorkGoal extends Goal {

    private final MerchantEntity merchant;
    private State state;
    private int cooldown;

    public MerchantWorkGoal(MerchantEntity merchant) {
        this.merchant = merchant;
        setFlags(EnumSet.of(Flag.LOOK, Flag.MOVE));
    }
    @Override
    public boolean canUse() {
        if (merchant.isCreative()) return false;
        return merchant.shouldWork() && !merchant.needsToGetToChest();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public boolean isInterruptable(){
        return true;
    }

    @Override
    public boolean requiresUpdateEveryTick(){
        return true;
    }

    @Override
    public void start() {
        setState(State.SELECT_WORK_AREA);
    }

    @Override
    public void stop() {
        if (merchant.currentMarketArea != null) {
            merchant.currentMarketArea.setBeingWorkedOn(false);
            merchant.currentMarketArea = null;
            merchant.setCurrentMarketName("");
        }
        merchant.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (merchant.getCommandSenderWorld().isClientSide()) return;
        if (state == null) return;

        if (state != State.SELECT_WORK_AREA && isCurrentAreaGone()) {
            leaveCurrentArea();
            setState(State.SELECT_WORK_AREA);
            return;
        }

        switch (state) {
            case SELECT_WORK_AREA -> {
                if (merchant.currentMarketArea != null) {
                    setState(State.WALK_TO_CENTER);
                    return;
                }

                if (++cooldown < merchant.getRandom().nextInt(200)) return;
                cooldown = 0;

                MarketArea found = findBestArea((ServerLevel) merchant.getCommandSenderWorld());
                if (found == null) return;

                merchant.currentMarketArea = found;
                found.setBeingWorkedOn(true);
                found.setTime(0);

                merchant.setCurrentMarketName(found.getMarketName());
                setState(State.WALK_TO_CENTER);
            }

            case WALK_TO_CENTER -> {
                if (moveToPosition(BlockPos.containing(merchant.currentMarketArea.getArea().getCenter()), 3)) return;
                merchant.getNavigation().stop();
                setState(State.WORKING);
            }

            case WORKING -> {
                if (!merchant.currentMarketArea.isOpen()) {
                    leaveCurrentArea();
                    setState(State.SELECT_WORK_AREA);
                    return;
                }

                merchant.getNavigation().stop();
                merchant.setFollowState(6);

                Player nearby = merchant.getCommandSenderWorld()
                        .getNearestPlayer(merchant, 8);
                if (nearby != null) {
                    merchant.getLookControl().setLookAt(nearby, 30, 30);
                }
                else {
                    merchant.setYRot(merchant.currentMarketArea.getFacing().getOpposite().toYRot());
                }

                merchant.setCurrentMarketName(merchant.currentMarketArea.getMarketName());
            }
        }
    }

    private void leaveCurrentArea() {
        if (merchant.currentMarketArea != null) {
            merchant.currentMarketArea.setBeingWorkedOn(false);
            merchant.currentMarketArea = null;
            merchant.setCurrentMarketName("");
        }
    }

    private boolean isCurrentAreaGone() {
        return merchant.currentMarketArea == null || merchant.currentMarketArea.isRemoved();
    }


    private boolean moveToPosition(BlockPos pos, int thresholdBlocks) {
        double dist = merchant.getHorizontalDistanceTo(pos.getCenter());
        if (dist < thresholdBlocks) {
            merchant.getNavigation().stop();
            return false;
        }
        merchant.getNavigation().moveTo(pos.getX(), pos.getY(), pos.getZ(), 0.8F);
        merchant.setFollowState(6);
        merchant.getLookControl().setLookAt(pos.getCenter());
        return true;
    }

    @Nullable
    private MarketArea findBestArea(ServerLevel level) {
        List<MarketArea> areas = level.getEntitiesOfClass(MarketArea.class, merchant.getBoundingBox().inflate(64));

        MarketArea best = null;
        int bestScore = -1;

        for (MarketArea area : areas) {
            if (area == null) continue;
            if (!area.canWorkHere(merchant)) continue;
            if (area.isBeingWorkedOn()) continue;
            int score = 0;
            score += area.getTime() * 10;

            if (score > bestScore) {
                bestScore = score;
                best = area;
            }
        }
        return best;
    }

    private void setState(State s) {
        this.state = s;
    }

    public enum State {
        SELECT_WORK_AREA,
        WALK_TO_CENTER,
        WORKING
    }
}
