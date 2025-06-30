package com.talhanation.workers.entities;

import com.google.common.collect.ImmutableSet;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.pathfinding.AsyncGroundPathNavigation;
import com.talhanation.workers.config.WorkersServerConfig;
import com.talhanation.workers.entities.ai.DepositItemsInChestsGoal;
import com.talhanation.workers.entities.ai.FarmerWorkController;
import com.talhanation.workers.entities.ai.LumberjackWorkGoal;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.WaterFluid;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class LumberjackEntity extends AbstractWorkerEntity{

    public LumberjackEntity(EntityType<? extends AbstractWorkerEntity> entityType, Level world) {
        super(entityType, world);
        //this.workController = new FarmerWorkController(this);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new LumberjackWorkGoal(this));
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
        this.setCustomName(Component.literal("Lumberman"));
        //this.setCost(WorkersServerConfig.FarmerCost.get());
        this.setCost(10);

        this.setEquipment();
        this.setDropEquipment();
        this.setRandomSpawnBonus();
        this.setPersistenceRequired();

        this.setGroup(0);

        AbstractRecruitEntity.applySpawnValues(this);
    }

    @Override
    public List<Item> inventoryInputHelp() {
        return null;
    }

    public boolean wantsToPickUp(ItemStack itemStack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(itemStack.getItem());
        if(id == null) return false;

        if(WorkersServerConfig.LUMBERMAN_PICKUP.contains(id.toString())) return true;
        if(itemStack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof SaplingBlock) return true;
        if(itemStack.getItem() instanceof BlockItem blockItem && blockItem.getBlock().defaultBlockState().is(BlockTags.LOGS)) return true;
        if(itemStack.getItem() instanceof BlockItem blockItem && blockItem.getBlock().defaultBlockState().is(BlockTags.LEAVES)) return true;

        if (itemStack.getItem() instanceof AxeItem && this.getMainHandItem().isEmpty()) {
            return !this.hasSameTypeOfItem(itemStack);
        }
        return super.wantsToPickUp(itemStack);
    }
}
