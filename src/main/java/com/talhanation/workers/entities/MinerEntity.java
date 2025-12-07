package com.talhanation.workers.entities;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.pathfinding.AsyncGroundPathNavigation;
import com.talhanation.workers.config.WorkersServerConfig;
import com.talhanation.workers.entities.ai.MinerWorkGoal;
import com.talhanation.workers.entities.workarea.AbstractWorkAreaEntity;
import com.talhanation.workers.entities.workarea.MiningArea;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.TierSortingRegistry;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Predicate;

public class MinerEntity extends AbstractWorkerEntity{
    public MiningArea currentMiningArea;
    public MinerEntity(EntityType<? extends AbstractWorkerEntity> entityType, Level world) {
        super(entityType, world);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new MinerWorkGoal(this));
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
        this.setCustomName(Component.literal("Miner"));
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

    public boolean wantsToPickUp(ItemStack itemStack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(itemStack.getItem());
        if(id == null) return false;

        if(WorkersServerConfig.MINER_PICKUP.contains(id.toString())) return true;

        if(itemStack.getItem() instanceof BlockItem blockItem && blockItem.getBlock().defaultBlockState().is(BlockTags.BASE_STONE_OVERWORLD)) return true;
        if(itemStack.getItem() instanceof BlockItem blockItem && blockItem.getBlock().defaultBlockState().is(Tags.Blocks.STONE)) return true;
        if(itemStack.getItem() instanceof BlockItem blockItem && blockItem.getBlock().defaultBlockState().is(BlockTags.BASE_STONE_NETHER)) return true;
        if(itemStack.is(Tags.Items.RAW_MATERIALS)) return true;
        if(itemStack.is(Tags.Items.SAND)) return true;
        if(itemStack.is(Tags.Items.STONE)) return true;
        if(itemStack.is(ItemTags.STONE_BRICKS)) return true;
        if(itemStack.is(ItemTags.COAL_ORES)) return true;
        if(itemStack.is(ItemTags.IRON_ORES)) return true;
        if(itemStack.is(ItemTags.COPPER_ORES)) return true;
        if(itemStack.is(ItemTags.DIAMOND_ORES)) return true;
        if(itemStack.is(ItemTags.EMERALD_ORES)) return true;
        if(itemStack.is(ItemTags.GOLD_ORES)) return true;
        if(itemStack.is(ItemTags.LAPIS_ORES)) return true;
        if(itemStack.is(ItemTags.LAPIS_ORES)) return true;

        if(itemStack.is(ItemTags.DIRT)) return true;
        if(itemStack.getItem() instanceof BlockItem blockItem && blockItem.getBlock().defaultBlockState().is(BlockTags.BASE_STONE_OVERWORLD)) return true;
        if (itemStack.getItem() instanceof PickaxeItem && this.getMainHandItem().isEmpty()) {
            return !this.hasSameTypeOfItem(itemStack);
        }
        return super.wantsToPickUp(itemStack);
    }

    @Override
    public AbstractWorkAreaEntity getCurrentWorkArea() {
        return currentMiningArea;
    }

    public boolean shouldIgnoreBlock(BlockState blockState) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(blockState.getBlock());
        if(id == null) return false;
        return (WorkersServerConfig.MINER_IGNORE.contains(id.toString()) || !canBreakBlock(blockState));
    }

    public boolean canBreakBlock(BlockState state){
        ItemStack tool = this.getMainHandItem();
        if(tool.getItem() instanceof DiggerItem diggerItem){
            return TierSortingRegistry.isCorrectTierForDrops(diggerItem.getTier(), state);
        }
        else
            return false;
    }


    public void changeTool(BlockState blockState) {
        if (blockState != null) {
            if (blockState.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
                switchMainHandItem(itemStack -> itemStack.getItem() instanceof ShovelItem);
            }
            else if (blockState.is(BlockTags.MINEABLE_WITH_PICKAXE)) {
                switchMainHandItem(itemStack -> itemStack.getItem() instanceof PickaxeItem);
            }
            else
                switchMainHandItem(ItemStack::isEmpty);
        }
    }
}
