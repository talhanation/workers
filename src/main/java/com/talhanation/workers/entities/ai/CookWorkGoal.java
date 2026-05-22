package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.CookEntity;
import com.talhanation.workers.entities.workarea.KitchenArea;
import com.talhanation.workers.world.VillagerInviteRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.*;

public class CookWorkGoal extends Goal {

    private static final int VILLAGER_SCAN_INTERVAL = 100;
    private static final int VILLAGER_TIMEOUT       = 600;
    private static final int FURNACE_CHECK_INTERVAL = 120;
    private static final int STORE_INTERVAL         = 180;
    private static final int INTERACT_TIME          = 20; // ~1 second spent at each furnace

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

        // ── Otherwise pick the next furnace, rebuilding the queue periodically ─
        if (furnaceQueue.isEmpty()) {
            cook.getNavigation().stop();
            if (++furnaceCheckCooldown < FURNACE_CHECK_INTERVAL) return;
            furnaceCheckCooldown = 0;

            area.pruneRemovedFurnaces();
            area.pruneRemovedContainers();
            // Re-scan keeps the furnace/container maps and the GUI slot counts fresh.
            area.scanArea();

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
                furnace.setChanged();
                return;
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

                ItemStack toLoad = slot.copy();
                toLoad.setCount(1);
                furnace.setItem(0, toLoad);
                furnace.setChanged();
                slot.shrink(1);
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
    }

    public enum State {
        SELECT_WORK_AREA,
        WALK_TO_CENTER,
        WORKING
    }
}