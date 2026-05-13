package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.CookEntity;
import com.talhanation.workers.entities.workarea.KitchenArea;
import com.talhanation.workers.world.VillagerInviteRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

import javax.annotation.Nullable;
import java.util.*;

public class CookWorkGoal extends Goal {

    private static final int VILLAGER_SCAN_INTERVAL = 100;
    private static final int VILLAGER_TIMEOUT       = 600;
    private static final int FURNACE_CHECK_INTERVAL = 40;
    private static final int STORE_INTERVAL         = 60;

    // Minimum cooked food in containers before inviting villagers
    private static final int MIN_FOOD_TO_SELL = 1;

    private final CookEntity cook;
    private State state;
    private int cooldown;
    private int villagerScanCooldown;
    private int furnaceCheckCooldown;
    private int storeCooldown;

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
        if (cook.currentKitchenArea != null) {
            cook.currentKitchenArea.setBeingWorkedOn(false);
            cook.currentKitchenArea = null;
        }
        cook.clearVillagerTrade();
        cook.setFollowState(0);
        cook.getNavigation().stop();
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

        switch (state) {
            case SELECT_WORK_AREA -> tickSelectWorkArea((ServerLevel) cook.getCommandSenderWorld());
            case WALK_TO_CENTER   -> tickWalkToCenter();
            case WORKING          -> tickWorking((ServerLevel) cook.getCommandSenderWorld());
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
        found.setTime(0);

        setState(State.WALK_TO_CENTER);
    }

    private void tickWalkToCenter() {
        if (moveToPosition(BlockPos.containing(cook.currentKitchenArea.getArea().getCenter()), 3)) return;
        cook.getNavigation().stop();
        setState(State.WORKING);
    }

    private void tickWorking(ServerLevel level) {
        cook.getNavigation().stop();
        cook.setFollowState(6);

        // ── Periodically check and unload furnaces ────────────────────────────
        if (++furnaceCheckCooldown >= FURNACE_CHECK_INTERVAL) {
            furnaceCheckCooldown = 0;
            tickFurnaces();
        }

        // ── Periodically load ingredients into furnaces ───────────────────────
        if (++storeCooldown >= STORE_INTERVAL) {
            storeCooldown = 0;
            tickLoadFurnaces(level);
        }

        // ── Villager trading ──────────────────────────────────────────────────
        if (cook.currentKitchenArea.isSellToVillagers()) {
            tickVillagerTrading(level);
        }
    }

    // ── Furnace logic ─────────────────────────────────────────────────────────

    private void tickFurnaces() {
        KitchenArea area = cook.currentKitchenArea;
        if (area == null) return;
        if (area.furnaceMap.isEmpty()) area.scanArea();

        for (AbstractFurnaceBlockEntity furnace : area.furnaceMap.values()) {
            ItemStack output = furnace.getItem(2);
            if (output.isEmpty()) continue;

            // Deposit output into kitchen containers
            boolean deposited = area.depositItemToContainers(output, output.getCount());
            if (deposited) {
                furnace.setItem(2, ItemStack.EMPTY);
            }
        }
    }

    private void tickLoadFurnaces(ServerLevel level) {
        KitchenArea area = cook.currentKitchenArea;
        if (area == null) return;
        if (area.furnaceMap.isEmpty() || area.containerMap.isEmpty()) area.scanArea();

        for (AbstractFurnaceBlockEntity furnace : area.furnaceMap.values()) {
            ItemStack inputSlot = furnace.getItem(0);
            ItemStack fuelSlot  = furnace.getItem(1);

            // Load fuel if missing
            if (fuelSlot.isEmpty()) {
                loadFuel(area, furnace);
            }

            // Load ingredient if input is empty
            if (inputSlot.isEmpty()) {
                loadIngredient(area, furnace, level);
            }
        }
    }

    private void loadFuel(KitchenArea area, AbstractFurnaceBlockEntity furnace) {
        ItemStack[] fuelCandidates = {
            new ItemStack(Items.COAL, 1),
            new ItemStack(Items.CHARCOAL, 1),
            new ItemStack(Items.COAL_BLOCK, 1),
            new ItemStack(Items.OAK_LOG, 1),
            new ItemStack(Items.OAK_PLANKS, 1)
        };

        for (ItemStack fuel : fuelCandidates) {
            int available = area.countItemInContainers(fuel);
            if (available > 0) {
                area.shrinkItemFromContainers(fuel, 1);
                furnace.setItem(1, fuel.copy());
                return;
            }
        }
    }

    private void loadIngredient(KitchenArea area, AbstractFurnaceBlockEntity furnace, ServerLevel level) {
        // Scan containers for any smeltable food item
        for (var entry : area.containerMap.entrySet()) {
            var container = entry.getValue();
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack slot = container.getItem(i);
                if (slot.isEmpty()) continue;
                if (!isSmeltableFood(slot, level)) continue;

                // Move 1 item into furnace input
                ItemStack toLoad = slot.copy();
                toLoad.setCount(1);
                furnace.setItem(0, toLoad);
                slot.shrink(1);
                return;
            }
        }
    }

    private boolean isSmeltableFood(ItemStack stack, ServerLevel level) {
        Optional<SmeltingRecipe> recipe = level.getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, new net.minecraft.world.SimpleContainer(stack), level);

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

        cook.clearVillagerTrade();
    }

    @Nullable
    private ItemStack findCookedFood(KitchenArea area) {
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
        if (cook.currentKitchenArea != null) {
            cook.currentKitchenArea.setBeingWorkedOn(false);
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
        cook.getNavigation().moveTo(pos.getX(), pos.getY(), pos.getZ(), 0.8F);
        cook.setFollowState(6);
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
    }

    public enum State {
        SELECT_WORK_AREA,
        WALK_TO_CENTER,
        WORKING
    }
}
