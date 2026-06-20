package com.talhanation.workers.entities;

import com.google.common.collect.ImmutableSet;
import com.talhanation.recruits.config.RecruitsClientConfig;
import com.talhanation.recruits.entities.AbstractChunkLoaderEntity;
import com.talhanation.workers.entities.ai.*;
import com.talhanation.workers.entities.ai.navigation.WorkerPathNavigation;
import com.talhanation.workers.entities.workarea.AbstractWorkAreaEntity;
import com.talhanation.workers.entities.workarea.HomeArea;
import com.talhanation.workers.world.NeededItem;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.MoveTowardsTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;


public abstract class AbstractWorkerEntity extends AbstractChunkLoaderEntity {

    public static final Set<Block> UNBREAKABLES = ImmutableSet.of(Blocks.BEDROCK, Blocks.BARRIER);
    @Nullable public UUID homeAreaUUID = null;
    private boolean moraleNightChecked = false;

    /** Set by WorkerClaimEvents when a siege is won — makes WorkerFleeGoal active. */
    public boolean isFleeing = false;
    @Nullable public BlockPos fleeTarget = null;

    public AbstractWorkerEntity(EntityType<? extends AbstractWorkerEntity> entityType, Level world) {
        super(entityType, world);
    }
    public List<NeededItem> neededItems = new ArrayList<>();
    public int farmedItems;
    public boolean forcedDeposit;
    public UUID lastStorage;
    @Override
    protected void registerGoals() {
        super.registerGoals();

        this.goalSelector.addGoal(0, new WorkerTakeCoverGoal(this));
        this.goalSelector.addGoal(0, new WorkerFleeGoal(this));
        this.goalSelector.addGoal(2, new DepositItemsToStorage(this));
        this.goalSelector.addGoal(2, new GetNeededItemsFromStorage(this));
        this.goalSelector.addGoal(1, new WorkerGoHomeGoal(this));

        this.goalSelector.removeGoal(new MoveTowardsTargetGoal(this, 0.9D, 32.0F));
    }

    public abstract AbstractWorkAreaEntity getCurrentWorkArea();

    /////////////////////////////////// TICK/////////////////////////////////////////
    @Override
    protected @NotNull PathNavigation createNavigation(@NotNull Level level) {
        // Workers use the recruits async pathfinder via this worker-side
        // navigation (exact arrival + underground-capable, which is what fixes
        // miners being dumb about buried targets).
        return new WorkerPathNavigation(this, level);
    }

    public boolean isWorking(){
        return this.getFollowState() == 6;
    }
    @Override
    public void aiStep() {
        super.aiStep();
        if(this.getCommandSenderWorld().isClientSide()) return;

        this.getCommandSenderWorld().getProfiler().push("looting");
        if (this.canPickUpLoot() && this.isAlive() && !this.dead) {
            List<ItemEntity> nearbyItems = this.getCommandSenderWorld().getEntitiesOfClass(
                    ItemEntity.class,
                    this.getBoundingBox().inflate(5.5D, 5.5D, 5.5D)
            );

            for (ItemEntity itementity : nearbyItems) {
                if (!itementity.isRemoved() && !itementity.getItem().isEmpty() && !itementity.hasPickUpDelay() && this.wantsToPickUp(itementity.getItem())) {
                    this.pickUpItem(itementity);
                }
            }
        }

        if(tickCount % 20 == 0){
            if(this.getCurrentWorkArea() != null){
                double distance = this.getHorizontalDistanceTo(getCurrentWorkArea().position());
                if(distance >= 1000) this.getCurrentWorkArea().isBeingWorkedOn = false;
            }
            this.tickMorale();
        }
    }

    public boolean canAddItem(ItemStack itemToAdd) {
        boolean flag = false;
        List<ItemStack> inventorySlots = new ArrayList<>();
        for(int i = 6; i < this.inventory.getContainerSize(); i++){
            inventorySlots.add(inventory.items.get(i));
        }

        for(ItemStack itemstack : inventorySlots) {
            if (itemstack.isEmpty() || ItemStack.isSameItemSameTags(itemstack, itemToAdd) && itemstack.getCount() < itemstack.getMaxStackSize()) {
                flag = true;
                break;
            }
        }

        return flag;
    }
    public boolean wantsToKeep(ItemStack itemStack) {
        return (itemStack.isEdible() && itemStack.getFoodProperties(this).getNutrition() > 4);
    }
    @Override
    public boolean wantsToPickUp(ItemStack itemStack) {
        if(wantsToKeep(itemStack)) return true;

        List<NeededItem> neededItems = this.neededItems;

        for (NeededItem needed : neededItems) {
            if (needed.matches(itemStack)) {
                return true;
            }
        }
        return super.wantsToPickUp(itemStack);
    }

