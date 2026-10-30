package com.talhanation.workers.entities.ai;

import com.talhanation.workers.WorkshopEvent;
import com.talhanation.workers.entities.CraftsmanEntity;
import com.talhanation.workers.entities.workarea.StorageArea;
import com.talhanation.workers.entities.workarea.WorkshopArea;
import com.talhanation.workers.world.CraftOrder;
import com.talhanation.workers.world.CraftOrder.SlotMatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nullable;
import java.util.*;

/**
 * The craftsman works like the cook: he goes through the orders in turn, collects the items from the workshop
 * chests, crafts at the crafting table (or loads a furnace) and puts everything back. Orders that can not be
 * produced right now are skipped. Items no active order needs any more go back to the storage.
 */
public class CraftsmanWorkGoal extends Goal {

    private static final int CHECK_INTERVAL        = 200;
    private static final int INTERACT_TIME         = 10;
    private static final int FURNACE_INTERACT_TIME = 20;
    private static final int DEPOSIT_RETRY_TIME    = 200;
    private static final int MAX_MOVE_TICKS        = 600;
    private static final int TIDY_RETRY_TIME       = 2400;
    private static final int FIRST_INV_SLOT        = 6;

    /** Fuel the craftsman uses, strictly in this order. Nothing else burnable is taken. */
    private static final List<TagKey<Item>> FUEL_TAGS = List.of(ItemTags.COALS, ItemTags.PLANKS, ItemTags.LOGS_THAT_BURN);
    /** Missing fuel is fetched from a storage as coal or charcoal only. */
    private static final SlotMatcher COAL_MATCHER = SlotMatcher.ingredient(Ingredient.of(ItemTags.COALS), new ItemStack(Items.COAL));

    private final CraftsmanEntity craftsman;
    private State state;
    private int cooldown;
    private int checkTimer;
    private int interactTimer;
    private int craftTimer;
    private int depositRetry;
    private int craftedSinceSound;

    @Nullable private BlockPos openContainerPos;
    @Nullable private BlockPos moveTarget;
    private int moveTicks;

    // ── Current job ───────────────────────────────────────────────────────────
    @Nullable private UUID currentOrderId;
    @Nullable private UUID lastServedOrder;
    @Nullable private SlotMatcher[] jobMatchers;
    @Nullable private SlotMatcher fuelMatcher;
    @Nullable private BlockPos furnacePos;
    @Nullable private BlockPos depositPos;
    private int batch;
    private int fuelCount;
    private final List<Need> needs = new ArrayList<>();
    private final List<BlockPos> chestQueue = new ArrayList<>();
    /** Items the craftsman took or produced in the workshop: these are always put back, even food. */
    private final Set<Item> handledItems = new HashSet<>();

    // ── Storage: fetch missing items, store finished products ────────────────
    /** Storage areas the craftsman may use, refreshed on every planning. Empty = everything stays in the workshop. */
    private final List<StorageArea> storages = new ArrayList<>();
    /** Missing items of blocked orders, collected during planning. */
    private final Map<UUID, List<Need>> missingNeeds = new HashMap<>();
    /** Results produced in this round, they are brought to the storage. */
    private final Set<Item> productItems = new HashSet<>();
    @Nullable private StorageArea fetchStorage;
    @Nullable private StorageArea deliverStorage;
    @Nullable private BlockPos deliverPos;
    private boolean deliveryFailed;
    /** Ingredients of the orders that still want to produce: these stay in the workshop, everything else may go to the storage. */
    private final List<SlotMatcher> orderIngredients = new ArrayList<>();
    /** The craftsman collects items no order needs right now to bring them to the storage. */
    private boolean tidying;
    /** After a failed delivery the workshop is not tidied up again before this game time. */
    private long tidyBlockedUntil;

