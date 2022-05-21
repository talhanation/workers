package com.talhanation.workers.entities;

import com.google.common.collect.ImmutableSet;
import com.talhanation.workers.Main;
import com.talhanation.workers.inventory.WorkerInventoryContainer;
import com.talhanation.workers.entities.ai.*;
import com.talhanation.workers.network.MessageOpenGuiWorker;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.function.Predicate;

import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.GolemRandomStrollInVillageGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveBackToVillageGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;

public class LumberjackEntity extends AbstractWorkerEntity{

    public final Predicate<ItemEntity> ALLOWED_ITEMS = (item) ->
            (!item.hasPickUpDelay() && item.isAlive() && this.wantsToPickUp(item.getItem()));

    public final Predicate<Block> ALLOWED_BLOCKS = (item) ->
            (this.wantsToBreak(item));

    public static final Set<Item> WANTED_SAPLINGS = ImmutableSet.of(
            Items.OAK_SAPLING,
            Items.BIRCH_SAPLING,
            Items.SPRUCE_SAPLING,
            Items.ACACIA_SAPLING,
            Items.JUNGLE_SAPLING,
            Items.DARK_OAK_SAPLING
    );

    public static final Set<Item> WANTED_ITEMS = ImmutableSet.of(
            Items.OAK_LOG,
            Items.OAK_WOOD,
            Items.BIRCH_LOG,
            Items.BIRCH_WOOD,
            Items.SPRUCE_LOG,
            Items.SPRUCE_WOOD,
            Items.ACACIA_LOG,
            Items.ACACIA_WOOD,
            Items.JUNGLE_LOG,
            Items.JUNGLE_WOOD,
            Items.DARK_OAK_LOG,
            Items.DARK_OAK_WOOD,
            Items.STRIPPED_OAK_LOG,
            Items.STRIPPED_OAK_WOOD,
            Items.STRIPPED_BIRCH_LOG,
            Items.STRIPPED_BIRCH_WOOD,
            Items.STRIPPED_SPRUCE_LOG,
            Items.STRIPPED_SPRUCE_WOOD,
            Items.STRIPPED_ACACIA_LOG,
            Items.STRIPPED_ACACIA_WOOD,
            Items.STRIPPED_JUNGLE_LOG,
            Items.STRIPPED_JUNGLE_WOOD,
            Items.STRIPPED_DARK_OAK_LOG,
            Items.STRIPPED_DARK_OAK_WOOD,
            Items.STICK,
            Items.APPLE
    );

    public static final Set<Block> WANTED_BLOCKS = ImmutableSet.of(
            Blocks.ACACIA_LOG,
            Blocks.ACACIA_WOOD,
            Blocks.BIRCH_LOG,
            Blocks.BIRCH_WOOD,
            Blocks.DARK_OAK_LOG,
            Blocks.DARK_OAK_WOOD,
            Blocks.JUNGLE_LOG,
            Blocks.JUNGLE_WOOD,
            Blocks.OAK_LOG,
            Blocks.OAK_WOOD,
            Blocks.SPRUCE_LOG,
            Blocks.SPRUCE_WOOD,
            Blocks.STRIPPED_ACACIA_LOG,
            Blocks.STRIPPED_ACACIA_WOOD,
            Blocks.STRIPPED_BIRCH_LOG,
            Blocks.STRIPPED_BIRCH_WOOD,
            Blocks.STRIPPED_DARK_OAK_LOG,
            Blocks.STRIPPED_DARK_OAK_WOOD,
            Blocks.STRIPPED_JUNGLE_LOG,
            Blocks.STRIPPED_JUNGLE_WOOD,
            Blocks.STRIPPED_OAK_LOG,
            Blocks.STRIPPED_OAK_WOOD,
            Blocks.STRIPPED_SPRUCE_LOG,
            Blocks.STRIPPED_SPRUCE_WOOD
    );

    public LumberjackEntity(EntityType<? extends AbstractWorkerEntity> entityType, Level world) {
        super(entityType, world);
    }

    public Predicate<ItemEntity> getAllowedItems(){
        return ALLOWED_ITEMS;
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    protected boolean shouldLoadChunk() {
        return true;
    }

    @Override
    public int workerCosts() {
        return 8;
    }

    //ATTRIBUTES
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
        this.goalSelector.addGoal(2, new WorkerFollowOwnerGoal(this, 1.2D, 7.F, 4.0F));
        this.goalSelector.addGoal(3, new LumberjackAI(this));

        this.goalSelector.addGoal(1, new PanicGoal(this, 1.3D));

        this.goalSelector.addGoal(9, new MoveBackToVillageGoal(this, 0.6D, false));
        this.goalSelector.addGoal(10, new GolemRandomStrollInVillageGoal(this, 0.6D));
        this.goalSelector.addGoal(10, new WaterAvoidingRandomStrollGoal(this, 1.0D, 0F));
        this.goalSelector.addGoal(11, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(12, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, LivingEntity.class, 8.0F));


        //this.targetSelector.addGoal(1, new (this));

    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel p_241840_1_, AgeableMob p_241840_2_) {
        return null;
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficultyInstance, MobSpawnType reason, @Nullable SpawnGroupData data, @Nullable CompoundTag nbt) {
        SpawnGroupData ilivingentitydata = super.finalizeSpawn(world, difficultyInstance, reason, data, nbt);
        ((GroundPathNavigation)this.getNavigation()).setCanOpenDoors(true);
        this.populateDefaultEquipmentEnchantments(difficultyInstance);

        this.initSpawn();

        return ilivingentitydata;
    }

    @Override
    public void initSpawn() {
        this.setCustomName(new TextComponent("Lumberjack"));
        this.setEquipment();
        this.getNavigation().setCanFloat(true);
        this.setDropEquipment();
        this.setRandomSpawnBonus();
        this.setPersistenceRequired();
        this.setCanPickUpLoot(true);
    }

    protected void pickUpItem(ItemEntity itemEntity) {
        ItemStack itemstack = itemEntity.getItem();
        if (this.wantsToPickUp(itemstack)) {
            SimpleContainer inventory = this.getInventory();
            boolean flag = inventory.canAddItem(itemstack);
            if (!flag) {
                return;
            }

            this.onItemPickup(itemEntity);
            this.take(itemEntity, itemstack.getCount());
            ItemStack itemstack1 = inventory.addItem(itemstack);
            if (itemstack1.isEmpty()) {
                itemEntity.remove(RemovalReason.DISCARDED);
            } else {
                itemstack.setCount(itemstack1.getCount());
            }
        }

    }
    @Override
    public boolean wantsToPickUp(ItemStack itemStack) {
        Item item = itemStack.getItem();
        return (WANTED_ITEMS.contains(item) || WANTED_SAPLINGS.contains(item));
    }

    public boolean wantsToBreak(Block block) {
        return (WANTED_BLOCKS.contains(block));
    }

    @Override
    public void setEquipment() {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_AXE));
    }

    @Override
    public void openGUI(Player player) {
        if (player instanceof ServerPlayer) {
            NetworkHooks.openGui((ServerPlayer) player, new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return getName();
                }

                @Nullable
                @Override
                public AbstractContainerMenu createMenu(int i, Inventory playerInventory, Player playerEntity) {
                    return new WorkerInventoryContainer(i, LumberjackEntity.this, playerInventory);
                }
            }, packetBuffer -> {packetBuffer.writeUUID(getUUID());});
        } else {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageOpenGuiWorker(player, this.getUUID()));
        }
    }
}