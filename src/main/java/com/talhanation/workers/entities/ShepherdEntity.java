package com.talhanation.workers.entities;

import com.google.common.collect.ImmutableSet;
import com.talhanation.workers.Main;
import com.talhanation.workers.entities.ai.ShepherdAI;
import com.talhanation.workers.entities.ai.WorkerFollowOwnerGoal;
import com.talhanation.workers.entities.ai.WorkerPickupWantedItemGoal;
import com.talhanation.workers.inventory.ShepherdInventoryContainer;
import com.talhanation.workers.network.MessageOpenGuiShepherd;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.function.Predicate;

public class ShepherdEntity extends AbstractWorkerEntity{
    private static final EntityDataAccessor<Integer> MAX_SHEEPS = SynchedEntityData.defineId(MinerEntity.class, EntityDataSerializers.INT);

    private final Predicate<ItemEntity> ALLOWED_ITEMS = (item) -> {
        return !item.hasPickUpDelay() && item.isAlive() && this.wantsToPickUp(item.getItem());
    };

    private static final Set<Item> WANTED_ITEMS = ImmutableSet.of(
            Items.BLACK_WOOL,
            Items.WHITE_WOOL,
            Items.RED_WOOL,
            Items.BLUE_WOOL,
            Items.ORANGE_WOOL,
            Items.GREEN_WOOL,
            Items.LIGHT_BLUE_WOOL,
            Items.GRAY_WOOL,
            Items.LIGHT_GRAY_WOOL,
            Items.BROWN_WOOL,
            Items.CYAN_WOOL,
            Items.MAGENTA_WOOL,
            Items.YELLOW_WOOL,
            Items.PINK_WOOL,
            Items.LIME_WOOL,
            Items.MUTTON
    );

    public ShepherdEntity(EntityType<? extends AbstractWorkerEntity> entityType, Level world) {
        super(entityType, world);
    }

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(MAX_SHEEPS, 8);
    }

    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("MaxSheep", this.getMaxSheepCount());
    }

    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.setMaxSheepCount(nbt.getInt("MaxSheep"));
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
        return 12;
    }

    @Override
    public Predicate<ItemEntity> getAllowedItems(){
        return ALLOWED_ITEMS;
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
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new WorkerFollowOwnerGoal(this, 1.2D, 7.F, 4.0F));
        this.goalSelector.addGoal(2, new WorkerPickupWantedItemGoal(this));
        this.goalSelector.addGoal(3, new ShepherdAI(this, 1));
        //this.goalSelector.addGoal(3, new BreedAnimalGoal<>(this, SheepEntity.class));
        //this.goalSelector.addGoal(4, new SlaughterAnimalGoal<>(this, SheepEntity.class, 8));
        //this.goalSelector.addGoal(5, new SheerSheepGoal(this, 60));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0D, 0F));
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
        this.setCustomName(new TextComponent("Shepherd"));
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
        return (WANTED_ITEMS.contains(item));
    }

    @Override
    public void setEquipment() {
        int i = this.random.nextInt(9);
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.SHEARS));
        this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.WOODEN_HOE));
    }

    public void setMaxSheepCount(int x){
        this.entityData.set(MAX_SHEEPS, x);
    }

    public int getMaxSheepCount(){
        return entityData.get(MAX_SHEEPS);
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
                    return new ShepherdInventoryContainer(i, ShepherdEntity.this, playerInventory);
                }
            }, packetBuffer -> {packetBuffer.writeUUID(getUUID());});
        } else {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageOpenGuiShepherd(player, this.getUUID()));
        }
    }
}