    public CraftsmanWorkGoal(CraftsmanEntity craftsman) {
        this.craftsman = craftsman;
        setFlags(EnumSet.of(Flag.LOOK, Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return craftsman.shouldWork() && !craftsman.needsToGetToChest() && isAreaNotRemoved();
    }

    private boolean isAreaNotRemoved() {
        if (craftsman.currentWorkshopArea == null || !craftsman.currentWorkshopArea.isRemoved()) return true;
        craftsman.currentWorkshopArea = null;
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
        if (craftsman.getFollowState() == 0) craftsman.setFollowState(6);
        setState(State.SELECT_WORK_AREA);
    }

    @Override
    public void stop() {
        if (craftsman.getCommandSenderWorld() instanceof ServerLevel level) {
            closeContainer(level);
        }
        clearJob();

        if (craftsman.currentWorkshopArea != null) {
            craftsman.currentWorkshopArea.setBeingWorkedOn(false);
            craftsman.currentWorkshopArea.setCraftsmanName("None");
            craftsman.currentWorkshopArea = null;
        }
        craftsman.getNavigation().stop();

        // Only reset to wander if we are still in the working state,
        // so owner commands (e.g. follow) are not overridden.
        if (craftsman.getFollowState() == 6) {
            craftsman.setFollowState(0);
        }
    }

    @Override
    public void tick() {
        if (craftsman.getCommandSenderWorld().isClientSide()) return;
        if (state == null) return;
        ServerLevel level = (ServerLevel) craftsman.getCommandSenderWorld();

        if (state != State.SELECT_WORK_AREA && isCurrentAreaGone()) {
            closeContainer(level);
            leaveCurrentArea();
            setState(State.SELECT_WORK_AREA);
            return;
        }

        WorkshopArea area = craftsman.currentWorkshopArea;
        if (area != null && !area.isBeingWorkedOn() && state != State.SELECT_WORK_AREA
                && craftsman.getHorizontalDistanceTo(area.position()) < 1000) {
            area.setBeingWorkedOn(true);
        }
        switch (state) {
            case SELECT_WORK_AREA -> tickSelectWorkArea(level);
            case WALK_TO_CENTER   -> tickWalkToCenter();
            case IDLE             -> tickIdle(level, area);
            case COLLECT          -> tickCollect(level, area);
            case CRAFT            -> tickCraft(level, area);
            case LOAD_FURNACE     -> tickLoadFurnace(level, area);
            case COLLECT_FURNACE  -> tickCollectFurnace(level, area);
            case DEPOSIT          -> tickDeposit(level, area);
            case DELIVER          -> tickDeliver(level, area);
        }
    }

    // ── States ────────────────────────────────────────────────────────────────

    private void tickSelectWorkArea(ServerLevel level) {
        if (craftsman.currentWorkshopArea != null) {
            setState(State.WALK_TO_CENTER);
            return;
        }

        if (++cooldown < craftsman.getRandom().nextInt(200)) return;
        cooldown = 0;

        WorkshopArea found = findBestArea(level);
        if (found == null) return;

        if (!found.hasMinimumSetup()) {
            craftsman.notifyOwner(Component.translatable("chat.workers.text.workshopIncomplete", craftsman.getName().getString()));
            return;
        }

        craftsman.currentWorkshopArea = found;
        found.setBeingWorkedOn(true);
        found.setCraftsmanName(craftsman.getName().getString());
        found.setTime(0);

        setState(State.WALK_TO_CENTER);
    }

    private void tickWalkToCenter() {
        if (moveToPosition(BlockPos.containing(craftsman.currentWorkshopArea.getArea().getCenter()), 3)) return;
        craftsman.getNavigation().stop();
        checkTimer = CHECK_INTERVAL;
        setState(State.IDLE);
    }

    private void tickIdle(ServerLevel level, WorkshopArea area) {
        craftsman.getNavigation().stop();

        if (hasItemsToDeposit() || hasProductsToDeliver(area)) {
            if (depositRetry > 0) depositRetry--;
            else {
                startDeposit(level);
                return;
            }
        }

        boolean due = ++checkTimer >= CHECK_INTERVAL;
        if (area.consumeCheckRequest()) due = true;
        if (!due) return;
        checkTimer = 0;

        plan(level, area);
    }

    private void tickCollect(ServerLevel level, WorkshopArea area) {
        if (allNeedsMet() || chestQueue.isEmpty()) {
            closeContainer(level);
            finishCollect(level, area);
            return;
        }

        BlockPos pos = chestQueue.get(0);
        if (moveToPosition(pos, 3)) return;
        craftsman.getNavigation().stop();
        craftsman.getLookControl().setLookAt(pos.getCenter());

        if (interactTimer == 0) {
            openContainerPos = pos;
            interactContainer(level, pos, true);
        }
        if (interactTimer < INTERACT_TIME) {
            interactTimer++;
            if (interactTimer % 5 == 0) craftsman.swing(InteractionHand.MAIN_HAND);
            return;
        }

        Container container = fetchStorage != null ? fetchStorage.storageMap.get(pos) : area.containerMap.get(pos);
        if (container != null) takeNeeds(container);

        closeContainer(level);
        chestQueue.remove(0);
        interactTimer = 0;
    }

    private void tickCraft(ServerLevel level, WorkshopArea area) {
        BlockPos table = area.craftingTablePos;
        CraftOrder order = getCurrentOrder(area);
        if (table == null || order == null || !order.enabled || batch <= 0) {
            finishJob(level, area);
            return;
        }

        if (moveToPosition(table, 3)) return;
        craftsman.getNavigation().stop();
        craftsman.getLookControl().setLookAt(table.getCenter());

        if (++craftTimer < craftsman.getTicksPerItem()) return;
        craftTimer = 0;

        if (!craftOnce(level, area, order, table)) {
            finishJob(level, area);
            return;
        }
        batch--;

        if (order.isTargetReached()) {
            completeOrder(area, order);
            finishJob(level, area);
            return;
        }
        if (batch <= 0) finishJob(level, area);
    }

    private void tickLoadFurnace(ServerLevel level, WorkshopArea area) {
        if (furnacePos == null) {
            finishJob(level, area);
            return;
        }
        if (moveToPosition(furnacePos, 3)) return;
        craftsman.getNavigation().stop();
        craftsman.getLookControl().setLookAt(furnacePos.getCenter());

        if (interactTimer < FURNACE_INTERACT_TIME) {
            interactTimer++;
            if (interactTimer % 5 == 0) craftsman.swing(InteractionHand.MAIN_HAND);
            return;
        }

        FurnaceBlockEntity furnace = getFurnace(level, furnacePos);
        if (furnace != null && jobMatchers != null) {
            loadFurnace(furnace, jobMatchers[0]);
        }
        finishJob(level, area);
    }

    private void tickCollectFurnace(ServerLevel level, WorkshopArea area) {
        if (furnacePos == null) {
            startDeposit(level);
            return;
        }
        if (moveToPosition(furnacePos, 3)) return;
        craftsman.getNavigation().stop();
        craftsman.getLookControl().setLookAt(furnacePos.getCenter());

        if (interactTimer < FURNACE_INTERACT_TIME) {
            interactTimer++;
            if (interactTimer % 5 == 0) craftsman.swing(InteractionHand.MAIN_HAND);
            return;
        }

        FurnaceBlockEntity furnace = getFurnace(level, furnacePos);
        if (furnace != null) {
            ItemStack output = furnace.getItem(2);
            if (!output.isEmpty()) {
                CraftOrder order = findSmeltOrderFor(area, output);
                Item item = output.getItem();
                int before = output.getCount();
                ItemStack rest = craftsman.addItem(output.copy());
                int taken = before - rest.getCount();
                if (taken > 0) {
                    handledItems.add(item);
                    productItems.add(item);
                    furnace.setItem(2, rest);
                    furnace.setChanged();
                    if (order != null) {
                        order.produced += taken;
                        area.onOrdersChanged(false);
                        if (order.isTargetReached()) completeOrder(area, order);
                    }
                }
            }
        }
        furnacePos = null;
        startDeposit(level);
    }

    private void tickDeposit(ServerLevel level, WorkshopArea area) {
        if (!hasItemsToDeposit()) {
            closeContainer(level);
            depositPos = null;
            if (hasProductsToDeliver(area)) {
                interactTimer = 0;
                setState(State.DELIVER);
                return;
            }
            handledItems.clear();
            productItems.clear();
            deliveryFailed = false;
            checkTimer = CHECK_INTERVAL;
            setState(State.IDLE);
            return;
        }

        if (depositPos == null) {
            depositPos = findDepositContainer(area);
            interactTimer = 0;
            if (depositPos == null) {
                craftsman.notifyOwner(Component.translatable("chat.workers.text.workshopFull", craftsman.getName().getString()));
                depositRetry = DEPOSIT_RETRY_TIME;
                setState(State.IDLE);
                return;
            }
        }

        if (moveToPosition(depositPos, 3)) return;
        craftsman.getNavigation().stop();
        craftsman.getLookControl().setLookAt(depositPos.getCenter());

        if (interactTimer == 0) {
            openContainerPos = depositPos;
            interactContainer(level, depositPos, true);
        }
        if (interactTimer < INTERACT_TIME) {
            interactTimer++;
            if (interactTimer % 5 == 0) craftsman.swing(InteractionHand.MAIN_HAND);
            return;
        }

        Container container = area.containerMap.get(depositPos);
        if (container != null) {
            SimpleContainer inventory = craftsman.getInventory();
            for (int i = FIRST_INV_SLOT; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                if (!shouldDeposit(stack) || isDeliverable(area, stack)) continue;
                WorkshopArea.insertInto(container, stack);
                inventory.setItem(i, stack.isEmpty() ? ItemStack.EMPTY : stack);
            }
        }

        closeContainer(level);
        depositPos = null;
        interactTimer = 0;
    }

    /** Brings the finished products to the nearest storage. If no storage has space, they stay in the workshop. */
    private void tickDeliver(ServerLevel level, WorkshopArea area) {
        if (!hasProductsToDeliver(area)) {
            closeContainer(level);
            deliverPos = null;
            deliverStorage = null;
            setState(State.DEPOSIT);
            return;
        }

        if (deliverPos == null) {
            if (!findDeliverTarget(level, area)) {
                deliveryFailed = true;
                tidyBlockedUntil = level.getGameTime() + TIDY_RETRY_TIME;
                setState(State.DEPOSIT);
                return;
            }
            interactTimer = 0;
        }

        if (moveToPosition(deliverPos, 3)) return;
        craftsman.getNavigation().stop();
        craftsman.getLookControl().setLookAt(deliverPos.getCenter());

        if (interactTimer == 0) {
            openContainerPos = deliverPos;
            interactContainer(level, deliverPos, true);
        }
        if (interactTimer < INTERACT_TIME) {
            interactTimer++;
            if (interactTimer % 5 == 0) craftsman.swing(InteractionHand.MAIN_HAND);
            return;
        }

        Container container = deliverStorage != null ? deliverStorage.storageMap.get(deliverPos) : null;
        if (container != null) {
            SimpleContainer inventory = craftsman.getInventory();
            for (int i = FIRST_INV_SLOT; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                if (!isDeliverable(area, stack)) continue;
                WorkshopArea.insertInto(container, stack);
                inventory.setItem(i, stack.isEmpty() ? ItemStack.EMPTY : stack);
            }
        }

        closeContainer(level);
        deliverPos = null;
        interactTimer = 0;
    }

    // ── Planning ──────────────────────────────────────────────────────────────

    private void plan(ServerLevel level, WorkshopArea area) {
        area.pruneRemoved();
        area.scanArea();
        if (!area.hasMinimumSetup()) {
            craftsman.notifyOwner(Component.translatable("chat.workers.text.workshopIncomplete", craftsman.getName().getString()));
            return;
        }

        // Finished smelting output of an order is collected first
        if (craftsman.hasFreeInvSlot()) {
            for (Map.Entry<BlockPos, FurnaceBlockEntity> entry : area.furnaceMap.entrySet()) {
                ItemStack output = entry.getValue().getItem(2);
                if (!output.isEmpty() && findSmeltOrderFor(area, output) != null) {
                    furnacePos = entry.getKey();
                    interactTimer = 0;
                    setState(State.COLLECT_FURNACE);
                    return;
                }
            }
        }

        List<CraftOrder> orders = new ArrayList<>(area.getOrders());

        refreshStorages(level, area);
        missingNeeds.clear();

        List<ItemStack> snapshot = area.snapshotContents();
        List<Job> candidates = new ArrayList<>();
        boolean changed = false;
        for (CraftOrder order : orders) {
            CraftOrder.Status before = order.status;
            int missingBefore = missingTotal(order);
            int stockBefore = order.stock;
            Job job = evaluate(level, area, order, snapshot);
            if (job != null) candidates.add(job);
            if (before != order.status || missingBefore != missingTotal(order) || stockBefore != order.stock) changed = true;
        }
        // the statuses are fresh now: only orders that still want to produce keep their ingredients in the workshop
        refreshIngredients(level, area);

        // A blocked order whose missing items lie in a storage: fetch them first
        if (!storages.isEmpty()) {
            for (CraftOrder order : orders) {
                List<Need> missing = missingNeeds.get(order.uuid);
                if (missing == null || missing.isEmpty()) continue;
                if (startFetch(area, order, missing)) {
                    order.status = CraftOrder.Status.FETCHING;
                    area.onOrdersChanged(false);
                    return;
                }
            }
        }

        if (changed) area.onOrdersChanged(false);

        // Items no order needs right now go back to the storage before new work starts
        if (startTidy(level, area)) return;
        if (candidates.isEmpty()) return;

        // round robin between all orders that can be produced
        Job pick = candidates.get(0);
        if (lastServedOrder != null) {
            for (int i = 0; i < candidates.size(); i++) {
                if (candidates.get(i).order.uuid.equals(lastServedOrder)) {
                    pick = candidates.get((i + 1) % candidates.size());
                    break;
                }
            }
        }
        startJob(area, pick);
    }

    @Nullable
    private Job evaluate(ServerLevel level, WorkshopArea area, CraftOrder order, List<ItemStack> snapshot) {
        if (!order.enabled) {
            setStatus(area, order, CraftOrder.Status.DISABLED, null);
            return null;
        }
        Recipe<?> recipe = order.getRecipe(level);
        if (recipe == null || order.result.isEmpty()) {
            setStatus(area, order, CraftOrder.Status.INVALID, null);
            return null;
        }
        if (order.isTargetReached()) {
            setStatus(area, order, CraftOrder.Status.DONE, null);
            return null;
        }

        SlotMatcher[] matchers = order.buildSlotMatchers(level);
        int perOutput = Math.max(1, order.result.getCount());

        // a furnace of this order that ran out of fuel is refueled first
        if (order.type == CraftOrder.OrderType.SMELTING && recipe instanceof SmeltingRecipe smelting && matchers[0] != null) {
            Job refuel = evaluateRefuel(area, order, smelting, matchers, snapshot);
            if (refuel != null) return refuel;
        }
        int pending = order.type == CraftOrder.OrderType.SMELTING ? countPendingInFurnaces(area, order, matchers[0], perOutput) : 0;
        if (order.mode == CraftOrder.Mode.STOCK) {
            // products go to the storage, so the stock counts the workshop and the used storages
            order.stock = area.countMatching(stack -> ItemStack.isSameItemSameTags(stack, order.result))
                    + countInStorages(stack -> ItemStack.isSameItemSameTags(stack, order.result));
        }
        int wanted = order.getRemaining() - pending;

        if (order.mode == CraftOrder.Mode.STOCK && order.stock >= order.amount) {
            setStatus(area, order, CraftOrder.Status.STOCK_FULL, null);
            return null;
        }
        if (wanted <= 0) {
            // everything that is still missing is already in a furnace
            setStatus(area, order, CraftOrder.Status.OK, null);
            return null;
        }
        if (!area.canStore(order.result)) {
            setStatus(area, order, CraftOrder.Status.NO_SPACE, null);
            return null;
        }

        int runs = (wanted + perOutput - 1) / perOutput;
        if (order.type == CraftOrder.OrderType.SMELTING && recipe instanceof SmeltingRecipe smelting) {
            return evaluateSmelting(level, area, order, smelting, matchers, runs, snapshot);
        }
        if (order.type == CraftOrder.OrderType.CRAFTING && recipe instanceof CraftingRecipe) {
            return evaluateCrafting(area, order, matchers, runs, perOutput, snapshot);
        }
        setStatus(area, order, CraftOrder.Status.INVALID, null);
        return null;
    }

    @Nullable
    private Job evaluateCrafting(WorkshopArea area, CraftOrder order, SlotMatcher[] matchers, int runs, int perOutput, List<ItemStack> snapshot) {
        List<Need> perCraft = groupMatchers(matchers);
        if (perCraft.isEmpty()) {
            setStatus(area, order, CraftOrder.Status.INVALID, null);
            return null;
        }

        // limit by the free inventory slots of the craftsman
        int free = countFreeInventorySlots();
        int bySpace = Math.min(runs, 64 * free);
        while (bySpace > 0 && stacksFor(perCraft, bySpace, order.result, perOutput) > free) bySpace--;
        if (bySpace <= 0) {
            setStatus(area, order, CraftOrder.Status.NO_SPACE, null);
            return null;
        }

        int lo = 0, hi = Math.min(bySpace, runs);
        while (lo < hi) {
            int mid = (lo + hi + 1) / 2;
            if (totalDeficit(allocate(perCraft, snapshot, mid)) == 0) lo = mid;
            else hi = mid - 1;
        }
        if (lo <= 0) {
            int[] deficits = allocate(perCraft, snapshot, runs);
            for (int i = 0; i < perCraft.size(); i++) {
                if (deficits[i] > 0) addMissingNeed(order, perCraft.get(i).matcher, deficits[i]);
            }
            setStatus(area, order, CraftOrder.Status.MISSING_ITEMS, missingFor(perCraft, deficits));
            return null;
        }

        setStatus(area, order, CraftOrder.Status.OK, null);
        Job job = new Job(order, lo, matchers);
        for (Need need : perCraft) {
            job.needs.add(new Need(need.matcher, need.amount * lo));
        }
        return job;
    }

    @Nullable
    private Job evaluateSmelting(ServerLevel level, WorkshopArea area, CraftOrder order, SmeltingRecipe recipe, SlotMatcher[] matchers, int runs, List<ItemStack> snapshot) {
        SlotMatcher input = matchers[0];
        if (input == null) {
            setStatus(area, order, CraftOrder.Status.INVALID, null);
            return null;
        }

        // a furnace that is free or already smelts this
        BlockPos chosen = null;
        int capacity = 0;
        for (Map.Entry<BlockPos, FurnaceBlockEntity> entry : area.furnaceMap.entrySet()) {
            FurnaceBlockEntity furnace = entry.getValue();
            ItemStack in = furnace.getItem(0);
            ItemStack out = furnace.getItem(2);
            if (!out.isEmpty() && !ItemStack.isSameItemSameTags(out, order.result)) continue;
            if (!in.isEmpty() && !input.test(in)) continue;
            int space = in.isEmpty() ? input.display.getMaxStackSize() : in.getMaxStackSize() - in.getCount();
            if (space > capacity) {
                capacity = space;
                chosen = entry.getKey();
            }
        }
        if (chosen == null || capacity <= 0) {
            // all furnaces are busy, try again later
            setStatus(area, order, CraftOrder.Status.OK, null);
            return null;
        }

        List<ItemStack> pool = copyPool(snapshot);
        int inputAmount = takeFromPool(pool, input, Math.min(runs, capacity));
        if (inputAmount <= 0) {
            addMissingNeed(order, input, runs);
            setStatus(area, order, CraftOrder.Status.MISSING_ITEMS, List.of(input.display.copyWithCount(runs)));
            return null;
        }

        // fuel: coal/charcoal, then planks, then logs
        FurnaceBlockEntity furnace = area.furnaceMap.get(chosen);
        int cookTime = Math.max(1, recipe.getCookingTime());
        ItemStack fuelSlot = furnace.getItem(1);
        int existingBurn = fuelSlot.isEmpty() ? 0 : fuelSlot.getCount() * getBurnTime(fuelSlot);
        int requiredBurn = inputAmount * cookTime - existingBurn;

        SlotMatcher fuel = null;
        int fuelAmount = 0;
        if (requiredBurn > 0) {
            if (!fuelSlot.isEmpty()) {
                int burn = Math.max(1, getBurnTime(fuelSlot));
                fuel = SlotMatcher.exact(fuelSlot);
                int space = fuelSlot.getMaxStackSize() - fuelSlot.getCount();
                fuelAmount = takeFromPool(pool, fuel, Math.min(space, (requiredBurn + burn - 1) / burn));
                inputAmount = Math.min(inputAmount, (existingBurn + fuelAmount * burn) / cookTime);
            }
            else {
                ItemStack fuelItem = chooseFuel(pool);
                if (fuelItem != null) {
                    int burn = Math.max(1, getBurnTime(fuelItem));
                    fuel = SlotMatcher.exact(fuelItem);
                    fuelAmount = takeFromPool(pool, fuel, Math.min(fuelItem.getMaxStackSize(), (requiredBurn + burn - 1) / burn));
                    inputAmount = Math.min(inputAmount, (fuelAmount * burn) / cookTime);
                }
                else {
                    inputAmount = 0;
                }
            }
        }
        if (inputAmount <= 0) {
            int coal = Math.max(1, (runs * cookTime + 1599) / 1600);
            addMissingNeed(order, COAL_MATCHER, coal);
            setStatus(area, order, CraftOrder.Status.MISSING_ITEMS, List.of(new ItemStack(Items.COAL, coal)));
            return null;
        }

        // the craftsman has to carry input and fuel
        int free = countFreeInventorySlots();
        int fuelStacks = fuelAmount > 0 ? (fuelAmount + fuel.display.getMaxStackSize() - 1) / fuel.display.getMaxStackSize() : 0;
        int inputStacks = (inputAmount + input.display.getMaxStackSize() - 1) / input.display.getMaxStackSize();
        if (inputStacks + fuelStacks > free) {
            setStatus(area, order, CraftOrder.Status.NO_SPACE, null);
            return null;
        }

        setStatus(area, order, CraftOrder.Status.OK, null);
        Job job = new Job(order, inputAmount, matchers);
        job.needs.add(new Need(input, inputAmount));
        if (fuelAmount > 0) {
            job.needs.add(new Need(fuel, fuelAmount));
            job.fuel = fuel;
            job.fuelAmount = fuelAmount;
        }
        job.furnacePos = chosen;
        return job;
    }

    /** Fuel for a furnace that still holds input of this order but is out of fuel and not burning. */
    @Nullable
    private Job evaluateRefuel(WorkshopArea area, CraftOrder order, SmeltingRecipe recipe, SlotMatcher[] matchers, List<ItemStack> snapshot) {
        for (Map.Entry<BlockPos, FurnaceBlockEntity> entry : area.furnaceMap.entrySet()) {
            FurnaceBlockEntity furnace = entry.getValue();
            ItemStack in = furnace.getItem(0);
            if (!matchers[0].test(in) || !furnace.getItem(1).isEmpty()) continue;
            BlockState furnaceState = furnace.getBlockState();
            if (furnaceState.hasProperty(AbstractFurnaceBlock.LIT) && furnaceState.getValue(AbstractFurnaceBlock.LIT)) continue;

            List<ItemStack> pool = copyPool(snapshot);
            ItemStack fuelItem = chooseFuel(pool);
            if (fuelItem == null) {
                int coal = Math.max(1, (in.getCount() * Math.max(1, recipe.getCookingTime()) + 1599) / 1600);
                addMissingNeed(order, COAL_MATCHER, coal);
                setStatus(area, order, CraftOrder.Status.MISSING_ITEMS, List.of(new ItemStack(Items.COAL, coal)));
                return null;
            }
            if (countFreeInventorySlots() <= 0) return null;

            int burn = Math.max(1, getBurnTime(fuelItem));
            SlotMatcher fuel = SlotMatcher.exact(fuelItem);
            int requiredBurn = in.getCount() * Math.max(1, recipe.getCookingTime());
            int fuelAmount = takeFromPool(pool, fuel, Math.min(fuelItem.getMaxStackSize(), (requiredBurn + burn - 1) / burn));
            if (fuelAmount <= 0) return null;

            setStatus(area, order, CraftOrder.Status.OK, null);
            Job job = new Job(order, 0, matchers);
            job.needs.add(new Need(fuel, fuelAmount));
            job.fuel = fuel;
            job.fuelAmount = fuelAmount;
            job.furnacePos = entry.getKey();
            return job;
        }
        return null;
    }

    private void startJob(WorkshopArea area, Job job) {
        clearJob();
        this.currentOrderId = job.order.uuid;
        this.lastServedOrder = job.order.uuid;
        this.jobMatchers = job.matchers;
        this.batch = job.batch;
        this.furnacePos = job.furnacePos;
        this.fuelMatcher = job.fuel;
        this.fuelCount = job.fuelAmount;
        this.needs.addAll(job.needs);

        chestQueue.addAll(area.findContainersWith(stack -> {
            for (Need need : needs) {
                if (need.matcher.test(stack)) return true;
            }
            return false;
        }));
        chestQueue.sort(Comparator.comparingDouble(pos -> pos.distToCenterSqr(craftsman.position())));

        interactTimer = 0;
        craftTimer = 0;
        setState(State.COLLECT);
    }

    private void finishCollect(ServerLevel level, WorkshopArea area) {
        if (tidying) {
            // the collected items count as products, so the delivery brings them to the storage
            tidying = false;
            for (Need need : needs) productItems.add(need.matcher.display.getItem());
            startDeposit(level);
            return;
        }
        if (fetchStorage != null) {
            // items from the storage are put into the workshop chests, then the orders are planned again
            fetchStorage = null;
            startDeposit(level);
            return;
        }
        CraftOrder order = getCurrentOrder(area);
        if (order == null || jobMatchers == null) {
            finishJob(level, area);
            return;
        }
        interactTimer = 0;
        craftTimer = 0;
        if (order.type == CraftOrder.OrderType.SMELTING) {
            boolean hasInput = jobMatchers[0] != null && batch > 0 && countCarried(jobMatchers[0]) > 0;
            boolean hasFuel = fuelMatcher != null && countCarried(fuelMatcher) > 0;
            if (hasInput || hasFuel) setState(State.LOAD_FURNACE);
            else finishJob(level, area);
        }
        else {
            setState(State.CRAFT);
        }
    }

    private void finishJob(ServerLevel level, WorkshopArea area) {
        if (area != null) area.onOrdersChanged(false);
        startDeposit(level);
    }

    private void completeOrder(WorkshopArea area, CraftOrder order) {
        order.status = CraftOrder.Status.DONE;
        order.missing.clear();
        MinecraftForge.EVENT_BUS.post(new WorkshopEvent.OrderCompleted(area, order));
        if (order.isGovernorOrder() && order.mode == CraftOrder.Mode.ONCE) {
            area.removeOrder(order.uuid);
        }
        else {
            area.onOrdersChanged(true);
        }
    }

    private void setStatus(WorkshopArea area, CraftOrder order, CraftOrder.Status status, @Nullable List<ItemStack> missing) {
        boolean blockedNow = status == CraftOrder.Status.MISSING_ITEMS
                && order.status != CraftOrder.Status.MISSING_ITEMS && order.status != CraftOrder.Status.FETCHING;
        order.status = status;
        order.missing.clear();
        if (missing != null) order.missing.addAll(missing);
        if (blockedNow) {
            MinecraftForge.EVENT_BUS.post(new WorkshopEvent.OrderBlocked(area, order, new ArrayList<>(order.missing)));
        }
    }

    // ── Crafting / smelting ───────────────────────────────────────────────────

    private boolean craftOnce(ServerLevel level, WorkshopArea area, CraftOrder order, BlockPos table) {
        Recipe<?> recipe = order.getRecipe(level);
        if (!(recipe instanceof CraftingRecipe craftingRecipe) || jobMatchers == null) {
            order.status = CraftOrder.Status.INVALID;
            return false;
        }

        CraftingContainer grid = CraftOrder.createCraftingContainer();
        // exact slots first, so variant slots do not use up items an exact slot needs
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < CraftOrder.GRID_SIZE; i++) {
                SlotMatcher matcher = jobMatchers[i];
                if (matcher == null || matcher.isExact() != (pass == 0)) continue;
                ItemStack taken = takeOneFromInventory(matcher);
                if (taken.isEmpty()) {
                    returnToInventory(grid);
                    return false;
                }
                grid.setItem(i, taken);
            }
        }

        if (!craftingRecipe.matches(grid, level)) {
            returnToInventory(grid);
            return false;
        }
        ItemStack result = craftingRecipe.assemble(grid, level.registryAccess());
        if (result.isEmpty() || !craftsman.canAddItem(result)) {
            returnToInventory(grid);
            return false;
        }
        NonNullList<ItemStack> remaining = craftingRecipe.getRemainingItems(grid);

        int count = result.getCount();
        handledItems.add(result.getItem());
        productItems.add(result.getItem());
        ItemStack rest = craftsman.addItem(result);
        if (!rest.isEmpty()) craftsman.spawnAtLocation(rest);

        for (ItemStack remainder : remaining) {
            if (remainder.isEmpty()) continue;
            handledItems.add(remainder.getItem());
            ItemStack left = craftsman.addItem(remainder);
            if (!left.isEmpty()) craftsman.spawnAtLocation(left);
        }

        order.produced += count;
        if (order.mode == CraftOrder.Mode.STOCK) order.stock += count;
        area.onOrdersChanged(false);

        craftsman.swing(InteractionHand.MAIN_HAND);
        if (++craftedSinceSound >= 5) {
            craftedSinceSound = 0;
            level.playSound(null, table, SoundEvents.VILLAGER_WORK_TOOLSMITH, craftsman.getSoundSource(), 0.6F, 0.9F + 0.2F * craftsman.getRandom().nextFloat());
        }
        return true;
    }

