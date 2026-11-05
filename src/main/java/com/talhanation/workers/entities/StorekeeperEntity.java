package com.talhanation.workers.entities;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.workers.config.WorkersServerConfig;
import com.talhanation.workers.entities.ai.StorekeeperWorkGoal;
import com.talhanation.workers.entities.ai.navigation.WorkersGroundPathNavigation;
import com.talhanation.workers.entities.workarea.AbstractWorkAreaEntity;
import com.talhanation.workers.entities.workarea.StorageArea;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.neoforged.neoforge.common.NeoForgeMod;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Works in exactly one storage area: unloads container entities that park in it (ships, chest boats, chest minecarts,
 * donkeys/mules/llamas with chests, carts) and keeps the chests sorted. Without work he waits in the storage for arrivals.
 */
public class StorekeeperEntity extends AbstractWorkerEntity {

    private final Predicate<ItemEntity> ALLOWED_ITEMS = (item) ->
            (!item.hasPickUpDelay() && item.isAlive() && getInventory().canAddItem(item.getItem()) && this.wantsToPickUp(item.getItem()));

    public StorageArea currentStorageArea;
    /** The storage he belongs to. Kept while he eats, sleeps or follows, so no other storekeeper takes it. */
    @Nullable public UUID storageUUID;
    @Nullable public BlockPos storagePos;

    public StorekeeperEntity(EntityType<? extends AbstractWorkerEntity> entityType, Level world) {
        super(entityType, world);
    }

    @Override
    public boolean shouldLoadChunk() {
        return true;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(3, new StorekeeperWorkGoal(this));
    }

    @Override
    public AbstractWorkAreaEntity getCurrentWorkArea() {
        return currentStorageArea;
    }

    @Override
    public List<Item> inventoryInputHelp() {
        return null;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        if (storageUUID != null) nbt.putUUID("StorageUUID", storageUUID);
        if (storagePos != null) nbt.put("StoragePos", NbtUtils.writeBlockPos(storagePos));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.storageUUID = nbt.hasUUID("StorageUUID") ? nbt.getUUID("StorageUUID") : null;
        this.storagePos = nbt.contains("StoragePos") ? NbtUtils.readBlockPos(nbt, "StoragePos").orElse(null) : null;
    }

    @Override
    public void die(DamageSource dmg) {
        if (!this.getCommandSenderWorld().isClientSide()) releaseStorage();
        super.die(dmg);
    }

    /** Gives the storage free for another storekeeper. */
    public void releaseStorage() {
        if (currentStorageArea != null && this.getUUID().equals(currentStorageArea.getStorekeeperUUID())) {
            currentStorageArea.setStorekeeperUUID(null);
        }
        currentStorageArea = null;
        storageUUID = null;
        storagePos = null;
    }

    public static AttributeSupplier.Builder setAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(NeoForgeMod.SWIM_SPEED, 0.3D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.1D)
                .add(Attributes.ATTACK_DAMAGE, 0.5D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.ENTITY_INTERACTION_RANGE, 0D)
                .add(Attributes.ATTACK_SPEED);
    }

    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficultyInstance, MobSpawnType reason, @Nullable SpawnGroupData data) {
        RandomSource randomsource = world.getRandom();
        SpawnGroupData ilivingentitydata = super.finalizeSpawn(world, difficultyInstance, reason, data);
        ((WorkersGroundPathNavigation) this.getNavigation()).setCanOpenDoors(true);
        this.populateDefaultEquipmentEnchantments(world, randomsource, difficultyInstance);
        this.initSpawn();
        return ilivingentitydata;
    }

    @Override
    public void initSpawn() {
        this.setCustomName(Component.literal("Storekeeper"));
        this.setCost(WorkersServerConfig.StorekeeperCost.get());
        this.setEquipment();
        this.setDropEquipment();
        this.setRandomSpawnBonus();
        this.setPersistenceRequired();
        this.setFollowState(2);
        AbstractRecruitEntity.applySpawnValues(this);
    }

    public Predicate<ItemEntity> getAllowedItems() {
        return ALLOWED_ITEMS;
    }
}
