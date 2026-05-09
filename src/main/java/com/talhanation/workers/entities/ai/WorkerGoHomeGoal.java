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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.UUID;

public class WorkerGoHomeGoal extends Goal {

    public final AbstractWorkerEntity worker;
    public State state;
    public int cooldown;
    public int sleepTick;

    // Bed navigation — mirrors how RestGoal works
    private Stack<BlockPos> bedStack = new Stack<>();
    @Nullable private BlockPos sleepPos;

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
        bedStack.clear();
        sleepPos = null;
        setState(State.SELECT_HOME_AREA);
    }

    @Override
    public void stop() {
        super.stop();
        worker.stopSleeping();
        worker.clearSleepingPos();
        worker.getNavigation().stop();
        bedStack.clear();
        sleepPos = null;
        sleepTick = 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (worker.getCommandSenderWorld().isClientSide()) return;
        if (state == null) return;

        // While sleeping, run every tick for smooth healing — no throttle
        if (state == State.SLEEP) {
            tickSleep();
            return;
        }

        if (worker.tickCount % 5 != 0) return;

        switch (state) {
            case SELECT_HOME_AREA -> tickSelectHome();
            case MOVE_TO_HOME    -> tickMoveToHome();
            case EAT_FOOD        -> tickEatFood();
            case FIND_BED        -> tickFindBed();
            case GO_TO_BED       -> tickGoToBed();
            case SLEEP_IN_PLACE  -> tickSleepInPlace();
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
        else {
            setState(State.SLEEP_IN_PLACE);
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

        setState(State.EAT_FOOD);
    }

    private void tickEatFood() {
        HomeArea home = resolveCurrentHome((ServerLevel) worker.getCommandSenderWorld());

        if (home == null) {
            setState(State.SLEEP_IN_PLACE);
            return;
        }

        if (worker.getHealth() < worker.getMaxHealth()) {
            tryEatFromHomeArea(home);
        }

        home.updateResidentSeen();
        setState(State.FIND_BED);
    }

    private void tickFindBed() {
        HomeArea home = resolveCurrentHome((ServerLevel) worker.getCommandSenderWorld());

        if (home == null) {
            setState(State.SLEEP_IN_PLACE);
            return;
        }

        bedStack = home.getBedsStack(worker);

        if (bedStack.isEmpty()) {
            setState(State.SLEEP_IN_PLACE);
        }
        else {
            sleepPos = bedStack.pop();
            setState(State.GO_TO_BED);
        }
    }

    private void tickGoToBed() {
        if (sleepPos == null) {
            setState(State.FIND_BED);
            return;
        }

        BlockState bedState = worker.getCommandSenderWorld().getBlockState(sleepPos);

        // Bed gone or was taken by another entity since we last checked → try next
        if (!bedState.isBed(worker.getCommandSenderWorld(), sleepPos, worker)
                || (bedState.hasProperty(BlockStateProperties.OCCUPIED)
                && bedState.getValue(BlockStateProperties.OCCUPIED))) {
            if (!bedStack.isEmpty()) {
                sleepPos = bedStack.pop();
            }
            else {
                setState(State.SLEEP_IN_PLACE);
            }
            return;
        }

        goToBed(sleepPos);
    }

    private void tickSleep() {
        if (!worker.isSleeping()) {
            // Woken up unexpectedly (e.g. explosion) → find another bed
            setState(State.FIND_BED);
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

    private void tickSleepInPlace() {
        worker.getNavigation().stop();

        if (++sleepTick % 200 == 0 && worker.getHealth() < worker.getMaxHealth()) {
            worker.heal(0.5F);
        }
    }

    //////////////////////////////// BED NAVIGATION /////////////////////////////

    /**
     * Mirrors RestGoal.goToBed():
     * Navigate to the bed FOOT block and call startSleeping() once in range.
     * startSleeping() sets Pose.SLEEPING and marks OCCUPIED=true on the bed
     * block — preventing villagers from claiming it while the worker rests.
     */
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
        // 1. Reconnect to the assigned home first (persists across restarts)
        if (worker.homeAreaUUID != null) {
            HomeArea assigned = findHomeAreaByUUID(level, worker.homeAreaUUID);
            if (assigned != null && !assigned.isRemoved()) {
                if (assigned.isResidentOf(worker.getUUID()) || !assigned.isOccupied()) {
                    return assigned;
                }
            }
            worker.homeAreaUUID = null;
        }

        // 2. Find nearest free HomeArea this worker is allowed to use
        List<HomeArea> areas = level.getEntitiesOfClass(
                HomeArea.class, worker.getBoundingBox().inflate(128));

        HomeArea nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (HomeArea area : areas) {
            if (area.isRemoved()) continue;
            if (area.isOccupied()) continue;
            if (!area.canWorkHere(worker)) continue;

            double dist = worker.distanceToSqr(area.position());
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = area;
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

    //////////////////////////////// EATING /////////////////////////////////////

    private void tryEatFromHomeArea(HomeArea homeArea) {
        homeArea.scanFoodContainers();

        Map<BlockPos, Container> containers = homeArea.foodContainerMap;

        for (Container container : containers.values()) {
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack stack = container.getItem(i);

                if (stack.isEmpty() || !stack.isEdible()) continue;

                FoodProperties food = stack.getFoodProperties(worker);
                if (food == null) continue;

                worker.heal(food.getNutrition());
                stack.shrink(1);
                container.setItem(i, stack);
                container.setChanged();
                return;
            }
        }
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
        EAT_FOOD,
        FIND_BED,
        GO_TO_BED,
        SLEEP,
        SLEEP_IN_PLACE
    }
}