    private void loadFurnace(FurnaceBlockEntity furnace, @Nullable SlotMatcher input) {
        SimpleContainer inventory = craftsman.getInventory();
        int inputLeft = input != null ? batch : 0;
        int fuelLeft = fuelMatcher != null ? fuelCount : 0;

        for (int i = FIRST_INV_SLOT; i < inventory.getContainerSize() && inputLeft > 0; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!input.test(stack)) continue;
            inputLeft -= moveIntoFurnaceSlot(furnace, 0, stack, inputLeft);
            inventory.setItem(i, stack.isEmpty() ? ItemStack.EMPTY : stack);
        }
        if (fuelMatcher != null) {
            for (int i = FIRST_INV_SLOT; i < inventory.getContainerSize() && fuelLeft > 0; i++) {
                ItemStack stack = inventory.getItem(i);
                if (!fuelMatcher.test(stack)) continue;
                fuelLeft -= moveIntoFurnaceSlot(furnace, 1, stack, fuelLeft);
                inventory.setItem(i, stack.isEmpty() ? ItemStack.EMPTY : stack);
            }
        }
        furnace.setChanged();
        craftsman.swing(InteractionHand.MAIN_HAND);
    }

    /** Moves up to max items of the stack into the furnace slot, the stack is shrunk. */
    private int moveIntoFurnaceSlot(FurnaceBlockEntity furnace, int slot, ItemStack stack, int max) {
        ItemStack current = furnace.getItem(slot);
        if (current.isEmpty()) {
            int put = Math.min(max, Math.min(stack.getCount(), stack.getMaxStackSize()));
            furnace.setItem(slot, stack.split(put));
            return put;
        }
        if (!ItemStack.isSameItemSameTags(current, stack)) return 0;
        int put = Math.min(max, Math.min(stack.getCount(), current.getMaxStackSize() - current.getCount()));
        if (put <= 0) return 0;
        current.grow(put);
        stack.shrink(put);
        return put;
    }

    private int countPendingInFurnaces(WorkshopArea area, CraftOrder order, @Nullable SlotMatcher input, int perOutput) {
        int pending = 0;
        for (FurnaceBlockEntity furnace : area.furnaceMap.values()) {
            ItemStack in = furnace.getItem(0);
            ItemStack out = furnace.getItem(2);
            if (input != null && input.test(in)) pending += in.getCount() * perOutput;
            if (!out.isEmpty() && ItemStack.isSameItemSameTags(out, order.result)) pending += out.getCount();
        }
        return pending;
    }

    /** The smelting order a furnace output belongs to: not finished ones first. */
    @Nullable
    private CraftOrder findSmeltOrderFor(WorkshopArea area, ItemStack output) {
        CraftOrder fallback = null;
        for (CraftOrder order : area.getOrders()) {
            if (order.type != CraftOrder.OrderType.SMELTING || !ItemStack.isSameItemSameTags(order.result, output)) continue;
            if (!order.isTargetReached()) return order;
            if (fallback == null) fallback = order;
        }
        return fallback;
    }

    @Nullable
    private ItemStack chooseFuel(List<ItemStack> pool) {
        for (TagKey<Item> tag : FUEL_TAGS) {
            Map<Item, Integer> counts = new LinkedHashMap<>();
            for (ItemStack stack : pool) {
                if (stack.isEmpty() || !stack.is(tag) || getBurnTime(stack) <= 0) continue;
                counts.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
            Item best = null;
            int bestCount = 0;
            for (Map.Entry<Item, Integer> entry : counts.entrySet()) {
                if (entry.getValue() > bestCount) {
                    bestCount = entry.getValue();
                    best = entry.getKey();
                }
            }
            if (best != null) return new ItemStack(best);
        }
        return null;
    }

    private static int getBurnTime(ItemStack stack) {
        return ForgeHooks.getBurnTime(stack, RecipeType.SMELTING);
    }

    @Nullable
    private FurnaceBlockEntity getFurnace(ServerLevel level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof FurnaceBlockEntity furnace && !furnace.isRemoved()) return furnace;
        return null;
    }

    // ── Item planning helpers ─────────────────────────────────────────────────

    /** Combines grid slots with the same matcher, exact matchers first. */
    private List<Need> groupMatchers(SlotMatcher[] matchers) {
        List<Need> out = new ArrayList<>();
        for (SlotMatcher matcher : matchers) {
            if (matcher == null) continue;
            Need existing = null;
            for (Need need : out) {
                if (need.matcher.isSame(matcher)) {
                    existing = need;
                    break;
                }
            }
            if (existing != null) existing.amount++;
            else out.add(new Need(matcher, 1));
        }
        out.sort(Comparator.comparing(need -> !need.matcher.isExact()));
        return out;
    }

    private int stacksFor(List<Need> perCraft, int crafts, ItemStack result, int perOutput) {
        int stacks = 0;
        for (Need need : perCraft) {
            int max = Math.max(1, need.matcher.display.getMaxStackSize());
            stacks += (need.amount * crafts + max - 1) / max;
        }
        int resultMax = Math.max(1, result.getMaxStackSize());
        stacks += (perOutput * crafts + resultMax - 1) / resultMax;
        return stacks;
    }

    /** Deficit per need when taking needs x crafts from a copy of the chest contents. */
    private int[] allocate(List<Need> perCraft, List<ItemStack> snapshot, int crafts) {
        List<ItemStack> pool = copyPool(snapshot);
        int[] deficits = new int[perCraft.size()];
        for (int i = 0; i < perCraft.size(); i++) {
            Need need = perCraft.get(i);
            int required = need.amount * crafts;
            deficits[i] = required - takeFromPool(pool, need.matcher, required);
        }
        return deficits;
    }

    private static int missingTotal(CraftOrder order) {
        int total = 0;
        for (ItemStack stack : order.missing) total += stack.getCount();
        return total;
    }

    private static int totalDeficit(int[] deficits) {
        int total = 0;
        for (int deficit : deficits) total += deficit;
        return total;
    }

    private List<ItemStack> missingFor(List<Need> perCraft, int[] deficits) {
        List<ItemStack> missing = new ArrayList<>();
        for (int i = 0; i < perCraft.size(); i++) {
            if (deficits[i] > 0) missing.add(perCraft.get(i).matcher.display.copyWithCount(deficits[i]));
        }
        return missing;
    }

    private static List<ItemStack> copyPool(List<ItemStack> snapshot) {
        List<ItemStack> pool = new ArrayList<>(snapshot.size());
        for (ItemStack stack : snapshot) pool.add(stack.copy());
        return pool;
    }

    private static int takeFromPool(List<ItemStack> pool, SlotMatcher matcher, int amount) {
        int taken = 0;
        for (ItemStack stack : pool) {
            if (taken >= amount) break;
            if (stack.isEmpty() || !matcher.test(stack)) continue;
            int take = Math.min(amount - taken, stack.getCount());
            stack.shrink(take);
            taken += take;
        }
        return taken;
    }

    // ── Inventory helpers ─────────────────────────────────────────────────────

    private void takeNeeds(Container container) {
        boolean changed = false;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack slot = container.getItem(i);
            if (slot.isEmpty()) continue;
            for (Need need : needs) {
                if (slot.isEmpty()) break;
                if (need.amount <= 0 || !need.matcher.test(slot)) continue;

                int take = Math.min(need.amount, slot.getCount());
                ItemStack moving = slot.copyWithCount(take);
                if (!craftsman.canAddItem(moving)) continue;
                Item item = slot.getItem();
                ItemStack rest = craftsman.addItem(moving);
                int added = take - rest.getCount();
                if (added <= 0) continue;

                handledItems.add(item);
                slot.shrink(added);
                need.amount -= added;
                changed = true;
            }
            if (slot.isEmpty()) container.setItem(i, ItemStack.EMPTY);
        }
        if (changed) container.setChanged();
    }

    private boolean allNeedsMet() {
        for (Need need : needs) {
            if (need.amount > 0) return false;
        }
        return true;
    }

    private ItemStack takeOneFromInventory(SlotMatcher matcher) {
        SimpleContainer inventory = craftsman.getInventory();
        for (int i = FIRST_INV_SLOT; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!matcher.test(stack)) continue;
            ItemStack one = stack.split(1);
            inventory.setItem(i, stack.isEmpty() ? ItemStack.EMPTY : stack);
            return one;
        }
        return ItemStack.EMPTY;
    }

    private void returnToInventory(CraftingContainer grid) {
        for (int i = 0; i < grid.getContainerSize(); i++) {
            ItemStack stack = grid.removeItemNoUpdate(i);
            if (stack.isEmpty()) continue;
            ItemStack left = craftsman.addItem(stack);
            if (!left.isEmpty()) craftsman.spawnAtLocation(left);
        }
    }

    private int countCarried(SlotMatcher matcher) {
        int count = 0;
        SimpleContainer inventory = craftsman.getInventory();
        for (int i = FIRST_INV_SLOT; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (matcher.test(stack)) count += stack.getCount();
        }
        return count;
    }

    private int countFreeInventorySlots() {
        int free = 0;
        SimpleContainer inventory = craftsman.getInventory();
        for (int i = FIRST_INV_SLOT; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).isEmpty()) free++;
        }
        return free;
    }

    private boolean shouldDeposit(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return !craftsman.wantsToKeep(stack) || handledItems.contains(stack.getItem());
    }

    private boolean hasItemsToDeposit() {
        WorkshopArea area = craftsman.currentWorkshopArea;
        SimpleContainer inventory = craftsman.getInventory();
        for (int i = FIRST_INV_SLOT; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (shouldDeposit(stack) && !isDeliverable(area, stack)) return true;
        }
        return false;
    }

    // ── Storage helpers ───────────────────────────────────────────────────────

    /** Storages near the workshop that allow craftsmen, nearest first. None if the workshop does not use storages. */
    private void refreshStorages(ServerLevel level, WorkshopArea area) {
        storages.clear();
        refreshIngredients(level, area);
        if (!area.getUseStorage()) return;
        List<StorageArea> found = level.getEntitiesOfClass(StorageArea.class, new AABB(area.blockPosition()).inflate(WorkshopArea.STORAGE_RANGE),
                storage -> storage.isAlive() && storage.canWorkHere(craftsman));
        found.sort(Comparator.comparingDouble(storage -> storage.distanceToSqr(area)));
        for (StorageArea storage : found) {
            storage.scanStorageBlocks();
            if (!storage.storageMap.isEmpty()) storages.add(storage);
        }
    }

    /** Ingredients of the orders that still want to produce, based on their last status. */
    private void refreshIngredients(ServerLevel level, WorkshopArea area) {
        orderIngredients.clear();
        for (CraftOrder order : area.getOrders()) {
            if (!isActive(order)) continue;
            for (SlotMatcher matcher : order.buildSlotMatchers(level)) {
                if (matcher != null) orderIngredients.add(matcher);
            }
        }
    }

    /** An order that still wants to produce. Done, full, disabled or invalid orders do not need their ingredients. */
    private static boolean isActive(CraftOrder order) {
        if (!order.enabled) return false;
        return switch (order.status) {
            case OK, MISSING_ITEMS, FETCHING, NO_SPACE -> true;
            default -> false;
        };
    }

    /**
     * Collects items from the workshop chests that no active order needs (products and ingredients of the orders)
     * and brings them to the storage. Coal always stays, other fuel stays while something is smelted.
     */
    private boolean startTidy(ServerLevel level, WorkshopArea area) {
        if (storages.isEmpty() || level.getGameTime() < tidyBlockedUntil) return false;
        int free = countFreeInventorySlots();
        if (free <= 0) return false;

        List<SlotMatcher> related = new ArrayList<>();
        boolean smelting = false;
        for (CraftOrder order : area.getOrders()) {
            for (SlotMatcher matcher : order.buildSlotMatchers(level)) {
                if (matcher != null) related.add(matcher);
            }
            if (isActive(order) && order.type == CraftOrder.OrderType.SMELTING) smelting = true;
        }

        List<Need> take = new ArrayList<>();
        int stacks = 0;
        for (ItemStack stack : area.snapshotContents()) {
            if (stacks >= free) break;
            if (!isTidyItem(area, stack, related, smelting)) continue;
            Need existing = null;
            for (Need need : take) {
                if (need.matcher.test(stack)) {
                    existing = need;
                    break;
                }
            }
            if (existing != null) existing.amount += stack.getCount();
            else take.add(new Need(SlotMatcher.exact(stack), stack.getCount()));
            stacks++;
        }
        if (take.isEmpty()) return false;

        clearJob();
        needs.addAll(take);
        tidying = true;
        chestQueue.addAll(area.findContainersWith(stack -> take.stream().anyMatch(need -> need.matcher.test(stack))));
        chestQueue.sort(Comparator.comparingDouble(pos -> pos.distToCenterSqr(craftsman.position())));
        interactTimer = 0;
        setState(State.COLLECT);
        return true;
    }

    private boolean isTidyItem(WorkshopArea area, ItemStack stack, List<SlotMatcher> related, boolean smelting) {
        if (stack.isEmpty() || stack.is(ItemTags.COALS)) return false;
        if (smelting && isFuel(stack)) return false;
        for (SlotMatcher matcher : orderIngredients) {
            if (matcher.test(stack)) return false;
        }
        for (CraftOrder order : area.getOrders()) {
            if (ItemStack.isSameItemSameTags(order.result, stack)) return true;
        }
        for (SlotMatcher matcher : related) {
            if (matcher.test(stack)) return true;
        }
        // items the orders have nothing to do with belong to the player and stay
        return false;
    }

    private static boolean isFuel(ItemStack stack) {
        for (TagKey<Item> tag : FUEL_TAGS) {
            if (stack.is(tag)) return true;
        }
        return false;
    }

    private int countInStorages(java.util.function.Predicate<ItemStack> predicate) {
        int total = 0;
        for (StorageArea storage : storages) {
            for (Container container : storage.storageMap.values()) {
                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack stack = container.getItem(i);
                    if (!stack.isEmpty() && predicate.test(stack)) total += stack.getCount();
                }
            }
        }
        return total;
    }

    private void addMissingNeed(CraftOrder order, SlotMatcher matcher, int amount) {
        if (amount <= 0) return;
        missingNeeds.computeIfAbsent(order.uuid, id -> new ArrayList<>()).add(new Need(matcher, amount));
    }

    /** Goes to the nearest storage that has some of the missing items. Only takes what is there and what he can carry. */
    private boolean startFetch(WorkshopArea area, CraftOrder order, List<Need> missing) {
        int free = countFreeInventorySlots();
        if (free <= 0) return false;

        for (StorageArea storage : storages) {
            List<ItemStack> pool = new ArrayList<>();
            for (Container container : storage.storageMap.values()) {
                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack stack = container.getItem(i);
                    if (!stack.isEmpty()) pool.add(stack.copy());
                }
            }

            List<Need> take = new ArrayList<>();
            int stacks = 0;
            for (Need need : missing) {
                int max = Math.max(1, need.matcher.display.getMaxStackSize());
                int room = free - stacks;
                if (room <= 0) break;
                int amount = takeFromPool(pool, need.matcher, Math.min(need.amount, room * max));
                if (amount <= 0) continue;
                take.add(new Need(need.matcher, amount));
                stacks += (amount + max - 1) / max;
            }
            if (take.isEmpty()) continue;

            clearJob();
            needs.addAll(take);
            fetchStorage = storage;
            currentOrderId = order.uuid;
            for (Map.Entry<BlockPos, Container> entry : storage.storageMap.entrySet()) {
                Container container = entry.getValue();
                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack stack = container.getItem(i);
                    if (take.stream().anyMatch(need -> need.matcher.test(stack))) {
                        chestQueue.add(entry.getKey());
                        break;
                    }
                }
            }
            chestQueue.sort(Comparator.comparingDouble(pos -> pos.distToCenterSqr(craftsman.position())));
            interactTimer = 0;
            setState(State.COLLECT);
            return true;
        }
        return false;
    }

    /**
     * A product goes to the storage, unless it is needed as an ingredient by an order of this workshop
     * (then it stays here) or no storage could take it.
     */
    private boolean isDeliverable(@Nullable WorkshopArea area, ItemStack stack) {
        if (stack.isEmpty() || area == null || deliveryFailed || storages.isEmpty() || !area.getUseStorage()) return false;
        if (!productItems.contains(stack.getItem())) return false;
        for (SlotMatcher matcher : orderIngredients) {
            if (matcher.test(stack)) return false;
        }
        return true;
    }

    private boolean hasProductsToDeliver(WorkshopArea area) {
        SimpleContainer inventory = craftsman.getInventory();
        for (int i = FIRST_INV_SLOT; i < inventory.getContainerSize(); i++) {
            if (isDeliverable(area, inventory.getItem(i))) return true;
        }
        return false;
    }

    /** A storage chest for one of the carried products: chests that already hold the item first. */
    private boolean findDeliverTarget(ServerLevel level, WorkshopArea area) {
        refreshStorages(level, area);
        SimpleContainer inventory = craftsman.getInventory();
        for (StorageArea storage : storages) {
            BlockPos fallback = null;
            for (int i = FIRST_INV_SLOT; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                if (!isDeliverable(area, stack)) continue;
                for (Map.Entry<BlockPos, Container> entry : storage.storageMap.entrySet()) {
                    Container container = entry.getValue();
                    if (WorkshopArea.getSpaceFor(container, stack) <= 0) continue;
                    for (int j = 0; j < container.getContainerSize(); j++) {
                        if (ItemStack.isSameItemSameTags(container.getItem(j), stack)) {
                            deliverStorage = storage;
                            deliverPos = entry.getKey();
                            return true;
                        }
                    }
                    if (fallback == null) fallback = entry.getKey();
                }
            }
            if (fallback != null) {
                deliverStorage = storage;
                deliverPos = fallback;
                return true;
            }
        }
        return false;
    }

    /** A container that can take one of the carried stacks, containers already holding the item first. */
    @Nullable
    private BlockPos findDepositContainer(WorkshopArea area) {
        SimpleContainer inventory = craftsman.getInventory();
        BlockPos fallback = null;
        for (int i = FIRST_INV_SLOT; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!shouldDeposit(stack) || isDeliverable(area, stack)) continue;
            for (Map.Entry<BlockPos, Container> entry : area.containerMap.entrySet()) {
                Container container = entry.getValue();
                if (WorkshopArea.getSpaceFor(container, stack) <= 0) continue;
                for (int j = 0; j < container.getContainerSize(); j++) {
                    if (ItemStack.isSameItemSameTags(container.getItem(j), stack)) return entry.getKey();
                }
                if (fallback == null) fallback = entry.getKey();
            }
        }
        return fallback;
    }

    private void startDeposit(ServerLevel level) {
        closeContainer(level);
        needs.clear();
        chestQueue.clear();
        currentOrderId = null;
        jobMatchers = null;
        fuelMatcher = null;
        batch = 0;
        depositPos = null;
        interactTimer = 0;
        setState(State.DEPOSIT);
    }

    private void clearJob() {
        needs.clear();
        chestQueue.clear();
        currentOrderId = null;
        jobMatchers = null;
        fuelMatcher = null;
        fuelCount = 0;
        furnacePos = null;
        depositPos = null;
        fetchStorage = null;
        deliverStorage = null;
        deliverPos = null;
        tidying = false;
        batch = 0;
        interactTimer = 0;
        craftTimer = 0;
    }

    @Nullable
    private CraftOrder getCurrentOrder(WorkshopArea area) {
        if (currentOrderId == null || area == null) return null;
        return area.getOrder(currentOrderId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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
            level.playSound(null, pos, SoundEvents.CHEST_OPEN, craftsman.getSoundSource(), 0.7F, 0.8F + 0.4F * craftsman.getRandom().nextFloat());
            data.putBoolean("isOpened", true);
        }
        else if (!open && isOpened) {
            level.blockEvent(pos, block, 1, 0);
            level.playSound(null, pos, SoundEvents.CHEST_CLOSE, craftsman.getSoundSource(), 0.7F, 0.8F + 0.4F * craftsman.getRandom().nextFloat());
            data.putBoolean("isOpened", false);
        }
    }

    private void leaveCurrentArea() {
        clearJob();
        if (craftsman.currentWorkshopArea != null) {
            craftsman.currentWorkshopArea.setBeingWorkedOn(false);
            craftsman.currentWorkshopArea.setCraftsmanName("None");
            craftsman.currentWorkshopArea = null;
        }
    }

    private boolean isCurrentAreaGone() {
        return craftsman.currentWorkshopArea == null || craftsman.currentWorkshopArea.isRemoved();
    }

    /** Walks to the position. Gives up after MAX_MOVE_TICKS so an unreachable block can not block the craftsman. */
    private boolean moveToPosition(BlockPos pos, int thresholdBlocks) {
        if (!pos.equals(moveTarget)) {
            moveTarget = pos;
            moveTicks = 0;
        }
        double dist = craftsman.getHorizontalDistanceTo(pos.getCenter());
        if (dist < thresholdBlocks) {
            moveTicks = 0;
            craftsman.getNavigation().stop();
            return false;
        }
        if (++moveTicks > MAX_MOVE_TICKS) {
            craftsman.getNavigation().stop();
            return false;
        }
        // Note: setFollowState is NOT called here — start() already set it to 6.
        craftsman.getNavigation().moveTo(pos.getX(), pos.getY(), pos.getZ(), 0.8F);
        craftsman.getLookControl().setLookAt(pos.getCenter());
        return true;
    }

    @Nullable
    private WorkshopArea findBestArea(ServerLevel level) {
        List<WorkshopArea> areas = level.getEntitiesOfClass(
                WorkshopArea.class, craftsman.getBoundingBox().inflate(64));

        WorkshopArea best = null;
        int bestScore    = -1;

        for (WorkshopArea area : areas) {
            if (area == null) continue;
            if (!area.canWorkHere(craftsman)) continue;
            if (area.isBeingWorkedOn()) continue;

            int score = area.getTime() * 10;
            if (score > bestScore) {
                bestScore = score;
                best = area;
            }
        }
        return best;
    }

    private void setState(State s) {
        this.state = s;
        this.moveTarget = null;
    }

    /** Items of one kind the craftsman still has to fetch. */
    private static class Need {
        final SlotMatcher matcher;
        int amount;

        Need(SlotMatcher matcher, int amount) {
            this.matcher = matcher;
            this.amount = amount;
        }
    }

    private static class Job {
        final CraftOrder order;
        final int batch;
        final SlotMatcher[] matchers;
        final List<Need> needs = new ArrayList<>();
        @Nullable BlockPos furnacePos;
        @Nullable SlotMatcher fuel;
        int fuelAmount;

        Job(CraftOrder order, int batch, SlotMatcher[] matchers) {
            this.order = order;
            this.batch = batch;
            this.matchers = matchers;
        }
    }

    public enum State {
        SELECT_WORK_AREA,
        WALK_TO_CENTER,
        IDLE,
        COLLECT,
        CRAFT,
        LOAD_FURNACE,
        COLLECT_FURNACE,
        DEPOSIT,
        DELIVER
    }
}
