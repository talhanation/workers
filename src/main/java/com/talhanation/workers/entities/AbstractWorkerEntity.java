package com.talhanation.workers.entities;

import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.monster.*;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.horse.AbstractHorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.particles.IParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public abstract class AbstractWorkerEntity extends TameableEntity {
    private static final DataParameter<Optional<BlockPos>> HOLD_POS = EntityDataManager.defineId(AbstractWorkerEntity.class, DataSerializers.OPTIONAL_BLOCK_POS);
    private static final DataParameter<Optional<BlockPos>> MOVE_POS = EntityDataManager.defineId(AbstractWorkerEntity.class, DataSerializers.OPTIONAL_BLOCK_POS);
    private static final DataParameter<Boolean> MOVE = EntityDataManager.defineId(AbstractWorkerEntity.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Integer> STATE = EntityDataManager.defineId(AbstractWorkerEntity.class, DataSerializers.INT);

    private UUID persistentAngerTarget;

    public AbstractWorkerEntity(EntityType<? extends TameableEntity> entityType, World world) {
        super(entityType, world);
        this.setOwned(false);
        this.xpReward = 6;
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

    protected void registerGoals() {

    }

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(STATE, 0);
        this.entityData.define(HOLD_POS, Optional.empty());
        this.entityData.define(MOVE_POS, Optional.empty());
        this.entityData.define(MOVE, true);
    }

    public void addAdditionalSaveData(CompoundNBT nbt) {
        super.addAdditionalSaveData(nbt);
    }

    public void readAdditionalSaveData(CompoundNBT p_70037_1_) {
        super.readAdditionalSaveData(p_70037_1_);
    }

    ////////////////////////////////////GET////////////////////////////////////

    public int getState() {
        return entityData.get(STATE);
    }

    public SoundEvent getHurtSound(DamageSource ds) {
        if (this.isBlocking())
            return SoundEvents.SHIELD_BLOCK;
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

    public int getMaxSpawnClusterSize() {
        return 8;
    }


    @Nullable
    public UUID getPersistentAngerTarget() {
        return this.persistentAngerTarget;
    }

    @Nullable
    public BlockPos getHoldPos(){
        return entityData.get(HOLD_POS).orElse(null);
    }

    @Nullable
    public BlockPos getMovePos(){
        return entityData.get(MOVE_POS).orElse(null);
    }

    public boolean getMove() {
        return entityData.get(MOVE);
    }


    ////////////////////////////////////SET////////////////////////////////////

    public void setState(int state) {
        switch (state){
            case 0:
                setTarget(null);//wird nur 1x aufgerufen
                break;
            case 1:
                break;
            case 2:
                break;
        }
        entityData.set(STATE, state);
    }


    public void setMovePos(BlockPos holdPos){
        entityData.set(MOVE_POS, Optional.of(holdPos));
    }

    public void setMove(boolean bool) {
        entityData.set(MOVE, bool);
    }

    public void setOwned(boolean owned) {
        super.setTame(owned);
    }


    public void setEquipment(){}


    ////////////////////////////////////ON FUNCTIONS////////////////////////////////////

    public ActionResultType mobInteract(PlayerEntity player, Hand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        Item item = itemstack.getItem();
        if (this.level.isClientSide) {
            boolean flag = this.isOwnedBy(player) || this.isTame() || isInSittingPose() || item == Items.BONE && !this.isTame();
            return flag ? ActionResultType.CONSUME : ActionResultType.PASS;
        } else {
            if (this.isTame() && player.getUUID().equals(this.getOwnerUUID())) {

                if (player.isCrouching()) {
                    int state = this.getState();

                }
                if(!player.isCrouching()) {

                }

            } else if (item == Items.EMERALD && !this.isTame() && playerHasEnoughEmeralds(player)) {
                if (!player.abilities.instabuild) {
                    if (!player.isCreative()) {
                        itemstack.shrink(workerCosts());
                    }
                }

                if (!net.minecraftforge.event.ForgeEventFactory.onAnimalTame(this, player)) {
                    this.tame(player);
                    this.navigation.stop();
                    this.setTarget(null);
                    this.setOrderedToSit(false);
                    this.setState(0);
                    this.level.broadcastEntityEvent(this, (byte)7);
                    return ActionResultType.SUCCESS;
                } else {
                    this.level.broadcastEntityEvent(this, (byte)6);
                }

                return ActionResultType.SUCCESS;
            }
            else if (item == Items.EMERALD  && !this.isTame() && !playerHasEnoughEmeralds(player)) {
                    player.sendMessage(new StringTextComponent("You need " + workerCosts() + " Emeralds to recruit me!"), player.getUUID());
            }
            else if (!this.isTame() && item != Items.EMERALD ) {
                int i = this.random.nextInt(5);
                switch (i) {
                    case 0:
                        player.sendMessage(new StringTextComponent("I am a " + workerName() + ". I'm here to keep things safe in these areas."), player.getUUID());
                        break;
                    case 1:
                        player.sendMessage(new StringTextComponent("Stay Safe, I'm here to protect you."), player.getUUID());
                        break;
                    case 2:
                        player.sendMessage(new StringTextComponent("Everyone needs a " + workerName() + " like me, to have peace in these Lands."), player.getUUID());
                        break;
                        default:
                        player.sendMessage(new StringTextComponent("I am a " + workerName() + " defending these areas from Monsters!"), player.getUUID());
                        break;
                }
            }
            return super.mobInteract(player, hand);
        }
    }

    private boolean playerHasEnoughEmeralds(PlayerEntity player) {
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



}
