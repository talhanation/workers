package com.talhanation.workers.entities;

import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.particles.IParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public abstract class AbstractWorkerEntity extends AbstractInventoryEntity {
    private static final DataParameter<Optional<BlockPos>> START_POS = EntityDataManager.defineId(AbstractWorkerEntity.class, DataSerializers.OPTIONAL_BLOCK_POS);
    private static final DataParameter<Optional<BlockPos>> DEST_POS = EntityDataManager.defineId(AbstractWorkerEntity.class, DataSerializers.OPTIONAL_BLOCK_POS);
    private static final DataParameter<Boolean> FOLLOW = EntityDataManager.defineId(AbstractWorkerEntity.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> IS_WORKING = EntityDataManager.defineId(AbstractWorkerEntity.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Integer> breakingTime = EntityDataManager.defineId(MinerEntity.class, DataSerializers.INT);
    private static final DataParameter<Integer> currentTimeBreak = EntityDataManager.defineId(MinerEntity.class, DataSerializers.INT);
    private static final DataParameter<Integer> previousTimeBreak = EntityDataManager.defineId(MinerEntity.class, DataSerializers.INT);



    public AbstractWorkerEntity(EntityType<? extends AbstractInventoryEntity> entityType, World world) {
        super(entityType, world);
        this.setOwned(false);
        this.xpReward = 2;
    }

    ///////////////////////////////////TICK/////////////////////////////////////////

    public double getMyRidingOffset() {
        return -0.35D;
    }

    @Override
    public void aiStep() {
        super.aiStep();
    }

    public void tick() {
        super.tick();
        updateSwingTime();
        updateSwimming();
    }


    public void rideTick() {
        super.rideTick();
        if (this.getVehicle() instanceof CreatureEntity) {
            CreatureEntity creatureentity = (CreatureEntity)this.getVehicle();
            this.yBodyRot = creatureentity.yBodyRot;
        }

    }

    @Nullable
    public ILivingEntityData finalizeSpawn(IServerWorld world, DifficultyInstance diff, SpawnReason reason, @Nullable ILivingEntityData spawnData, @Nullable CompoundNBT nbt) {
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
        this.entityData.define(IS_WORKING, false);
        this.entityData.define(FOLLOW, false);
        this.entityData.define(START_POS, Optional.empty());
        this.entityData.define(DEST_POS, Optional.empty());
        this.entityData.define(breakingTime, 0);
        this.entityData.define(currentTimeBreak, -1);
        this.entityData.define(previousTimeBreak, -1);
    }

    public void addAdditionalSaveData(CompoundNBT nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putBoolean("Follow", this.getFollow());
        nbt.putBoolean("isWorking", this.getIsWorking());
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
    }

    public void readAdditionalSaveData(CompoundNBT nbt) {
        super.readAdditionalSaveData(nbt);
        this.setFollow(nbt.getBoolean("Follow"));
        this.setBreakingTime(nbt.getInt("breakTime"));
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

    }


    ////////////////////////////////////GET////////////////////////////////////

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

    public SoundEvent getHurtSound(DamageSource ds) {
        return SoundEvents.VILLAGER_HURT;
    }

    protected SoundEvent getDeathSound() {
        return SoundEvents.VILLAGER_DEATH;
    }

    protected float getSoundVolume() {
        return 0.4F;
    }

    protected float getStandingEyeHeight(Pose pos, EntitySize size) {
        return size.height * 0.9F;
    }

    public int getMaxHeadXRot() {
        return this.isInSittingPose() ? 20 : super.getMaxHeadXRot();
    }


    ////////////////////////////////////SET////////////////////////////////////

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
            owner.sendMessage(new StringTextComponent("I will follow you!"), owner.getUUID());
        }
        else
            owner.sendMessage(new StringTextComponent("I will not follow you!"), owner.getUUID());
    }

    public void setIsWorking(boolean bool) {
        entityData.set(IS_WORKING, bool);

        LivingEntity owner = this.getOwner();


        if (owner != null)
        if (bool) {
            owner.sendMessage(new StringTextComponent("Im working now!"), owner.getUUID());
        }
        else
            owner.sendMessage(new StringTextComponent("I stopped working now!"), owner.getUUID());
    }

    public void setOwned(boolean owned) {
        super.setTame(owned);
    }


    public void setEquipment(){}


    ////////////////////////////////////ON FUNCTIONS////////////////////////////////////

    boolean playerHasEnoughEmeralds(PlayerEntity player) {
        int recruitCosts = this.workerCosts();
        int emeraldCount = player.getItemInHand(Hand.MAIN_HAND).getCount();
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
            if (entity != null && !(entity instanceof PlayerEntity) && !(entity instanceof AbstractArrowEntity)) {
                amt = (amt + 1.0F) / 2.0F;
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

    public boolean isOwnedByThisPlayer(AbstractWorkerEntity recruit, PlayerEntity player){
        return  (recruit.getOwnerUUID() == player.getUUID());
    }



    @Override
    public boolean canBeLeashed(PlayerEntity player) {
        return false;
    }
    public abstract int workerCosts() ;

    public abstract String workerName();

    @Override
    @OnlyIn(Dist.CLIENT)
    protected void spawnTamingParticles(boolean p_70908_1_) {
        IParticleData iparticledata = ParticleTypes.HAPPY_VILLAGER;
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

    @OnlyIn(Dist.CLIENT)
    public void workerSwingArm(){
        if (this.getRandom().nextInt(5) == 0) {
            if (!this.swinging) {
                this.swing(this.getUsedItemHand());
            }
        }
    }

    public abstract Predicate<ItemEntity> getAllowedItems();
    //public abstract void openGUI(PlayerEntity player);
}
