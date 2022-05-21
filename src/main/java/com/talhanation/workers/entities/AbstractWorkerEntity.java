package com.talhanation.workers.entities;

import com.talhanation.workers.entities.ai.WorkerMoveToCampGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Predicate;

public abstract class AbstractWorkerEntity extends AbstractChunkLoaderEntity {
    private static final EntityDataAccessor<Optional<BlockPos>> START_POS = SynchedEntityData.defineId(AbstractWorkerEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Optional<BlockPos>> DEST_POS = SynchedEntityData.defineId(AbstractWorkerEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Optional<BlockPos>> CAMP = SynchedEntityData.defineId(AbstractWorkerEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Boolean> FOLLOW = SynchedEntityData.defineId(AbstractWorkerEntity.class, EntityDataSerializers.BOOLEAN);
    public  static final EntityDataAccessor<Boolean> IS_WORKING = SynchedEntityData.defineId(AbstractWorkerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_PICKING_UP = SynchedEntityData.defineId(AbstractWorkerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> breakingTime = SynchedEntityData.defineId(MinerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> currentTimeBreak = SynchedEntityData.defineId(MinerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> previousTimeBreak = SynchedEntityData.defineId(MinerEntity.class, EntityDataSerializers.INT);
    int hurtTimeStamp = 0;


    public AbstractWorkerEntity(EntityType<? extends AbstractWorkerEntity> entityType, Level world) {
        super(entityType, world);
        this.setOwned(false);
        this.xpReward = 2;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new WorkerMoveToCampGoal(this, 6.0F));
    }


    ///////////////////////////////////TICK/////////////////////////////////////////

    public double getMyRidingOffset(){
        return -0.35D;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        this.level.getProfiler().push("looting");
        if (!this.level.isClientSide && this.canPickUpLoot() && this.isAlive() && !this.dead && net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(this.level, this)) {
            for(ItemEntity itementity : this.level.getEntitiesOfClass(ItemEntity.class, this.getBoundingBox().inflate(2.5D, 2.5D, 2.5D))) {
                if (!itementity.isRemoved() && !itementity.getItem().isEmpty() && !itementity.hasPickUpDelay() && this.wantsToPickUp(itementity.getItem())) {
                    this.pickUpItem(itementity);
                }
            }
        }
    }

    public void tick() {
        super.tick();
        updateSwingTime();
        updateSwimming();
        if(hurtTimeStamp > 0) hurtTimeStamp--;
    }


    public void rideTick() {
        super.rideTick();
        if (this.getVehicle() instanceof PathfinderMob) {
            PathfinderMob creatureentity = (PathfinderMob)this.getVehicle();
            this.yBodyRot = creatureentity.yBodyRot;
        }

    }

    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance diff, MobSpawnType reason, @Nullable SpawnGroupData spawnData, @Nullable CompoundTag nbt) {
        setRandomSpawnBonus();
        canPickUpLoot();
        return spawnData;
    }
    public void setRandomSpawnBonus(){
        getAttribute(Attributes.MAX_HEALTH).addPermanentModifier(new AttributeModifier("heath_bonus", this.random.nextGaussian() * 0.10D, AttributeModifier.Operation.MULTIPLY_BASE));
        getAttribute(Attributes.MOVEMENT_SPEED).addPermanentModifier(new AttributeModifier("speed_bonus", this.random.nextGaussian() * 0.10D, AttributeModifier.Operation.MULTIPLY_BASE));

    }

    public void setDropEquipment(){
        this.dropEquipment();
    }

    ////////////////////////////////////REGISTER////////////////////////////////////


    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(CAMP, Optional.empty());
        this.entityData.define(START_POS, Optional.empty());
        this.entityData.define(DEST_POS, Optional.empty());
        this.entityData.define(IS_WORKING, false);
        this.entityData.define(IS_PICKING_UP, false);
        this.entityData.define(FOLLOW, false);
        this.entityData.define(breakingTime, 0);
        this.entityData.define(currentTimeBreak, -1);
        this.entityData.define(previousTimeBreak, -1);

    }

    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putBoolean("Follow", this.getFollow());
        nbt.putBoolean("isWorking", this.getIsWorking());
        nbt.putBoolean("isPickingUp", this.getIsPickingUp());
        nbt.putInt("breakTime", this.getBreakingTime());
        nbt.putInt("currentTimeBreak", this.getCurrentTimeBreak());
        nbt.putInt("previousTimeBreak", this.getPreviousTimeBreak());

        this.getStartPos().ifPresent((pos) -> {
            nbt.putInt("StartPosX", pos.getX());
            nbt.putInt("StartPosY", pos.getY());
            nbt.putInt("StartPosZ", pos.getZ());
        });

        this.getDestPos().ifPresent((pos) -> {
            nbt.putInt("DestPosX", pos.getX());
            nbt.putInt("DestPosY", pos.getY());
            nbt.putInt("DestPosZ", pos.getZ());
        });

        if(this.getCampPos() != null){
            nbt.putInt("CampPosX", this.getCampPos().getX());
            nbt.putInt("CampPosY", this.getCampPos().getY());
            nbt.putInt("CampPosZ", this.getCampPos().getZ());
        }
    }

    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.setFollow(nbt.getBoolean("Follow"));
        this.setBreakingTime(nbt.getInt("breakTime"));
        this.setIsPickingUp(nbt.getBoolean("isPickingUp"));
        this.setCurrentTimeBreak(nbt.getInt("currentTimeBreak"));
        this.setPreviousTimeBreak(nbt.getInt("previousTimeBreak"));
        this.setIsWorking(nbt.getBoolean("isWorking"));


        if (nbt.contains("StartPosX", 99) &&
                nbt.contains("StartPosY", 99) &&
                nbt.contains("StartPosZ", 99)) {
            BlockPos blockpos = new BlockPos(
                    nbt.getInt("StartPosX"),
                    nbt.getInt("StartPosY"),
                    nbt.getInt("StartPosZ"));
            this.setStartPos(Optional.of(blockpos));
        }

        if (nbt.contains("DestPosX", 99) &&
                nbt.contains("DestPosY", 99) &&
                nbt.contains("DestPosZ", 99)) {
            BlockPos blockpos = new BlockPos(
                    nbt.getInt("DestPosX"),
                    nbt.getInt("DestPosY"),
                    nbt.getInt("DestPosZ"));
            this.setDestPos(blockpos);
        }

        if (nbt.contains("CampPosX", 99) &&
                nbt.contains("CampPosY", 99) &&
                nbt.contains("CampPosZ", 99)) {
            BlockPos blockpos = new BlockPos(
                    nbt.getInt("CampPosX"),
                    nbt.getInt("CampPosY"),
                    nbt.getInt("CampPosZ"));
            this.setCampPos(Optional.of(blockpos));
        }

    }


    ////////////////////////////////////GET////////////////////////////////////

    public BlockPos getCampPos(){
        return this.entityData.get(CAMP).orElse(null);
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

    public BlockPos getWorkerOnPos(){
        return this.getOnPos();
    }

    public Optional<BlockPos> getDestPos(){
        return this.entityData.get(DEST_POS);
    }


    public Optional<BlockPos> getStartPos(){
        return this.entityData.get(START_POS);
    }

    public boolean getFollow(){
        return this.entityData.get(FOLLOW);
    }

    public boolean getIsWorking(){
        return this.entityData.get(IS_WORKING);
    }

    public boolean getIsPickingUp(){
        return this.entityData.get(IS_PICKING_UP);
    }

    public SoundEvent getHurtSound(DamageSource ds) {
        return SoundEvents.VILLAGER_HURT;
    }

    protected SoundEvent getDeathSound() {
        return SoundEvents.VILLAGER_DEATH;
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


    ////////////////////////////////////SET////////////////////////////////////

    public void setCampPos(Optional<BlockPos> pos){
        LivingEntity owner = this.getOwner();
        this.entityData.set(CAMP, pos);
        if (owner != null) owner.sendMessage(new TextComponent(this.getName().getString() + ": I will camp here."), owner.getUUID());
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

    public void setDestPos(BlockPos pos){
        this.entityData.set(DEST_POS, Optional.of(pos));
    }

    public void setStartPos(Optional<BlockPos> pos){
        this.entityData.set(START_POS, pos);

    }

    public void setFollow(boolean bool){
        this.entityData.set(FOLLOW, bool);

        LivingEntity owner = this.getOwner();
        if (owner != null)
        if (bool) {
            owner.sendMessage(new TextComponent(this.getName().getString() + ": I will follow you!"), owner.getUUID());
        }
        else
            owner.sendMessage(new TextComponent(this.getName().getString() + ": I will not follow you!"), owner.getUUID());
    }

    public void setIsWorking(boolean bool) {
        entityData.set(IS_WORKING, bool);

        LivingEntity owner = this.getOwner();


        if (owner != null)
        if (bool) {
            owner.sendMessage(new TextComponent(this.getName().getString() + ": Im working now!"), owner.getUUID());
        }
        else
            owner.sendMessage(new TextComponent(this.getName().getString() + ": Work is done!"), owner.getUUID());
    }

    public void setIsPickingUp(boolean bool) {
        entityData.set(IS_PICKING_UP, bool);
    }

    public void setOwned(boolean owned) {
        super.setTame(owned);
    }


    public void setEquipment(){}


    ////////////////////////////////////ON FUNCTIONS////////////////////////////////////

    boolean playerHasEnoughEmeralds(Player player) {
        int recruitCosts = this.workerCosts();
        int emeraldCount = player.getItemInHand(InteractionHand.MAIN_HAND).getCount();
        if (emeraldCount >= recruitCosts){
            return true;
        }
        if (player.isCreative()){
            return true;
        }
        else return false;
    }

    ////////////////////////////////////ATTACK FUNCTIONS////////////////////////////////////

    public boolean hurt(DamageSource dmg, float amt) {
        if (this.isInvulnerableTo(dmg)) {
            return false;
        } else {
            Entity entity = dmg.getEntity();
            this.setOrderedToSit(false);
            if (entity != null && !(entity instanceof Player) && !(entity instanceof AbstractArrow)) {
                amt = (amt + 1.0F) / 2.0F;
            }

            LivingEntity attacker = this.getLastHurtByMob();
            if(this.isTame() && attacker != null && hurtTimeStamp <= 0){
                LivingEntity owner = this.getOwner();
                if (owner!= null && owner != attacker) {
                    owner.sendMessage(new TextComponent(this.getName().getString() + " is getting attacked by " + attacker.getName().getString()), owner.getUUID());
                    hurtTimeStamp = 80;
                }
            }

            return super.hurt(dmg, amt);
        }
    }

    public boolean doHurtTarget(Entity entity) {
        boolean flag = entity.hurt(DamageSource.mobAttack(this), (float)((int)this.getAttributeValue(Attributes.ATTACK_DAMAGE)));
        if (flag) {
            this.doEnchantDamageEffects(this, entity);

        }

        return flag;
    }

    public void die(DamageSource dmg) {
        super.die(dmg);

    }


    ////////////////////////////////////OTHER FUNCTIONS////////////////////////////////////

    public void resetWorkerParameters(){
        this.setBreakingTime(0);
        this.setCurrentTimeBreak(-1);
        this.setPreviousTimeBreak(-1);
    }

    public boolean isOwnedByThisPlayer(AbstractWorkerEntity recruit, Player player){
        return  (recruit.getOwnerUUID() == player.getUUID());
    }

    @Override
    public boolean canBeLeashed(Player player) {
        return false;
    }
    public abstract int workerCosts() ;

    @Override
    @OnlyIn(Dist.CLIENT)
    protected void spawnTamingParticles(boolean p_70908_1_) {
        ParticleOptions iparticledata = ParticleTypes.HAPPY_VILLAGER;
        if (!p_70908_1_) {
            iparticledata = ParticleTypes.SMOKE;
        }

        for(int i = 0; i < 7; ++i) {
            double d0 = this.random.nextGaussian() * 0.02D;
            double d1 = this.random.nextGaussian() * 0.02D;
            double d2 = this.random.nextGaussian() * 0.02D;
            this.level.addParticle(iparticledata, this.getRandomX(1.0D), this.getRandomY() + 0.5D, this.getRandomZ(1.0D), d0, d1, d2);
        }

    }

    public void workerSwingArm(){
        if (this.getRandom().nextInt(5) == 0) {
            if (!this.swinging) {
                this.swing(this.getUsedItemHand());
            }
        }
    }

    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        Item item = itemstack.getItem();
        if (this.level.isClientSide) {
            boolean flag = this.isOwnedBy(player) || this.isTame() || isInSittingPose() || item == Items.BONE && !this.isTame();
            return flag ? InteractionResult.CONSUME : InteractionResult.PASS;
        } else {
            if (this.isTame() && player.getUUID().equals(this.getOwnerUUID())) {

                if (player.isCrouching()) {
                    openGUI(player);

                }
                if(!player.isCrouching()) {
                    setFollow(!getFollow());
                    return InteractionResult.SUCCESS;
                }

            } else if (item == Items.EMERALD && !this.isTame() && playerHasEnoughEmeralds(player)) {
                if (!player.getAbilities().instabuild) {
                    if (!player.isCreative()) {
                        itemstack.shrink(workerCosts());
                    }
                }
                this.tame(player);
                this.navigation.stop();
                this.setTarget(null);
                this.setOrderedToSit(false);
                this.setIsWorking(false);
                return InteractionResult.SUCCESS;
            }
            else if (item == Items.EMERALD  && !this.isTame() && !playerHasEnoughEmeralds(player)) {
                player.sendMessage(new TextComponent("" + this.getName().getString() + ": You need " + workerCosts() + " Emeralds to hire me!"), player.getUUID());
            }
            else if (!this.isTame() && item != Items.EMERALD ) {
                player.sendMessage(new TextComponent("I am a " + this.getName().getString()), player.getUUID());

            }
            return super.mobInteract(player, hand);
        }
    }

    public abstract Predicate<ItemEntity> getAllowedItems();
    public abstract void openGUI(Player player);
    public abstract void initSpawn();
}