package com.talhanation.workers.entities;

import com.google.common.collect.ImmutableSet;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.pathfinding.AsyncGroundPathNavigation;
import com.talhanation.workers.WorkersMain;
import com.talhanation.workers.compat.FarmersDelight;
import com.talhanation.workers.config.WorkersServerConfig;
import com.talhanation.workers.entities.ai.FarmerWorkGoal;
import com.talhanation.workers.entities.workarea.AbstractWorkAreaEntity;
import com.talhanation.workers.entities.workarea.CropArea;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.WaterFluid;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class FarmerEntity extends AbstractWorkerEntity{
    public CropArea currentCropArea;
    public static final Set<Block> TILLABLES = ImmutableSet.of(
            Blocks.DIRT,
            Blocks.ROOTED_DIRT,
            Blocks.COARSE_DIRT,
            Blocks.GRASS_BLOCK);

    public FarmerEntity(EntityType<? extends AbstractWorkerEntity> entityType, Level world) {
        super(entityType, world);

    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new FarmerWorkGoal(this));
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
        this.setCustomName(Component.literal("Farmer"));
        //this.setCost(WorkersServerConfig.FarmerCost.get());
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
    public boolean isBucketWithWater(ItemStack itemStack) {
        if(itemStack.getItem() instanceof BucketItem bucketItem){
            Fluid fluid = bucketItem.getFluid();
            if(fluid instanceof WaterFluid || fluid.isSame(Fluids.WATER)) return true;
        }
        return false;
    }
    public boolean wantsToKeep(ItemStack itemStack) {
        if (itemStack.getItem() instanceof HoeItem) {
            int items = countMatchingItems(stack -> stack.getItem() instanceof HoeItem);
            return items <= 1;
        }

        if(currentCropArea != null) {
            ItemStack crop = currentCropArea.getSeedStack();
            if(ItemStack.isSameItem(crop, itemStack)){
                int items = countMatchingStacks(stack -> crop.is(stack.getItem()));
                return items <= 1;
            }
        }

        return super.wantsToKeep(itemStack);
    }
    public boolean wantsToPickUp(ItemStack itemStack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(itemStack.getItem());
        if(id == null) return false;

        if(WorkersServerConfig.FarmerPickup.get().contains(id.toString())) return true;
        if(itemStack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof CropBlock) return true;
        if(FarmersDelight.isRicePlantItem(itemStack) || FarmersDelight.isRiceSeedItem(itemStack)) return true;

        return super.wantsToPickUp(itemStack);
    }

    @Override
    protected void finalizeBlockBreak(BlockPos pos) {
        if (WorkersMain.isFarmersDelightInstalled && FarmersDelight.isKnife(this.getMainHandItem())) {
            Level level = this.getCommandSenderWorld();
            if (level.isClientSide()) return;

            BlockState state = level.getBlockState(pos);
            BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;

            List<ItemStack> drops = Block.getDrops(state, (ServerLevel) level, pos, blockEntity, this, this.getMainHandItem());
            for (ItemStack drop : drops) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), drop);
            }

            level.levelEvent(2001, pos, Block.getId(state));
            level.setBlock(pos, level.getFluidState(pos).createLegacyBlock(), 3);
            this.damageMainHandItem();
            return;
        }
        super.finalizeBlockBreak(pos);
    }

    public AbstractWorkAreaEntity getCurrentWorkArea(){
        return currentCropArea;
    }
}