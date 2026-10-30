package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.CourierEntity;
import com.talhanation.workers.entities.workarea.AbstractWorkAreaEntity;
import com.talhanation.workers.entities.workarea.KitchenArea;
import com.talhanation.workers.entities.workarea.StorageArea;
import com.talhanation.workers.entities.workarea.MarketArea;
import com.talhanation.workers.entities.workarea.WorkshopArea;
import com.talhanation.workers.world.CourierAction;
import com.talhanation.workers.world.CourierRoute;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.*;

public class CourierWorkGoal extends Goal {

    private static final int ARRIVAL_DIST_SQ    = 9;
    // Actions are only executed near the waypoint; further away the courier walks back first.
    private static final int EXECUTE_DIST_SQ    = 81;
    public static final int SCAN_RADIUS          = 16;
    private static final int NAVIGATION_TIMEOUT  = 2000;
    private static final int REPATH_INTERVAL     = 20;
    private static final int TICKS_PER_TRANSFER  = 20;
    private static final float WALK_SPEED        = 0.85F;

    private final CourierEntity courier;
    private State state;
    private int   actionIndex;
    private int   waitTicks;
    private int   navigationTicks;
    // When night falls mid-route, we flag this and finish the current cycle safely
    // before releasing control to WorkerGoHomeGoal.
    private boolean pendingGoHome = false;
    // Ensures the "target full" message is sent at most once per deposit action,
    // not every tick while the courier keeps retrying a full container.
    private boolean depositFullReported = false;
    // Route version the goal state belongs to; a new route resets the goal (see CourierEntity.routeVersion).
    private int lastRouteVersion;
    // Containers found at the current waypoint, per source/target. Cleared when the courier moves on.
    private final Map<String, List<Container>> containerCache = new HashMap<>();
    // Selected target areas that were reported missing, so the owner is told only once per route.
    private final Set<UUID> reportedMissingTargets = new HashSet<>();

    // ── Active transfer state ─────────────────────────────────────────────────
    private CourierAction               activeAction;
    private CourierRoute.CourierWaypoint activeWp;
    private List<Container>             activeTargets;
    private int                         activeContainerIdx; // PICKUP: which source container
    private int                         activeSlotIdx;      // PICKUP: slot in source / DEPOSIT: slot in workInv
    private int                         activeTransferred;  // total items moved so far
    private int                         activeFillLimit;    // PUT_FILL/TAKE_FILL: pre-computed missing amount (desired count − already present)
    private final Set<Container>        openedContainers = new HashSet<>();

    public CourierWorkGoal(CourierEntity courier){
        this.courier = courier;
        setFlags(EnumSet.of(Flag.LOOK, Flag.MOVE));
    }

    @Override public boolean canUse(){
        return courier.isWorking() && courier.hasRoute() && !courier.needsToGetToChest();
    }
    @Override public boolean canContinueToUse(){
        if (!courier.isWorking() || !courier.hasRoute() || courier.needsToGetToChest()) return false;
        // Night fell → complete the current cycle back to waypoint 0 before stopping
        if (courier.needsToSleep()) {
            pendingGoHome = true;
            return !isSafeToGoHome();
        }
        return true;
    }

    // Safe to stop = we are at waypoint 0 and not mid-return (full cycle completed)
    private boolean isSafeToGoHome() {
        return courier.currentWaypointIndex == 0 && !courier.returning;
    }
    @Override public boolean isInterruptable(){
        return true;
    }
    @Override public boolean requiresUpdateEveryTick(){
        return true;
    }

    @Override
    public void start(){
        actionIndex        = 0;
        navigationTicks    = 0;
        waitTicks          = 0;
        activeTransferred  = 0;
        pendingGoHome      = false;
        lastRouteVersion   = courier.routeVersion;
        containerCache.clear();
        // The working state (6) is set by the route-start command, not here, so an
        // owner command issued mid-route is never overridden back to working.
        state = State.NAVIGATE_TO_WAYPOINT;
    }

    @Override
    public void stop(){
        closeOpenedChests();
        courier.getNavigation().stop();
    }

