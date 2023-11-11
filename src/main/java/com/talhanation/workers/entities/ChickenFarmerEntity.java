package com.talhanation.workers.entities;

import com.google.common.collect.ImmutableSet;
import com.talhanation.workers.config.WorkersModConfig;
import com.talhanation.workers.entities.ai.ChickenFarmerAI;
import com.talhanation.workers.entities.ai.WorkerPickupWantedItemGoal;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.function.Predicate;

public class ChickenFarmerEntity extends AbstractAnimalFarmerEntity {

    private static final EntityDataAccessor<Boolean> USE_EGGS = SynchedEntityData.defineId(ChickenFarmerEntity.class, EntityDataSerializers.BOOLEAN);

    private final Predicate<ItemEntity> ALLOWED_ITEMS = (item) -> {
        return !item.hasPickUpDelay() && item.isAlive() && this.wantsToPickUp(item.getItem());
    };

    public final ItemStack MAIN_TOOL = new ItemStack(Items.STONE_AXE);
    private static final Set<Item> WANTED_ITEMS = ImmutableSet.of(
            Items.FEATHER,
            Items.CHICKEN,
            Items.EGG,
            Items.WHEAT_SEEDS,
            Items.BEETROOT_SEEDS,
            Items.MELON_SEEDS,
            Items.PUMPKIN_SEEDS);

    public ChickenFarmerEntity(EntityType<? extends AbstractAnimalFarmerEntity> entityType, Level world) {
        super(entityType, world);
        this.initSpawn();
    }

    protected void defineSynchedData() {
        this.entityData.define(USE_EGGS, true);
        super.defineSynchedData();
    }

    public void addAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putBoolean("UseEggs", this.getUseEggs());
    }

    public void readAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.setUseEggs(nbt.getBoolean("UseEggs"));
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public int workerCosts() {
        return WorkersModConfig.ChickenFarmerCost.get();
    }

    @Override
    public Predicate<ItemEntity> getAllowedItems() {
        return ALLOWED_ITEMS;
    }

    // ATTRIBUTES
    public static AttributeSupplier.Builder setAttributes() {
        return createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new WorkerPickupWantedItemGoal(this));
        this.goalSelector.addGoal(3, new ChickenFarmerAI(this));
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel p_241840_1_, AgeableMob p_241840_2_) {
        return null;
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficultyInstance,
            MobSpawnType reason, @Nullable SpawnGroupData data, @Nullable CompoundTag nbt) {
        SpawnGroupData ilivingentitydata = super.finalizeSpawn(world, difficultyInstance, reason, data, nbt);

        this.populateDefaultEquipmentEnchantments(difficultyInstance);

        this.initSpawn();

        return ilivingentitydata;
    }

    @Override
    public void initSpawn() {
        super.initSpawn();
        TextComponent name = new TextComponent("Chicken Farmer");

        this.setProfessionName(name.getString());
        this.setCustomName(name);
    }
    @Override
    public boolean wantsToPickUp(ItemStack itemStack) {
        Item item = itemStack.getItem();
        return (WANTED_ITEMS.contains(item));
    }

    @Override
    public boolean wantsToKeep(ItemStack itemStack) {
        return super.wantsToKeep(itemStack) || isBreedItem(itemStack) || (this.getUseEggs() && itemStack.getItem().equals(Items.EGG));
    }

    @Override
    public void setEquipment() {
        ItemStack initialTool = MAIN_TOOL;
        this.updateInventory(0, initialTool);
        this.equipTool(initialTool);
    }

    @Override
    public boolean isRequiredMainTool(ItemStack tool) {
        return tool.getItem() instanceof AxeItem;
    }

    @Override
    public boolean isRequiredSecondTool(ItemStack tool) {
        return false;
    }

    public boolean isBreedItem(ItemStack itemStack){
        return itemStack.getItem().equals(Items.WHEAT_SEEDS)
                || itemStack.getItem().equals(Items.MELON_SEEDS)
                || itemStack.getItem().equals(Items.BEETROOT_SEEDS)
                || itemStack.getItem().equals(Items.PUMPKIN_SEEDS);
    }

    public void setUseEggs(boolean useEggs) {
        entityData.set(USE_EGGS, useEggs);
    }

    public boolean getUseEggs() {
        return entityData.get(USE_EGGS);
    }
}
