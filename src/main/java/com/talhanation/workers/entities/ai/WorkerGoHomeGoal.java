package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import com.talhanation.workers.entities.workarea.HomeArea;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class WorkerGoHomeGoal extends Goal {

    public final AbstractWorkerEntity worker;
    public State state;
    public int cooldown;
    public int sleepTick;

    public WorkerGoHomeGoal(AbstractWorkerEntity worker) {
        this.worker = worker;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!worker.needsToSleep()) return false;
        if (!worker.isOwned()) return false;
        if (worker.getFollowState() != 0 && worker.getFollowState() != 6) return false;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return worker.needsToSleep()
                && worker.isOwned()
                && worker.getFollowState() != 1;
    }

    @Override
    public void start() {
        super.start();
        worker.setFollowState(6);
        setState(State.SELECT_HOME_AREA);
    }

    @Override
    public void stop() {
        super.stop();
        worker.stopSleeping();
        worker.clearSleepingPos();
        worker.getNavigation().stop();
        sleepTick = 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (worker.getCommandSenderWorld().isClientSide()) return;
        if (state == null) return;

        if (state == State.SLEEP) {
            tickSleep();
            return;
        }

        if (worker.tickCount % 5 != 0) return;

        switch (state) {
            case SELECT_HOME_AREA -> tickSelectHome();
            case MOVE_TO_HOME    -> tickMoveToHome();
            case GO_TO_BED       -> tickGoToBed();
        }
    }

    //////////////////////////////// STATES /////////////////////////////////////

    private void tickSelectHome() {
        if (++cooldown < 20) return;
        cooldown = 0;

        HomeArea found = findAssignedOrNearestFreeHomeArea((ServerLevel) worker.getCommandSenderWorld());

        if (found != null) {
            claimHomeArea(found);
            setState(State.MOVE_TO_HOME);
        }
    }

    private void tickMoveToHome() {
        HomeArea home = resolveCurrentHome((ServerLevel) worker.getCommandSenderWorld());

        if (home == null) {
            worker.homeAreaUUID = null;
            setState(State.SELECT_HOME_AREA);
            return;
        }

        if (moveToPosition(home.getOnPos(), 20)) return;

        if (home.assignedBedPos != null) {
            setState(State.GO_TO_BED);
        }
    }

    private void tickGoToBed() {
        HomeArea home = resolveCurrentHome((ServerLevel) worker.getCommandSenderWorld());

        BlockPos bedPos = home.assignedBedPos;
        if (!worker.getCommandSenderWorld().getBlockState(bedPos)
                .isBed(worker.getCommandSenderWorld(), bedPos, worker)) {
            home.assignedBedPos = null;
            return;
        }

        goToBed(bedPos);
    }

    private void tickSleep() {
        if (!worker.isSleeping()) {
            setState(State.GO_TO_BED);
            return;
        }

        worker.getNavigation().stop();

        if (++sleepTick % 200 == 0) {
            HomeArea home = resolveCurrentHome((ServerLevel) worker.getCommandSenderWorld());
            if (home != null) home.updateResidentSeen();

            if (worker.getHealth() < worker.getMaxHealth()) {
                worker.heal(1.0F);
            }
        }
    }

    //////////////////////////////// BED NAVIGATION /////////////////////////////

    private void goToBed(BlockPos bedPos) {
        PathNavigation nav = worker.getNavigation();
        nav.moveTo(bedPos.getX(), bedPos.getY(), bedPos.getZ(), 1.0D);

        worker.getLookControl().setLookAt(
                bedPos.getX(),
                bedPos.getY() + 1,
                bedPos.getZ(),
                10.0F,
                (float) worker.getMaxHeadXRot()
        );

        if (bedPos.distToCenterSqr(worker.position()) <= 35) {
            worker.startSleeping(bedPos);
            worker.setSleepingPos(bedPos);
            nav.stop();
            setState(State.SLEEP);
        }
    }

    //////////////////////////////// HOME AREA SELECTION ////////////////////////

    @Nullable
    private HomeArea findAssignedOrNearestFreeHomeArea(ServerLevel level) {
        if (worker.homeAreaUUID != null) {
            HomeArea homeArea = findHomeAreaByUUID(level, worker.homeAreaUUID);
            if (homeArea != null && !homeArea.isRemoved()) {
                if (homeArea.isResidentOf(worker.getUUID()) || !homeArea.isPlayerHome()) {
                    return homeArea;
                }
            }
            worker.homeAreaUUID = null;
        }

        List<HomeArea> areas = level.getEntitiesOfClass(
                HomeArea.class, worker.getBoundingBox().inflate(128));

        HomeArea nearest     = null;
        double   nearestDist = Double.MAX_VALUE;

        for (HomeArea homeArea : areas) {
            if (homeArea.isRemoved()) continue;
            if (homeArea.isOccupied()) continue;
            if (homeArea.isPlayerHome()) continue;
            if (!homeArea.canWorkHere(worker)) continue;

            homeArea.scanRoomQuality();

            if (!homeArea.canMoveIn()) continue;

            double dist = worker.distanceToSqr(homeArea.position());
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest     = homeArea;
            }
        }
        return nearest;
    }

    @Nullable
    private HomeArea resolveCurrentHome(ServerLevel level) {
        if (worker.homeAreaUUID == null) return null;
        HomeArea home = findHomeAreaByUUID(level, worker.homeAreaUUID);
        if (home == null || home.isRemoved()) {
            worker.homeAreaUUID = null;
            return null;
        }
        return home;
    }

    @Nullable
    private HomeArea findHomeAreaByUUID(ServerLevel level, UUID uuid) {
        return level.getEntitiesOfClass(HomeArea.class, worker.getBoundingBox().inflate(256))
                .stream()
                .filter(a -> uuid.equals(a.getUUID()))
                .findFirst()
                .orElse(null);
    }

    private void claimHomeArea(HomeArea area) {
        String name = worker.getCustomName() != null
                ? worker.getCustomName().getString()
                : worker.getUUID().toString();
        area.setResident(worker.getUUID(), name);
        worker.homeAreaUUID = area.getUUID();
    }

    //////////////////////////////// MOVEMENT ///////////////////////////////////

    public boolean moveToPosition(BlockPos pos, int threshold) {
        if (pos == null) return false;

        double distance = worker.getHorizontalDistanceTo(pos.getCenter());

        if (distance < threshold) {
            worker.getNavigation().stop();
            return false;
        }
        else {
            worker.getNavigation().moveTo(pos.getX(), pos.getY(), pos.getZ(), 0.8F);
            worker.getLookControl().setLookAt(pos.getCenter());
        }
        return true;
    }

    public void setState(State state) {
        this.state = state;
    }

    public enum State {
        SELECT_HOME_AREA,
        MOVE_TO_HOME,
        GO_TO_BED,
        SLEEP,
    }
}
