package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.CookEntity;
import com.talhanation.workers.entities.workarea.KitchenArea;
import com.talhanation.workers.entities.workarea.StorageArea;
import com.talhanation.workers.entities.workarea.WorkshopArea;
import com.talhanation.workers.world.CraftOrder;
import com.talhanation.workers.world.VillagerInviteRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.crafting.IShapedRecipe;

import javax.annotation.Nullable;
import java.util.*;

public class CookWorkGoal extends Goal {

    private static final int VILLAGER_SCAN_INTERVAL = 100;
    private static final int VILLAGER_TIMEOUT       = 600;
    private static final int FURNACE_CHECK_INTERVAL = 120;
    private static final int STORE_INTERVAL         = 180;
    private static final int INTERACT_TIME          = 20; // ~1 second spent at each furnace
    private static final int FETCH_CHECK_INTERVAL   = 600; // how often an empty kitchen looks into the storages
    private static final int MAX_FETCH_STACKS       = 4;
    private static final int MAX_MOVE_TICKS         = 600;
    private static final int DELIVER_CHECK_INTERVAL = 600; // how often cooked food is brought to a storage (feeding off)
    private static final int FURNACE_BATCH          = 8;   // raw food put into a furnace per visit (1 coal)
    private static final int CRAFT_CHECK_INTERVAL   = 1200; // how often the storages are checked for kitchen recipes
    private static final int CRAFT_TIME             = 80;  // ticks per craft, slow because there is no crafting table
    private static final int MAX_CRAFTS_PER_RUN     = 16;
    private static final int STORAGE_SHARE          = 4;   // the cook takes at most 1/4 of each ingredient in the storages

    /** Fuel the cook uses, strictly in this order (same as the craftsman). */
    private static final List<TagKey<Item>> FUEL_TAGS = List.of(ItemTags.COALS, ItemTags.PLANKS, ItemTags.LOGS_THAT_BURN);
    /** Edible results of smelting recipes, built once per goal. */
    @Nullable private Set<Item> cookedFoods;

    // Minimum cooked food in containers before inviting villagers
    private static final int MIN_FOOD_TO_SELL = 1;

    private final CookEntity cook;
    private State state;
    private int cooldown;
    private int villagerScanCooldown;
    private int furnaceCheckCooldown;
    private int storeCooldown;

    // ── Per-furnace task state (realistic, one furnace at a time) ──────────────
    private final List<BlockPos> furnaceQueue = new ArrayList<>();
    @Nullable private BlockPos currentFurnace;
    @Nullable private BlockPos openContainerPos;
    private int interactTimer;

    // ── Fetching raw food / fuel from a storage ──────────────────────────────
    private int fetchCooldown;
    @Nullable private StorageArea fetchStorage;
    private final List<BlockPos> fetchQueue = new ArrayList<>();
    private int wantedFood;
    private int wantedFuel;
    /** Items the cook brought from a storage: they go into the kitchen containers. */
    private final Set<Item> fetchedItems = new HashSet<>();
    @Nullable private BlockPos moveTarget;
    private int moveTicks;

    // ── Bringing cooked food to a storage ────────────────────────────────────
    private int deliverCooldown;
    private final List<StorageArea> deliverStorages = new ArrayList<>();
    @Nullable private StorageArea deliverStorage;
    @Nullable private BlockPos deliverPos;
    /** Cooked food the cook carries to a storage. */
    private final Set<Item> deliverItems = new HashSet<>();

    // ── Crafting simple food (bread, cookies, stews...) without a crafting table ──
    /** Crafting recipes with an edible result and only kitchen ingredients, built once per goal. */
    @Nullable private List<CraftingRecipe> kitchenRecipes;
    private int craftCooldown;
    private final Map<Item, Integer> craftShopping = new HashMap<>();
    private final List<BlockPos> craftFetchQueue = new ArrayList<>();
    private final Map<BlockPos, Container> craftFetchContainers = new HashMap<>();
    private final Set<Item> craftedItems = new HashSet<>();
    @Nullable private CraftingRecipe currentCraft;
    private int craftTimer;
    private int craftsLeft;
    private int craftRecipeIndex;

