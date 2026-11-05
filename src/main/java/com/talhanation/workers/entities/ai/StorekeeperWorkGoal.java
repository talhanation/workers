package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.CourierEntity;
import com.talhanation.workers.entities.StorekeeperEntity;
import com.talhanation.workers.entities.workarea.AbstractWorkAreaEntity;
import com.talhanation.workers.entities.workarea.StorageArea;
import com.talhanation.workers.world.StorageSorter;
import com.talhanation.workers.world.WorkAreaIndex;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

import javax.annotation.Nullable;
import java.util.*;

/**
 * The storekeeper belongs to one storage area. He unloads container entities that park in it for a few seconds
 * (Small Ships, chest boats, chest minecarts, donkeys/mules/llamas with chests, carts) and keeps the chests in order,
 * see {@link StorageSorter}. Without work he waits in the storage for the next arrival. The player sets nothing.
 */
public class StorekeeperWorkGoal extends Goal {

    private static final int SELECT_INTERVAL        = 40;
    private static final int VEHICLE_CHECK_INTERVAL = 20;
    private static final int SORT_INTERVAL          = 600;
    private static final int TRANSFER_TIME          = 10;
    private static final int PARKED_SCANS           = 3;
    private static final int MAX_MOVE_TICKS         = 300;
    private static final int VEHICLE_BLOCK_TIME     = 1200;
    private static final int FULL_RETRY_TIME        = 1200;
    private static final int UNREACHABLE_TIME       = 6000;
    private static final int VEHICLE_SCAN_INFLATE   = 8; // vehicles up to 8 blocks around the storage
    private static final int FIRST_INV_SLOT         = 6;
    private static final double VEHICLE_REACH       = 3.0D;
    private static final double CHEST_REACH         = 3.0D;
    /** Squared movement between two scans that still counts as standing still. */
    private static final double PARKED_MOVEMENT     = 0.01D;

    private final StorekeeperEntity storekeeper;
    private State state;
    private int cooldown;
    private int vehicleTimer;
    private int sortTimer;
    private int transferTimer;
    private boolean fullReported;
    private long putAwayBlockedUntil;

    @Nullable private BlockPos openContainerPos;
    @Nullable private BlockPos moveTarget;
    private int moveTicks;
    /** Source chest while sorting, target chest while putting away. */
    @Nullable private BlockPos chestPos;
    /** Chests he could not reach, left out until UNREACHABLE_TIME has passed. */
    private final Set<BlockPos> unreachableChests = new HashSet<>();
    private long unreachableUntil;

    // ── Vehicles ──────────────────────────────────────────────────────────────
    @Nullable private Entity vehicle;
    /** The entity of the vehicle that is in the area, for Small Ships one of the hull parts, otherwise the vehicle itself. */
    @Nullable private Entity vehicleAccess;
    @Nullable private Vec3 vehicleParkedPos;
    private final Map<UUID, Vec3> lastVehiclePos = new HashMap<>();
    private final Map<UUID, Integer> stillScans = new HashMap<>();
    private final Map<UUID, Long> blockedVehicles = new HashMap<>();

