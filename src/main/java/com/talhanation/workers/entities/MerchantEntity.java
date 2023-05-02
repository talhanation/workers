package com.talhanation.workers.entities;

import com.talhanation.workers.Main;
import com.talhanation.workers.entities.ai.MerchantAI;
import com.talhanation.workers.inventory.MerchantInventoryContainer;
import com.talhanation.workers.inventory.MerchantTradeContainer;
import com.talhanation.workers.entities.ai.WorkerFollowOwnerGoal;
import com.talhanation.workers.network.MessageOpenGuiMerchant;
import com.talhanation.workers.network.MessageOpenGuiWorker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.Containers;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.network.chat.Component;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import org.jetbrains.annotations.NotNull;

import static com.talhanation.workers.Translatable.TEXT_HELLO;
import static com.talhanation.workers.Translatable.TEXT_HELLO_OWNED;

public class MerchantEntity extends AbstractWorkerEntity {

    private static final EntityDataAccessor<Boolean> TRAVELING = SynchedEntityData.defineId(MerchantEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(MerchantEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<BlockPos>> CURRENT_WAYPOINT = SynchedEntityData.defineId(MerchantEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Integer> CURRENT_WAYPOINT_INDEX = SynchedEntityData.defineId(MerchantEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> RETURNING_TIME = SynchedEntityData.defineId(MerchantEntity.class, EntityDataSerializers.INT);
    private final SimpleContainer tradeInventory = new SimpleContainer(8);
    public boolean isTrading;

    public List<BlockPos> WAYPOINTS = new ArrayList<>();

    public MerchantEntity(EntityType<? extends AbstractWorkerEntity> entityType, Level world) {
        super(entityType, world);
        this.initSpawn();
    }

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(TRAVELING, false);
        this.entityData.define(RETURNING_TIME, 1);
        this.entityData.define(CURRENT_WAYPOINT_INDEX, 0);
        this.entityData.define(CURRENT_WAYPOINT, Optional.empty());
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    protected boolean shouldLoadChunk() {
        return false;
    }

    @Override
    public int workerCosts() {
        return 25;
    }

    @Override
    public Predicate<ItemEntity> getAllowedItems() {
        return null;
    }
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.level.isClientSide) {
            return InteractionResult.CONSUME;
        } else {
            if (this.isTame() && player.getUUID().equals(this.getOwnerUUID())) {
                if (player.isCrouching()) {
                    openGUI(player);
                }
                if (!player.isCrouching()) {
                    setFollow(!getFollow());
                    return InteractionResult.SUCCESS;
                }
            } else if (this.isTame() && !player.getUUID().equals(this.getOwnerUUID())) {
                this.tellPlayer(player, TEXT_HELLO_OWNED(this.getProfessionName(), this.getOwnerName()));
                if (!player.isCrouching()) {
                    openTradeGUI(player);
                    return InteractionResult.SUCCESS;
                }
            } else if (!this.isTame()) {
                this.tellPlayer(player, TEXT_HELLO(this.getProfessionName()));
                this.openHireGUI(player);
                this.navigation.stop();
                return InteractionResult.SUCCESS;
            }
            return super.mobInteract(player, hand);
        }
    }

    public void openTradeGUI(Player player) {
        if (player instanceof ServerPlayer) {
            NetworkHooks.openScreen((ServerPlayer) player, new MenuProvider() {
                @Override
                public @NotNull Component getDisplayName() {
                    return getName();
                }

                @Override
                public @NotNull AbstractContainerMenu createMenu(int i, @NotNull Inventory playerInventory, @NotNull Player playerEntity) {
                    return new MerchantTradeContainer(i, MerchantEntity.this, playerInventory);
                }
            }, packetBuffer -> {
                packetBuffer.writeUUID(getUUID());
            });
        } else {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageOpenGuiMerchant(player, this.getUUID()));
        }
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
                    return new MerchantInventoryContainer(i, MerchantEntity.this, playerInventory);
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
                .add(Attributes.MAX_HEALTH, 100.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new WorkerFollowOwnerGoal(this, 1.2D, 7.F, 1.0F));
        this.goalSelector.addGoal(3, new PanicGoal(this, 2D));
        this.goalSelector.addGoal(4, new MerchantAI(this));
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(@NotNull ServerLevel p_241840_1_, @NotNull AgeableMob p_241840_2_) {
        return null;
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor world, @NotNull DifficultyInstance difficultyInstance, @NotNull MobSpawnType reason, @Nullable SpawnGroupData data, @Nullable CompoundTag nbt) {
        SpawnGroupData ilivingentitydata = super.finalizeSpawn(world, difficultyInstance, reason, data, nbt);
        this.populateDefaultEquipmentEnchantments(random, difficultyInstance);

        this.initSpawn();

        return ilivingentitydata;
    }

    @Override
    public void initSpawn() {
        super.initSpawn();
        Component name = Component.translatable("entity.workers.merchant");

        this.setProfessionName(name.getString());
        this.setCustomName(name);

        this.heal(100);
    }

    @Override
    public boolean shouldDirectNavigation() {
        return false;
    }

    @Override
    public void setEquipment() {
        int i = this.random.nextInt(9);
        if (i == 0) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOOK));
        } else {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.PAPER));
        }
    }

    public void addAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        ListTag list = new ListTag();
        for (int i = 0; i < this.tradeInventory.getContainerSize(); ++i) {
            ItemStack itemstack = this.tradeInventory.getItem(i);
            if (!itemstack.isEmpty()) {
                CompoundTag compoundnbt = new CompoundTag();
                compoundnbt.putByte("TradeSlot", (byte) i);
                itemstack.save(compoundnbt);
                list.add(compoundnbt);
            }
        }
        nbt.put("TradeInventory", list);

        nbt.putBoolean("Traveling", this.getTraveling());
        nbt.putInt("CurrentWayPointIndex", this.getCurrentWayPointIndex());
        nbt.putInt("ReturningTime", this.getReturningTime());

        BlockPos currentWayPoint = this.getCurrentWayPoint();
        if (currentWayPoint != null) this.setNbtPosition(nbt, "CurrentWayPoint", currentWayPoint);


        ListTag waypoints = new ListTag();
        for(int i = 0; i < WAYPOINTS.size(); i++){
            CompoundTag compoundnbt = new CompoundTag();
            compoundnbt.putByte("Waypoint", (byte) i);
            BlockPos pos = WAYPOINTS.get(i);
            compoundnbt.putDouble("PosX", pos.getX());
            compoundnbt.putDouble("PosY", pos.getY());
            compoundnbt.putDouble("PosZ", pos.getZ());

            waypoints.add(compoundnbt);
        }
        nbt.put("Waypoints", waypoints);
    }

    public void readAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        ListTag list = nbt.getList("TradeInventory", 10);
        for (int i = 0; i < list.size(); ++i) {
            CompoundTag compoundnbt = list.getCompound(i);
            int j = compoundnbt.getByte("TradeSlot") & 255;

            this.tradeInventory.setItem(j, ItemStack.of(compoundnbt));
        }

        this.setTraveling(nbt.getBoolean("Traveling"));
        this.setCurrentWayPointIndex(nbt.getInt("CurrentWayPointIndex"));
        this.setReturningTime(nbt.getInt("ReturningTime"));

        BlockPos startPos = this.getNbtPosition(nbt, "CurrentWayPoint");
        if (startPos != null) this.setCurrentWayPoint(startPos);


        ListTag waypoints = nbt.getList("Waypoints", 10);
        for (int i = 0; i < waypoints.size(); ++i) {
            CompoundTag compoundnbt = waypoints.getCompound(i);
            BlockPos pos = new BlockPos(
                    compoundnbt.getDouble("PosX"),
                    compoundnbt.getDouble("PosY"),
                    compoundnbt.getDouble("PosZ"));


            this.WAYPOINTS.add(pos);
        }
    }

    @Override
    public void setStartPos(BlockPos pos) {
        WAYPOINTS.add(pos);
    }

    public SimpleContainer getTradeInventory() {
        return this.tradeInventory;
    }

    public boolean getTraveling() {
        return this.entityData.get(TRAVELING);
    }

    public void setTraveling(boolean traveling){
        this.entityData.set(TRAVELING, traveling);
    }

    public BlockPos getCurrentWayPoint(){
        return this.entityData.get(CURRENT_WAYPOINT).orElse(null);
    }

    public void setCurrentWayPoint(BlockPos wayPoint){
        this.entityData.set(CURRENT_WAYPOINT, Optional.of(wayPoint));
    }

    public int getCurrentWayPointIndex() {
        return entityData.get(CURRENT_WAYPOINT_INDEX);
    }

    public void setCurrentWayPointIndex(int x) {
        entityData.set(CURRENT_WAYPOINT_INDEX, x);
    }

    public int getReturningTime() { //MC Days
        return entityData.get(RETURNING_TIME);
    }

    public void setReturningTime(int x) {
        entityData.set(RETURNING_TIME, x);
    }

    public void die(@NotNull DamageSource dmg) {
        super.die(dmg);
        for (int i = 0; i < this.tradeInventory.getContainerSize(); i++)
            Containers.dropItemStack(this.level, getX(), getY(), getZ(), this.tradeInventory.getItem(i));
    }

    public enum State{
        IDLE,
        HOME,
        TRAVELING,
        PAUSING,
        ARRIVED,
        RETURNING,
    }
}