package com.talhanation.workers.entities;

import com.talhanation.workers.config.WorkersModConfig;
import com.talhanation.workers.entities.ai.ShepherdAI;
import com.talhanation.workers.entities.ai.WorkerPickupWantedItemGoal;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public class ShepherdEntity extends AbstractAnimalFarmerEntity {
    public final ItemStack MAIN_TOOL = new ItemStack(Items.SHEARS);
    public final ItemStack SECOND_TOOL = new ItemStack(Items.STONE_AXE);
    private final Predicate<ItemEntity> ALLOWED_ITEMS = (item) -> {
        return !item.hasPickUpDelay() && item.isAlive() && this.wantsToPickUp(item.getItem());
    };

    public boolean canWorkWithoutTool(){
        return false;
    }

    public ShepherdEntity(EntityType<? extends AbstractWorkerEntity> entityType, Level world) {
        super(entityType, world);
    }

    @Override
    public void tick() {
        super.tick();
    }


    @Override
    public int getWorkerCost() {
        return WorkersModConfig.ShepherdCost.get();
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
        this.goalSelector.addGoal(3, new ShepherdAI(this));
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(@NotNull ServerLevel p_241840_1_, @NotNull AgeableMob p_241840_2_) {
        return null;
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficultyInstance,
            MobSpawnType reason, @Nullable SpawnGroupData data, @Nullable CompoundTag nbt) {
        SpawnGroupData ilivingentitydata = super.finalizeSpawn(world, difficultyInstance, reason, data, nbt);
        this.populateDefaultEquipmentEnchantments(random, difficultyInstance);

        this.initSpawn();

        return ilivingentitydata;
    }

    @Override
    public void initSpawn() {
        super.initSpawn();
        Component name = Component.literal("Shepherd");

        this.setProfessionName(name.getString());
        this.setCustomName(name);
        this.cost = WorkersModConfig.ShepherdCost.get();
    }

    @Override
    public boolean wantsToPickUp(ItemStack itemStack) {
        return (itemStack.is(ItemTags.WOOL) || itemStack.is(Items.MUTTON));
    }

    @Override
    public boolean wantsToKeep(ItemStack itemStack) {
        return super.wantsToKeep(itemStack);
    }

    @Override
    public void setEquipment() {
        ItemStack initialTool = MAIN_TOOL;
        this.updateInventory(0, initialTool);
        ItemStack initialTool2 = SECOND_TOOL;
        this.updateInventory(1, initialTool2);

        this.equipTool(initialTool);
        this.equipTool(initialTool2);
    }

    public boolean isRequiredMainTool(ItemStack tool) {
        return tool.getItem() instanceof ShearsItem;
    }

    public boolean isRequiredSecondTool(ItemStack tool) {
        return tool.getItem() instanceof AxeItem;
    }
    public boolean hasAMainTool(){
        return true;
    }
    public boolean hasASecondTool(){
        return true;
    }
    @Override
    public List<Item> inventoryInputHelp() {
        return Arrays.asList(Items.IRON_AXE, Items.SHEARS , Items.WHEAT);
    }
}