    public CookWorkGoal(CookEntity cook) {
        this.cook = cook;
        setFlags(EnumSet.of(Flag.LOOK, Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return cook.shouldWork() && !cook.needsToGetToChest() && isAreaNotRemoved();
    }

    private boolean isAreaNotRemoved() {
        if (cook.currentKitchenArea == null || !cook.currentKitchenArea.isRemoved()) return true;
        cook.currentKitchenArea = null;
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
        cook.setFollowState(6);
        setState(State.SELECT_WORK_AREA);
    }

    @Override
    public void stop() {
        // Close any chest we left open and drop the current furnace task.
        if (openContainerPos != null && cook.getCommandSenderWorld() instanceof ServerLevel sl) {
            interactContainer(sl, openContainerPos, false);
        }
        clearFurnaceTask();
        furnaceQueue.clear();

        if (cook.currentKitchenArea != null) {
            cook.currentKitchenArea.setBeingWorkedOn(false);
            cook.currentKitchenArea.setCookName("None");
            cook.currentKitchenArea = null;
        }
        cook.clearVillagerTrade();
        cook.getNavigation().stop();

        // Only reset to wander if we are still in the working state.
        // If the owner changed the follow state (e.g. follow = 2),
        // do NOT override that command back to wander.
        if (cook.getFollowState() == 6) {
            cook.setFollowState(0);
        }
    }

    @Override
    public void tick() {
        if (cook.getCommandSenderWorld().isClientSide()) return;
        if (state == null) return;

        if (state != State.SELECT_WORK_AREA && isCurrentAreaGone()) {
            leaveCurrentArea();
            setState(State.SELECT_WORK_AREA);
            return;
        }

        // aiStep frees the kitchen while the cook is far away (e.g. at a storage), take it back when he returns
        KitchenArea current = cook.currentKitchenArea;
        if (current != null && state != State.SELECT_WORK_AREA && !current.isBeingWorkedOn()
                && cook.getHorizontalDistanceTo(current.position()) < 1000) {
            current.setBeingWorkedOn(true);
        }

        switch (state) {
            case SELECT_WORK_AREA -> tickSelectWorkArea((ServerLevel) cook.getCommandSenderWorld());
            case WALK_TO_CENTER   -> tickWalkToCenter();
            case WORKING          -> tickWorking((ServerLevel) cook.getCommandSenderWorld());
            case FETCH            -> tickFetch((ServerLevel) cook.getCommandSenderWorld());
            case STORE_FETCHED    -> tickStoreFetched((ServerLevel) cook.getCommandSenderWorld());
            case DELIVER          -> tickDeliver((ServerLevel) cook.getCommandSenderWorld());
            case CRAFT_FETCH      -> tickCraftFetch((ServerLevel) cook.getCommandSenderWorld());
            case CRAFTING         -> tickCrafting((ServerLevel) cook.getCommandSenderWorld());
        }
    }

    // ── States ────────────────────────────────────────────────────────────────

    private void tickSelectWorkArea(ServerLevel level) {
        if (cook.currentKitchenArea != null) {
            setState(State.WALK_TO_CENTER);
            return;
        }

        if (++cooldown < cook.getRandom().nextInt(200)) return;
        cooldown = 0;

        KitchenArea found = findBestArea(level);
        if (found == null) return;

        if (!found.hasMinimumSetup()) return;

        cook.currentKitchenArea = found;
        found.setBeingWorkedOn(true);
        found.setCookName(cook.getName().getString());
        found.setTime(0);

        setState(State.WALK_TO_CENTER);
    }

    private void tickWalkToCenter() {
        if (moveToPosition(BlockPos.containing(cook.currentKitchenArea.getArea().getCenter()), 3)) return;
        cook.getNavigation().stop();
        setState(State.WORKING);
    }

    private void tickWorking(ServerLevel level) {
        KitchenArea area = cook.currentKitchenArea;
        if (area == null) return;

        // ── Villager trading runs alongside furnace work ──────────────────────
        if (area.getFeedVillagers()) {
            tickVillagerTrading(level);
        }

        // ── Already servicing a furnace? Advance that interaction ─────────────
        if (currentFurnace != null) {
            advanceFurnaceTask(level, area);
            return;
        }

        // ── Items from a storage that are still carried go into the kitchen first ─
        if (carriesFetchedItems()) {
            interactTimer = 0;
            setState(State.STORE_FETCHED);
            return;
        }
        if (carriesDeliverItems()) {
            interactTimer = 0;
            setState(State.DELIVER);
            return;
        }

        // ── Otherwise pick the next furnace, rebuilding the queue periodically ─
        if (furnaceQueue.isEmpty()) {
            cook.getNavigation().stop();
            if (++furnaceCheckCooldown < FURNACE_CHECK_INTERVAL) return;
            furnaceCheckCooldown = 0;

            area.pruneRemovedFurnaces();
            area.pruneRemovedContainers();
            // Re-scan keeps the furnace/container maps and the GUI slot counts fresh.
            area.scanArea();

            // Kitchen ran out of raw food or fuel: fetch some from a nearby storage
            if (fetchCooldown > 0) fetchCooldown -= FURNACE_CHECK_INTERVAL;
            if (fetchCooldown <= 0 && area.getUseStorage() && tryStartFetch(level, area)) return;

            // Simple food (bread, cookies, stews...): a quarter of the ingredients from the storages, crafted by hand
            if (craftCooldown > 0) craftCooldown -= FURNACE_CHECK_INTERVAL;
            if (craftCooldown <= 0 && area.getUseStorage() && tryStartCraft(level, area)) return;

            // Feeding is off: cooked food goes to a nearby storage (upkeep for recruits and workers)
            if (deliverCooldown > 0) deliverCooldown -= FURNACE_CHECK_INTERVAL;
            if (deliverCooldown <= 0 && area.getUseStorage() && !area.getFeedVillagers() && tryStartDeliver(level, area)) return;

            furnaceQueue.addAll(area.furnaceMap.keySet());
            if (furnaceQueue.isEmpty()) return;
        }

        currentFurnace   = furnaceQueue.remove(0);
        openContainerPos = null;
        interactTimer    = 0;
    }

    // Walk to the furnace, face it, open a nearby chest, wait ~1s, then transfer items.
    private void advanceFurnaceTask(ServerLevel level, KitchenArea area) {
        AbstractFurnaceBlockEntity furnace = area.furnaceMap.get(currentFurnace);
        if (furnace == null || furnace.isRemoved()) {
            finishFurnaceTask(level);
            return;
        }

        // Move next to the furnace and look at it.
        if (moveToPosition(currentFurnace, 3)) return;
        cook.getNavigation().stop();
        cook.getLookControl().setLookAt(currentFurnace.getCenter());

        // On arrival, open the nearest chest so the player sees the cook "fetching".
        if (interactTimer == 0) {
            openContainerPos = findNearestContainer(area, currentFurnace);
            interactContainer(level, openContainerPos, true);
        }

        // Spend ~1 second working before actually moving items.
        if (interactTimer < INTERACT_TIME) {
            interactTimer++;
            if (interactTimer % 5 == 0) cook.swing(InteractionHand.MAIN_HAND);
            return;
        }

        serviceFurnace(level, area, furnace);
        finishFurnaceTask(level);
    }

    private void serviceFurnace(ServerLevel level, KitchenArea area, AbstractFurnaceBlockEntity furnace) {
        // Unload finished output into containers
        ItemStack output = furnace.getItem(2);
        if (!output.isEmpty()) {
            if (area.depositItemToContainers(output, output.getCount())) {
                furnace.setItem(2, ItemStack.EMPTY);
                furnace.setChanged();
            }
        }

        // Load fuel and ingredient if the corresponding slots are empty
        if (furnace.getItem(1).isEmpty()) loadFuel(area, furnace);
        if (furnace.getItem(0).isEmpty()) loadIngredient(area, furnace, level);

        cook.swing(InteractionHand.MAIN_HAND);
    }

    // ── Storage fetching ──────────────────────────────────────────────────────

    /**
     * Checks without walking whether the kitchen lacks raw food or fuel and a storage (with the cook
     * checkbox, within range of the kitchen) has some. If so, the cook goes there and takes it.
     */
    private boolean tryStartFetch(ServerLevel level, KitchenArea area) {
        fetchCooldown = FETCH_CHECK_INTERVAL;

        boolean freeInput = false;
        boolean freeFuel = false;
        for (AbstractFurnaceBlockEntity furnace : area.furnaceMap.values()) {
            if (furnace.getItem(0).isEmpty()) freeInput = true;
            if (furnace.getItem(1).isEmpty()) freeFuel = true;
        }

        boolean needFood = freeInput && !kitchenHas(area, stack -> isSmeltableFood(stack, level));
        boolean needFuel = freeFuel && !kitchenHas(area, this::isKitchenFuel);
        if (!needFood && !needFuel) return false;

        int room = Math.min(MAX_FETCH_STACKS, Math.min(countFreeInventorySlots(), area.getFreeSlots()));
        if (room <= 0) return false;

        for (StorageArea storage : findStorages(level, area)) {
            List<BlockPos> queue = new ArrayList<>();
            boolean hasFood = false;
            boolean hasFuel = false;
            for (Map.Entry<BlockPos, Container> entry : storage.storageMap.entrySet()) {
                boolean useful = false;
                Container container = entry.getValue();
                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack stack = container.getItem(i);
                    if (stack.isEmpty()) continue;
                    if (needFood && isSmeltableFood(stack, level)) { hasFood = true; useful = true; }
                    if (needFuel && isFuel(stack)) { hasFuel = true; useful = true; }
                }
                if (useful) queue.add(entry.getKey());
            }
            if (queue.isEmpty()) continue;

            // food gets all stacks but one, fuel one stack
            this.wantedFuel = hasFuel ? 1 : 0;
            this.wantedFood = hasFood ? Math.max(1, room - wantedFuel) : 0;
            this.fetchStorage = storage;
            this.fetchQueue.clear();
            this.fetchQueue.addAll(queue);
            this.fetchQueue.sort(Comparator.comparingDouble(pos -> pos.distToCenterSqr(cook.position())));
            this.interactTimer = 0;
            setState(State.FETCH);
            return true;
        }
        return false;
    }

    private void tickFetch(ServerLevel level) {
        if (fetchStorage == null || fetchStorage.isRemoved() || fetchQueue.isEmpty() || (wantedFood <= 0 && wantedFuel <= 0)) {
            closeOpenContainer(level);
            fetchStorage = null;
            fetchQueue.clear();
            interactTimer = 0;
            setState(State.STORE_FETCHED);
            return;
        }

        BlockPos pos = fetchQueue.get(0);
        if (moveToPositionTimed(pos, 3)) return;
        cook.getNavigation().stop();
        cook.getLookControl().setLookAt(pos.getCenter());

        if (interactTimer == 0) {
            openContainerPos = pos;
            interactContainer(level, pos, true);
        }
        if (interactTimer < INTERACT_TIME) {
            interactTimer++;
            if (interactTimer % 5 == 0) cook.swing(InteractionHand.MAIN_HAND);
            return;
        }

        Container container = fetchStorage.storageMap.get(pos);
        if (container != null) {
            boolean changed = false;
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack stack = container.getItem(i);
                if (stack.isEmpty()) continue;
                boolean food = wantedFood > 0 && isSmeltableFood(stack, level);
                boolean fuel = !food && wantedFuel > 0 && isFuel(stack);
                if (!food && !fuel) continue;

                ItemStack moving = stack.copy();
                if (!cook.canAddItem(moving)) break;
                Item item = stack.getItem();
                int before = moving.getCount();
                ItemStack rest = cook.addItem(moving);
                int taken = before - rest.getCount();
                if (taken <= 0) continue;

                fetchedItems.add(item);
                stack.shrink(taken);
                if (stack.isEmpty()) container.setItem(i, ItemStack.EMPTY);
                changed = true;
                if (food) wantedFood--;
                else wantedFuel--;
                if (wantedFood <= 0 && wantedFuel <= 0) break;
            }
            if (changed) container.setChanged();
        }

        closeOpenContainer(level);
        fetchQueue.remove(0);
        interactTimer = 0;
    }

