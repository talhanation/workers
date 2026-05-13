package com.talhanation.workers.entities;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.pathfinding.AsyncGroundPathNavigation;
import com.talhanation.workers.config.WorkersServerConfig;
import com.talhanation.workers.entities.ai.CookWorkGoal;
import com.talhanation.workers.entities.workarea.AbstractWorkAreaEntity;
import com.talhanation.workers.entities.workarea.KitchenArea;
import com.talhanation.workers.world.VillagerInviteRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraftforge.common.ForgeMod;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Predicate;

public class CookEntity extends AbstractWorkerEntity {

    private final Predicate<ItemEntity> ALLOWED_ITEMS = (item) ->
            (!item.hasPickUpDelay() && item.isAlive() && getInventory().canAddItem(item.getItem()) && this.wantsToPickUp(item.getItem()));

    public KitchenArea currentKitchenArea;

    @Nullable public Villager activeTradingVillager;
    public int villagerTradeTimeout;

    public CookEntity(EntityType<? extends AbstractWorkerEntity> entityType, Level world) {
        super(entityType, world);
    }

    @Override
    public boolean shouldLoadChunk() {
        return true;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(3, new CookWorkGoal(this));
    }

    @Override
    public AbstractWorkAreaEntity getCurrentWorkArea() {
        return currentKitchenArea;
    }

    @Override
    public List<Item> inventoryInputHelp() {
        return null;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
    }

    public static AttributeSupplier.Builder setAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(ForgeMod.SWIM_SPEED.get(), 0.3D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.1D)
                .add(Attributes.ATTACK_DAMAGE, 0.5D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(ForgeMod.ENTITY_REACH.get(), 0D)
                .add(Attributes.ATTACK_SPEED);
    }

    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficultyInstance, MobSpawnType reason, @Nullable SpawnGroupData data, @Nullable CompoundTag nbt) {
        RandomSource randomsource = world.getRandom();
        SpawnGroupData ilivingentitydata = super.finalizeSpawn(world, difficultyInstance, reason, data, nbt);
        ((AsyncGroundPathNavigation) this.getNavigation()).setCanOpenDoors(true);
        this.populateDefaultEquipmentEnchantments(randomsource, difficultyInstance);
        this.initSpawn();
        return ilivingentitydata;
    }

    @Override
    public void initSpawn() {
        this.setCustomName(Component.literal("Chef"));
        this.setCost(WorkersServerConfig.CookCost.get());
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

    public void clearVillagerTrade() {
        if (this.activeTradingVillager != null) {
            VillagerInviteRegistry.release(this.activeTradingVillager.getUUID());
        }
        this.activeTradingVillager = null;
        this.villagerTradeTimeout  = 0;
    }
}
