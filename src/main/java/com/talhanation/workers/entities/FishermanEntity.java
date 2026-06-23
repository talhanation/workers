package com.talhanation.workers.entities;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.pathfinding.AsyncGroundPathNavigation;
import com.talhanation.workers.config.WorkersServerConfig;
import com.talhanation.workers.entities.ai.FishermanWorkGoal;
import com.talhanation.workers.entities.ai.navigation.WorkersGroundPathNavigation;
import com.talhanation.workers.entities.workarea.FishingArea;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
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
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Predicate;

public class FishermanEntity extends AbstractWorkerEntity{
    public FishingArea currentFishingArea;
    public FishermanEntity(EntityType<? extends AbstractWorkerEntity> entityType, Level world) {
        super(entityType, world);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new FishermanWorkGoal(this));
    }

    public static AttributeSupplier.Builder setAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
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
        ((WorkersGroundPathNavigation)this.getNavigation()).setCanOpenDoors(true);
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
        this.setCustomName(Component.literal("Fisherman"));
        //this.setCost(WorkersServerConfig.Fisherman.get());
        this.setCost(10);

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

    @Override
    public boolean wantsToKeep(ItemStack itemStack) {
        if (itemStack.getItem() instanceof FishingRodItem) {
            int rods = countMatchingItems(stack -> stack.getItem() instanceof FishingRodItem);
            return rods <= 1;
        }

        return super.wantsToKeep(itemStack);
    }

    public boolean wantsToPickUp(ItemStack itemStack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(itemStack.getItem());
        if(id == null) return false;

        if(WorkersServerConfig.FishermanPickup.get().contains(id.toString())) return true;


        return super.wantsToPickUp(itemStack);
    }

    @Override
    public FishingArea getCurrentWorkArea() {
        return currentFishingArea;
    }

    public FishingBobberEntity throwFishingHook(Vec3 target){
        FishingBobberEntity fishingBobber = new FishingBobberEntity(this.getCommandSenderWorld(), this);
        fishingBobber.setPos(this.getEyePosition());

        double d0 = target.x() - this.getX();
        double d1 = target.y() - this.getY();
        double d2 = target.z() - this.getZ();
        double d3 = Mth.sqrt((float) (d0 * d0 + d2 * d2));

        float angle = 0.25F;
        float force = 0.75F;
        float accuracy = 40;

        fishingBobber.shoot(d0, d1 + d3 * angle, d2, force, accuracy);

        this.getCommandSenderWorld().addFreshEntity(fishingBobber);

        return fishingBobber;
    }
}
