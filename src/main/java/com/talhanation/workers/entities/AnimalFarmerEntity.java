package com.talhanation.workers.entities;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.pathfinding.AsyncGroundPathNavigation;
import com.talhanation.workers.config.WorkersServerConfig;
import com.talhanation.workers.entities.ai.AnimalFarmerWorkGoal;
import com.talhanation.workers.entities.workarea.AbstractWorkAreaEntity;
import com.talhanation.workers.entities.workarea.AnimalPenArea;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Predicate;

public class AnimalFarmerEntity extends AbstractWorkerEntity{
    public AnimalPenArea currentAnimalPen;
    public AnimalFarmerEntity(EntityType<? extends AbstractWorkerEntity> entityType, Level world) {
        super(entityType, world);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new AnimalFarmerWorkGoal(this));
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
        ((AsyncGroundPathNavigation)this.getNavigation()).setCanOpenDoors(true);
        this.populateDefaultEquipmentEnchantments(randomsource, difficultyInstance);

        this.initSpawn();

        return ilivingentitydata;
    }

    protected void defineSynchedData() {
        super.defineSynchedData();
    }

    @Override//not used
    public Predicate<ItemEntity> getAllowedItems() {
        return null;
    }

    @Override
    public void initSpawn() {
        this.setCustomName(Component.literal("Animal Farmer"));
        //this.setCost(WorkersServerConfig.FarmerCost.get());
        this.setCost(20);

        this.setEquipment();
        this.setDropEquipment();
        this.setRandomSpawnBonus();
        this.setPersistenceRequired();


        AbstractRecruitEntity.applySpawnValues(this);
    }

    @Override
    public List<Item> inventoryInputHelp() {
        return null;
    }

    public boolean wantsToKeep(ItemStack itemStack) {
        if (itemStack.getItem() instanceof AxeItem) {
            int items = countMatchingItems(stack -> stack.getItem() instanceof AxeItem);
            return items <= 1;
        }
        if(currentAnimalPen != null) {
            ItemStack breedItem = currentAnimalPen.getAnimalType().getBreedItem().getDefaultInstance();
            if(ItemStack.isSameItem(breedItem, itemStack)){
                int items = countMatchingStacks(stack -> breedItem.is(stack.getItem()));
                return items <= 1;
            }
        }
        return super.wantsToKeep(itemStack);
    }
    public boolean wantsToPickUp(ItemStack itemStack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(itemStack.getItem());
        if(id == null) return false;

        if(WorkersServerConfig.AnimalFarmerPickup.get().contains(id.toString())) return true;

        if(itemStack.is(ItemTags.WOOL)) return true;

        return super.wantsToPickUp(itemStack);
    }

    @Override
    public AbstractWorkAreaEntity getCurrentWorkArea() {
        return currentAnimalPen;
    }


}