    @Override
    public void tick(){
        if (courier.getCommandSenderWorld().isClientSide()) return;
        if (state == null) return;
        if (lastRouteVersion != courier.routeVersion){
            // The owner loaded a new route: drop whatever was in progress and start fresh.
            lastRouteVersion = courier.routeVersion;
            resetRouteState();
            return;
        }
        switch (state){
            case NAVIGATE_TO_WAYPOINT -> tickNavigate();
            case EXECUTE_ACTIONS      -> tickExecute();
            case WAIT_AT_WAYPOINT     -> tickWait();
            case TRANSFER_STEP        -> tickTransferStep();
            case WAIT_TRANSFER_STEP   -> tickWaitTransfer();
        }
    }

    private void tickNavigate(){
        CourierRoute.CourierWaypoint waypoint = courier.getCurrentWaypoint();
        if (waypoint == null) return;

        BlockPos target = waypoint.getPosition();
        double distSq = courier.distanceToSqr(target.getX() + 0.5, target.getY(), target.getZ() + 0.5);
        if (distSq <= ARRIVAL_DIST_SQ){
            courier.getNavigation().stop();
            actionIndex = 0;
            state = State.EXECUTE_ACTIONS;
            return;
        }
        if (navigationTicks % REPATH_INTERVAL == 0){
            courier.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, WALK_SPEED);
        }
        navigationTicks++;

        if (navigationTicks >= NAVIGATION_TIMEOUT){
            notifyOwner(courier.TEXT_WAYPOINT_UNREACHABLE(waypointLabel(waypoint)));
            advanceAndNavigate();
            return;
        }

