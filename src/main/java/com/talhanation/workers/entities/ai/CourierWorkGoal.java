package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import com.talhanation.workers.entities.CourierEntity;
import com.talhanation.workers.entities.workarea.StorageArea;
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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class CourierWorkGoal extends Goal {

    private static final int ARRIVAL_DIST_SQ = 9;
    private static final int SCAN_RADIUS = 16;
    private static final int NAVIGATION_TIMEOUT = 2000;
    private static final int REPATH_INTERVAL = 20;
    private static final float WALK_SPEED = 0.85F;
    private final CourierEntity courier;
    private State state;
    private int   actionIndex;
    private int   waitTicks;
    private int   navigationTicks;

    public CourierWorkGoal(CourierEntity courier){
        this.courier = courier;
        setFlags(EnumSet.of(Flag.LOOK, Flag.MOVE));
    }

    @Override public boolean canUse(){
        return courier.isWorking() && courier.hasRoute() && !courier.needsToGetToChest();
    }
    @Override public boolean canContinueToUse(){
        return canUse();
    }
    @Override public boolean isInterruptable(){
        return true;
    }
    @Override public boolean requiresUpdateEveryTick(){
        return true;
    }

    @Override
    public void start(){
        actionIndex = 0;
        navigationTicks = 0;
        waitTicks = 0;
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

            case PICKUP ->{
                executePickup(action, wp);
                actionIndex++;
            }

            case DEPOSIT ->{
                executeDeposit(action, wp);
                actionIndex++;
            }

            case PICKUP_ANY ->{
                executePickupAny(action, wp);
                actionIndex++;
            }

            case DEPOSIT_ANY ->{
                executeDepositAny(action, wp);
                actionIndex++;
            }

            case PICKUP_ALL ->{
                executePickupAll(action, wp);
                actionIndex++;
            }

            case DEPOSIT_ALL -> {
                executeDepositAll(action, wp);
                actionIndex++;
            }
        }
    }

    private void tickWait(){
        if (--waitTicks <= 0){
            actionIndex++;
            state = State.EXECUTE_ACTIONS;
        }
    }

    private Container getWorkingInventory(){
        if (courier.useVehicleInventory){
            Entity vehicle = courier.getVehicle();
            if (vehicle instanceof AbstractChestedHorse horse){
                return horse.inventory;
            }
            else if (vehicle instanceof InventoryCarrier carrier){
                return carrier.getInventory();
            }
            else if (vehicle instanceof Container containerEntity){
                return containerEntity;
            }
        }
        return courier.getInventory();
    }

    private void executePickup(CourierAction action, CourierRoute.CourierWaypoint wp){
        if (action.getItemStack() == null || action.getItemStack().isEmpty()) return;

        ItemStack tpl = action.getItemStack();
        List<Container> targets = findContainers(action.getSourceType(), wp.getPosition());

        if (targets.isEmpty()){
            notifyOwner(courier.TEXT_NO_TARGET_FOUND(wp.displayName));
            return;
        }

        int total = 0, max = tpl.getCount();

        for (Container c : targets){
            if (total >= max) break;

            total += transferToWorkingInv(c, tpl, max - total);
        }

        if (total == 0)
            notifyOwner(courier.TEXT_SOURCE_EMPTY(wp.displayName, tpl.getHoverName().getString()));
    }

    private void executeDeposit(CourierAction action, CourierRoute.CourierWaypoint wp){
        if (action.getItemStack() == null || action.getItemStack().isEmpty()) return;

        ItemStack tpl = action.getItemStack();
        List<Container> targets = findContainers(action.getSourceType(), wp.getPosition());

        if (targets.isEmpty()){
            notifyOwner(courier.TEXT_NO_TARGET_FOUND(wp.displayName));
            return;
        }

        int total = 0, max = tpl.getCount();

        for (Container c : targets){
            if (total >= max) break;
            total += transferFromWorkingInv(c, tpl, max - total);
        }

        if (total == 0)
            notifyOwner(courier.TEXT_TARGET_FULL(wp.displayName, tpl.getHoverName().getString()));
    }

    private void executePickupAny(CourierAction action, CourierRoute.CourierWaypoint wp){
        if (action.getItemStack() == null || action.getItemStack().isEmpty()) return;

        ItemStack tpl = action.getItemStack();
        List<Container> targets = findContainers(action.getSourceType(), wp.getPosition());

        if (targets.isEmpty()){
            notifyOwner(courier.TEXT_NO_TARGET_FOUND(wp.displayName));
            return;
        }

        int total = 0;
        for (Container c : targets)
            total += transferToWorkingInv(c, tpl, Integer.MAX_VALUE);

        if (total == 0)
            notifyOwner(courier.TEXT_SOURCE_EMPTY(wp.displayName, tpl.getHoverName().getString()));
    }

    private void executeDepositAny(CourierAction action, CourierRoute.CourierWaypoint wp){
        if (action.getItemStack() == null || action.getItemStack().isEmpty()) return;
        ItemStack tpl = action.getItemStack();
        List<Container> targets = findContainers(action.getSourceType(), wp.getPosition());

        if (targets.isEmpty()){
            notifyOwner(courier.TEXT_NO_TARGET_FOUND(wp.displayName));
            return;
        }

        int total = 0;
        for (Container c : targets)
            total += transferFromWorkingInv(c, tpl, Integer.MAX_VALUE);

        if (total == 0)
            notifyOwner(courier.TEXT_TARGET_FULL(wp.displayName, tpl.getHoverName().getString()));
    }

    // ── Transfer logic — bulk / wildcard ─────────────────────────────────────

    private void executePickupAll(CourierAction action, CourierRoute.CourierWaypoint wp){
        List<Container> targets = findContainers(action.getSourceType(), wp.getPosition());
        if (targets.isEmpty()){ notifyOwner(courier.TEXT_NO_TARGET_FOUND(wp.displayName)); return; }
        for (Container src : targets){
            for (int i = 0; i < src.getContainerSize(); i++){
                ItemStack slot = src.getItem(i);
                if (slot.isEmpty()) continue;
                if (transferToWorkingInv(src, slot, Integer.MAX_VALUE) == 0) return; // full
            }
        }
    }

    private void executeDepositAll(CourierAction action, CourierRoute.CourierWaypoint wp){
        List<Container> targets = findContainers(action.getSourceType(), wp.getPosition());

        if (targets.isEmpty()){
            notifyOwner(courier.TEXT_NO_TARGET_FOUND(wp.displayName));
            return;
        }

        Container workInv = getWorkingInventory();

        int startSlot = (workInv == courier.getInventory()) ? 6 : 0;
        for (int i = startSlot; i < workInv.getContainerSize(); i++){
            ItemStack slot = workInv.getItem(i);
            if (slot.isEmpty()) continue;
            int deposited = 0;
            for (Container dest : targets){
                int ins = insertIntoContainer(dest, slot, slot.getCount() - deposited);
                deposited += ins;
                slot.shrink(ins);
                if (slot.isEmpty()) break;
                playChestSound(dest);
            }
            workInv.setItem(i, slot.isEmpty() ? ItemStack.EMPTY : slot);
        }
    }

    private int transferToWorkingInv(Container container, ItemStack template, int maxAmount){
        Container workInv = getWorkingInventory();
        if (workInv != courier.getInventory()){
            // Vehicle inventory — direct container-to-container
            return transferBetweenContainers(container, workInv, template, maxAmount);
        }
        // Courier's own inventory — use entity add methods so equipment / weight logic applies
        return transferFromContainerToCourier(container, template, maxAmount);
    }

    private int transferFromWorkingInv(Container destination, ItemStack template, int maxAmount){
        Container workInv = getWorkingInventory();
        int startSlot = (workInv == courier.getInventory()) ? 6 : 0;
        int transferred = 0;
        for (int i = startSlot; i < workInv.getContainerSize(); i++){
            ItemStack slot = workInv.getItem(i);

            if (slot.isEmpty() || !ItemStack.isSameItem(slot, template))
                continue;

            int toMove = maxAmount == Integer.MAX_VALUE ? slot.getCount() : Math.min(slot.getCount(), maxAmount - transferred);
            int inserted = insertIntoContainer(destination, slot, toMove);

            if (inserted == 0)
                break;

            slot.shrink(inserted);
            workInv.setItem(i, slot.isEmpty() ? ItemStack.EMPTY : slot);
            transferred += inserted;

            if (maxAmount != Integer.MAX_VALUE && transferred >= maxAmount)
                break;

            playChestSound(destination);
        }
        return transferred;
    }

    private int transferBetweenContainers(Container container, Container destination, ItemStack template, int maxAmount){
        int transferred = 0;
        for (int i = 0; i < container.getContainerSize(); i++){
            ItemStack slot = container.getItem(i);
            if (slot.isEmpty() || !ItemStack.isSameItem(slot, template)) continue;

            int take = maxAmount == Integer.MAX_VALUE ? slot.getCount() : Math.min(slot.getCount(), maxAmount - transferred);
            int inserted = insertIntoContainer(destination, slot, take);

            if (inserted == 0) break;

            slot.shrink(inserted);
            container.setItem(i, slot.isEmpty() ? ItemStack.EMPTY : slot);
            container.setChanged();
            transferred += inserted;
            playChestSound(container);

            if (maxAmount != Integer.MAX_VALUE && transferred >= maxAmount)
                break;
        }
        return transferred;
    }

    private int transferFromContainerToCourier(Container container, ItemStack template, int maxAmount){
        int transferred = 0;
        for (int i = 0; i < container.getContainerSize(); i++){
            ItemStack slot = container.getItem(i);
            if (slot.isEmpty() || !ItemStack.isSameItem(slot, template)) continue;

            int take = maxAmount == Integer.MAX_VALUE ? slot.getCount() : Math.min(slot.getCount(), maxAmount - transferred);
            ItemStack toAdd = slot.copyWithCount(take);

            if (!courier.canAddItem(toAdd)) break;

            ItemStack remainder = courier.addItem(toAdd);
            int added = take - remainder.getCount();
            if (added <= 0) break;

            slot.shrink(added);
            container.setItem(i, slot.isEmpty() ? ItemStack.EMPTY : slot);
            container.setChanged();
            transferred += added;
            playChestSound(container);

            if (maxAmount != Integer.MAX_VALUE && transferred >= maxAmount) break;
        }
        return transferred;
    }

    private List<Container> findContainers(CourierAction.SourceType type, BlockPos center){
        List<Container> result = new ArrayList<>();
        if (type == null || center == null) return result;

        AABB scanBox = new AABB(center).inflate(SCAN_RADIUS);
        ServerLevel level = (ServerLevel) courier.getCommandSenderWorld();

        switch (type){
            case CHEST -> BlockPos.betweenClosed(center.offset(-SCAN_RADIUS, -SCAN_RADIUS, -SCAN_RADIUS), center.offset( SCAN_RADIUS,  SCAN_RADIUS,  SCAN_RADIUS))
                    .forEach(pos -> {
                        BlockState st = level.getBlockState(pos);

                        if (st.getBlock() instanceof ChestBlock chestBlock){
                            Container c = ChestBlock.getContainer(chestBlock, st, level, pos, true);
                            if (c != null) result.add(c);
                        }
                    }
            );
            case STORAGE -> {
                List<StorageArea> areas = level.getEntitiesOfClass(StorageArea.class, scanBox, s -> s.canWorkHere(courier));

                for (StorageArea a : areas){
                    a.scanStorageBlocks();
                    result.addAll(a.storageMap.values());
                }
            }
            /*
            case WORKER -> level.getEntitiesOfClass(AbstractWorkerEntity.class, scanBox,
                    w -> w != courier && w.getOwnerUUID() != null && w.getOwnerUUID().equals(courier.getOwnerUUID()))
                    .forEach(w -> result.add(w.getInventory()));
            */
        }
        return result;
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
        actionIndex = 0;
        navigationTicks = 0;
        state = State.NAVIGATE_TO_WAYPOINT;
    }

    private void notifyOwner(Component msg){
        Player owner = courier.getOwner();
        if (owner != null) owner.sendSystemMessage(msg);
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
        WAIT_AT_WAYPOINT
    }
}