    @Override
    protected void pickUpItem(ItemEntity itemEntity) {
        ItemStack itemstack = itemEntity.getItem();
        if (this.wantsToPickUp(itemstack)) {
            if (!this.canAddItem(itemstack)) return;

            this.onItemPickup(itemEntity);
            this.take(itemEntity, itemstack.getCount());
            ItemStack itemstack1 = this.addItem(itemstack);

            this.farmedItems += itemstack.getCount() - itemstack1.getCount();
            NeededItem.applyToNeededItems(itemstack, neededItems);

            if (itemstack1.isEmpty()) {
                itemEntity.remove(RemovalReason.DISCARDED);
            } else {
                itemstack.setCount(itemstack1.getCount());
            }
        }
    }

    public ItemStack addItem(ItemStack itemStackToAdd) {
        if (itemStackToAdd.isEmpty()) {
            return ItemStack.EMPTY;
        } else {
            ItemStack itemstack = itemStackToAdd.copy();
            this.moveItemToOccupiedSlotsWithSameType(itemstack);
            if (itemstack.isEmpty()) {
                return ItemStack.EMPTY;
            } else {
                this.moveItemToEmptySlots(itemstack);
                return itemstack.isEmpty() ? ItemStack.EMPTY : itemstack;
            }
        }
    }

    private void moveItemToOccupiedSlotsWithSameType(ItemStack itemStackToMove) {
        for(int i = 6; i < this.getInventory().getContainerSize(); ++i) {
            ItemStack itemstack = this.getInventory().getItem(i);
            if (ItemStack.isSameItemSameTags(itemstack, itemStackToMove)) {
                this.moveItemsBetweenStacks(itemStackToMove, itemstack);
                if (itemStackToMove.isEmpty()) {
                    return;
                }
            }
        }
    }

    private void moveItemToEmptySlots(ItemStack itemStack) {
        for(int i = 6; i < this.getInventory().getContainerSize(); ++i) {
            ItemStack itemstack = this.getInventory().getItem(i);
            if (itemstack.isEmpty()) {
                this.getInventory().setItem(i, itemStack.copyAndClear());
                return;
            }
        }

    }

    private void moveItemsBetweenStacks(ItemStack p_19186_, ItemStack p_19187_) {
        int i = Math.min(64, p_19187_.getMaxStackSize());
        int j = Math.min(p_19186_.getCount(), i - p_19187_.getCount());
        if (j > 0) {
            p_19187_.grow(j);
            p_19186_.shrink(j);
            this.getInventory().setChanged();
        }
    }

