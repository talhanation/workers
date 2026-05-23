package com.talhanation.workers.entities.ai;

import com.talhanation.recruits.FactionEvents;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.IRangedRecruit;
import com.talhanation.recruits.entities.ai.RecruitUpkeepEntityGoal;
import com.talhanation.workers.entities.workarea.StorageArea;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Upkeep goal that EXTENDS the recruits mod's {@link RecruitUpkeepEntityGoal}.
 *
 * <p>It changes behaviour only when the recruit's upkeep target resolves to a Workers
 * {@link StorageArea}. In that case the recruit walks to each chest of the storage that actually
 * holds something useful, opens it (vanilla animation) and pulls out food / equipment / tools /
 * ammo / payment — exactly the items the recruits mod itself handles for upkeep, with no
 * needed-item system. For every other upkeep target it simply defers to {@code super}, so normal
 * single-container upkeep behaves exactly as before.</p>
 *
 * <p>This class is meant to REPLACE the stock {@code RecruitUpkeepEntityGoal} in the goal selector
 * (see VillagerEvents). Because there is then only one entity-upkeep goal, the stock goal can no
 * longer fire for a storage (which — being a Container — it would otherwise grab and abort with a
 * "no food" message).</p>
 */
public class RecruitStorageUpkeepGoal extends RecruitUpkeepEntityGoal {

    private static final int OPEN_TIME       = 16;  // ticks "interacting" at an opened chest (~0.8s)
    private static final int REACH_SQR       = 9;   // squared distance considered "at the chest" (~3 blocks)
    private static final int FOOD_BUDGET     = 4;   // max food items pulled per upkeep run
    private static final int MAX_CHESTS      = 16;  // safety cap on chests visited per run
    private static final int FOOD_SLOT_START = 6;   // recruit food/back slots (mirrors recruits mod)
    private static final int FOOD_SLOT_END   = 14;

    private boolean storageMode;
    @Nullable private StorageArea storageArea;
    private final List<BlockPos> chestQueue = new ArrayList<>();
    @Nullable private BlockPos currentChestPos;
    @Nullable private Container chestContainer;

    private int interactTimer;
    private int moveTimer;
    private int chestsVisited;
    private int foodTaken;
    private boolean paymentReset;

    public RecruitStorageUpkeepGoal(AbstractRecruitEntity recruit) {
        super(recruit);
    }

    @Override
    public boolean canUse() {
        // Storage upkeep: not throttled, so this single goal reliably handles a storage target.
        // needsToGetFood() is cheap and almost always false, so the entity search rarely runs.
        if (this.recruit.needsToGetFood() && resolveStorage() != null) {
            return true;
        }
        // Any other upkeep target → original recruits behaviour.
        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        if (this.storageMode) {
            return this.storageArea != null
                    && !this.storageArea.isRemoved()
                    && this.recruit.needsToGetFood()
                    && (this.currentChestPos != null || !this.chestQueue.isEmpty());
        }
        return super.canContinueToUse();
    }

    @Override
    public void start() {
        StorageArea sa = resolveStorage();
        if (sa != null) {
            this.storageMode = true;
            this.storageArea = sa;
            this.chestQueue.clear();
            this.currentChestPos = null;
            this.chestContainer = null;
            this.interactTimer = 0;
            this.moveTimer = 0;
            this.chestsVisited = 0;
            this.foodTaken = 0;
            this.paymentReset = false;

            this.storageArea.scanStorageBlocks();
            List<BlockPos> keys = new ArrayList<>(this.storageArea.storageMap.keySet());
            keys.sort(Comparator.comparingDouble(p -> p.distSqr(this.recruit.blockPosition())));
            // Only queue chests that actually hold something the recruit would take for upkeep.
            for (BlockPos chestPos : keys) {
                Container c = this.storageArea.storageMap.get(chestPos);
                if (c != null && containerHasUpkeepItem(c)) {
                    this.chestQueue.add(chestPos);
                }
            }
        } else {
            this.storageMode = false;
            super.start();
        }
    }

    @Override
    public void tick() {
        if (!this.storageMode) {
            super.tick();
            return;
        }
        if (this.storageArea == null) return;

        // Pick the next chest to service.
        if (this.currentChestPos == null) {
            if (this.chestQueue.isEmpty() || this.chestsVisited >= MAX_CHESTS) {
                this.chestQueue.clear(); // nothing (more) to do — canContinueToUse will end the goal
                return;
            }
            this.currentChestPos = this.chestQueue.remove(0);
            this.chestContainer = this.storageArea.storageMap.get(this.currentChestPos);
            this.interactTimer = 0;
            if (this.chestContainer == null) {
                this.currentChestPos = null; // stale entry, skip
                return;
            }
        }

        // Walk to the chest and look at it.
        double dist = this.currentChestPos.getCenter().distanceToSqr(this.recruit.position());
        if (dist > REACH_SQR) {
            if (--this.moveTimer <= 0) {
                this.moveTimer = 10;
                this.recruit.getNavigation().moveTo(
                        this.currentChestPos.getX() + 0.5D,
                        this.currentChestPos.getY(),
                        this.currentChestPos.getZ() + 0.5D, 1.0D);
            }
            if (this.recruit.horizontalCollision) {
                this.recruit.getJumpControl().jump();
            }
            return;
        }

        this.recruit.getNavigation().stop();
        this.recruit.getLookControl().setLookAt(
                this.currentChestPos.getX() + 0.5D,
                this.currentChestPos.getY() + 0.5D,
                this.currentChestPos.getZ() + 0.5D);

        // Open the chest and spend a moment interacting.
        if (this.interactTimer == 0) {
            interactChest(true);
        }
        if (this.interactTimer < OPEN_TIME) {
            this.interactTimer++;
            return;
        }

        // Take payment, equipment/tools/ammo and food — same as the recruits mod.
        serviceContainer(this.chestContainer);
        this.chestsVisited++;

        interactChest(false);
        this.currentChestPos = null;
        this.chestContainer = null;

        if (this.foodTaken >= FOOD_BUDGET) {
            this.chestQueue.clear();
        }
    }

