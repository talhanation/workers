package com.talhanation.workers.entities;

import com.talhanation.workers.CommandEvents;
import com.talhanation.workers.Main;
import com.talhanation.workers.config.WorkersModConfig;
import com.talhanation.workers.entities.ai.*;
import com.talhanation.workers.entities.ai.navigation.SailorPathNavigation;
import com.talhanation.workers.entities.ai.navigation.WorkersPathNavigation;
import com.talhanation.workers.entities.ai.navigation.door.WorkersOpenDoorGoal;
import com.talhanation.workers.inventory.WorkerHireContainer;
import com.talhanation.workers.network.MessageHireGui;
import com.talhanation.workers.network.MessageToClientUpdateHireScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;
import org.checkerframework.checker.units.qual.C;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static com.talhanation.workers.Translatable.*;

public abstract class AbstractWorkerEntity extends AbstractChunkLoaderEntity {
    private static final EntityDataAccessor<Optional<BlockPos>> START_POS = SynchedEntityData.defineId(AbstractWorkerEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Optional<BlockPos>> DEST_POS = SynchedEntityData.defineId(AbstractWorkerEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Optional<BlockPos>> HOME = SynchedEntityData.defineId(AbstractWorkerEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Optional<BlockPos>> CHEST = SynchedEntityData.defineId(AbstractWorkerEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Optional<BlockPos>> BED = SynchedEntityData.defineId(AbstractWorkerEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Integer> breakingTime = SynchedEntityData.defineId(AbstractWorkerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> currentTimeBreak = SynchedEntityData.defineId(AbstractWorkerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> previousTimeBreak = SynchedEntityData.defineId(AbstractWorkerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> HUNGER = SynchedEntityData.defineId(AbstractWorkerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> OWNER_NAME = SynchedEntityData.defineId(AbstractWorkerEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> PROFESSION_NAME = SynchedEntityData.defineId(AbstractWorkerEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Byte> STATUS = SynchedEntityData.defineId(AbstractWorkerEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> NEEDS_TOOL = SynchedEntityData.defineId(AbstractWorkerEntity.class, EntityDataSerializers.BOOLEAN);

    public boolean needsSecondTool;
    public boolean needsMainTool;
    int hurtTimeStamp = 0;
    public boolean isPickingUp;
    public boolean startPosChanged;
    private int farmedItems;
    protected Status status;
    public Status prevStatus;
    public boolean shouldDepositBeforeSleep;

    public AbstractWorkerEntity(EntityType<? extends AbstractWorkerEntity> entityType, Level world) {
        super(entityType, world);
        this.xpReward = 2;
        this.maxUpStep = 1.25F;
    }

    @Override
    @NotNull
    protected PathNavigation createNavigation(@NotNull Level level) {
        return new WorkersPathNavigation(this, level);
    }

    @Override
    @NotNull
    public PathNavigation getNavigation() {
        if (this instanceof IBoatController sailor && this.getVehicle() instanceof Boat) {
            return new SailorPathNavigation(sailor, level);
        }
        else
            return super.getNavigation();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new EatGoal(this));
        this.goalSelector.addGoal(0, new SleepGoal(this));
        this.goalSelector.addGoal(0, new WorkerFloatGoal(this));
        this.goalSelector.addGoal(0, new WorkersOpenDoorGoal(this, true));
        this.goalSelector.addGoal(1, new DepositItemsInChestGoal(this));
        this.goalSelector.addGoal(2, new WorkerFollowOwnerGoal(this, 1.0F));
        this.goalSelector.addGoal(11, new WorkerLookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(12, new WorkerRandomLookAroundGoal(this));
        this.goalSelector.addGoal(10, new WorkerLookAtPlayerGoal(this, LivingEntity.class, 8.0F));
    }

    /////////////////////////////////// TICK/////////////////////////////////////////

    public double getMyRidingOffset() {
        return -0.35D;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        //Main.LOGGER.debug("Running goals are: {}", this.goalSelector.getRunningGoals().map(WrappedGoal::getGoal).toArray());
        this.level.getProfiler().push("looting");
        if (
            !this.level.isClientSide && 
            this.canPickUpLoot() && 
            this.isAlive() && 
            !this.dead && 
            ForgeEventFactory.getMobGriefingEvent(this.level, this)
        ) {
            List<ItemEntity> nearbyItems = this.level.getEntitiesOfClass(
                ItemEntity.class,
                this.getBoundingBox().inflate(5.5D, 5.5D, 5.5D)
            );
            for (ItemEntity itementity : nearbyItems) {
                if (
                    !itementity.isRemoved() && 
                    !itementity.getItem().isEmpty() && 
                    !itementity.hasPickUpDelay() && 
                    this.wantsToPickUp(itementity.getItem())
                ) {
                    this.pickUpItem(itementity);
                }
            }
        }
    }

    @Override
    protected void pickUpItem(ItemEntity itemEntity) {
        ItemStack itemstack = itemEntity.getItem();
        if (this.wantsToPickUp(itemstack)) {
            SimpleContainer inventory = this.getInventory();
            if (!inventory.canAddItem(itemstack)) return;

            this.onItemPickup(itemEntity);
            this.take(itemEntity, itemstack.getCount());
            ItemStack itemstack1 = inventory.addItem(itemstack);
            if (itemstack1.isEmpty()) {
                itemEntity.remove(RemovalReason.DISCARDED);
            } else {
                itemstack.setCount(itemstack1.getCount());
                this.increaseFarmedItems();
            }
        }
    }

    // TODO: Boolean for worker "can work without tools" like lumberjacks punching trees, or farmers.
    // TODO: GoalAI#canUse() should check for this boolean.
    public void consumeToolDurability() {
        Iterable<ItemStack> hands = this.getHandSlots();
        // Damage the tool
        hands.forEach(itemStack ->  itemStack.hurtAndBreak(1, this, (worker) -> {
            this.broadcastBreakEvent(EquipmentSlot.MAINHAND);
            this.broadcastBreakEvent(InteractionHand.MAIN_HAND);
            this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);

            if (this.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) {
                if(isRequiredMainTool(itemStack)) this.needsMainTool = true;
                if(isRequiredSecondTool(itemStack)) this.needsSecondTool = true;

            }
            this.stopUsingItem();
        }));
        this.upgradeTool();
        this.updateNeedsTool();
    }

    public boolean canWorkWithoutTool(){
        return true;
    }

    public void rideTick() {
        super.rideTick();
        if (this.getVehicle() instanceof PathfinderMob creatureentity) {
            this.yBodyRot = creatureentity.yBodyRot;
        }
    }

    @Nullable
    public SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor world, @NotNull DifficultyInstance diff, @NotNull MobSpawnType reason, @Nullable SpawnGroupData spawnData, @Nullable CompoundTag nbt) {
        return spawnData;
    }
    public void setDropEquipment() {
        this.dropEquipment();
    }


    //////////////////////////////////// REGISTER////////////////////////////////////

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(HOME, Optional.empty());
        this.entityData.define(START_POS, Optional.empty());
        this.entityData.define(DEST_POS, Optional.empty());
        this.entityData.define(CHEST, Optional.empty());
        this.entityData.define(BED, Optional.empty());
        this.entityData.define(breakingTime, 0);
        this.entityData.define(currentTimeBreak, -1);
        this.entityData.define(previousTimeBreak, -1);
        this.entityData.define(HUNGER, 75);
        this.entityData.define(OWNER_NAME, "");
        this.entityData.define(PROFESSION_NAME, "");
        this.entityData.define(STATUS, (byte) 0);
        this.entityData.define(NEEDS_TOOL, false);
    }

    public void addAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putByte("Status", this.getStatusByte());
        if(prevStatus != null) nbt.putByte("prevStatus", this.prevStatus.getIndex());

        nbt.putBoolean("shouldDepositBeforeSleep", this.shouldDepositBeforeSleep);

        nbt.putBoolean("needsSecondTool", this.needsSecondTool);
        nbt.putBoolean("needsMainTool", this.needsMainTool);
        nbt.putInt("breakTime", this.getBreakingTime());
        nbt.putInt("currentTimeBreak", this.getCurrentTimeBreak());
        nbt.putInt("previousTimeBreak", this.getPreviousTimeBreak());
        nbt.putString("OwnerName", this.getOwnerName());
        nbt.putFloat("Hunger", this.getHunger());
        nbt.putString("ProfessionName", this.getProfessionName());
        nbt.putInt("FarmedItems", this.getFarmedItems());

        BlockPos startPos = this.getStartPos();
        if (startPos != null) this.setNbtPosition(nbt, "Start", startPos);
        BlockPos destPos = this.getDestPos();
        if (destPos != null) this.setNbtPosition(nbt, "Dest", destPos);
        BlockPos chestPos = this.getChestPos();
        if (chestPos != null) this.setNbtPosition(nbt, "Chest", chestPos);
        BlockPos bedPos = this.getBedPos();
        if (bedPos != null) this.setNbtPosition(nbt, "Bed", bedPos);
    }

    public void setNbtPosition(CompoundTag nbt, String blockName, @Nullable BlockPos pos) {
        if (pos == null) return;
        nbt.putInt(String.format("%sPosX", blockName), pos.getX());
        nbt.putInt(String.format("%sPosY", blockName), pos.getY());
        nbt.putInt(String.format("%sPosZ", blockName), pos.getZ());
    }

    public void readAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        if(nbt.contains("Status")) this.setStatus(nbt.getByte("Status"));
        else this.setStatus(Status.FOLLOW);

        if(nbt.contains("prevStatus")) this.prevStatus = Status.fromIndex(nbt.getByte("prevStatus"));
        else this.prevStatus = Status.IDLE;

        this.shouldDepositBeforeSleep = nbt.getBoolean("shouldDepositBeforeSleep");

        this.setBreakingTime(nbt.getInt("breakTime"));
        this.needsSecondTool = nbt.getBoolean("needsSecondTool");
        this.needsMainTool = nbt.getBoolean("needsMainTool");
        this.setCurrentTimeBreak(nbt.getInt("currentTimeBreak"));
        this.setPreviousTimeBreak(nbt.getInt("previousTimeBreak"));
        this.setHunger(nbt.getInt("Hunger"));
        this.setOwnerName(nbt.getString("OwnerName"));
        this.setProfessionName(nbt.getString("ProfessionName"));
        this.setFarmedItems(nbt.getInt("FarmedItems"));

        BlockPos startPos = this.getNbtPosition(nbt, "Start");
        if (startPos != null) this.setStartPos(startPos);
        BlockPos destPos = this.getNbtPosition(nbt, "Dest");
        if (destPos != null) this.setDestPos(destPos);
        BlockPos homePos = this.getNbtPosition(nbt, "Home");
        if (homePos != null) this.setHomePos(homePos);
        BlockPos chestPos = this.getNbtPosition(nbt, "Chest");
        if (chestPos != null) this.setChestPos(chestPos);
        BlockPos bedPos = this.getNbtPosition(nbt, "Bed");
        if (bedPos != null) this.setBedPos(bedPos);
    }

    public BlockPos getNbtPosition(CompoundTag nbt, String blockName) {
        if (
            nbt.contains(String.format("%sPosX", blockName)) &&
            nbt.contains(String.format("%sPosY", blockName)) &&
            nbt.contains(String.format("%sPosZ", blockName))
        ) {
            return new BlockPos(
                nbt.getInt(String.format("%sPosX", blockName)),
                nbt.getInt(String.format("%sPosY", blockName)),
                nbt.getInt(String.format("%sPosZ", blockName))
            );
        }
        return null;
    }

    //////////////////////////////////// GET////////////////////////////////////

    public int getFarmedItems() {
        return farmedItems;
    }

    public String getProfessionName() {
        return entityData.get(PROFESSION_NAME);
    }

    public String getOwnerName() {
        return entityData.get(OWNER_NAME);
    }
    @Nullable
    public BlockPos getChestPos() {
        return this.entityData.get(CHEST).orElse(null);
    }

    @Nullable
    public BlockPos getBedPos() {
        return this.entityData.get(BED).orElse(null);
    }

    public int getCurrentTimeBreak() {
        return this.entityData.get(currentTimeBreak);
    }

    public int getPreviousTimeBreak() {
        return this.entityData.get(previousTimeBreak);
    }

    public int getBreakingTime() {
        return this.entityData.get(breakingTime);
    }

    public int getHunger() {
        return this.entityData.get(HUNGER);
    }

    public BlockPos getWorkerOnPos() {
        return this.getOnPos();
    }

    public BlockPos getDestPos() {
        return this.entityData.get(DEST_POS).orElse(null);
    }

    public BlockPos getStartPos() {
        return this.entityData.get(START_POS).orElse(null);
    }

    public byte getStatusByte() {
        return this.entityData.get(STATUS);
    }

    public Status getStatus(){
        return status;
    }

    public SoundEvent getHurtSound(DamageSource ds) {
        if(WorkersModConfig.WorkersLookLikeVillagers.get()){
            return SoundEvents.VILLAGER_HURT;
        }
        else
            return SoundEvents.PLAYER_HURT;
    }

    protected SoundEvent getDeathSound() {
        if(WorkersModConfig.WorkersLookLikeVillagers.get()){
            return SoundEvents.VILLAGER_DEATH;
        }
        else
            return SoundEvents.PLAYER_DEATH;
    }

    protected float getSoundVolume() {
        return 0.4F;
    }

    protected float getStandingEyeHeight(Pose pos, EntityDimensions size) {
        return size.height * 0.9F;
    }

    public int getMaxHeadXRot() {
        return this.isInSittingPose() ? 20 : super.getMaxHeadXRot();
    }

    /**
     * This is used to determine whether the worker should store an ItemStack 
     * in a chest or keep it in its inventory.
     * 
     * For example, lumberjacks need saplings to replant trees, farmers need seeds, etc.
     * 
     * @param itemStack The ItemStack to compare against
     * @return true if the ItemStack will be kept in inventory, false if it will be stored in a chest.
     */
    public boolean wantsToKeep(ItemStack itemStack) {
        return (itemStack.isEdible() && itemStack.getFoodProperties(this).getNutrition() > 4);
    }

    //////////////////////////////////// SET////////////////////////////////////

    public void setFarmedItems(int x) {
        this.farmedItems = x;
    }
    public void setProfessionName(String string) {
        this.entityData.set(PROFESSION_NAME, string);
    }

    public void setOwnerName(String string) {
        this.entityData.set(OWNER_NAME, string);
    }

    public void setHomePos(BlockPos pos) {
        this.entityData.set(HOME, Optional.of(pos));            
    }

    public void setChestPos(BlockPos pos) {
        this.entityData.set(CHEST, Optional.of(pos));
    }
    
    public void setBedPos(BlockPos pos) {
        this.entityData.set(BED, Optional.of(pos));
    }

    public void setPreviousTimeBreak(int value) {
        this.entityData.set(previousTimeBreak, value);
    }

    public void setCurrentTimeBreak(int value) {
        this.entityData.set(currentTimeBreak, value);
    }

    public void setBreakingTime(int value) {
        this.entityData.set(breakingTime, value);
    }

    public void setHunger(int value) {
        this.entityData.set(HUNGER, value);
    }

    public void setDestPos(BlockPos pos) {
        this.entityData.set(DEST_POS, Optional.of(pos));
    }

    public void setStartPos(BlockPos pos) {
        if(this.getStartPos() != pos){
            this.startPosChanged = true;
        }
        this.entityData.set(START_POS, Optional.ofNullable(pos));
    }

    public void clearStartPos() {
        this.entityData.set(START_POS, Optional.empty());
    }

    public void clearChestPos() {
        this.entityData.set(CHEST, Optional.empty());
    }

    public void setStatus(byte x) {
        this.entityData.set(STATUS, x);
        if(prevStatus != status)this.prevStatus = this.status;
        this.status = Status.fromIndex(x);
    }

    public void setStatus(Status status){
        this.setStatus(status.getIndex());
    }

    public void setStatus(Status status, boolean withDialog){
        this.setStatus(status);
        LivingEntity owner = getOwner();
        if(withDialog && owner != null) {
            switch(status){
                case WORK -> {
                    if(prevStatus == Status.FOLLOW) this.tellPlayer(owner, TEXT_CONTINUE);
                    else this.tellPlayer(owner, TEXT_WORKING);
                }
                case FOLLOW -> {
                    this.tellPlayer(owner, TEXT_FOLLOW);
                }

                case WANDER,IDLE,DEPOSIT -> {
                    this.tellPlayer(owner, TEXT_WANDER);
                }

            }
        }
    }
    public void setOwned(boolean owned) {
        super.setTame(owned);
    }

    public void setEquipment() {
    }

    //////////////////////////////////// ATTACK ////////////////////////////////////
    //////////////////////////////////// FUNCTIONS////////////////////////////////////

    public boolean hurt(DamageSource dmg, float amt) {
        String name = this.getDisplayName().getString();
        String attacker_name;

        if (this.isInvulnerableTo(dmg)) {
            return false;
        } else {
            Entity entity = dmg.getEntity();

            if (entity != null && !(entity instanceof Player) && !(entity instanceof AbstractArrow)) {
                amt = (amt + 1.0F) / 2.0F;
            }

            for(ItemStack armor : this.getArmorSlots()){
                armor.hurtAndBreak(1, this, (worker) -> {
                       if(armor.getItem() instanceof ArmorItem armorItem){
                           EquipmentSlot slot = armorItem.getSlot();
                           setItemSlot(slot, ItemStack.EMPTY);
                       }
                });
            }
            LivingEntity attacker = this.getLastHurtByMob();

            if (this.isTame() && attacker != null && hurtTimeStamp <= 0) {
                attacker_name = attacker.getDisplayName().getString();

                LivingEntity owner = this.getOwner();
                if (owner != null && owner != attacker) {
                    this.tellPlayer(owner, TEXT_ATTACKED(name, attacker_name));
                    hurtTimeStamp = 80;
                }
            }

            return super.hurt(dmg, amt);
        }
    }

    public boolean doHurtTarget(Entity entity) {
        boolean flag = entity.hurt(
            DamageSource.mobAttack(this),
            (float) ((int) this.getAttributeValue(Attributes.ATTACK_DAMAGE))
        );
        if (flag) {
            this.doEnchantDamageEffects(this, entity);
        }

        return flag;
    }

    public void die(@NotNull DamageSource dmg) {

        // TODO: Liberate POI on death.
        super.die(dmg);

    }

    //////////////////////////////////// OTHER     ////////////////////////////////////
    //////////////////////////////////// FUNCTIONS ////////////////////////////////////

    public boolean needsToSleep() {
        return !this.level.isDay();
    }

     public void updateHunger() {
         Status status = getStatus();
         int hunger = getHunger();
         if (status == Status.WORK || getBedPos() == null) {
             hunger -= 5;
         }
         else{
             hunger -= 3;
         }

         if (hunger < 0)
             hunger = 0;

         setHunger(hunger);
     }
    public void tellPlayer(LivingEntity player, Component message) {
		if(WorkersModConfig.OwnerReceiveInfo.get()){
			Component dialogue = new TextComponent(this.getName().getString())
				.append(": ")
				.append(message);
			player.sendSystemMessage(dialogue);
		}
    }

    public void walkTowards(BlockPos pos, double speed) {
        if(pos != null){
            this.getNavigation().moveTo(pos.getX(), pos.getY(), pos.getZ(), speed);
            this.getLookControl().setLookAt(
                    pos.getX(),
                    pos.getY() + 1,
                    pos.getZ(),
                    10.0F,
                    this.getMaxHeadXRot()
            );
        }
    }
    public boolean needsToGetFood(){
        boolean isChest = this.getChestPos() != null;
        return this.needsToEat() && (isChest);
    }
    public boolean needsToEat() {
        return (getHunger() <= 25F || getHealth() < getMaxHealth() * 0.2) || isStarving();
    }

    public boolean isStarving() {
        return (getHunger() <= 5);
    }

    public boolean isSaturated() {
        return (getHunger() >= 90F);
    }

    public void resetWorkerParameters() {
        this.resetFarmedItems();
        this.setBreakingTime(0);
        this.setCurrentTimeBreak(-1);
        this.setPreviousTimeBreak(-1);
    }

    public boolean needsToDeposit(){
        boolean needsTool = this.needsMainTool || this.needsSecondTool;
        boolean farmed = this.getFarmedItems() >= getFarmedItemsDepositAmount();
        boolean needsToEat = needsToEat();
        return (needsTool || farmed || needsToEat);
    }

    protected int getFarmedItemsDepositAmount() {
        return 64;
    }

    public void increaseFarmedItems(){
        this.setFarmedItems(getFarmedItems() + 1);
    }

    public void resetFarmedItems(){
        this.setFarmedItems(0);
    }

    @Override
    public boolean canBeLeashed(Player player) {
        return false;
    }

    public abstract int workerCosts();

    @Override
    public boolean canBreed() {
        return false;
    }

    @Override
    protected void spawnTamingParticles(boolean smoke) {

    }

    public void workerSwingArm() {
        if (!this.swinging) {
            this.swing(InteractionHand.MAIN_HAND);
        }
    }

    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.level.isClientSide) {
            return InteractionResult.CONSUME;
        } else {
            if (this.isTame() && player.getUUID().equals(this.getOwnerUUID())) {
                if (player.isCrouching()) {
                    openGUI(player);
                }
                if (!player.isCrouching()) {
                    Status status = getStatus();
                    if(status != Status.FOLLOW) this.setStatus(Status.FOLLOW, true);
                    else this.setStatus(Objects.requireNonNullElse(prevStatus, Status.WANDER), true);
                    return InteractionResult.SUCCESS;
                }
            } else if (this.isTame() && !player.getUUID().equals(this.getOwnerUUID())) {
                this.tellPlayer(player, TEXT_HELLO_OWNED(this.getProfessionName(), this.getOwnerName()));
            } else if (!this.isTame()) {
                this.tellPlayer(player, TEXT_HELLO(this.getProfessionName()));
                this.openHireGUI(player);
                this.navigation.stop();
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }
    }

    public boolean hire(Player player) {
        this.makeHireSound();

        this.tame(player);
        this.setOwnerName(player.getDisplayName().getString());
        this.setOrderedToSit(false);
        this.setOwnerUUID(player.getUUID());
        this.setOwned(true);
        this.setStatus(Status.FOLLOW);
        this.navigation.stop();

        int i = this.random.nextInt(4);
        switch (i) {
            case 1 -> this.tellPlayer(player, TEXT_RECRUITED1);
            case 2 -> this.tellPlayer(player, TEXT_RECRUITED2);
            case 3 -> this.tellPlayer(player, TEXT_RECRUITED3);
        }

        return true;
    }

    public void makeHireSound() {
        this.level.playSound(
            null, 
            this.getX(), 
            this.getY() + 4, 
            this.getZ(),
            WorkersModConfig.WorkersLookLikeVillagers.get() ? SoundEvents.VILLAGER_AMBIENT : SoundEvents.PLAYER_BREATH,
            this.getSoundSource(), 
            15.0F, 
            0.8F + 0.4F * this.random.nextFloat()
        );
    }
    public abstract Predicate<ItemEntity> getAllowedItems();

    public abstract void openGUI(Player player);

    public void initSpawn(){
        this.setEquipment();
        this.setDropEquipment();
        this.setPersistenceRequired();
        this.setCanPickUpLoot(true);
    }

    public void openHireGUI(Player player) {
        this.navigation.stop();
        if (player instanceof ServerPlayer serverPlayer) {
            Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(()-> (ServerPlayer) player), new MessageToClientUpdateHireScreen(CommandEvents.getWorkersCurrency() ,this.workerCosts()));

            MenuProvider containerSupplier = new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return AbstractWorkerEntity.this.getCustomName();
                }

                @Nullable
                @Override
                public AbstractContainerMenu createMenu(
                    int i, 
                    @NotNull Inventory playerInventory,
                    @NotNull Player playerEntity
                ) {
                    return new WorkerHireContainer(
                        i, 
                        playerInventory.player, 
                        AbstractWorkerEntity.this,
                        playerInventory
                    );
                }
            };

            Consumer<FriendlyByteBuf> extraDataWriter = packetBuffer -> {
                packetBuffer.writeUUID(getUUID());
            };

            NetworkHooks.openGui((ServerPlayer) player, containerSupplier, extraDataWriter);
        } else {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageHireGui(player, this.getUUID()));
        }
    }

    public double getDistanceToOwner(){
        return this.getOwner() != null ? this.distanceToSqr(this.getOwner()) : 1D;
    }

    public abstract boolean isRequiredMainTool(ItemStack tool);
    public abstract boolean isRequiredSecondTool(ItemStack tool);


    public boolean hasMainToolInInv() {
        SimpleContainer inventory = this.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (this.isRequiredMainTool(itemStack)) return true;
        }
        return false;
    }

    public boolean hasSecondToolInInv() {
        SimpleContainer inventory = this.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (this.isRequiredSecondTool(itemStack)) return true;
        }
        return false;
    }

