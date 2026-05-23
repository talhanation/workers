package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.CourierEntity;
import com.talhanation.workers.entities.workarea.KitchenArea;
import com.talhanation.workers.entities.workarea.StorageArea;
import com.talhanation.workers.entities.workarea.MarketArea;
import com.talhanation.workers.world.CourierAction;
import com.talhanation.workers.world.CourierRoute;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.*;

public class CourierWorkGoal extends Goal {

    private static final int ARRIVAL_DIST_SQ    = 9;
    private static final int SCAN_RADIUS         = 16;
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

    // ── Active transfer state ─────────────────────────────────────────────────
    private CourierAction               activeAction;
    private CourierRoute.CourierWaypoint activeWp;
    private List<Container>             activeTargets;
    private int                         activeContainerIdx; // PICKUP: which source container
    private int                         activeSlotIdx;      // PICKUP: slot in source / DEPOSIT: slot in workInv
    private int                         activeTransferred;  // total items moved so far
    private int                         activeFillLimit;    // PUT_FILL/TAKE_FILL: pre-computed missing amount (desired count − already present)

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
        courier.setFollowState(6); //Working
        state = State.NAVIGATE_TO_WAYPOINT;
    }

    @Override
    public void stop(){
        courier.getNavigation().stop();
    }

    @Override
    public void tick(){
        if (courier.getCommandSenderWorld().isClientSide()) return;
        if (state == null) return;
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
            advanceAndNavigate();
            return;
        }

        courier.setFollowState(6);
        courier.getLookControl().setLookAt(target.getX() + 0.5, courier.getY() + 0.5, target.getZ() + 0.5);
    }

    private void tickExecute(){
        CourierRoute.CourierWaypoint wp = courier.getCurrentWaypoint();
        if(courier.returning && courier.currentWaypointIndex == 0){
            courier.returning = false;
            courier.shouldCycle = courier.pendingShouldCycle;

            // Safe stop point: full route cycle complete, night is pending → let GoHomeGoal take over
            if (pendingGoHome) {
                pendingGoHome = false;
                return; // canContinueToUse will now return false → goal stops
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
        List<Container> targets = findContainers(action.getSourceType(), wp.getPosition());
        if (targets.isEmpty()){
            notifyOwner(courier.TEXT_NO_TARGET_FOUND(wp.displayName));
            actionIndex++;
            return;
        }

        activeAction       = action;
        activeWp           = wp;
        activeTargets      = targets;
        activeTransferred  = 0;
        activeContainerIdx = 0;
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
                deposited += ins;
                slot.shrink(ins);
                if (slot.isEmpty()) break;
                if (ins > 0) playChestSound(dest);
            }

            workInv.setItem(sIdx, slot.isEmpty() ? ItemStack.EMPTY : slot);

            if (deposited > 0){
                activeTransferred += deposited;
                waitTicks = TICKS_PER_TRANSFER;
                state = State.WAIT_TRANSFER_STEP;
                return;
            }

            CourierAction.ActionType type = activeAction.getActionType();
            if ((type == CourierAction.ActionType.PUT || type == CourierAction.ActionType.PUT_ANY) && activeTransferred == 0){
                notifyOwner(courier.TEXT_TARGET_FULL(
                        activeWp.displayName, slot.getHoverName().getString()));
            }
            finishTransfer();
            return;
        }

        CourierAction.ActionType type = activeAction.getActionType();
        if ((type == CourierAction.ActionType.PUT || type == CourierAction.ActionType.PUT_ANY)
                && activeTransferred == 0){
            ItemStack tpl = activeAction.getItemStack();
            if (tpl != null && !tpl.isEmpty())
                notifyOwner(courier.TEXT_TARGET_FULL(activeWp.displayName, tpl.getHoverName().getString()));
        }
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
                playChestSound(src);
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
            playChestSound(src);
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

    private List<Container> findContainers(CourierAction.SourceType type, BlockPos center){
        List<Container> result = new ArrayList<>();
        if (type == null || center == null) return result;

        AABB scanBox = new AABB(center).inflate(SCAN_RADIUS);
        ServerLevel level = (ServerLevel) courier.getCommandSenderWorld();

        switch (type){
            case CHEST -> {
                List<MarketArea> blockedMarkets = level.getEntitiesOfClass(
                        MarketArea.class, scanBox, m -> !canAccessMarket(m));
                List<MarketArea> allowedMarkets = level.getEntitiesOfClass(
                        MarketArea.class, scanBox, this::canAccessMarket);

                Set<BlockPos> blockedPositions = new HashSet<>();
                for (MarketArea bm : blockedMarkets){
                    bm.scanContainers();
                    blockedPositions.addAll(bm.containerMap.keySet());
                }

                Set<BlockPos> allowedMarketPositions = new java.util.HashSet<>();
                for (MarketArea am : allowedMarkets){
                    am.scanContainers();
                    allowedMarketPositions.addAll(am.containerMap.keySet());
                }

                BlockPos.betweenClosed(
                        center.offset(-SCAN_RADIUS, -SCAN_RADIUS, -SCAN_RADIUS),
                        center.offset( SCAN_RADIUS,  SCAN_RADIUS,  SCAN_RADIUS)
                ).forEach(pos -> {
                    if (blockedPositions.contains(pos.immutable())) return;
                    if (allowedMarketPositions.contains(pos.immutable())) return;

                    BlockState st = level.getBlockState(pos);
                    if (st.getBlock() instanceof ChestBlock chestBlock){
                        Container c = ChestBlock.getContainer(chestBlock, st, level, pos, true);
                        if (c != null) result.add(c);
                    }
                });
            }
            case STORAGE -> {
                List<StorageArea> areas = level.getEntitiesOfClass(StorageArea.class, scanBox, s -> s.canWorkHere(courier));
                for (StorageArea a : areas){
                    a.scanStorageBlocks();
                    result.addAll(a.storageMap.values());
                }
            }
            case MARKET -> {
                List<MarketArea> markets = level.getEntitiesOfClass(
                        MarketArea.class, scanBox, this::canAccessMarket);
                for (MarketArea m : markets){
                    m.scanContainers();
                    result.addAll(m.containerMap.values());
                }
            }

            case KITCHEN -> {
                List<KitchenArea> kitchens = level.getEntitiesOfClass(
                        KitchenArea.class, scanBox, this::canAccessKitchen);
                for (KitchenArea k : kitchens){
                    k.scanArea();
                    result.addAll(k.containerMap.values());
                }
            }

        }
        return result;
    }
    private boolean canAccessKitchen(KitchenArea kitchen){
        if (!courier.isOwned()) return false;
        boolean sameOwner = courier.getOwnerUUID().equals(kitchen.getPlayerUUID());
        boolean sameTeam  = kitchen.getTeamAccess() && courier.getTeam() != null
                && kitchen.getTeamStringID() != null
                && !kitchen.getTeamStringID().isEmpty()
                && kitchen.getTeamStringID().equals(courier.getTeam().getName());
        return sameOwner || sameTeam;
    }

    private boolean canAccessMarket(MarketArea market){
        if (!courier.isOwned()) return false;
        boolean sameOwner = courier.getOwnerUUID().equals(market.getPlayerUUID());
        boolean sameTeam  = market.getTeamAccess() && courier.getTeam() != null
                && market.getTeamStringID() != null
                && !market.getTeamStringID().isEmpty()
                && market.getTeamStringID().equals(courier.getTeam().getName());
        return sameOwner || sameTeam;
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
        state = State.NAVIGATE_TO_WAYPOINT;
    }

    private void notifyOwner(Component msg){
        courier.notifyOwner(msg);
    }

    private void playChestSound(Container container){
        if (container instanceof ChestBlockEntity chestBlockEntity){
            courier.getCommandSenderWorld().playSound(null, chestBlockEntity.getBlockPos(),
                    SoundEvents.CHEST_OPEN, courier.getSoundSource(),
                    0.4F, 0.9F + courier.getRandom().nextFloat() * 0.2F);
        }
    }

    public enum State{
        NAVIGATE_TO_WAYPOINT,
        EXECUTE_ACTIONS,
        WAIT_AT_WAYPOINT,
        TRANSFER_STEP,
        WAIT_TRANSFER_STEP
    }
}