    public StorekeeperWorkGoal(StorekeeperEntity storekeeper) {
        this.storekeeper = storekeeper;
        setFlags(EnumSet.of(Flag.LOOK, Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return storekeeper.shouldWork() && !storekeeper.needsToGetToChest() && isAreaNotRemoved();
    }

    private boolean isAreaNotRemoved() {
        if (storekeeper.currentStorageArea == null || !storekeeper.currentStorageArea.isRemoved()) return true;
        storekeeper.currentStorageArea = null;
        return false;
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
        if (storekeeper.getFollowState() == 0) storekeeper.setFollowState(6);
        setState(State.SELECT_WORK_AREA);
    }

    @Override
    public void stop() {
        if (storekeeper.getCommandSenderWorld() instanceof ServerLevel level) {
            closeContainer(level);
        }
        // the storage stays his: eating, sleeping or following does not give it away
        leaveVehicle();
        chestPos = null;
        transferTimer = 0;
        storekeeper.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (storekeeper.getCommandSenderWorld().isClientSide()) return;
        if (state == null) return;
        ServerLevel level = (ServerLevel) storekeeper.getCommandSenderWorld();

        if (state != State.SELECT_WORK_AREA && !isCurrentAreaValid()) {
            closeContainer(level);
            leaveVehicle();
            storekeeper.currentStorageArea = null;
            setState(State.SELECT_WORK_AREA);
            return;
        }

        StorageArea area = storekeeper.currentStorageArea;
        switch (state) {
            case SELECT_WORK_AREA -> tickSelectWorkArea(level);
            case WALK_TO_CENTER   -> tickWalkToCenter(area);
            case IDLE             -> tickIdle(level, area);
            case MOVE_TO_VEHICLE  -> tickMoveToVehicle(level, area);
            case UNLOAD           -> tickUnload(level, area);
            case SORT_TAKE        -> tickSortTake(level, area);
            case PUT_AWAY         -> tickPutAway(level, area);
        }
    }

    // ── States ────────────────────────────────────────────────────────────────

    private void tickSelectWorkArea(ServerLevel level) {
        if (storekeeper.currentStorageArea != null) {
            setState(State.WALK_TO_CENTER);
            return;
        }
        if (++cooldown < SELECT_INTERVAL) return;
        cooldown = 0;

        if (storekeeper.storageUUID != null) {
            StorageArea bound = WorkAreaIndex.get(level).get(storekeeper.storageUUID, StorageArea.class);
            if (bound != null && bound.canWorkHere(storekeeper) && isFreeFor(level, bound)) {
                claim(bound);
                return;
            }
            if (bound == null && storekeeper.storagePos != null && storekeeper.getHorizontalDistanceTo(storekeeper.storagePos.getCenter()) > 16) {
                // his storage is not loaded: go back to it before giving it up
                if (moveToPosition(storekeeper.storagePos, 3)) return;
            }
            // the storage is gone, no longer allowed or another storekeeper works there now
            if (bound != null && storekeeper.getUUID().equals(bound.getStorekeeperUUID())) bound.setStorekeeperUUID(null);
            storekeeper.storageUUID = null;
            storekeeper.storagePos = null;
        }

        StorageArea found = findFreeArea(level);
        if (found == null) {
            storekeeper.notifyOwner(Component.translatable("chat.workers.text.storekeeperNoStorage", storekeeper.getName().getString()));
            return;
        }
        claim(found);
    }

    private void tickWalkToCenter(StorageArea area) {
        if (moveToPosition(getCenter(area), 3)) return;
        storekeeper.getNavigation().stop();
        setState(State.IDLE);
    }

    private void tickIdle(ServerLevel level, StorageArea area) {
        if (hasCarriedItems() && level.getGameTime() >= putAwayBlockedUntil) {
            startPutAway(level, area);
            return;
        }

        if (++vehicleTimer >= VEHICLE_CHECK_INTERVAL) {
            vehicleTimer = 0;
            if (findParkedVehicle(level, area) && storekeeper.hasFreeInvSlot()) {
                setState(State.MOVE_TO_VEHICLE);
                return;
            }
        }

        if (++sortTimer >= SORT_INTERVAL) {
            sortTimer = 0;
            if (startSort(level, area)) return;
        }

        // nothing to do: wait in the storage for the next arrival
        BlockPos center = getCenter(area);
        if (storekeeper.getHorizontalDistanceTo(center.getCenter()) > 4) moveToPosition(center, 3);
        else storekeeper.getNavigation().stop();
    }

    private void tickMoveToVehicle(ServerLevel level, StorageArea area) {
        if (!isVehicleStillParked(area)) {
            leaveVehicle();
            setState(State.IDLE);
            return;
        }
        if (getVehicleDistance() <= VEHICLE_REACH) {
            storekeeper.getNavigation().stop();
            transferTimer = 0;
            setState(State.UNLOAD);
            return;
        }
        if (++moveTicks > MAX_MOVE_TICKS) {
            blockedVehicles.put(vehicle.getUUID(), level.getGameTime() + VEHICLE_BLOCK_TIME);
            storekeeper.notifyOwner(Component.translatable("chat.workers.text.storekeeperVehicleUnreachable",
                    storekeeper.getName().getString(), vehicle.getName().getString()));
            leaveVehicle();
            setState(State.IDLE);
            return;
        }
        if (moveTicks % 10 == 1) {
            Vec3 target = getClosestPoint(vehicleAccess.getBoundingBox(), storekeeper.position());
            storekeeper.getNavigation().moveTo(target.x, target.y, target.z, 0.8F);
        }
        storekeeper.getLookControl().setLookAt(vehicleAccess);
    }

    private void tickUnload(ServerLevel level, StorageArea area) {
        if (!isVehicleStillParked(area)) {
            leaveVehicle();
            finishVehicle(level, area);
            return;
        }
        if (getVehicleDistance() > VEHICLE_REACH + 1.0D) {
            setState(State.MOVE_TO_VEHICLE);
            return;
        }
        storekeeper.getNavigation().stop();
        storekeeper.getLookControl().setLookAt(vehicleAccess);
        if (++transferTimer < TRANSFER_TIME) return;
        transferTimer = 0;

        Container cargo = getCargo(vehicle);
        if (cargo != null) {
            for (int i = getFirstCargoSlot(vehicle); i < cargo.getContainerSize(); i++) {
                ItemStack stack = cargo.getItem(i);
                if (stack.isEmpty()) continue;

                ItemStack rest = storekeeper.addItem(stack.copy());
                if (rest.getCount() == stack.getCount()) {
                    // hands full: put away and come back, the vehicle is found again while it stays
                    startPutAway(level, area);
                    return;
                }
                cargo.setItem(i, rest);
                cargo.setChanged();
                storekeeper.swing(InteractionHand.MAIN_HAND);
                return;
            }
        }
        // the vehicle is empty
        leaveVehicle();
        finishVehicle(level, area);
    }

    private void tickSortTake(ServerLevel level, StorageArea area) {
        Container container = chestPos == null ? null : area.storageMap.get(chestPos);
        if (container == null) {
            finishTake(level, area);
            return;
        }
        if (moveToPosition(chestPos, 3)) return;
        if (!isNear(chestPos)) {
            markUnreachable(level, chestPos);
            finishTake(level, area);
            return;
        }
        openContainer(level, chestPos);
        storekeeper.getLookControl().setLookAt(chestPos.getCenter());
        if (++transferTimer < TRANSFER_TIME) return;
        transferTimer = 0;

        if (storekeeper.hasFreeInvSlot()) {
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack stack = container.getItem(i);
                if (!StorageSorter.canMove(area, chestPos, stack, unreachableChests)) continue;

                ItemStack rest = storekeeper.addItem(stack.copy());
                if (rest.getCount() == stack.getCount()) break;
                container.setItem(i, rest);
                container.setChanged();
                storekeeper.swing(InteractionHand.MAIN_HAND);
                return;
            }
        }
        StorageSorter.compact(container);
        finishTake(level, area);
    }

    private void tickPutAway(ServerLevel level, StorageArea area) {
        int slot = getNextCarriedSlot();
        if (slot < 0) {
            finishPutAway(level);
            return;
        }
        ItemStack stack = storekeeper.getInventory().getItem(slot);

        if (chestPos == null) {
            chestPos = StorageSorter.findTarget(area, stack, storekeeper.position(), null, unreachableChests);
            if (chestPos == null) chestPos = StorageSorter.findAnySpace(area, stack, storekeeper.position(), unreachableChests);
            if (chestPos == null) {
                if (!fullReported) {
                    storekeeper.notifyOwner(Component.translatable("chat.workers.text.storekeeperStorageFull", storekeeper.getName().getString()));
                    fullReported = true;
                }
                putAwayBlockedUntil = level.getGameTime() + FULL_RETRY_TIME;
                finishPutAway(level);
                return;
            }
            if (!chestPos.equals(openContainerPos)) closeContainer(level);
        }

        Container container = area.storageMap.get(chestPos);
        if (container == null) {
            chestPos = null;
            return;
        }
        if (moveToPosition(chestPos, 3)) return;
        if (!isNear(chestPos)) {
            markUnreachable(level, chestPos);
            chestPos = null;
            return;
        }
        openContainer(level, chestPos);
        storekeeper.getLookControl().setLookAt(chestPos.getCenter());
        if (++transferTimer < TRANSFER_TIME) return;
        transferTimer = 0;

        ItemStack rest = StorageSorter.insert(container, stack);
        if (rest.getCount() < stack.getCount()) {
            storekeeper.swing(InteractionHand.MAIN_HAND);
            fullReported = false;
        }
        storekeeper.getInventory().setItem(slot, rest);
        // the next stack looks for its own chest, the open chest stays open if it is the same
        chestPos = null;
    }

    // ── Starting and finishing ────────────────────────────────────────────────

    private void claim(StorageArea area) {
        area.setStorekeeperUUID(storekeeper.getUUID());
        storekeeper.currentStorageArea = area;
        storekeeper.storageUUID = area.getUUID();
        storekeeper.storagePos = area.getOnPos();
        unreachableChests.clear();
        sortTimer = SORT_INTERVAL;
        setState(State.WALK_TO_CENTER);
    }

    /** Plans the order and looks for a chest with stacks that are in the wrong place. */
    private boolean startSort(ServerLevel level, StorageArea area) {
        if (unreachableUntil != 0 && level.getGameTime() > unreachableUntil) {
            unreachableChests.clear();
            unreachableUntil = 0;
        }
        area.scanStorageBlocks();
        StorageSorter.plan(area, getCarriedStacks());
        BlockPos source = StorageSorter.findSource(area, storekeeper.position(), unreachableChests);
        if (source == null) return false;

        chestPos = source;
        transferTimer = 0;
        setState(State.SORT_TAKE);
        return true;
    }

    private void finishTake(ServerLevel level, StorageArea area) {
        closeContainer(level);
        chestPos = null;
        if (hasCarriedItems()) startPutAway(level, area);
        else setState(State.IDLE);
    }

    private void startPutAway(ServerLevel level, StorageArea area) {
        area.scanStorageBlocks();
        StorageSorter.plan(area, getCarriedStacks());
        chestPos = null;
        transferTimer = 0;
        setState(State.PUT_AWAY);
    }

    private void finishPutAway(ServerLevel level) {
        closeContainer(level);
        chestPos = null;
        // look for the vehicle again right away and keep on sorting soon
        vehicleTimer = VEHICLE_CHECK_INTERVAL;
        sortTimer = SORT_INTERVAL - 20;
        setState(State.IDLE);
    }

    private void finishVehicle(ServerLevel level, StorageArea area) {
        if (hasCarriedItems()) startPutAway(level, area);
        else setState(State.IDLE);
    }

    // ── Storage helpers ───────────────────────────────────────────────────────

    private boolean isCurrentAreaValid() {
        StorageArea area = storekeeper.currentStorageArea;
        return area != null && !area.isRemoved() && area.canWorkHere(storekeeper) && storekeeper.getUUID().equals(area.getStorekeeperUUID());
    }

    /** A storage is free if it has no storekeeper or its storekeeper is not around any more. */
    private boolean isFreeFor(ServerLevel level, StorageArea area) {
        UUID keeper = area.getStorekeeperUUID();
        if (keeper == null || keeper.equals(storekeeper.getUUID())) return true;
        Entity entity = level.getEntity(keeper);
        return !(entity instanceof StorekeeperEntity other && other.isAlive() && area.getUUID().equals(other.storageUUID));
    }

    @Nullable
    private StorageArea findFreeArea(ServerLevel level) {
        StorageArea best = null;
        for (StorageArea area : storekeeper.findWorkAreas(StorageArea.class, 64)) {
            if (area == null || !area.canWorkHere(storekeeper) || !isFreeFor(level, area)) continue;
            if (best == null || area.distanceToSqr(storekeeper) < best.distanceToSqr(storekeeper)) best = area;
        }
        return best;
    }

    private static BlockPos getCenter(StorageArea area) {
        return BlockPos.containing(area.getArea().getCenter());
    }

    /** The first slot with an item to put away. One stack of good food is kept to eat. */
    private int getNextCarriedSlot() {
        SimpleContainer inventory = storekeeper.getInventory();
        int food = getKeptFoodSlot();
        for (int i = FIRST_INV_SLOT; i < inventory.getContainerSize(); i++) {
            if (i != food && !inventory.getItem(i).isEmpty()) return i;
        }
        return -1;
    }

    private int getKeptFoodSlot() {
        SimpleContainer inventory = storekeeper.getInventory();
        for (int i = FIRST_INV_SLOT; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && storekeeper.wantsToKeep(stack)) return i;
        }
        return -1;
    }