    /** Puts the items from the storage into the kitchen containers, then back to the furnaces. */
    private void tickStoreFetched(ServerLevel level) {
        KitchenArea area = cook.currentKitchenArea;
        if (area == null) return;

        BlockPos target = findNearestContainer(area, cook.blockPosition());
        if (target == null || !carriesFetchedItems()) {
            fetchedItems.clear();
            setState(State.WORKING);
            return;
        }

        if (moveToPositionTimed(target, 3)) return;
        cook.getNavigation().stop();
        cook.getLookControl().setLookAt(target.getCenter());

        if (interactTimer == 0) {
            openContainerPos = target;
            interactContainer(level, target, true);
        }
        if (interactTimer < INTERACT_TIME) {
            interactTimer++;
            if (interactTimer % 5 == 0) cook.swing(InteractionHand.MAIN_HAND);
            return;
        }

        SimpleContainer inventory = cook.getInventory();
        for (int i = 6; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty() || !fetchedItems.contains(stack.getItem())) continue;
            if (area.depositItemToContainers(stack, stack.getCount())) {
                inventory.setItem(i, ItemStack.EMPTY);
            }
        }

        closeOpenContainer(level);
        // whatever did not fit stays with the cook and is used later, no endless retry
        fetchedItems.clear();
        interactTimer = 0;
        furnaceCheckCooldown = FURNACE_CHECK_INTERVAL;
        setState(State.WORKING);
    }

    /** Storages with the cook checkbox within range of the kitchen, nearest first, already scanned. */
    private List<StorageArea> findStorages(ServerLevel level, KitchenArea area) {
        List<StorageArea> storages = level.getEntitiesOfClass(StorageArea.class,
                new AABB(area.blockPosition()).inflate(KitchenArea.STORAGE_RANGE),
                storage -> storage.isAlive() && storage.canWorkHere(cook));
        storages.sort(Comparator.comparingDouble(storage -> storage.distanceToSqr(area)));
        storages.forEach(StorageArea::scanStorageBlocks);
        storages.removeIf(storage -> storage.storageMap.isEmpty());
        return storages;
    }

    // ── Crafting simple food ──────────────────────────────────────────────────

    /**
     * Plans which kitchen recipes can be made from what the cook carries plus a quarter of the
     * ingredients in the storages. If something can be made he fetches the missing items, otherwise false.
     */
    private boolean tryStartCraft(ServerLevel level, KitchenArea area) {
        craftCooldown = CRAFT_CHECK_INTERVAL;

        List<CraftingRecipe> recipes = getKitchenRecipes(level);
        if (recipes.isEmpty()) return false;

        // two slots stay free for the results
        int room = countFreeInventorySlots() - 2;

        // what the cook carries can be used completely, from the storages only a quarter
        Map<Item, Integer> carried = new HashMap<>();
        SimpleContainer inventory = cook.getInventory();
        for (int i = 6; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && isKitchenIngredient(stack)) carried.merge(stack.getItem(), stack.getCount(), Integer::sum);
        }

        List<StorageArea> storages = findStorages(level, area);
        Map<Item, Integer> stored = new HashMap<>();
        for (StorageArea storage : storages) {
            for (Container container : storage.storageMap.values()) {
                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack stack = container.getItem(i);
                    if (!stack.isEmpty() && isKitchenIngredient(stack)) stored.merge(stack.getItem(), stack.getCount(), Integer::sum);
                }
            }
        }
        stored.replaceAll((item, count) -> count / STORAGE_SHARE);

        // round robin over the recipes, so he makes a bit of everything
        Map<Item, Integer> shopping = new HashMap<>();
        int crafts = 0;
        boolean progress = true;
        while (progress && crafts < MAX_CRAFTS_PER_RUN) {
            progress = false;
            for (CraftingRecipe recipe : recipes) {
                if (crafts >= MAX_CRAFTS_PER_RUN) break;

                Map<Item, Integer> carriedCopy = new HashMap<>(carried);
                Map<Item, Integer> storedCopy = new HashMap<>(stored);
                Map<Item, Integer> shoppingCopy = new HashMap<>(shopping);
                if (!reserveIngredients(recipe, carriedCopy, storedCopy, shoppingCopy)) continue;
                if (countStacks(shoppingCopy) > room) continue;

                carried = carriedCopy;
                stored = storedCopy;
                shopping = shoppingCopy;
                crafts++;
                progress = true;
            }
        }
        if (crafts == 0) return false;

        craftsLeft = crafts;
        craftShopping.clear();
        craftShopping.putAll(shopping);
        craftFetchQueue.clear();
        craftFetchContainers.clear();
        currentCraft = null;
        craftTimer = 0;

        if (craftShopping.isEmpty()) {
            setState(State.CRAFTING);
            return true;
        }

        for (StorageArea storage : storages) {
            for (Map.Entry<BlockPos, Container> entry : storage.storageMap.entrySet()) {
                Container container = entry.getValue();
                for (int i = 0; i < container.getContainerSize(); i++) {
                    if (craftShopping.containsKey(container.getItem(i).getItem())) {
                        craftFetchQueue.add(entry.getKey());
                        craftFetchContainers.put(entry.getKey(), container);
                        break;
                    }
                }
            }
        }
        craftFetchQueue.sort(Comparator.comparingDouble(pos -> pos.distToCenterSqr(cook.position())));
        interactTimer = 0;
        setState(State.CRAFT_FETCH);
        return true;
    }

    /** Reserves one craft of the recipe: carried items first, then the storage share (added to the shopping list). */
    private boolean reserveIngredients(CraftingRecipe recipe, Map<Item, Integer> carried, Map<Item, Integer> stored, Map<Item, Integer> shopping) {
        for (Ingredient ingredient : recipe.getIngredients()) {
            if (ingredient.isEmpty()) continue;

            boolean found = false;
            for (ItemStack option : ingredient.getItems()) {
                Item item = option.getItem();
                if (carried.getOrDefault(item, 0) > 0) {
                    carried.merge(item, -1, Integer::sum);
                    found = true;
                    break;
                }
            }
            if (!found) {
                for (ItemStack option : ingredient.getItems()) {
                    Item item = option.getItem();
                    if (stored.getOrDefault(item, 0) > 0) {
                        stored.merge(item, -1, Integer::sum);
                        shopping.merge(item, 1, Integer::sum);
                        found = true;
                        break;
                    }
                }
            }
            if (!found) return false;
        }
        return true;
    }

    private static int countStacks(Map<Item, Integer> items) {
        int stacks = 0;
        for (Map.Entry<Item, Integer> entry : items.entrySet()) {
            int max = entry.getKey().getDefaultInstance().getMaxStackSize();
            stacks += (entry.getValue() + max - 1) / max;
        }
        return stacks;
    }

    private void tickCraftFetch(ServerLevel level) {
        boolean nothingLeft = craftShopping.values().stream().allMatch(count -> count <= 0);
        if (craftFetchQueue.isEmpty() || nothingLeft) {
            closeOpenContainer(level);
            craftFetchQueue.clear();
            craftFetchContainers.clear();
            craftShopping.clear();
            craftTimer = 0;
            setState(State.CRAFTING);
            return;
        }

        BlockPos pos = craftFetchQueue.get(0);
        if (moveToPositionTimed(pos, 3)) return;
        cook.getNavigation().stop();
        cook.getLookControl().setLookAt(pos.getCenter());

        if (interactTimer == 0) {
            openContainerPos = pos;
            interactContainer(level, pos, true);
        }
        if (interactTimer < INTERACT_TIME) {
            interactTimer++;
            if (interactTimer % 5 == 0) cook.swing(InteractionHand.MAIN_HAND);
            return;
        }

        Container container = craftFetchContainers.get(pos);
        if (container != null && !(container instanceof BlockEntity be && be.isRemoved())) {
            boolean changed = false;
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack stack = container.getItem(i);
                if (stack.isEmpty()) continue;
                int want = craftShopping.getOrDefault(stack.getItem(), 0);
                if (want <= 0) continue;

                int take = Math.min(want, stack.getCount());
                ItemStack moving = stack.copyWithCount(take);
                if (!cook.canAddItem(moving)) break;
                ItemStack rest = cook.addItem(moving);
                int taken = take - rest.getCount();
                if (taken <= 0) continue;

                craftShopping.put(stack.getItem(), want - taken);
                stack.shrink(taken);
                if (stack.isEmpty()) container.setItem(i, ItemStack.EMPTY);
                changed = true;
            }
            if (changed) container.setChanged();
        }

        closeOpenContainer(level);
        craftFetchQueue.remove(0);
        interactTimer = 0;
    }

    /** Back in the kitchen the cook crafts by hand, one item every CRAFT_TIME ticks. */
    private void tickCrafting(ServerLevel level) {
        KitchenArea area = cook.currentKitchenArea;
        if (area == null) return;

        if (area.getFeedVillagers()) tickVillagerTrading(level);

        if (moveToPositionTimed(BlockPos.containing(area.getArea().getCenter()), 3)) return;
        cook.getNavigation().stop();

        if (currentCraft == null) {
            currentCraft = craftsLeft > 0 ? findCraftableRecipe(level) : null;
            craftTimer = 0;
            if (currentCraft == null) {
                finishCrafting();
                return;
            }
        }

        if (++craftTimer < CRAFT_TIME) {
            if (craftTimer % 10 == 0) cook.swing(InteractionHand.MAIN_HAND);
            return;
        }

        if (craftFromInventory(level, currentCraft)) {
            craftsLeft--;
            level.playSound(null, cook.blockPosition(), SoundEvents.VILLAGER_WORK_BUTCHER, cook.getSoundSource(), 0.6F, 0.9F + 0.2F * cook.getRandom().nextFloat());
        }
        currentCraft = null;
    }

    /** The crafted food goes into the kitchen containers like fetched items. */
    private void finishCrafting() {
        craftsLeft = 0;
        currentCraft = null;
        craftTimer = 0;
        interactTimer = 0;
        if (!craftedItems.isEmpty()) {
            fetchedItems.addAll(craftedItems);
            craftedItems.clear();
            setState(State.STORE_FETCHED);
        }
        else {
            setState(State.WORKING);
        }
    }

    /** Next recipe (round robin) whose ingredients the cook carries. */
    @Nullable
    private CraftingRecipe findCraftableRecipe(ServerLevel level) {
        List<CraftingRecipe> recipes = getKitchenRecipes(level);
        if (recipes.isEmpty()) return null;

        Map<Item, Integer> carried = new HashMap<>();
        SimpleContainer inventory = cook.getInventory();
        for (int i = 6; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) carried.merge(stack.getItem(), stack.getCount(), Integer::sum);
        }

        for (int n = 0; n < recipes.size(); n++) {
            int index = (craftRecipeIndex + n) % recipes.size();
            CraftingRecipe recipe = recipes.get(index);
            if (reserveIngredients(recipe, new HashMap<>(carried), new HashMap<>(), new HashMap<>())) {
                craftRecipeIndex = index + 1;
                return recipe;
            }
        }
        return null;
    }

    private boolean craftFromInventory(ServerLevel level, CraftingRecipe recipe) {
        CraftingContainer grid = CraftOrder.createCraftingContainer();
        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        // shaped recipes keep their shape, shapeless ones are filled row by row
        int width = recipe instanceof IShapedRecipe<?> shaped ? shaped.getRecipeWidth() : 3;

        for (int i = 0; i < ingredients.size(); i++) {
            Ingredient ingredient = ingredients.get(i);
            if (ingredient.isEmpty()) continue;

            ItemStack taken = takeOneFromInventory(ingredient);
            if (taken.isEmpty()) {
                returnToInventory(grid);
                return false;
            }
            grid.setItem((i / width) * 3 + (i % width), taken);
        }

        if (!recipe.matches(grid, level)) {
            returnToInventory(grid);
            return false;
        }
        ItemStack result = recipe.assemble(grid, level.registryAccess());
        if (result.isEmpty()) {
            returnToInventory(grid);
            return false;
        }
        NonNullList<ItemStack> remaining = recipe.getRemainingItems(grid);

        craftedItems.add(result.getItem());
        ItemStack rest = cook.addItem(result);
        if (!rest.isEmpty()) cook.spawnAtLocation(rest);

        for (ItemStack remainder : remaining) {
            if (remainder.isEmpty()) continue;
            craftedItems.add(remainder.getItem());
            ItemStack left = cook.addItem(remainder);
            if (!left.isEmpty()) cook.spawnAtLocation(left);
        }

        cook.swing(InteractionHand.MAIN_HAND);
        return true;
    }

    private ItemStack takeOneFromInventory(Ingredient ingredient) {
        SimpleContainer inventory = cook.getInventory();
        for (int i = 6; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty() || !ingredient.test(stack)) continue;

            ItemStack one = stack.split(1);
            if (stack.isEmpty()) inventory.setItem(i, ItemStack.EMPTY);
            inventory.setChanged();
            return one;
        }
        return ItemStack.EMPTY;
    }

    private void returnToInventory(CraftingContainer grid) {
        for (int i = 0; i < grid.getContainerSize(); i++) {
            ItemStack stack = grid.getItem(i);
            if (stack.isEmpty()) continue;
            ItemStack rest = cook.addItem(stack);
            if (!rest.isEmpty()) cook.spawnAtLocation(rest);
            grid.setItem(i, ItemStack.EMPTY);
        }
    }

    /** Crafting recipes with an edible result whose ingredients are all kitchen ingredients (bread, cookies, stews...). */
    private List<CraftingRecipe> getKitchenRecipes(ServerLevel level) {
        if (kitchenRecipes == null) {
            kitchenRecipes = new ArrayList<>();
            for (CraftingRecipe recipe : level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
                if (recipe.isSpecial()) continue;
                ItemStack result = recipe.getResultItem(level.registryAccess());
                if (result.isEmpty() || !result.isEdible()) continue;
                if (isKitchenRecipe(recipe)) kitchenRecipes.add(recipe);
            }
        }
        return kitchenRecipes;
    }

    private static boolean isKitchenRecipe(CraftingRecipe recipe) {
        boolean hasIngredient = false;
        for (Ingredient ingredient : recipe.getIngredients()) {
            if (ingredient.isEmpty()) continue;
            ItemStack[] options = ingredient.getItems();
            if (options.length == 0) return false;
            for (ItemStack option : options) {
                if (!isKitchenIngredient(option)) return false;
            }
            hasIngredient = true;
        }
        return hasIngredient;
    }

    /** Food and typical baking items. Gold, honey blocks and the like are no kitchen ingredients. */
    private static boolean isKitchenIngredient(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.isEdible()) return true;
        if (stack.is(Tags.Items.CROPS) || stack.is(Tags.Items.EGGS) || stack.is(Tags.Items.MUSHROOMS)) return true;
        return stack.is(Items.SUGAR) || stack.is(Items.BOWL) || stack.is(Items.MILK_BUCKET)
                || stack.is(Items.COCOA_BEANS) || stack.is(Items.PUMPKIN);
    }

    // ── Delivering cooked food ────────────────────────────────────────────────

    /** Takes cooked food out of the kitchen containers and carries it to the nearest storage. */
    private boolean tryStartDeliver(ServerLevel level, KitchenArea area) {
        deliverCooldown = DELIVER_CHECK_INTERVAL;
        if (!kitchenHas(area, stack -> isCookedFood(stack, level))) return false;

        List<StorageArea> storages = findStorages(level, area);
        if (storages.isEmpty()) return false;

        int room = Math.min(MAX_FETCH_STACKS, countFreeInventorySlots());
        int taken = 0;
        for (Container container : area.containerMap.values()) {
            boolean changed = false;
            for (int i = 0; i < container.getContainerSize() && taken < room; i++) {
                ItemStack stack = container.getItem(i);
                if (stack.isEmpty() || !isCookedFood(stack, level) || !cook.canAddItem(stack)) continue;
                Item item = stack.getItem();
                ItemStack rest = cook.addItem(stack.copy());
                if (rest.getCount() == stack.getCount()) continue;
                container.setItem(i, rest);
                deliverItems.add(item);
                changed = true;
                taken++;
            }
            if (changed) container.setChanged();
        }
        if (taken == 0) return false;

        cook.swing(InteractionHand.MAIN_HAND);
        deliverStorages.clear();
        deliverStorages.addAll(storages);
        deliverPos = null;
        interactTimer = 0;
        setState(State.DELIVER);
        return true;
    }

    private void tickDeliver(ServerLevel level) {
        if (!carriesDeliverItems()) {
            closeOpenContainer(level);
            deliverItems.clear();
            deliverPos = null;
            deliverStorage = null;
            setState(State.WORKING);
            return;
        }

        if (deliverPos == null) {
            if (deliverStorages.isEmpty() && cook.currentKitchenArea != null) {
                deliverStorages.addAll(findStorages(level, cook.currentKitchenArea));
            }
            if (!findDeliverTarget()) {
                // no storage has space: the food goes back into the kitchen
                fetchedItems.addAll(deliverItems);
                deliverItems.clear();
                deliverStorages.clear();
                interactTimer = 0;
                setState(State.STORE_FETCHED);
                return;
            }
            interactTimer = 0;
        }

        if (moveToPositionTimed(deliverPos, 3)) return;
        cook.getNavigation().stop();
        cook.getLookControl().setLookAt(deliverPos.getCenter());

        if (interactTimer == 0) {
            openContainerPos = deliverPos;
            interactContainer(level, deliverPos, true);
        }
        if (interactTimer < INTERACT_TIME) {
            interactTimer++;
            if (interactTimer % 5 == 0) cook.swing(InteractionHand.MAIN_HAND);
            return;
        }

        Container container = deliverStorage != null ? deliverStorage.storageMap.get(deliverPos) : null;
        if (container != null) {
            SimpleContainer inventory = cook.getInventory();
            for (int i = 6; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                if (stack.isEmpty() || !deliverItems.contains(stack.getItem())) continue;
                WorkshopArea.insertInto(container, stack);
                inventory.setItem(i, stack.isEmpty() ? ItemStack.EMPTY : stack);
            }
        }

        closeOpenContainer(level);
        deliverPos = null;
        interactTimer = 0;
    }

    /** A storage chest for the carried food: chests that already hold the item first. */
    private boolean findDeliverTarget() {
        SimpleContainer inventory = cook.getInventory();
        for (StorageArea storage : deliverStorages) {
            if (storage.isRemoved()) continue;
            BlockPos fallback = null;
            for (int i = 6; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                if (stack.isEmpty() || !deliverItems.contains(stack.getItem())) continue;
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

    private boolean carriesDeliverItems() {
        if (deliverItems.isEmpty()) return false;
        SimpleContainer inventory = cook.getInventory();
        for (int i = 6; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && deliverItems.contains(stack.getItem())) return true;
        }
        return false;
    }

    /** Edible results of smelting recipes (cooked beef, baked potato, ...) and of kitchen recipes (bread, cookies, ...). */
    private boolean isCookedFood(ItemStack stack, ServerLevel level) {
        if (cookedFoods == null) {
            cookedFoods = new HashSet<>();
            for (SmeltingRecipe recipe : level.getRecipeManager().getAllRecipesFor(RecipeType.SMELTING)) {
                ItemStack result = recipe.getResultItem(level.registryAccess());
                if (!result.isEmpty() && result.getItem().isEdible()) cookedFoods.add(result.getItem());
            }
            for (CraftingRecipe recipe : getKitchenRecipes(level)) {
                cookedFoods.add(recipe.getResultItem(level.registryAccess()).getItem());
            }
        }
        return !stack.isEmpty() && cookedFoods.contains(stack.getItem());
    }

    private boolean carriesFetchedItems() {
        if (fetchedItems.isEmpty()) return false;
        SimpleContainer inventory = cook.getInventory();
        for (int i = 6; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && fetchedItems.contains(stack.getItem())) return true;
        }
        return false;
    }

    private boolean kitchenHas(KitchenArea area, java.util.function.Predicate<ItemStack> predicate) {
        for (Container container : area.containerMap.values()) {
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack stack = container.getItem(i);
                if (!stack.isEmpty() && predicate.test(stack)) return true;
            }
        }
        return false;
    }

    /** Fuel fetched from a storage: coal and charcoal only. */
    private boolean isFuel(ItemStack stack) {
        return stack.is(ItemTags.COALS);
    }

    /** Fuel the cook burns from the kitchen: coal/charcoal, planks, logs. */
    private boolean isKitchenFuel(ItemStack stack) {
        for (TagKey<Item> tag : FUEL_TAGS) {
            if (stack.is(tag) && ForgeHooks.getBurnTime(stack, RecipeType.SMELTING) > 0) return true;
        }
        return false;
    }

    private int countFreeInventorySlots() {
        int free = 0;
        SimpleContainer inventory = cook.getInventory();
        for (int i = 6; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).isEmpty()) free++;
        }
        return free;
    }

    private void closeOpenContainer(ServerLevel level) {
        if (openContainerPos != null) {
            interactContainer(level, openContainerPos, false);
            openContainerPos = null;
        }
    }

    /** Like moveToPosition, but gives up after MAX_MOVE_TICKS so an unreachable chest can not block the cook. */
    private boolean moveToPositionTimed(BlockPos pos, int thresholdBlocks) {
        if (!pos.equals(moveTarget)) {
            moveTarget = pos;
            moveTicks = 0;
        }
        if (++moveTicks > MAX_MOVE_TICKS) {
            cook.getNavigation().stop();
            return false;
        }
        return moveToPosition(pos, thresholdBlocks);
    }

    private void finishFurnaceTask(ServerLevel level) {
        interactContainer(level, openContainerPos, false);
        clearFurnaceTask();
    }

    private void clearFurnaceTask() {
        currentFurnace   = null;
        openContainerPos = null;
        interactTimer    = 0;
    }

    @Nullable
    private BlockPos findNearestContainer(KitchenArea area, BlockPos near) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos pos : area.containerMap.keySet()) {
            double d = pos.distSqr(near);
            if (d < bestDist) {
                bestDist = d;
                best = pos;
            }
        }
        return best;
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
            level.playSound(null, pos, SoundEvents.CHEST_OPEN, cook.getSoundSource(), 0.7F, 0.8F + 0.4F * cook.getRandom().nextFloat());
            data.putBoolean("isOpened", true);
        }
        else if (!open && isOpened) {
            level.blockEvent(pos, block, 1, 0);
            level.playSound(null, pos, SoundEvents.CHEST_CLOSE, cook.getSoundSource(), 0.7F, 0.8F + 0.4F * cook.getRandom().nextFloat());
            data.putBoolean("isOpened", false);
        }
    }

    // ── Furnace logic ─────────────────────────────────────────────────────────

    /** Loads one fuel item: coal/charcoal first, then planks, then logs (all wood types). */
    private void loadFuel(KitchenArea area, AbstractFurnaceBlockEntity furnace) {
        for (TagKey<Item> tag : FUEL_TAGS) {
            for (Container container : area.containerMap.values()) {
                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack slot = container.getItem(i);
                    if (slot.isEmpty() || !slot.is(tag)) continue;
                    int burnTime = ForgeHooks.getBurnTime(slot, RecipeType.SMELTING);
                    if (burnTime <= 0) continue;

                    // enough fuel for a whole batch (one item = 200 ticks)
                    int amount = Math.min(slot.getCount(), Math.max(1, (FURNACE_BATCH * 200 + burnTime - 1) / burnTime));
                    furnace.setItem(1, slot.copyWithCount(amount));
                    furnace.setChanged();
                    slot.shrink(amount);
                    container.setChanged();
                    return;
                }
            }
        }
    }

    private void loadIngredient(KitchenArea area, AbstractFurnaceBlockEntity furnace, ServerLevel level) {
        for (var entry : area.containerMap.entrySet()) {
            var container = entry.getValue();
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack slot = container.getItem(i);
                if (slot.isEmpty()) continue;
                if (!isSmeltableFood(slot, level)) continue;

                ItemStack toLoad = slot.copyWithCount(Math.min(FURNACE_BATCH, slot.getCount()));
                furnace.setItem(0, toLoad);
                furnace.setChanged();
                slot.shrink(toLoad.getCount());
                container.setChanged();
                return;
            }
        }
    }

    private boolean isSmeltableFood(ItemStack stack, ServerLevel level) {
        Optional<SmeltingRecipe> recipe = level.getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, new SimpleContainer(stack), level);

        if (recipe.isEmpty()) return false;

        // Only accept if result is a food item
        ItemStack result = recipe.get().getResultItem(level.registryAccess());
        return !result.isEmpty() && result.getItem().isEdible();
    }

    // ── Villager trading ──────────────────────────────────────────────────────

    private void tickVillagerTrading(ServerLevel level) {
        if (cook.activeTradingVillager != null) {
            Villager v = cook.activeTradingVillager;

            if (v.isRemoved()) {
                cook.clearVillagerTrade();
                return;
            }

            if (++cook.villagerTradeTimeout > VILLAGER_TIMEOUT) {
                cook.clearVillagerTrade();
                return;
            }

            if (cook.distanceTo(v) < 3.5) {
                executeFeedVillager(v, level);
            }
            return;
        }

        if (++villagerScanCooldown < VILLAGER_SCAN_INTERVAL) return;
        villagerScanCooldown = 0;
        inviteVillager(level);
    }

    private void inviteVillager(ServerLevel level) {
        KitchenArea area = cook.currentKitchenArea;
        if (area == null) return;

        // Only invite if we actually have cooked food available
        if (!hasAnyCookedFood(area)) return;

        List<Villager> nearby = level.getEntitiesOfClass(
                Villager.class, cook.getBoundingBox().inflate(48));
        if (nearby.isEmpty()) return;

        List<Villager> candidates = new ArrayList<>();

        for (Villager v : nearby) {
            if (v.isRemoved() || v.isSleeping() || v.isTrading()) continue;
            if (VillagerInviteRegistry.isInvited(v.getUUID())) continue;
            candidates.add(v);
        }

        if (candidates.isEmpty()) return;

        Villager chosen = candidates.get(cook.getRandom().nextInt(candidates.size()));

        if (!VillagerInviteRegistry.tryInvite(chosen.getUUID(), cook.getUUID())) return;

        cook.activeTradingVillager = chosen;
        cook.villagerTradeTimeout  = 0;
    }

    private void executeFeedVillager(Villager villager, ServerLevel level) {
        KitchenArea area = cook.currentKitchenArea;
        if (area == null) {
            cook.clearVillagerTrade();
            return;
        }

        ItemStack foodToGive = findCookedFood(area);
        if (foodToGive == null || foodToGive.isEmpty()) {
            cook.clearVillagerTrade();
            return;
        }

        // Take 1 food item from containers and give it to the villager
        // The mixin on Villager.eatAndHeal will track accumulated saturation
        // and trigger breeding once the threshold is reached
        area.shrinkItemFromContainers(foodToGive, 1);
        villager.getInventory().addItem(foodToGive.copyWithCount(1));

        spawnTradeParticles(level, villager);
        level.playSound(null, cook.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0F, 1.0F);

        cook.clearVillagerTrade();
    }
    private void spawnTradeParticles(ServerLevel level, Villager villager) {
        double vx = villager.getX();
        double vy = villager.getY() + 1.8;
        double vz = villager.getZ();
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, vx, vy, vz, 6, 0.3, 0.3, 0.3, 0.0);

        double mx = cook.getX();
        double my = cook.getY() + 1.8;
        double mz = cook.getZ();
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, mx, my, mz, 6, 0.3, 0.3, 0.3, 0.0);
    }

    @Nullable
    private ItemStack findCookedFood(KitchenArea area) {
        // cooked and crafted food first, raw food only if nothing else is there
        if (area.getCommandSenderWorld() instanceof ServerLevel level) {
            for (var container : area.containerMap.values()) {
                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack slot = container.getItem(i);
                    if (!slot.isEmpty() && isCookedFood(slot, level)) return slot.copy();
                }
            }
        }
        for (var container : area.containerMap.values()) {
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack slot = container.getItem(i);
                if (slot.isEmpty()) continue;
                if (slot.getItem().isEdible()) return slot.copy();
            }
        }
        return null;
    }

    private boolean hasAnyCookedFood(KitchenArea area) {
        return findCookedFood(area) != null;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void leaveCurrentArea() {
        clearFurnaceTask();
        furnaceQueue.clear();
        if (cook.currentKitchenArea != null) {
            cook.currentKitchenArea.setBeingWorkedOn(false);
            cook.currentKitchenArea.setCookName("None");
            cook.currentKitchenArea = null;
        }
    }

    private boolean isCurrentAreaGone() {
        return cook.currentKitchenArea == null || cook.currentKitchenArea.isRemoved();
    }

    private boolean moveToPosition(BlockPos pos, int thresholdBlocks) {
        double dist = cook.getHorizontalDistanceTo(pos.getCenter());
        if (dist < thresholdBlocks) {
            cook.getNavigation().stop();
            return false;
        }
        // Note: setFollowState is NOT called here — start() already set it to 6.
        // Calling it here would override owner commands on every tick.
        cook.getNavigation().moveTo(pos.getX(), pos.getY(), pos.getZ(), 0.8F);
        cook.getLookControl().setLookAt(pos.getCenter());
        return true;
    }

    @Nullable
    private KitchenArea findBestArea(ServerLevel level) {
        List<KitchenArea> areas = level.getEntitiesOfClass(
                KitchenArea.class, cook.getBoundingBox().inflate(64));

        KitchenArea best = null;
        int bestScore    = -1;

        for (KitchenArea area : areas) {
            if (area == null) continue;
            if (!area.canWorkHere(cook)) continue;
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

    public enum State {
        SELECT_WORK_AREA,
        WALK_TO_CENTER,
        FETCH,
        STORE_FETCHED,
        DELIVER,
        CRAFT_FETCH,
        CRAFTING,
        WORKING
    }
}