    public void updateNeedsTool(){
        this.entityData.set(NEEDS_TOOL, this.needsMainTool || this.needsSecondTool);
    }

    public void tick() {
        super.tick();
        updateSwingTime();
        Main.LOGGER.info("Status: " + this.status);
        if(this.getOwner() != null && getOwner().getTeam() != null && this.getTeam() != null && !getOwner().getTeam().isAlliedTo(this.getTeam())){
            PlayerTeam playerteam = level.getScoreboard().getPlayerTeam(getOwner().getTeam().getName());
            if(playerteam != null) getCommandSenderWorld().getScoreboard().addPlayerToTeam(this.getStringUUID(), playerteam);
        }

        if(!this.getMainHandItem().isEmpty() && this.entityData.get(NEEDS_TOOL)){
            this.broadcastBreakEvent(EquipmentSlot.MAINHAND);
            this.broadcastBreakEvent(InteractionHand.MAIN_HAND);
            this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            //WORKAROUND FOR ITEM BREAKS BUT STILL IN HAND
        }

        //Main.LOGGER.info("Hunger: " + this.getHunger());

        if (hurtTimeStamp > 0) hurtTimeStamp--;


        if(status != null){

            switch (status){
                case IDLE,WANDER -> {
                    if(needsToSleep()){
                        this.setStatus(Status.SLEEP);
                    }
                    else if(needsToDeposit()){
                        this.setStatus(Status.DEPOSIT);
                    }
                    else if(getStartPos() != null){
                        this.setStatus(Status.WORK);
                    }
                }

                case SLEEP -> {
                    if(shouldDepositBeforeSleep && canDepositBeforeSleep()){
                        setStatus(Status.DEPOSIT);
                        shouldDepositBeforeSleep = false;
                    }
                }

                case WORK -> {
                    if(needsToSleep()){
                        this.setStatus(Status.SLEEP);
                    }
                    else if(needsToDeposit()){
                        this.setStatus(Status.DEPOSIT);
                    }
                }

                case FOLLOW, DEPOSIT -> {

                }
            }
        }
    }

    private boolean canDepositBeforeSleep() {
        return getFarmedItems() > 0 && getChestPos() != null;
    }
    public abstract boolean hasASecondTool();
    public abstract boolean hasAMainTool();
    public enum Status{
        IDLE((byte) 0),
        WANDER((byte) 1),
        FOLLOW((byte) 2),
        WORK((byte) 3),
        DEPOSIT((byte) 4),
        SLEEP((byte) 5);

        private final byte index;
        Status(byte index){
            this.index = index;
        }

        public byte getIndex(){
            return this.index;
        }
        public static Status fromIndex(byte index) {
            for (Status state : Status.values()) {
                if (state.getIndex() == index) {
                    return state;
                }
            }
            throw new IllegalArgumentException("Invalid Status index: " + index);
        }
    }

    public abstract List<Item> inventoryInputHelp();

}