    private boolean hasCarriedItems() {
        return getNextCarriedSlot() >= 0;
    }

    private List<ItemStack> getCarriedStacks() {
        List<ItemStack> list = new ArrayList<>();
        SimpleContainer inventory = storekeeper.getInventory();
        int food = getKeptFoodSlot();
        for (int i = FIRST_INV_SLOT; i < inventory.getContainerSize(); i++) {
            if (i != food && !inventory.getItem(i).isEmpty()) list.add(inventory.getItem(i));
        }
        return list;
    }

    private boolean isNear(BlockPos pos) {
        return storekeeper.getHorizontalDistanceTo(pos.getCenter()) < CHEST_REACH + 1.0D
                && Math.abs(storekeeper.getY() - pos.getY()) < 3.0D;
    }

    private void markUnreachable(ServerLevel level, BlockPos pos) {
        unreachableChests.add(pos);
        if (unreachableUntil == 0) unreachableUntil = level.getGameTime() + UNREACHABLE_TIME;
    }

    // ── Vehicle helpers ───────────────────────────────────────────────────────

    /** Scans the area for container entities. One that stood still for PARKED_SCANS scans and has cargo becomes the vehicle. */
    private boolean findParkedVehicle(ServerLevel level, StorageArea area) {
        AABB box = area.getArea().inflate(VEHICLE_SCAN_INFLATE);
        long time = level.getGameTime();
        blockedVehicles.values().removeIf(until -> until < time);

        // vehicle -> {vehicle, the entity of it that is in the area}
        Map<UUID, Entity[]> found = new LinkedHashMap<>();
        for (Entity entity : level.getEntities((Entity) null, box, this::isVehicleCandidate)) {
            Entity root = getVehicleOf(level, entity);
            if (root != entity && !isVehicleCandidate(root)) continue;
            Entity[] known = found.get(root.getUUID());
            if (known == null || getDistance(entity.getBoundingBox()) < getDistance(known[1].getBoundingBox())) {
                found.put(root.getUUID(), new Entity[]{root, entity});
            }
        }
        lastVehiclePos.keySet().retainAll(found.keySet());
        stillScans.keySet().retainAll(found.keySet());

        Entity best = null;
        Entity bestAccess = null;
        double bestDistance = Double.MAX_VALUE;
        for (Entity[] pair : found.values()) {
            Entity root = pair[0];
            UUID id = root.getUUID();
            Vec3 last = lastVehiclePos.put(id, root.position());
            int still = last != null && last.distanceToSqr(root.position()) < PARKED_MOVEMENT ? stillScans.getOrDefault(id, 0) + 1 : 0;
            stillScans.put(id, still);

            if (still < PARKED_SCANS || blockedVehicles.containsKey(id) || !isUnloadable(root) || !hasCargo(root)) continue;
            double distance = getDistance(pair[1].getBoundingBox());
            if (distance < bestDistance) {
                best = root;
                bestAccess = pair[1];
                bestDistance = distance;
            }
        }
        if (best == null) return false;

        vehicle = best;
        vehicleAccess = bestAccess;
        vehicleParkedPos = best.position();
        return true;
    }

