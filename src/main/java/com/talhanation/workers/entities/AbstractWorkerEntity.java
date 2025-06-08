package com.talhanation.workers.entities;

import com.talhanation.recruits.config.RecruitsClientConfig;
import com.talhanation.recruits.entities.AbstractChunkLoaderEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;


public abstract class AbstractWorkerEntity extends AbstractChunkLoaderEntity {
    public IWorkerController workController;
    public AbstractWorkerEntity(EntityType<? extends AbstractWorkerEntity> entityType, Level world) {
        super(entityType, world);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
    }

    /////////////////////////////////// TICK/////////////////////////////////////////


    @Override
    public void aiStep() {
        super.aiStep();
        if(this.getCommandSenderWorld().isClientSide())return;

        if(workController != null) this.workController.tick();

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

            }
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
    }

    public void addAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);

    }

    public void readAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
    }

    public int getMaxAreas() {
        return 3;
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
        return this.needsToGetFood(); // || needsToChangeTool || needsToDeposit
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

    int currentTimeBreak;
    int breakingTime;
    int previousTimeBreak;

    public void mineBlock(BlockPos pos){
        if (!this.isAlive()) return;

        BlockState state = this.getCommandSenderWorld().getBlockState(pos);
        if (state.isAir()) return;

        // Hit-Sound alle 5 Ticks
        if (currentTimeBreak % 5 == 4) {
            this.getCommandSenderWorld().playLocalSound(pos.getX(), pos.getY(), pos.getZ(),
                    state.getSoundType().getHitSound(), SoundSource.BLOCKS, 1F, 0.75F, false);
        }

        if (breakingTime == 0) {
            breakingTime = (int) (state.getDestroySpeed(this.level(), pos) * 30);
        }

        float destroySpeed = this.getUseItem().getDestroySpeed(state);
        currentTimeBreak += (int) destroySpeed;

        int stage = (int) ((float) currentTimeBreak / breakingTime * 10);
        if (stage != previousTimeBreak) {
            this.getCommandSenderWorld().destroyBlockProgress(1, pos, stage);
            previousTimeBreak = stage;
        }

        if (currentTimeBreak >= breakingTime) {
            this.getCommandSenderWorld().destroyBlock(pos, true, this);
            currentTimeBreak = 0;
            breakingTime = 0;
            previousTimeBreak = 0;
        }

        this.swing(InteractionHand.MAIN_HAND);
    }
}