    @Nullable
    public SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor world, @NotNull DifficultyInstance diff, @NotNull MobSpawnType reason, @Nullable SpawnGroupData spawnData, @Nullable CompoundTag nbt) {
        return spawnData;
    }
    public void setDropEquipment()  {
        this.dropEquipment();
    }


    //////////////////////////////////// REGISTER////////////////////////////////////

    protected void defineSynchedData() {
        super.defineSynchedData();
    }

    public void addAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);

        nbt.putInt("farmedItems", farmedItems);
        if(lastStorage != null) nbt.putUUID("lastStorage", lastStorage);
        if(homeAreaUUID != null) nbt.putUUID("homeAreaUUID", homeAreaUUID);
    }

    public void readAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);

        this.farmedItems = nbt.getInt("farmedItems");
        if(nbt.contains("lastStorage")) this.lastStorage = nbt.getUUID("lastStorage");
        if(nbt.contains("homeAreaUUID")) this.homeAreaUUID = nbt.getUUID("homeAreaUUID");
    }

    @OnlyIn(Dist.CLIENT)
    public SoundEvent getHurtSound(DamageSource ds) {
        if(RecruitsClientConfig.RecruitsLookLikeVillagers.get()){
            return SoundEvents.VILLAGER_HURT;
        }
        else
            return SoundEvents.PLAYER_HURT;
    }

    @OnlyIn(Dist.CLIENT)
    protected SoundEvent getDeathSound() {
        if(RecruitsClientConfig.RecruitsLookLikeVillagers.get()){
            return SoundEvents.VILLAGER_DEATH;
        }
        else
            return SoundEvents.PLAYER_DEATH;
    }

    protected float getSoundVolume() {
        return 0.4F;
    }

    //////////////////////////////////// SET////////////////////////////////////

    public void setEquipment() {
    }
    public boolean needsToSleep() {
        return !this.getCommandSenderWorld().isDay();
    }

    public abstract Predicate<ItemEntity> getAllowedItems();

    public void initSpawn(){
        this.setEquipment();
        this.setDropEquipment();
        this.setPersistenceRequired();
        this.setCanPickUpLoot(true);
    }

    public double getDistanceToOwner(){
        return this.getOwner() != null ? this.distanceToSqr(this.getOwner()) : 1D;
    }

    public void tick() {
        super.tick();
        if(this.getCommandSenderWorld().isClientSide()) return;
    }

    public abstract List<Item> inventoryInputHelp();

    public boolean needsToGetToChest() {
        return this.needsToGetFood() || needsToDeposit() || needsToGetItems();
    }

    public boolean needsToDeposit() {
        return forcedDeposit || farmedItems > 128;
    }
    @Nullable
    public ItemStack getMatchingItem(Predicate<ItemStack> predicate) {
        for (ItemStack stack : this.getInventory().items) {
            if (!stack.isEmpty() && predicate.test(stack)) {
                return stack;
            }
        }
        return null;
    }

    public int countMatchingItems(Predicate<ItemStack> predicate) {
        int count = 0;

        for (ItemStack stack : this.getInventory().items) {
            if (!stack.isEmpty() && predicate.test(stack)) {
                count += stack.getCount();
            }
        }

        return count;
    }

    public int countMatchingStacks(Predicate<ItemStack> predicate) {
        int count = 0;

        for (ItemStack stack : this.getInventory().items) {
            if (!stack.isEmpty() && predicate.test(stack)) {
                count++;
            }
        }

        return count;
    }

    int currentTimeBreak;
    int breakingTime;
    int previousTimeBreak;

    public void mineBlock(BlockPos pos) {
        if (!this.isAlive()) return;

        Level level = this.getCommandSenderWorld();
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return;

        // Laubblöcke: mit Schere manuell abbauen und droppen
        if (state.getBlock() instanceof LeavesBlock && this.getMainHandItem().getItem() instanceof ShearsItem) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            List<ItemStack> drops = Block.getDrops(state, (ServerLevel) level, pos, blockEntity, this, this.getMainHandItem());

            for (ItemStack drop : drops) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), drop);
            }

            state.onRemove(level, pos, Blocks.AIR.defaultBlockState(), false); // z.B. für Sounds/Particles
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            level.playSound(null, pos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 1.0F, 1.0F); // optionaler Scher-Sound

            return;
        }

        // Normaler Abbau
        if (currentTimeBreak % 5 == 4) {
            level.playLocalSound(pos.getX(), pos.getY(), pos.getZ(),
                    state.getSoundType().getHitSound(), SoundSource.BLOCKS, 1F, 0.75F, false);
        }

        if (breakingTime == 0) {
            breakingTime = (int) (state.getDestroySpeed(level, pos) * 30);
        }

        float destroySpeed = this.getUseItem().getDestroySpeed(state) * 2;
        currentTimeBreak += (int) destroySpeed;

        int stage = (int) ((float) currentTimeBreak / breakingTime * 10);
        if (stage != previousTimeBreak) {
            level.destroyBlockProgress(1, pos, stage);
            previousTimeBreak = stage;
        }

        if (currentTimeBreak >= breakingTime) {
            this.finalizeBlockBreak(pos);
            currentTimeBreak = 0;
            breakingTime = 0;
            previousTimeBreak = 0;
        }

        this.swing(InteractionHand.MAIN_HAND);
    }

    protected void finalizeBlockBreak(BlockPos pos) {
        Level level = this.getCommandSenderWorld();
        level.destroyBlock(pos, true, this);
        this.damageMainHandItem();
    }

    public boolean needsToGetItems() {
        return neededItems.stream().anyMatch(neededItem -> neededItem.required);
    }

    private long lastNotifyDay = Long.MIN_VALUE;
    private boolean notifyGateOpen = true;

    public void resetNotifyGate() {
        this.notifyGateOpen = true;
    }

    public boolean canNotifyOwner() {
        long day = this.getCommandSenderWorld().getDayTime() / 24000L;
        if (day != lastNotifyDay) return true; // a new day always re-opens the gate
        return notifyGateOpen;
    }

    public void notifyOwner(Component message) {
        if (!canNotifyOwner()) return;
        Player owner = this.getOwner();
        if (owner != null) {
            owner.sendSystemMessage(message);
            this.lastNotifyDay = this.getCommandSenderWorld().getDayTime() / 24000L;
            this.notifyGateOpen = false;
        }
    }

    @Override
    public @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        // The owner is interacting → let the worker speak up again about anything it needs.
        if (!this.getCommandSenderWorld().isClientSide() && player.getUUID().equals(this.getOwnerUUID())) {
            this.resetNotifyGate();
        }
        return super.mobInteract(player, hand);
    }

    public void addNeededItem(NeededItem neededItem) {
        // Same item + same source -> merge by taking the higher count instead of
        // appending a duplicate. Different sources stay separate so e.g. two
        // crop fields each asking for 8 bone meal accumulate to 16.
        for (NeededItem existing : neededItems) {
            if (existing.isSameRequest(neededItem)) {
                existing.count = Math.max(existing.count, neededItem.count);
                return;
            }
        }
        neededItems.add(neededItem);
    }
    //@Override
    public void onItemStackAdded(ItemStack itemStack1){
        //super.onItemStackAdded(itemStack);
        ItemStack itemStack = itemStack1.copy();
        for(NeededItem neededItem : neededItems){
            if(neededItem.matches(itemStack)){
                NeededItem.applyToNeededItems(itemStack, neededItems);;
                break;
            }
        }
    }

    public void switchMainHandItem(Predicate<ItemStack> predicate) {
        if (!this.isAlive() || predicate == null) return;

        SimpleContainer inventory = this.getInventory();
        ItemStack mainHand = this.getMainHandItem();
        if (predicate.test(mainHand)) return;

        for (int i = 6; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (predicate.test(stack)) {

                inventory.setItem(i, mainHand);
                this.setItemInHand(InteractionHand.MAIN_HAND, stack);
                return;
            }
        }
    }

    public double getHorizontalDistanceTo(Vec3 target){
        Vec3 position = new Vec3(position().x, 0, position().z);
        Vec3 toTarget = new Vec3(target.x, 0, target.z);

        return position.distanceToSqr(toTarget);
    }

    @Override
    public void die(DamageSource dmg) {
        super.die(dmg);
        if(this.getCurrentWorkArea() != null) getCurrentWorkArea().setBeingWorkedOn(false);
        // Release the home assignment so another worker can claim it
        releaseHomeArea();
    }

    public void releaseHomeArea() {
        if (homeAreaUUID == null) return;
        if (this.getCommandSenderWorld() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.getEntitiesOfClass(HomeArea.class, this.getBoundingBox().inflate(256))
                    .stream()
                    .filter(a -> homeAreaUUID.equals(a.getUUID()))
                    .findFirst()
                    .ifPresent(a -> a.clearResident());
        }
        homeAreaUUID = null;
    }

    public void startFleeing(BlockPos target) {
        this.isFleeing  = true;
        this.fleeTarget = target;
        this.setFollowState(6);
    }

    public void stopFleeing() {
        this.isFleeing  = false;
        this.fleeTarget = null;
    }

    private void tickMorale() {
        if (this.getCommandSenderWorld().isClientSide()) return;

        if (needsToSleep() && !moraleNightChecked) {
            moraleNightChecked = true;
            float morale = this.getMorale();

            if (homeAreaUUID == null) {
                // No home assigned → morale drops
                morale = Math.max(0, morale - 10);

                if (morale <= 50) {
                    Player owner = this.getOwner();
                    if (owner != null) {
                        owner.sendSystemMessage(Component.translatable("chat.workers.text.noHome", this.getName().getString()));
                    }
                }
            }
            else {
                // Has a home → morale recovers

                if(morale < 60){
                    morale = Math.min(100, morale + 5);
                }
            }

            this.setMoral(morale);
        }

        if (!needsToSleep()) {
            moraleNightChecked = false;
        }
    }

    public static boolean isPosBroken(BlockPos pos, Level level, boolean allowWater) {
        BlockState state = level.getBlockState(pos);
        if(state.isAir() || UNBREAKABLES.contains(state.getBlock())) return true;
        if(allowWater){
            Fluid fluidState = level.getFluidState(pos).getType();
            return fluidState == Fluids.WATER || fluidState == Fluids.FLOWING_WATER;
        }
        return false;
    }

    public boolean hasFreeInvSlot() {
        for (int i = 6; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public boolean shouldWork() {
        return this.isOwned() && (this.getFollowState() == 0 || this.getFollowState() == 6);
    }
}