    private boolean isVehicleCandidate(Entity entity) {
        if (!entity.isAlive() || entity instanceof Player || entity instanceof AbstractWorkAreaEntity) return false;
        if (entity instanceof LivingEntity) return entity instanceof AbstractChestedHorse horse && horse.hasChest();
        return entity instanceof Container || entity.getCapability(Capabilities.ItemHandler.ENTITY, null) != null;
    }

    /**
     * Small Ships finds its ships by their hull parts, they hand every container call to the hold and answer
     * {@code is(ship)}. The ship itself is the vehicle, so a ship counts once and its own position decides if it is parked.
     */
    private static Entity getVehicleOf(ServerLevel level, Entity entity) {
        if (entity instanceof ContainerEntity || entity instanceof LivingEntity) return entity;
        List<Entity> owners = level.getEntities(entity, entity.getBoundingBox().inflate(16), entity::is);
        return owners.isEmpty() ? entity : owners.get(0);
    }

    /** Nobody uses it right now: no player and no courier aboard, no tamed animal of someone else. */
    private boolean isUnloadable(Entity entity) {
        for (Entity passenger : entity.getPassengers()) {
            if (passenger instanceof Player || passenger instanceof CourierEntity) return false;
        }
        if (entity instanceof AbstractChestedHorse horse && horse.getOwnerUUID() != null && !horse.getOwnerUUID().equals(storekeeper.getOwnerUUID())) {
            return false;
        }
        return getCargo(entity) != null;
    }

