package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import com.talhanation.workers.entities.CourierEntity;
import com.talhanation.workers.entities.workarea.StorageArea;
import com.talhanation.workers.world.CourierAction;
import com.talhanation.workers.world.CourierRoute;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class CourierWorkGoal extends Goal {

    private static final int   ARRIVAL_DIST_SQ   = 9;        // 3 blocks²
    private static final int   SCAN_RADIUS        = 16;
    private static final int   NAVIGATION_TIMEOUT = 20 * 30; // 30 seconds
    private static final int   REPATH_INTERVAL    = 40;      // re-issue path every 2 seconds
    private static final float WALK_SPEED         = 0.85F;

    private final CourierEntity courier;

    private State state;
    private int   actionIndex;     // index into current waypoint's action list
    private int   waitTicks;       // remaining ticks for a WAIT action
    private int   navigationTicks; // ticks spent navigating to the current waypoint

    public CourierWorkGoal(CourierEntity courier) {
        this.courier = courier;
        setFlags(EnumSet.of(Flag.LOOK, Flag.MOVE));
    }

    // ── Goal contract ─────────────────────────────────────────────────────────

    @Override
    public boolean canUse() {
        return courier.isWorking() && courier.hasRoute() && !courier.needsToGetToChest();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public boolean isInterruptable() {
        return true;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        actionIndex     = 0;
        navigationTicks = 0;
        waitTicks       = 0;
        state           = State.NAVIGATE_TO_WAYPOINT;
    }

    @Override
    public void stop() {
        courier.getNavigation().stop();
    }

    // ── Tick ──────────────────────────────────────────────────────────────────

    @Override
    public void tick() {
        if (courier.getCommandSenderWorld().isClientSide()) return;
        if (state == null) return;

        switch (state) {
            case NAVIGATE_TO_WAYPOINT -> tickNavigate();
            case EXECUTE_ACTIONS      -> tickExecute();
            case WAIT_AT_WAYPOINT     -> tickWait();
        }
    }

    // ── Navigate ──────────────────────────────────────────────────────────────

    private void tickNavigate() {
        CourierRoute.CourierWaypoint waypoint = courier.getCurrentWaypoint();
        if (waypoint == null) return;

        BlockPos target = waypoint.getPosition();
        double distSq = courier.distanceToSqr(
                target.getX() + 0.5, target.getY(), target.getZ() + 0.5);

        if (distSq <= ARRIVAL_DIST_SQ) {
            courier.getNavigation().stop();
            actionIndex = 0;
            state       = State.EXECUTE_ACTIONS;
            return;
        }

        // Issue path on first tick (navigationTicks == 0) and every REPATH_INTERVAL ticks after.
        // FIX: moveTo is called before increment so tick 0 fires immediately.
        if (navigationTicks % REPATH_INTERVAL == 0) {
            courier.getNavigation().moveTo(
                    target.getX() + 0.5, target.getY(), target.getZ() + 0.5, WALK_SPEED);
        }
        navigationTicks++;

        // Give up if the waypoint appears unreachable.
        if (navigationTicks >= NAVIGATION_TIMEOUT) {
            advanceAndNavigate();
            return;
        }

        courier.setFollowState(6);
        courier.getLookControl().setLookAt(
                target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5);
    }

    // ── Execute actions ───────────────────────────────────────────────────────

    private void tickExecute() {
        CourierRoute.CourierWaypoint waypoint = courier.getCurrentWaypoint();
        if (waypoint == null) {
            advanceAndNavigate();
            return;
        }

        List<CourierAction> actions = waypoint.actions;

        // No actions or all done → move to next waypoint.
        if (actions.isEmpty() || actionIndex >= actions.size()) {
            advanceAndNavigate();
            return;
        }

        CourierAction action = actions.get(actionIndex);

        switch (action.getActionType()) {
            case WAIT -> {
                // Transition to wait state. actionIndex is advanced when the wait ends.
                waitTicks = action.getWaitSeconds() * 20;
                state     = State.WAIT_AT_WAYPOINT;
                // NOTE: do NOT touch actionIndex here — tickWait() increments it.
            }
            case PICKUP -> {
                executePickup(action, waypoint);
                actionIndex++;
            }
            case DEPOSIT -> {
                executeDeposit(action, waypoint);
                actionIndex++;
            }
        }
    }

    private void tickWait() {
        if (--waitTicks <= 0) {
            // FIX: increment BEFORE switching state — setState no longer resets actionIndex.
            actionIndex++;
            state = State.EXECUTE_ACTIONS;
        }
    }

    // ── Transfer logic ────────────────────────────────────────────────────────

    private void executePickup(CourierAction action, CourierRoute.CourierWaypoint waypoint) {
        if (action.getItemStack() == null || action.getItemStack().isEmpty()) return;

        ItemStack template = action.getItemStack();
        int maxAmount      = template.getCount();

        List<Container> targets = findContainers(action.getSourceType(), waypoint.getPosition());

        if (targets.isEmpty()) {
            notifyOwner(courier.TEXT_NO_TARGET_FOUND(waypoint.displayName));
            return;
        }

        int totalPickedUp = 0;
        for (Container container : targets) {
            if (totalPickedUp >= maxAmount) break;
            totalPickedUp += transferFromContainerToCourier(
                    container, template, maxAmount - totalPickedUp);
        }

        if (totalPickedUp == 0) {
            notifyOwner(courier.TEXT_SOURCE_EMPTY(
                    waypoint.displayName, template.getHoverName().getString()));
        }
    }

    private void executeDeposit(CourierAction action, CourierRoute.CourierWaypoint waypoint) {
        if (action.getItemStack() == null || action.getItemStack().isEmpty()) return;

        ItemStack template = action.getItemStack();
        int maxAmount      = template.getCount();

        List<Container> targets = findContainers(action.getSourceType(), waypoint.getPosition());

        if (targets.isEmpty()) {
            notifyOwner(courier.TEXT_NO_TARGET_FOUND(waypoint.displayName));
            return;
        }

        int totalDeposited = 0;
        for (Container container : targets) {
            if (totalDeposited >= maxAmount) break;
            totalDeposited += transferFromCourierToContainer(
                    container, template, maxAmount - totalDeposited);
        }

        if (totalDeposited == 0) {
            notifyOwner(courier.TEXT_TARGET_FULL(
                    waypoint.displayName, template.getHoverName().getString()));
        }
    }

    // ── Container discovery ───────────────────────────────────────────────────

    private List<Container> findContainers(CourierAction.SourceType type, BlockPos center) {
        List<Container> result = new ArrayList<>();
        if (type == null || center == null) return result;

        AABB scanBox = new AABB(center).inflate(SCAN_RADIUS);
        ServerLevel level = (ServerLevel) courier.getCommandSenderWorld();

        switch (type) {
            case CHEST -> {
                BlockPos.betweenClosed(
                        center.offset(-SCAN_RADIUS, -SCAN_RADIUS, -SCAN_RADIUS),
                        center.offset( SCAN_RADIUS,  SCAN_RADIUS,  SCAN_RADIUS)
                ).forEach(pos -> {
                    BlockState blockState = level.getBlockState(pos);
                    if (blockState.getBlock() instanceof ChestBlock chestBlock) {
                        Container c = ChestBlock.getContainer(chestBlock, blockState, level, pos, true);
                        if (c != null) result.add(c);
                    }
                });
            }

            case STORAGE -> {
                List<StorageArea> areas = level.getEntitiesOfClass(
                        StorageArea.class, scanBox, s -> s.canWorkHere(courier));
                for (StorageArea area : areas) {
                    area.scanStorageBlocks();
                    result.addAll(area.storageMap.values());
                }
            }

            case WORKER -> {
                level.getEntitiesOfClass(AbstractWorkerEntity.class, scanBox, w ->
                        w != courier
                        && w.getOwnerUUID() != null
                        && w.getOwnerUUID().equals(courier.getOwnerUUID())
                ).forEach(worker -> result.add(worker.getInventory()));
            }
        }

        return result;
    }

    // ── Item transfer helpers ─────────────────────────────────────────────────

    /**
     * Transfers up to {@code maxAmount} of the item type described by {@code template}
     * from {@code src} into the courier's inventory.
     *
     * @return number of items actually transferred
     */
    private int transferFromContainerToCourier(Container src, ItemStack template, int maxAmount) {
        int transferred = 0;

        for (int i = 0; i < src.getContainerSize() && transferred < maxAmount; i++) {
            ItemStack slot = src.getItem(i);
            if (slot.isEmpty() || !ItemStack.isSameItem(slot, template)) continue;

            int take = Math.min(slot.getCount(), maxAmount - transferred);
            ItemStack toAdd = slot.copyWithCount(take);

            if (!courier.canAddItem(toAdd)) break; // inventory full — no point continuing

            // addItem() returns whatever it could NOT place. We must shrink the source
            // by exactly what was actually added — not by the full requested amount —
            // otherwise items disappear when the courier inventory is partially full.
            ItemStack remainder = courier.addItem(toAdd);
            int actuallyAdded = take - remainder.getCount();
            if (actuallyAdded <= 0) break; // nothing fit; stop trying this container

            slot.shrink(actuallyAdded);
            src.setItem(i, slot.isEmpty() ? ItemStack.EMPTY : slot);
            src.setChanged();
            transferred += actuallyAdded;

            playChestSound(src);
        }

        return transferred;
    }

    /**
     * Transfers up to {@code maxAmount} of the item type described by {@code template}
     * from the courier's inventory into {@code dest}.
     *
     * @return number of items actually transferred
     */
    private int transferFromCourierToContainer(Container dest, ItemStack template, int maxAmount) {
        net.minecraft.world.SimpleContainer courierInv = courier.getInventory();
        int transferred = 0;

        // Slots 0–5 are equipment slots; start from 6 for item storage.
        for (int i = 6; i < courierInv.getContainerSize() && transferred < maxAmount; i++) {
            ItemStack slot = courierInv.getItem(i);
            if (slot.isEmpty() || !ItemStack.isSameItem(slot, template)) continue;

            int toTransfer = Math.min(slot.getCount(), maxAmount - transferred);
            int inserted   = insertIntoContainer(dest, slot, toTransfer);

            if (inserted == 0) break; // destination full — no point continuing

            slot.shrink(inserted);
            courierInv.setItem(i, slot.isEmpty() ? ItemStack.EMPTY : slot);
            transferred += inserted;

            playChestSound(dest);
        }

        return transferred;
    }

    /**
     * Inserts up to {@code amount} items from {@code stack} into {@code dest}.
     *
     * @return number of items actually inserted
     */
    private int insertIntoContainer(Container dest, ItemStack stack, int amount) {
        int inserted = 0;

        // First pass: top up existing stacks of the same item.
        for (int j = 0; j < dest.getContainerSize() && inserted < amount; j++) {
            ItemStack destSlot = dest.getItem(j);
            if (destSlot.isEmpty() || !ItemStack.isSameItemSameTags(destSlot, stack)) continue;

            int canAdd = destSlot.getMaxStackSize() - destSlot.getCount();
            if (canAdd <= 0) continue;

            int add = Math.min(canAdd, amount - inserted);
            destSlot.grow(add);
            dest.setItem(j, destSlot);
            dest.setChanged();
            inserted += add;
        }

        // Second pass: fill empty slots.
        for (int j = 0; j < dest.getContainerSize() && inserted < amount; j++) {
            if (!dest.getItem(j).isEmpty()) continue;

            int place = Math.min(stack.getMaxStackSize(), amount - inserted);
            dest.setItem(j, stack.copyWithCount(place));
            dest.setChanged();
            inserted += place;
        }

        return inserted;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void advanceAndNavigate() {
        courier.advanceWaypoint();
        actionIndex     = 0;
        navigationTicks = 0;
        state           = State.NAVIGATE_TO_WAYPOINT;
    }

    private void notifyOwner(net.minecraft.network.chat.Component msg) {
        Player owner = courier.getOwner();
        if (owner != null) owner.sendSystemMessage(msg);
    }

    private void playChestSound(Container container) {
        if (container instanceof ChestBlockEntity cbe) {
            courier.getCommandSenderWorld().playSound(
                    null, cbe.getBlockPos(),
                    SoundEvents.CHEST_OPEN, courier.getSoundSource(),
                    0.4F, 0.9F + courier.getRandom().nextFloat() * 0.2F);
        }
    }

    // ── State ─────────────────────────────────────────────────────────────────

    public enum State {
        NAVIGATE_TO_WAYPOINT,
        EXECUTE_ACTIONS,
        WAIT_AT_WAYPOINT
    }
}