        // start() already claimed the working state; calling it here every
        // tick would override owner commands and pull the courier back to work.
        courier.getLookControl().setLookAt(target.getX() + 0.5, courier.getY() + 0.5, target.getZ() + 0.5);
    }

    private void tickExecute(){
        CourierRoute.CourierWaypoint wp = courier.getCurrentWaypoint();
        if (wp != null && courier.distanceToSqr(wp.getPosition().getX() + 0.5, wp.getPosition().getY(), wp.getPosition().getZ() + 0.5) > EXECUTE_DIST_SQ){
            // Pushed away or the waypoint changed under us → walk back before touching any container.
            navigationTicks = 0;
            state = State.NAVIGATE_TO_WAYPOINT;
            return;
        }
        if(courier.returning && courier.currentWaypointIndex == 0){
            courier.returning = false;
            courier.shouldCycle = courier.pendingShouldCycle;

            // Safe stop point: full route cycle complete, night is pending → let GoHomeGoal take over
            if (pendingGoHome) {
                pendingGoHome = false;
                return; // canContinueToUse will now return false → goal stops
            }
        }
        else if(courier.shouldCycle && courier.currentWaypointIndex == 0){
            // In cycle mode `returning` is never set, so the ping-pong branch above
            // never fires. A wrap-around back to waypoint 0 is the cycle's natural
            // end: apply any pending mode change and honour a pending go-home here.
            courier.shouldCycle = courier.pendingShouldCycle;

            if (pendingGoHome) {
                pendingGoHome = false;
                return;
            }
        }

        if (wp == null || courier.returning){
            advanceAndNavigate();
            return;
        }
        List<CourierAction> actions = wp.actions;
        if (actions.isEmpty() || actionIndex >= actions.size()){
            advanceAndNavigate();
            return;
        }
        CourierAction action = actions.get(actionIndex);
        switch (action.getActionType()){
            case WAIT -> {
                waitTicks = action.getWaitSeconds() * 20;
                state = State.WAIT_AT_WAYPOINT;
            }
            case TAKE, TAKE_ANY, TAKE_ALL, TAKE_FILL,
                    PUT, PUT_ANY, PUT_ALL, PUT_FILL -> initTransferAction(action, wp);
        }
    }

    private void initTransferAction(CourierAction action, CourierRoute.CourierWaypoint wp){
        List<Container> targets = getTargetContainers(action, wp);
        if (targets == null){
            // A specific work area was selected but it is gone → skip, the owner was informed.
            actionIndex++;
            return;
        }
        if (targets.isEmpty()){
            notifyOwner(courier.TEXT_NO_TARGET_FOUND(waypointLabel(wp)));
            actionIndex++;
            return;
        }

        activeAction       = action;
        activeWp           = wp;
        activeTargets      = targets;
        activeTransferred  = 0;
        activeContainerIdx = 0;
        depositFullReported = false;
        activeSlotIdx      = isDepositType(action.getActionType())
                ? ((getWorkingInventory() == courier.getInventory()) ? 6 : 0)
                : 0;

        // PUT_FILL / TAKE_FILL: pre-compute how many more of the item are still needed.
        //   PUT_FILL  → measured against the TARGET containers (how many to put in).
        //   TAKE_FILL → measured against the courier's OWN inventory (how many to take out).
        // If the goal is already met, skip the action entirely.
        CourierAction.ActionType atype = action.getActionType();
        if (atype == CourierAction.ActionType.PUT_FILL || atype == CourierAction.ActionType.TAKE_FILL){
            ItemStack tpl = action.getItemStack();
            if (tpl == null || tpl.isEmpty()){
                actionIndex++;
                return;
            }
            int already = 0;
            if (atype == CourierAction.ActionType.PUT_FILL){
                for (Container dest : targets){
                    for (int j = 0; j < dest.getContainerSize(); j++){
                        ItemStack s = dest.getItem(j);
                        if (!s.isEmpty() && ItemStack.isSameItem(s, tpl)) already += s.getCount();
                    }
                }
            }
            else { // TAKE_FILL → count what the courier already carries
                Container inv = getWorkingInventory();
                for (int j = 0; j < inv.getContainerSize(); j++){
                    ItemStack s = inv.getItem(j);
                    if (!s.isEmpty() && ItemStack.isSameItem(s, tpl)) already += s.getCount();
                }
            }
            activeFillLimit = Math.max(0, tpl.getCount() - already);
            if (activeFillLimit == 0){
                actionIndex++;
                return;
            }
        }

        state = State.TRANSFER_STEP;
    }


    private void tickTransferStep(){
        if (isDepositType(activeAction.getActionType()))
            tickDepositStep();
        else
            tickPickupStep();
    }

    private void tickPickupStep(){
        int limit = getTransferLimit();
        if (activeTransferred >= limit){ finishTransfer(); return; }

        while (activeContainerIdx < activeTargets.size()){
            Container src = activeTargets.get(activeContainerIdx);

            while (activeSlotIdx < src.getContainerSize()){
                ItemStack slot  = src.getItem(activeSlotIdx);
                int       sIdx  = activeSlotIdx++;

                if (slot.isEmpty() || !matchesTemplate(slot)) continue;

                int toTake = limit == Integer.MAX_VALUE
                        ? slot.getCount()
                        : Math.min(slot.getCount(), limit - activeTransferred);

                int taken = doPickupOneSlot(src, sIdx, slot, toTake);

                if (taken > 0){
                    activeTransferred += taken;
                    waitTicks = TICKS_PER_TRANSFER;
                    state = State.WAIT_TRANSFER_STEP;
                    return;
                }
                // Matched the item but couldn't take any → the courier's own
                // inventory is full. Only tell the owner if we picked up nothing at
                // all this action (avoid spam once a partial transfer succeeded).
                if (activeTransferred == 0){
                    notifyOwner(courier.TEXT_INVENTORY_FULL(
                            waypointLabel(activeWp)));
                }
                finishTransfer();
                return;
            }
            activeContainerIdx++;
            activeSlotIdx = 0;
        }
        finishTransfer();
    }

    private void tickDepositStep(){
        Container workInv = getWorkingInventory();
        int       limit   = getTransferLimit();

        while (activeSlotIdx < workInv.getContainerSize()){
            ItemStack slot = workInv.getItem(activeSlotIdx);
            int       sIdx = activeSlotIdx++;

            if (slot.isEmpty() || !matchesTemplate(slot)) continue;
            if (activeTransferred >= limit){ finishTransfer(); return; }

            int toMove   = limit == Integer.MAX_VALUE
                    ? slot.getCount()
                    : Math.min(slot.getCount(), limit - activeTransferred);
            int deposited = 0;

            for (Container dest : activeTargets){
                int ins = insertIntoContainer(dest, slot, toMove - deposited);
                if (ins > 0) openChestFor(dest);
                deposited += ins;
                slot.shrink(ins);
                if (slot.isEmpty()) break;
            }

            workInv.setItem(sIdx, slot.isEmpty() ? ItemStack.EMPTY : slot);

            if (deposited > 0){
                activeTransferred += deposited;
                waitTicks = TICKS_PER_TRANSFER;
                state = State.WAIT_TRANSFER_STEP;
                return;
            }

            // Matched a stack to deposit but the target(s) took none → target is
            // full. Report once per action, bypassing the daily gate so the player
            // always sees it, for every deposit type.
            if (!depositFullReported){
                depositFullReported = true;
                notifyOwnerAlways(courier.TEXT_CANNOT_DEPOSIT_TARGET_FULL(
                        waypointLabel(activeWp)));
            }
            finishTransfer();
            return;
        }

        // Loop exhausted with nothing deposited: the courier simply has none of the
        // requested item to deposit (item not found). Per design this is silent — no
        // owner notification.
        finishTransfer();
    }

    /** 20-tick pause between individual stack transfers. */
    private void tickWaitTransfer(){
        if (--waitTicks <= 0)
            state = State.TRANSFER_STEP;
    }

    private void tickWait(){
        if (--waitTicks <= 0){
            actionIndex++;
            state = State.EXECUTE_ACTIONS;
        }
    }

    private void finishTransfer(){
        closeOpenedChests();
        actionIndex++;
        state = State.EXECUTE_ACTIONS;
    }


    private int doPickupOneSlot(Container src, int slotIdx, ItemStack slot, int amount){
        Container workInv = getWorkingInventory();
        if (workInv != courier.getInventory()){
            int inserted = insertIntoContainer(workInv, slot, amount);
            if (inserted > 0){
                slot.shrink(inserted);
                src.setItem(slotIdx, slot.isEmpty() ? ItemStack.EMPTY : slot);
                src.setChanged();
                openChestFor(src);
            }
            return inserted;
        }
        ItemStack toAdd     = slot.copyWithCount(amount);
        if (!courier.canAddItem(toAdd)) return 0;
        ItemStack remainder = courier.addItem(toAdd);
        int added = amount - remainder.getCount();
        if (added > 0){
            slot.shrink(added);
            src.setItem(slotIdx, slot.isEmpty() ? ItemStack.EMPTY : slot);
            src.setChanged();
            openChestFor(src);
        }
        return added;
    }

    private boolean matchesTemplate(ItemStack stack){
        CourierAction.ActionType type = activeAction.getActionType();
        if (type == CourierAction.ActionType.TAKE_ALL || type == CourierAction.ActionType.PUT_ALL)
            return true;
        ItemStack tpl = activeAction.getItemStack();
        return tpl != null && !tpl.isEmpty() && ItemStack.isSameItem(stack, tpl);
    }

    private int getTransferLimit(){
        CourierAction.ActionType type = activeAction.getActionType();
        if (type == CourierAction.ActionType.TAKE || type == CourierAction.ActionType.PUT){
            ItemStack tpl = activeAction.getItemStack();
            return (tpl != null) ? tpl.getCount() : Integer.MAX_VALUE;
        }
        if (type == CourierAction.ActionType.PUT_FILL || type == CourierAction.ActionType.TAKE_FILL){
            return activeFillLimit;
        }
        return Integer.MAX_VALUE;
    }

    private static boolean isDepositType(CourierAction.ActionType type){
        return type == CourierAction.ActionType.PUT
                || type == CourierAction.ActionType.PUT_ANY
                || type == CourierAction.ActionType.PUT_ALL
                || type == CourierAction.ActionType.PUT_FILL;
    }

    private Container getWorkingInventory(){
        if (courier.useVehicleInventory){
            Entity vehicle = courier.getVehicle();
            if (vehicle instanceof AbstractChestedHorse horse)
                return horse.inventory;
            else if (vehicle instanceof InventoryCarrier carrier)
                return carrier.getInventory();
            else if (vehicle instanceof Container containerEntity)
                return containerEntity;
        }
        return courier.getInventory();
    }

    /**
     * Containers the action works on at this waypoint. Returns {@code null} when the action points to a
     * specific work area that no longer exists or is no longer accessible (the owner is informed once).
     */
    @Nullable
    private List<Container> getTargetContainers(CourierAction action, CourierRoute.CourierWaypoint wp){
        CourierAction.SourceType type = action.getSourceType();
        if (type == null) return new ArrayList<>();

        ServerLevel level = (ServerLevel) courier.getCommandSenderWorld();
        BlockPos center = wp.getPosition();
        boolean specific = action.hasTarget() && type != CourierAction.SourceType.CHEST;
        String key = type.name() + (specific ? ":" + action.getTargetId() : "");

        List<Container> cached = containerCache.get(key);
        if (cached != null) return cached;

        List<Container> result = new ArrayList<>();
        if (specific){
            AbstractWorkAreaEntity area = resolveTargetArea(courier, level, center, type, action.getTargetId());
            if (area == null){
                if (reportedMissingTargets.add(action.getTargetId())){
                    String name = action.getTargetName().isEmpty() ? type.name() : action.getTargetName();
                    notifyOwnerAlways(courier.TEXT_TARGET_AREA_MISSING(name, waypointLabel(wp)));
                }
                return null;
            }
            result.addAll(getTargetContainersOf(area));
        }
        else if (type == CourierAction.SourceType.CHEST){
            result.addAll(findChests(courier, level, center));
        }
        else {
            for (AbstractWorkAreaEntity area : findTargetAreas(courier, level, center, type)){
                for (Container c : getTargetContainersOf(area)){
                    if (!containsContainer(result, c)) result.add(c);
                }
            }
        }
        containerCache.put(key, result);
        return result;
    }

    private static AABB getScanBox(BlockPos center){
        return new AABB(center).inflate(SCAN_RADIUS);
    }

    /** All work areas of the given source type in range of the waypoint that the courier may use, nearest first. */
    public static List<AbstractWorkAreaEntity> findTargetAreas(CourierEntity courier, ServerLevel level, BlockPos center, CourierAction.SourceType type){
        List<AbstractWorkAreaEntity> result = new ArrayList<>();
        if (type == null || type == CourierAction.SourceType.CHEST) return result;

        for (AbstractWorkAreaEntity area : level.getEntitiesOfClass(AbstractWorkAreaEntity.class, getScanBox(center))){
            if (area.isAlive() && matchesSourceType(area, type) && isValidTarget(courier, area)) result.add(area);
        }
        result.sort(Comparator.comparingDouble(a -> a.blockPosition().distSqr(center)));
        return result;
    }

    @Nullable
    public static AbstractWorkAreaEntity resolveTargetArea(CourierEntity courier, ServerLevel level, BlockPos center, CourierAction.SourceType type, UUID targetId){
        if (targetId == null) return null;
        Entity entity = level.getEntity(targetId);
        if (!(entity instanceof AbstractWorkAreaEntity area) || !area.isAlive()) return null;
        if (!matchesSourceType(area, type)) return null;
        if (!area.getBoundingBox().intersects(getScanBox(center))) return null;
        if (!isValidTarget(courier, area)) return null;
        return area;
    }

    public static boolean matchesSourceType(AbstractWorkAreaEntity area, CourierAction.SourceType type){
        return switch (type){
            case STORAGE  -> area instanceof StorageArea;
            case MARKET   -> area instanceof MarketArea;
            case KITCHEN  -> area instanceof KitchenArea;
            case WORKSHOP -> area instanceof WorkshopArea;
            case CHEST    -> false;
        };
    }

    /** Storages additionally need the courier storage type enabled, like for every other worker. */
    public static boolean isValidTarget(CourierEntity courier, AbstractWorkAreaEntity area){
        if (area instanceof StorageArea storage) return storage.canWorkHere(courier);
        return canAccessArea(courier, area);
    }

    /** Owner or team access, independent of which worker type the area is made for. */
    public static boolean canAccessArea(CourierEntity courier, AbstractWorkAreaEntity area){
        if (!courier.isOwned() || courier.getOwnerUUID() == null) return false;
        boolean sameOwner = courier.getOwnerUUID().equals(area.getPlayerUUID());
        boolean sameTeam  = area.getTeamAccess() && courier.getTeam() != null
                && area.getTeamStringID() != null
                && !area.getTeamStringID().isEmpty()
                && area.getTeamStringID().equals(courier.getTeam().getName());
        return sameOwner || sameTeam;
    }

    /** Scans the area and returns the containers it links. The workshop is itself a container over all its chests. */
    public static List<Container> getTargetContainersOf(AbstractWorkAreaEntity area){
        if (area instanceof WorkshopArea workshop){
            workshop.scanArea();
            return List.of(workshop);
        }
        return new ArrayList<>(scanAreaContainers(area));
    }

    public static Collection<Container> scanAreaContainers(AbstractWorkAreaEntity area){
        if (area instanceof StorageArea storage){
            storage.scanStorageBlocks();
            return storage.storageMap.values();
        }
        if (area instanceof MarketArea market){
            market.scanContainers();
            return market.containerMap.values();
        }
        if (area instanceof KitchenArea kitchen){
            kitchen.scanArea();
            return kitchen.containerMap.values();
        }
        if (area instanceof WorkshopArea workshop){
            workshop.scanArea();
            return workshop.containerMap.values();
        }
        return List.of();
    }

    /**
     * Loose chests around the waypoint, nearest first. Chests belonging to a work area the courier may not
     * use are skipped, and so are market chests (market stock is only handled via the MARKET source).
     * Double chests are only listed once.
     */
    private static List<Container> findChests(CourierEntity courier, ServerLevel level, BlockPos center){
        List<Container> excluded = new ArrayList<>();
        for (AbstractWorkAreaEntity area : level.getEntitiesOfClass(AbstractWorkAreaEntity.class, getScanBox(center))){
            if (!(area instanceof MarketArea) && canAccessArea(courier, area)) continue;
            excluded.addAll(scanAreaContainers(area));
        }

        int minX = center.getX() - SCAN_RADIUS, maxX = center.getX() + SCAN_RADIUS;
        int minY = center.getY() - SCAN_RADIUS, maxY = center.getY() + SCAN_RADIUS;
        int minZ = center.getZ() - SCAN_RADIUS, maxZ = center.getZ() + SCAN_RADIUS;

        // Only look at block entities of loaded chunks instead of every block in the 33³ box.
        List<BlockPos> chestPositions = new ArrayList<>();
        for (int cx = minX >> 4; cx <= maxX >> 4; cx++){
            for (int cz = minZ >> 4; cz <= maxZ >> 4; cz++){
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) continue;
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()){
                    BlockPos pos = blockEntity.getBlockPos();
                    if (pos.getX() < minX || pos.getX() > maxX || pos.getY() < minY || pos.getY() > maxY || pos.getZ() < minZ || pos.getZ() > maxZ) continue;
                    if (blockEntity instanceof ChestBlockEntity) chestPositions.add(pos.immutable());
                }
            }
        }
        chestPositions.sort(Comparator.comparingDouble(pos -> pos.distSqr(center)));

        List<Container> result = new ArrayList<>();
        for (BlockPos pos : chestPositions){
            BlockState st = level.getBlockState(pos);
            if (!(st.getBlock() instanceof ChestBlock chestBlock)) continue;
            Container c = ChestBlock.getContainer(chestBlock, st, level, pos, true);
            if (c == null || containsContainer(excluded, c) || containsContainer(result, c)) continue;
            result.add(c);
        }
        return result;
    }

    private static boolean containsContainer(Collection<Container> containers, Container container){
        for (Container c : containers){
            if (AbstractWorkAreaEntity.isSameContainer(c, container)) return true;
        }
        return false;
    }

    private String waypointLabel(CourierRoute.CourierWaypoint wp){
        if (wp == null) return "";
        if (wp.displayName != null && !wp.displayName.isEmpty()) return wp.displayName;
        int index = courier.currentRoute != null ? courier.currentRoute.getWaypoints().indexOf(wp) : -1;
        return index >= 0 ? String.valueOf(index + 1) : "?";
    }

    private int insertIntoContainer(Container destination, ItemStack stack, int amount){
        int inserted = 0;
        for (int j = 0; j < destination.getContainerSize() && inserted < amount; j++){
            ItemStack d = destination.getItem(j);
            if (d.isEmpty() || !ItemStack.isSameItemSameTags(d, stack)) continue;
            int canAdd = d.getMaxStackSize() - d.getCount();
            if (canAdd <= 0) continue;
            int add = Math.min(canAdd, amount - inserted);
            d.grow(add); destination.setItem(j, d); destination.setChanged(); inserted += add;
        }
        for (int j = 0; j < destination.getContainerSize() && inserted < amount; j++){
            if (!destination.getItem(j).isEmpty()) continue;
            int place = Math.min(stack.getMaxStackSize(), amount - inserted);
            destination.setItem(j, stack.copyWithCount(place)); destination.setChanged(); inserted += place;
        }
        return inserted;
    }

    private void advanceAndNavigate(){
        courier.advanceWaypoint();
        actionIndex     = 0;
        navigationTicks = 0;
        containerCache.clear();
        state = State.NAVIGATE_TO_WAYPOINT;
    }

    private void resetRouteState(){
        closeOpenedChests();
        courier.getNavigation().stop();
        actionIndex       = 0;
        navigationTicks   = 0;
        waitTicks         = 0;
        activeTransferred = 0;
        containerCache.clear();
        reportedMissingTargets.clear();
        state = State.NAVIGATE_TO_WAYPOINT;
    }

    private void notifyOwner(Component msg){
        courier.notifyOwner(msg);
    }

    private void notifyOwnerAlways(Component msg){
        courier.notifyOwnerAlways(msg);
    }

    /**
     * Opens the destination/source chest visually + audibly the first time we
     * interact with it during the current action. Mirrors the pattern used by
     * {@code AbstractChestGoal.interactChest}; handles both single chests and
     * double chests (CompoundContainer).
     */
    private void openChestFor(Container container){
        if (container == null || openedContainers.contains(container)) return;
        ChestBlockEntity chest = resolveChest(container);
        if (chest == null) return;
        setChestOpenState(chest, true);
        openedContainers.add(container);
    }

    /** Closes every chest the courier opened during the current action. */
    private void closeOpenedChests(){
        if (openedContainers.isEmpty()) return;
        for (Container c : openedContainers){
            ChestBlockEntity chest = resolveChest(c);
            if (chest != null) setChestOpenState(chest, false);
        }
        openedContainers.clear();
    }

    private void setChestOpenState(ChestBlockEntity chest, boolean open){
        BlockPos pos = chest.getBlockPos();
        Level level = courier.getCommandSenderWorld();
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        if (!(block instanceof ChestBlock)) return;

        CompoundTag tag = chest.getPersistentData();
        boolean isOpened = tag.getBoolean("isOpened");

        if (open && !isOpened){
            level.blockEvent(pos, block, 1, 1);
            level.playSound(null, pos, SoundEvents.CHEST_OPEN, courier.getSoundSource(),
                    0.7F, 0.8F + 0.4F * courier.getRandom().nextFloat());
            tag.putBoolean("isOpened", true);
        }
        else if (!open && isOpened){
            level.blockEvent(pos, block, 1, 0);
            level.playSound(null, pos, SoundEvents.CHEST_CLOSE, courier.getSoundSource(),
                    0.7F, 0.8F + 0.4F * courier.getRandom().nextFloat());
            tag.putBoolean("isOpened", false);
        }
        level.gameEvent(courier, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
    }

    private ChestBlockEntity resolveChest(Container container){
        if (container instanceof ChestBlockEntity cbe) return cbe;
        if (container instanceof CompoundContainer cc){
            if (cc.container1 instanceof ChestBlockEntity cbe) return cbe;
            if (cc.container2 instanceof ChestBlockEntity cbe) return cbe;
        }
        return null;
    }

    public enum State{
        NAVIGATE_TO_WAYPOINT,
        EXECUTE_ACTIONS,
        WAIT_AT_WAYPOINT,
        TRANSFER_STEP,
        WAIT_TRANSFER_STEP
    }
}