    private boolean isVehicleStillParked(StorageArea area) {
        if (vehicle == null || !vehicle.isAlive() || vehicleParkedPos == null) return false;
        if (vehicleAccess == null || !vehicleAccess.isAlive()) vehicleAccess = vehicle;
        if (vehicle.position().distanceToSqr(vehicleParkedPos) > 1.0D) return false;
        if (!vehicleAccess.getBoundingBox().intersects(area.getArea().inflate(VEHICLE_SCAN_INFLATE))) return false;
        return isUnloadable(vehicle);
    }

    private double getVehicleDistance() {
        if (vehicle == null || vehicleAccess == null) return Double.MAX_VALUE;
        return Math.min(getDistance(vehicleAccess.getBoundingBox()), getDistance(vehicle.getBoundingBox()));
    }

    private void leaveVehicle() {
        vehicle = null;
        vehicleAccess = null;
        vehicleParkedPos = null;
    }

    @Nullable
    private static Container getCargo(Entity entity) {
        if (entity instanceof AbstractChestedHorse horse) return horse.hasChest() ? horse.inventory : null;
        if (entity instanceof Container container) return container.getContainerSize() > 0 ? container : null;
        IItemHandler handler = entity.getCapability(Capabilities.ItemHandler.ENTITY, null);
        if (handler instanceof IItemHandlerModifiable modifiable && modifiable.getSlots() > 0) return new ItemHandlerContainer(modifiable);
        return null;
    }