    @Override
    public void stop() {
        if (!this.storageMode) {
            super.stop();
            return;
        }

        interactChest(false);
        this.currentChestPos = null;
        this.chestContainer = null;
        this.chestQueue.clear();
        this.recruit.getNavigation().stop();

        this.recruit.setUpkeepTimer(this.recruit.getUpkeepCooldown());
        this.recruit.forcedUpkeep = false;
        if (this.recruit.paymentTimer == 0 && this.paymentReset) {
            this.paymentReset = false;
            this.recruit.resetPaymentTimer();
        }

        this.storageArea = null;
        this.storageMode = false;
    }

    private void serviceContainer(Container c) {
        // Payment (only when due) — identical to the stock upkeep goal.
        if (this.recruit.paymentTimer == 0) {
            this.recruit.checkPayment(c);
            this.paymentReset = true;
        }

        // Equipment / tools / arrows / cartridges etc. — handled entirely by the recruits mod.
        this.recruit.upkeepReequip(c);

        // Food.
        int slot = findFoodSlot(c);
        while (slot >= 0 && this.foodTaken < FOOD_BUDGET && canAddFood()) {
            ItemStack stackInChest = c.getItem(slot);
            ItemStack food = stackInChest.copy();
            food.setCount(1);
            this.recruit.getInventory().addItem(food);
            stackInChest.shrink(1);
            c.setChanged();
            this.foodTaken++;
            slot = findFoodSlot(c);
        }
    }

    private int findFoodSlot(Container c) {
        for (int i = 0; i < c.getContainerSize(); i++) {
            ItemStack stack = c.getItem(i);
            if (!stack.isEmpty() && this.recruit.canEatItemStack(stack)) {
                return i;
            }
        }
        return -1;
    }

    /** True if the chest holds at least one item the recruit would actually take during upkeep. */
    private boolean containerHasUpkeepItem(Container c) {
        Item currency = FactionEvents.getCurrency().getItem();
        for (int i = 0; i < c.getContainerSize(); i++) {
            if (isUpkeepItem(c.getItem(i), currency)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Mirrors what {@link AbstractRecruitEntity#upkeepReequip(Container)} / checkPayment / the food
     * loop would actually pull: food, the payment currency, equippable gear upgrades
     * (swords/armor/tools) and arrows for ranged units.
     */
    private boolean isUpkeepItem(ItemStack stack, Item currency) {
        if (stack.isEmpty()) return false;
        if (this.recruit.canEatItemStack(stack)) return true;          // food
        if (stack.is(currency)) return true;                           // payment
        if (this.recruit.wantsToPickUp(stack)) {
            if (this.recruit.canEquipItem(stack)) return true;         // swords / armor / tools
            if (this.recruit instanceof IRangedRecruit && stack.is(ItemTags.ARROWS)) return true; // ammo
        }
        return false;
    }

    /** Mirrors the recruits mod: food goes into the recruit's back/food slots. */
    private boolean canAddFood() {
        SimpleContainer inv = this.recruit.getInventory();
        for (int i = FOOD_SLOT_START; i < FOOD_SLOT_END; i++) {
            if (i < inv.getContainerSize() && inv.getItem(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private StorageArea resolveStorage() {
        UUID id = this.recruit.getUpkeepUUID();
        if (id == null) return null;

        List<StorageArea> list = this.recruit.getCommandSenderWorld().getEntitiesOfClass(
                StorageArea.class,
                this.recruit.getBoundingBox().inflate(100.0D),
                area -> area.getUUID().equals(id));

        return list.isEmpty() ? null : list.get(0);
    }

    /** Opens/closes a chest with the vanilla animation + sound (mirrors AbstractChestGoal). */
    private void interactChest(boolean open) {
        if (this.currentChestPos == null) return;
        if (!(this.recruit.getCommandSenderWorld() instanceof ServerLevel level)) return;
        if (!(this.chestContainer instanceof CompoundContainer || this.chestContainer instanceof ChestBlockEntity)) return;

        BlockState state = level.getBlockState(this.currentChestPos);
        Block block = state.getBlock();

        boolean isOpened = false;
        CompoundTag tag = new CompoundTag();
        if (level.getBlockEntity(this.currentChestPos) instanceof ChestBlockEntity chest) {
            tag = chest.getPersistentData();
            isOpened = tag.getBoolean("isOpened");
        }

        if (open && !isOpened) {
            level.blockEvent(this.currentChestPos, block, 1, 1);
            level.playSound(null, this.currentChestPos, SoundEvents.CHEST_OPEN, this.recruit.getSoundSource(),
                    0.7F, 0.8F + 0.4F * this.recruit.getRandom().nextFloat());
            tag.putBoolean("isOpened", true);
        } else if (!open && isOpened) {
            level.blockEvent(this.currentChestPos, block, 1, 0);
            level.playSound(null, this.currentChestPos, SoundEvents.CHEST_CLOSE, this.recruit.getSoundSource(),
                    0.7F, 0.8F + 0.4F * this.recruit.getRandom().nextFloat());
            tag.putBoolean("isOpened", false);
        }

        level.gameEvent(this.recruit, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, this.currentChestPos);
    }
}