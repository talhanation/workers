package com.talhanation.workers.entities;

import com.google.common.collect.ImmutableSet;
import com.talhanation.workers.Main;
import com.talhanation.workers.config.WorkersModConfig;
import com.talhanation.workers.inventory.WorkerInventoryContainer;
import com.talhanation.workers.entities.ai.FarmerAI;
import com.talhanation.workers.entities.ai.WorkerPickupWantedItemGoal;
import com.talhanation.workers.network.MessageOpenGuiWorker;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.function.Predicate;

public class FarmerEntity extends AbstractWorkerEntity {

    private final Predicate<ItemEntity> ALLOWED_ITEMS = (item) -> !item.hasPickUpDelay() && item.isAlive()
            && this.wantsToPickUp(item.getItem());

    public static final Set<Item> WANTED_SEEDS = ImmutableSet.of(
            Items.WHEAT_SEEDS,
            Items.MELON_SEEDS,
            Items.POTATO,
            Items.BEETROOT_SEEDS,
            Items.CARROT);

    public final Set<Item> WANTED_ITEMS = ImmutableSet.of(
            Items.WHEAT,
            Items.MELON_SLICE,
            Items.POTATO,
            Items.BEETROOT,
            Items.CARROT);

    public static final Set<Block> CROP_BLOCKS = ImmutableSet.of(
            Blocks.WHEAT,
            Blocks.POTATOES,
            Blocks.CARROTS,
            Blocks.BEETROOTS,
            Blocks.MELON,
            Blocks.PUMPKIN);

    public FarmerEntity(EntityType<? extends AbstractWorkerEntity> entityType, Level world) {
        super(entityType, world);
        this.initSpawn();
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public int workerCosts() {
        return WorkersModConfig.FarmerCost.get();
    }

    @Override
    public Predicate<ItemEntity> getAllowedItems() {
        return ALLOWED_ITEMS;
    }

    @Override
    public void openGUI(Player player) {
        if (player instanceof ServerPlayer) {
            NetworkHooks.openScreen((ServerPlayer) player, new MenuProvider() {
                @Override
                public @NotNull Component getDisplayName() {
                    return getName();
                }

                @Override
                public @NotNull AbstractContainerMenu createMenu(int i, @NotNull Inventory playerInventory, @NotNull Player playerEntity) {
                    return new WorkerInventoryContainer(i, FarmerEntity.this, playerInventory);
                }
            }, packetBuffer -> {
                packetBuffer.writeUUID(getUUID());
            });
        } else {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageOpenGuiWorker(player, this.getUUID()));
        }
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
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new WorkerPickupWantedItemGoal(this));
        this.goalSelector.addGoal(3, new FarmerAI(this));

    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(@NotNull ServerLevel p_241840_1_, @NotNull AgeableMob p_241840_2_) {
        return null;
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor world, @NotNull DifficultyInstance difficultyInstance,
                                        @NotNull MobSpawnType reason, @Nullable SpawnGroupData data, @Nullable CompoundTag nbt) {
        SpawnGroupData ilivingentitydata = super.finalizeSpawn(world, difficultyInstance, reason, data, nbt);
        this.populateDefaultEquipmentEnchantments(random, difficultyInstance);

        this.initSpawn();

        return ilivingentitydata;
    }

    @Override
    public void initSpawn() {
        super.initSpawn();
        Component name = Component.literal("Farmer");

        this.setProfessionName(name.getString());
        this.setCustomName(name);
    }

    @Override
    public boolean wantsToPickUp(ItemStack itemStack) {
        Item item = itemStack.getItem();
        return (WANTED_ITEMS.contains(item) || WANTED_SEEDS.contains(item));
    }

    @Override
    public boolean wantsToKeep(ItemStack itemStack) {
        Item item = itemStack.getItem();
        return super.wantsToKeep(itemStack) || (WANTED_SEEDS.contains(item));
    }

    @Override
    public void setEquipment() {
        ItemStack initialTool = new ItemStack(Items.WOODEN_HOE);
        this.updateInventory(0, initialTool);
        this.equipTool(initialTool);
    }
}