    /** Saddle and armor/carpet of a chested animal stay where they are. */
    private static int getFirstCargoSlot(Entity entity) {
        return entity instanceof AbstractChestedHorse ? 2 : 0;
    }

    private static boolean hasCargo(Entity entity) {
        Container cargo = getCargo(entity);
        if (cargo == null) return false;
        for (int i = getFirstCargoSlot(entity); i < cargo.getContainerSize(); i++) {
            if (!cargo.getItem(i).isEmpty()) return true;
        }
        return false;
    }

    /** Distance from the storekeeper to the nearest point of the box. */
    private double getDistance(AABB box) {
        return getClosestPoint(box, storekeeper.position()).distanceTo(storekeeper.position());
    }

    private static Vec3 getClosestPoint(AABB box, Vec3 pos) {
        return new Vec3(Math.max(box.minX, Math.min(pos.x, box.maxX)),
                Math.max(box.minY, Math.min(pos.y, box.maxY)),
                Math.max(box.minZ, Math.min(pos.z, box.maxZ)));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void openContainer(ServerLevel level, BlockPos pos) {
        if (pos.equals(openContainerPos)) return;
        closeContainer(level);
        interactContainer(level, pos, true);
        openContainerPos = pos;
    }

    private void closeContainer(ServerLevel level) {
        if (openContainerPos != null) {
            interactContainer(level, openContainerPos, false);
            openContainerPos = null;
        }
    }

    // Opens/closes a chest with the vanilla animation + sound. No-op for non-chest containers.
    private void interactContainer(ServerLevel level, @Nullable BlockPos pos, boolean open) {
        if (pos == null) return;
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof ChestBlockEntity)) return;

        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        CompoundTag data = be.getPersistentData();
        boolean isOpened = data.getBoolean("isOpened");

