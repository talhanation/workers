package com.talhanation.workers.entities.workarea;

import com.talhanation.workers.WorkersMain;
import com.talhanation.workers.client.gui.WorkshopAreaScreen;
import com.talhanation.workers.entities.AbstractWorkerEntity;
import com.talhanation.workers.entities.CraftsmanEntity;
import com.talhanation.workers.inventory.WorkshopEditOrderContainer;
import com.talhanation.workers.network.MessageOpenWorkshopEditOrderScreen;
import com.talhanation.workers.world.CraftOrder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;

/**
 * Work area of the craftsman. Needs at least one crafting table, one furnace and four containers.
 * The area itself is a {@link Container} that links all its containers (used by the courier, hoppers and upkeep),
 * the craftsman still walks to the real chest of a slot.
 */
public class WorkshopArea extends AbstractWorkAreaEntity implements IPermissionArea, Container {

    public static final EntityDataAccessor<String>      CRAFTSMAN_NAME  = SynchedEntityData.defineId(WorkshopArea.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Boolean>     HAS_TABLE       = SynchedEntityData.defineId(WorkshopArea.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Integer>     FURNACE_COUNT   = SynchedEntityData.defineId(WorkshopArea.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer>     CONTAINER_COUNT = SynchedEntityData.defineId(WorkshopArea.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer>     TOTAL_SLOTS     = SynchedEntityData.defineId(WorkshopArea.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer>     FREE_SLOTS      = SynchedEntityData.defineId(WorkshopArea.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<CompoundTag> ORDERS          = SynchedEntityData.defineId(WorkshopArea.class, EntityDataSerializers.COMPOUND_TAG);

    public static final int MIN_CONTAINERS = 4;
    /** Minimum ticks between two material checks triggered by container changes. */
    public static final int CHECK_COOLDOWN = 40;
    private static final int SCAN_INTERVAL = 200;
    private static final int SYNC_INTERVAL = 10;
    /** Max orders per workshop. */
    public static final int MAX_ORDERS = 16;

    public Map<BlockPos, Container>          containerMap = new LinkedHashMap<>();
    public Map<BlockPos, FurnaceBlockEntity> furnaceMap   = new LinkedHashMap<>();
    @Nullable public BlockPos craftingTablePos;

    // aggregate slot index -> container position + local slot
    private final List<BlockPos> slotPositions = new ArrayList<>();
    private int[] slotLocal = new int[0];

    // server side order list, pushed to ORDERS for the client
    private final List<CraftOrder> orders = new ArrayList<>();
    private boolean ordersDirty;

    private boolean checkRequested = true;
    private long lastCheckTime = -CHECK_COOLDOWN;

    public WorkshopArea(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(CRAFTSMAN_NAME, "None");
        this.entityData.define(HAS_TABLE, false);
        this.entityData.define(FURNACE_COUNT, 0);
        this.entityData.define(CONTAINER_COUNT, 0);
        this.entityData.define(TOTAL_SLOTS, 0);
        this.entityData.define(FREE_SLOTS, 0);
        this.entityData.define(ORDERS, new CompoundTag());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.orders.clear();
        this.orders.addAll(CraftOrder.listFromNbt(tag.getCompound("Orders")));
        this.pushOrders();
        setBeingWorkedOn(false);
        this.setCraftsmanName("None");
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("Orders", CraftOrder.listToNbt(this.orders));
    }

    @Override
    public Item getRenderItem() {
        return Items.CRAFTING_TABLE;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public Screen getScreen(Player player) {
        return new WorkshopAreaScreen(this, player);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getCommandSenderWorld().isClientSide()) return;

        if (tickCount == 1 || tickCount % SCAN_INTERVAL == 0) scanArea();
        if (ordersDirty && tickCount % SYNC_INTERVAL == 0) pushOrders();
    }

    // ── Scanning ──────────────────────────────────────────────────────────────

    public void scanArea() {
        if (this.getCommandSenderWorld().isClientSide()) return;
        if (area == null) area = this.getArea();

        Level level = this.getCommandSenderWorld();
        containerMap.clear();
        furnaceMap.clear();
        craftingTablePos = null;

        BlockPos.betweenClosedStream(area).forEach(pos -> {
            BlockEntity be = level.getBlockEntity(pos);
            BlockState  bs = level.getBlockState(pos);

            if (bs.is(Blocks.CRAFTING_TABLE) || bs.getBlock() instanceof CraftingTableBlock) {
                if (craftingTablePos == null) craftingTablePos = pos.immutable();
            }
            else if (be instanceof FurnaceBlockEntity furnace && !furnace.isRemoved()) {
                furnaceMap.put(pos.immutable(), furnace);
            }
            else if (be instanceof AbstractFurnaceBlockEntity || be instanceof HopperBlockEntity || be instanceof BrewingStandBlockEntity) {
                // not a storage container
            }
            else if (bs.getBlock() instanceof ChestBlock chestBlock) {
                Container chest = ChestBlock.getContainer(chestBlock, bs, level, pos, false);
                if (chest != null && !isAlreadyMapped(containerMap, chest)) {
                    containerMap.put(pos.immutable(), chest);
                }
            }
            else if (be instanceof Container container && !be.isRemoved()) {
                if (!isAlreadyMapped(containerMap, container)) {
                    containerMap.put(pos.immutable(), container);
                }
            }
        });

        rebuildSlotMap();

        this.setHasTable(craftingTablePos != null);
        this.setFurnaceCount(furnaceMap.size());
        this.setContainerCount(containerMap.size());
        this.setTotalSlots(slotLocal.length);
        int free = 0;
        for (Container container : containerMap.values()) {
            for (int i = 0; i < container.getContainerSize(); i++) {
                if (container.getItem(i).isEmpty()) free++;
            }
        }
        this.setFreeSlots(free);
    }

    private void rebuildSlotMap() {
        slotPositions.clear();
        List<Integer> locals = new ArrayList<>();
        for (Map.Entry<BlockPos, Container> entry : containerMap.entrySet()) {
            for (int i = 0; i < entry.getValue().getContainerSize(); i++) {
                slotPositions.add(entry.getKey());
                locals.add(i);
            }
        }
        slotLocal = new int[locals.size()];
        for (int i = 0; i < locals.size(); i++) slotLocal[i] = locals.get(i);
    }

    public boolean hasMinimumSetup() {
        if (craftingTablePos == null || furnaceMap.isEmpty() || containerMap.size() < MIN_CONTAINERS) scanArea();
        return craftingTablePos != null && !furnaceMap.isEmpty() && containerMap.size() >= MIN_CONTAINERS;
    }

    /** Removes stale entries whose BlockEntity has been removed from the world. */
    public void pruneRemoved() {
        furnaceMap.values().removeIf(AbstractFurnaceBlockEntity::isRemoved);
        boolean dirty = containerMap.entrySet().removeIf(e -> e.getValue() instanceof BlockEntity be && be.isRemoved());
        if (dirty) rebuildSlotMap();
    }

    // ── Aggregate container helpers ───────────────────────────────────────────

    @Nullable
    public BlockPos getSlotPos(int slot) {
        if (slot < 0 || slot >= slotPositions.size()) return null;
        return slotPositions.get(slot);
    }

    @Nullable
    private Container getSlotContainer(int slot) {
        BlockPos pos = getSlotPos(slot);
        if (pos == null) return null;
        Container container = containerMap.get(pos);
        if (container instanceof BlockEntity be && be.isRemoved()) return null;
        if (container == null || slotLocal[slot] >= container.getContainerSize()) return null;
        return container;
    }

    public int countMatching(Predicate<ItemStack> predicate) {
        int total = 0;
        for (Container container : containerMap.values()) {
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack stack = container.getItem(i);
                if (!stack.isEmpty() && predicate.test(stack)) total += stack.getCount();
            }
        }
        return total;
    }

    /** Copies of all stacks in the workshop containers, used to plan without touching the chests. */
    public List<ItemStack> snapshotContents() {
        List<ItemStack> out = new ArrayList<>();
        for (Container container : containerMap.values()) {
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack stack = container.getItem(i);
                if (!stack.isEmpty()) out.add(stack.copy());
            }
        }
        return out;
    }

    /** Positions of all containers holding at least one matching item. */
    public List<BlockPos> findContainersWith(Predicate<ItemStack> predicate) {
        List<BlockPos> out = new ArrayList<>();
        for (Map.Entry<BlockPos, Container> entry : containerMap.entrySet()) {
            Container container = entry.getValue();
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack stack = container.getItem(i);
                if (!stack.isEmpty() && predicate.test(stack)) {
                    out.add(entry.getKey());
                    break;
                }
            }
        }
        return out;
    }

    /** How many of the stack still fit into the given container. */
    public static int getSpaceFor(Container container, ItemStack stack) {
        int space = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack slot = container.getItem(i);
            if (slot.isEmpty()) space += stack.getMaxStackSize();
            else if (ItemStack.isSameItemSameTags(slot, stack)) space += Math.max(0, slot.getMaxStackSize() - slot.getCount());
        }
        return space;
    }

    public boolean canStore(ItemStack stack) {
        for (Container container : containerMap.values()) {
            if (getSpaceFor(container, stack) > 0) return true;
        }
        return false;
    }

    /** Inserts as much of the stack as possible, the stack is shrunk by the inserted amount. */
    public static int insertInto(Container container, ItemStack stack) {
        int inserted = 0;
        for (int i = 0; i < container.getContainerSize() && !stack.isEmpty(); i++) {
            ItemStack slot = container.getItem(i);
            if (slot.isEmpty() || !ItemStack.isSameItemSameTags(slot, stack)) continue;
            int add = Math.min(stack.getCount(), slot.getMaxStackSize() - slot.getCount());
            if (add <= 0) continue;
            slot.grow(add);
            stack.shrink(add);
            inserted += add;
        }
        for (int i = 0; i < container.getContainerSize() && !stack.isEmpty(); i++) {
            if (!container.getItem(i).isEmpty()) continue;
            int put = Math.min(stack.getCount(), stack.getMaxStackSize());
            container.setItem(i, stack.split(put));
            inserted += put;
        }
        if (inserted > 0) container.setChanged();
        return inserted;
    }

    // ── Material check trigger ────────────────────────────────────────────────

    /** Something changed in the workshop containers: the craftsman checks his orders again (with cooldown). */
    public void requestCheck() {
        this.checkRequested = true;
    }

    public boolean consumeCheckRequest() {
        if (!checkRequested) return false;
        long time = this.getCommandSenderWorld().getGameTime();
        if (time - lastCheckTime < CHECK_COOLDOWN) return false;
        checkRequested = false;
        lastCheckTime = time;
        return true;
    }

    /** Called when a player opens a container: if it belongs to a workshop, that workshop checks again. */
    public static void onContainerInteraction(Level level, BlockPos pos) {
        for (WorkshopArea workshop : level.getEntitiesOfClass(WorkshopArea.class, new AABB(pos).inflate(32))) {
            AABB box = workshop.getArea();
            if (pos.getX() >= box.minX && pos.getX() <= box.maxX
                    && pos.getY() >= box.minY && pos.getY() <= box.maxY
                    && pos.getZ() >= box.minZ && pos.getZ() <= box.maxZ) {
                workshop.requestCheck();
            }
        }
    }

    // ── Orders ────────────────────────────────────────────────────────────────

    /** Server: the live order list. Client: the last synced copy. */
    public List<CraftOrder> getOrders() {
        if (this.getCommandSenderWorld().isClientSide()) {
            return CraftOrder.listFromNbt(this.entityData.get(ORDERS));
        }
        return orders;
    }

    public CompoundTag getSyncedOrdersTag() {
        return this.entityData.get(ORDERS);
    }

    @Nullable
    public CraftOrder getOrder(UUID uuid) {
        for (CraftOrder order : getOrders()) {
            if (order.uuid.equals(uuid)) return order;
        }
        return null;
    }

    @Nullable
    public CraftOrder findOrder(String sourceKey) {
        if (sourceKey == null) return null;
        for (CraftOrder order : orders) {
            if (sourceKey.equals(order.sourceKey)) return order;
        }
        return null;
    }

    public boolean isOrderListFull() {
        return getOrders().size() >= MAX_ORDERS;
    }

    public boolean addOrder(CraftOrder order) {
        return addOrUpdateOrder(order);
    }

    /** Replaces the order with the same uuid or adds it. Returns false if the list is full (max 16). */
    public boolean addOrUpdateOrder(CraftOrder order) {
        if (order == null) return false;
        for (int i = 0; i < orders.size(); i++) {
            if (orders.get(i).uuid.equals(order.uuid)) {
                orders.set(i, order);
                onOrdersChanged(true);
                return true;
            }
        }
        if (orders.size() >= MAX_ORDERS) return false;
        orders.add(order);
        onOrdersChanged(true);
        return true;
    }

    public boolean removeOrder(UUID uuid) {
        boolean removed = orders.removeIf(order -> order.uuid.equals(uuid));
        if (removed) onOrdersChanged(true);
        return removed;
    }

    /**
     * Creates or updates the order with the given source key (e.g. "loadout:claim:bowman:head").
     * Progress of an existing order is kept, so the caller can set the wanted amount every interval.
     * Returns null if the order is new and the list is full.
     */
    @Nullable
    public CraftOrder upsertOrder(String sourceKey, CraftOrder template) {
        CraftOrder existing = findOrder(sourceKey);
        CraftOrder order = template.copy();
        order.sourceKey = sourceKey;
        if (existing != null) {
            order.uuid = existing.uuid;
            order.produced = existing.produced;
            order.stock = existing.stock;
            order.status = existing.status;
        }
        order.clampValues();
        return addOrUpdateOrder(order) ? order : null;
    }

    public int removeOrdersBySource(String prefix) {
        int before = orders.size();
        orders.removeIf(order -> order.sourceKey != null && order.sourceKey.startsWith(prefix));
        int removed = before - orders.size();
        if (removed > 0) onOrdersChanged(true);
        return removed;
    }

    /** Marks the order list for syncing. Immediate syncs are used for changes from the GUI. */
    public void onOrdersChanged(boolean immediate) {
        if (immediate) pushOrders();
        else ordersDirty = true;
        requestCheck();
    }

    private void pushOrders() {
        this.entityData.set(ORDERS, CraftOrder.listToNbt(orders));
        this.ordersDirty = false;
    }

    public void openEditOrderGUI(Player player, CraftOrder order) {
        if (player instanceof ServerPlayer sp) {
            NetworkHooks.openScreen(sp, new MenuProvider() {
                public @NotNull Component getDisplayName() { return Component.literal("workshop_order_edit_screen"); }
                public @NotNull AbstractContainerMenu createMenu(int i, @NotNull Inventory inv, @NotNull Player p) {
                    return new WorkshopEditOrderContainer(i, WorkshopArea.this, inv, order);
                }
            }, buf -> { buf.writeUUID(this.getUUID()); buf.writeNbt(order.toNbt()); });
        } else {
            WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageOpenWorkshopEditOrderScreen(this.getUUID(), order));
        }
    }

    // ── canWorkHere ───────────────────────────────────────────────────────────

    @Override
    public boolean canWorkHere(AbstractWorkerEntity worker) {
        if (!(worker instanceof CraftsmanEntity)) return false;
        if (!super.canWorkHere(worker)) return false;
        if (isBeingWorkedOn()) return false;
        return true;
    }

    // ── Container (links all workshop containers) ─────────────────────────────

    @Override
    public int getContainerSize() {
        return slotLocal.length;
    }

    @Override
    public boolean isEmpty() {
        for (Container container : containerMap.values()) {
            if (!container.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public @NotNull ItemStack getItem(int slot) {
        Container container = getSlotContainer(slot);
        if (container == null) return ItemStack.EMPTY;
        return container.getItem(slotLocal[slot]);
    }

    @Override
    public @NotNull ItemStack removeItem(int slot, int amount) {
        Container container = getSlotContainer(slot);
        if (container == null) return ItemStack.EMPTY;
        ItemStack removed = container.removeItem(slotLocal[slot], amount);
        if (!removed.isEmpty()) requestCheck();
        return removed;
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int slot) {
        Container container = getSlotContainer(slot);
        if (container == null) return ItemStack.EMPTY;
        ItemStack removed = container.removeItemNoUpdate(slotLocal[slot]);
        if (!removed.isEmpty()) requestCheck();
        return removed;
    }

    @Override
    public void setItem(int slot, @NotNull ItemStack stack) {
        Container container = getSlotContainer(slot);
        if (container == null) return;
        container.setItem(slotLocal[slot], stack);
        requestCheck();
    }

    @Override
    public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
        Container container = getSlotContainer(slot);
        return container != null && container.canPlaceItem(slotLocal[slot], stack);
    }

    @Override
    public void setChanged() {
        for (Container container : containerMap.values()) {
            container.setChanged();
        }
        requestCheck();
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return false;
    }

    @Override
    public void clearContent() {
        // never wipe the linked chests through the workshop
    }

    // ── Synced data ───────────────────────────────────────────────────────────

    public String getCraftsmanName()          { return this.entityData.get(CRAFTSMAN_NAME); }
    public void setCraftsmanName(String name) { this.entityData.set(CRAFTSMAN_NAME, name); }

    public boolean hasTable()                 { return this.entityData.get(HAS_TABLE); }
    public void setHasTable(boolean b)        { this.entityData.set(HAS_TABLE, b); }

    public int getFurnaceCount()              { return this.entityData.get(FURNACE_COUNT); }
    public void setFurnaceCount(int x)        { this.entityData.set(FURNACE_COUNT, x); }

    public int getContainerCount()            { return this.entityData.get(CONTAINER_COUNT); }
    public void setContainerCount(int x)      { this.entityData.set(CONTAINER_COUNT, x); }

    public int getTotalSlots()                { return this.entityData.get(TOTAL_SLOTS); }
    public void setTotalSlots(int x)          { this.entityData.set(TOTAL_SLOTS, x); }

    public int getFreeSlots()                 { return this.entityData.get(FREE_SLOTS); }
    public void setFreeSlots(int x)           { this.entityData.set(FREE_SLOTS, x); }
}