        if (open && !isOpened) {
            level.blockEvent(pos, block, 1, 1);
            level.playSound(null, pos, SoundEvents.CHEST_OPEN, storekeeper.getSoundSource(), 0.7F, 0.8F + 0.4F * storekeeper.getRandom().nextFloat());
            data.putBoolean("isOpened", true);
        }
        else if (!open && isOpened) {
            level.blockEvent(pos, block, 1, 0);
            level.playSound(null, pos, SoundEvents.CHEST_CLOSE, storekeeper.getSoundSource(), 0.7F, 0.8F + 0.4F * storekeeper.getRandom().nextFloat());
            data.putBoolean("isOpened", false);
        }
    }

    /** Walks to the position. Gives up after MAX_MOVE_TICKS, the caller checks if he really got there. */
    private boolean moveToPosition(BlockPos pos, int thresholdBlocks) {
        if (!pos.equals(moveTarget)) {
            moveTarget = pos;
            moveTicks = 0;
        }
        double dist = storekeeper.getHorizontalDistanceTo(pos.getCenter());
        if (dist < thresholdBlocks) {
            moveTicks = 0;
            storekeeper.getNavigation().stop();
            return false;
        }
        if (++moveTicks > MAX_MOVE_TICKS) {
            storekeeper.getNavigation().stop();
            return false;
        }
        // Note: setFollowState is NOT called here — start() already set it to 6.
        storekeeper.getNavigation().moveTo(pos.getX(), pos.getY(), pos.getZ(), 0.8F);
        storekeeper.getLookControl().setLookAt(pos.getCenter());
        return true;
    }

    private void setState(State s) {
        this.state = s;
        this.moveTarget = null;
        this.moveTicks = 0;
    }

    public enum State {
        SELECT_WORK_AREA,
        WALK_TO_CENTER,
        IDLE,
        MOVE_TO_VEHICLE,
        UNLOAD,
        SORT_TAKE,
        PUT_AWAY
    }

    /**
     * 1.21: NeoForge's RecipeWrapper is a RecipeInput now and no Container anymore.
     * This is the Container part of the former Forge 1.20.1 RecipeWrapper.
     */
    private static class ItemHandlerContainer implements Container {
        protected final IItemHandlerModifiable inv;

        public ItemHandlerContainer(IItemHandlerModifiable inv) {
            this.inv = inv;
        }

        @Override
        public int getContainerSize() {
            return inv.getSlots();
        }

        @Override
        public ItemStack getItem(int slot) {
            return inv.getStackInSlot(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int count) {
            ItemStack stack = inv.getStackInSlot(slot);
            return stack.isEmpty() ? ItemStack.EMPTY : stack.split(count);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            inv.setStackInSlot(slot, stack);
        }

        @Override
        public ItemStack removeItemNoUpdate(int index) {
            ItemStack s = getItem(index);
            if (s.isEmpty()) return ItemStack.EMPTY;
            setItem(index, ItemStack.EMPTY);
            return s;
        }

        @Override
        public boolean isEmpty() {
            for (int i = 0; i < inv.getSlots(); i++) {
                if (!inv.getStackInSlot(i).isEmpty()) return false;
            }
            return true;
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return inv.isItemValid(slot, stack);
        }

        @Override
        public void clearContent() {
            for (int i = 0; i < inv.getSlots(); i++) {
                inv.setStackInSlot(i, ItemStack.EMPTY);
            }
        }

        @Override
        public int getMaxStackSize() { return 0; }
        @Override
        public void setChanged() {}
        @Override
        public boolean stillValid(Player player) { return false; }
        @Override
        public void startOpen(Player player) {}
        @Override
        public void stopOpen(Player player) {}
    }
}
