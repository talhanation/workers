package com.talhanation.workers.entities;

import com.google.common.collect.ImmutableSet;
import com.talhanation.workers.Main;
import com.talhanation.workers.Translatable;
import com.talhanation.workers.config.WorkersModConfig;
import com.talhanation.workers.entities.ai.ControlBoatAI;
import com.talhanation.workers.entities.ai.MerchantAI;
import com.talhanation.workers.inventory.MerchantInventoryContainer;
import com.talhanation.workers.inventory.MerchantTradeContainer;
import com.talhanation.workers.inventory.MerchantWaypointContainer;
import com.talhanation.workers.network.MessageOpenGuiMerchant;
import com.talhanation.workers.network.MessageOpenGuiWorker;
import com.talhanation.workers.network.MessageToClientUpdateMerchantScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.network.chat.Component;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;

import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import static com.talhanation.workers.Translatable.*;

public class MerchantEntity extends AbstractWorkerEntity implements IBoatController {
    private static final EntityDataAccessor<Boolean> TRAVELING = SynchedEntityData.defineId(MerchantEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(MerchantEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TRAVEL_SPEED_STATE = SynchedEntityData.defineId(MerchantEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<BlockPos>> CURRENT_WAYPOINT = SynchedEntityData.defineId(MerchantEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Integer> CURRENT_WAYPOINT_INDEX = SynchedEntityData.defineId(MerchantEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CURRENT_RETURNING_TIME = SynchedEntityData.defineId(MerchantEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> RETURNING_TIME = SynchedEntityData.defineId(MerchantEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<BlockPos>> SAIL_POS = SynchedEntityData.defineId(MerchantEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Boolean> RETURNING = SynchedEntityData.defineId(MerchantEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_CREATIVE = SynchedEntityData.defineId(MerchantEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_DAY_COUNTED = SynchedEntityData.defineId(MerchantEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<UUID>> HORSE_ID = SynchedEntityData.defineId(MerchantEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Optional<UUID>> BOAT_ID = SynchedEntityData.defineId(MerchantEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Boolean> AUTO_START_TRAVEL = SynchedEntityData.defineId(MerchantEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> INFO = SynchedEntityData.defineId(MerchantEntity.class, EntityDataSerializers.BOOLEAN);

    private final SimpleContainer tradeInventory = new SimpleContainer(8);
    public boolean isTrading;
    private List<Integer> TRADE_LIMITS = new ArrayList<>();
    private List<Integer> CURRENT_TRADES = new ArrayList<>();
    public List<BlockPos> WAYPOINTS = new ArrayList<>();
    public List<ItemStack> WAYPOINT_ITEMS = new ArrayList<>();
    public MerchantEntity(EntityType<? extends AbstractWorkerEntity> entityType, Level world) {
        super(entityType, world);
    }

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(TRAVELING, false);
        this.entityData.define(RETURNING, false);
        this.entityData.define(RETURNING_TIME, 1);
        this.entityData.define(TRAVEL_SPEED_STATE, 1);
        this.entityData.define(CURRENT_RETURNING_TIME, 0);
        this.entityData.define(CURRENT_WAYPOINT_INDEX, 0);
        this.entityData.define(STATE, 0);
        this.entityData.define(CURRENT_WAYPOINT, Optional.empty());
        this.entityData.define(SAIL_POS, Optional.empty());
        this.entityData.define(HORSE_ID, Optional.empty());
        this.entityData.define(BOAT_ID, Optional.empty());
        this.entityData.define(IS_CREATIVE, false);
        this.entityData.define(IS_DAY_COUNTED, false);
        this.entityData.define(AUTO_START_TRAVEL, true);
        this.entityData.define(INFO, true);
    }
    @Override
    public boolean needsToSleep() {
        return !this.getTraveling() && super.needsToSleep();
    }

    private void initTradeLimits() {
        if(TRADE_LIMITS.isEmpty()){
            TRADE_LIMITS = Arrays.asList(16,16,16,16);
        }
    }

    private void initCurrentTrades() {
        if(CURRENT_TRADES.isEmpty()){
            CURRENT_TRADES = Arrays.asList(0,0,0,0);
        }
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public int workerCosts() {
        return WorkersModConfig.MerchantCost.get();
    }

    @Override
    public Predicate<ItemEntity> getAllowedItems() {
        return null;
    }
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.level.isClientSide) {
            return InteractionResult.CONSUME;
        }
        else {
            if (this.isCreative()) {
                if (player.isCreative() && player.createCommandSourceStack().hasPermission(4)) {
                    if (player.isCrouching()) {
                        openGUI(player);
                    }
                    else {
                        if(status == Status.FOLLOW) this.setStatus(prevStatus);
                        else this.setStatus(Status.FOLLOW, true);
                        return InteractionResult.SUCCESS;
                    }
                }
                else {
                    openTradeGUI(player);
                    return InteractionResult.SUCCESS;
                }
            }
            else {
                if (this.isTame() && (player.getUUID().equals(this.getOwnerUUID()))) {
                    if (player.isCrouching()) {
                        openGUI(player);
                    }
                    if (!player.isCrouching()) {
                        if(status == Status.FOLLOW)
                            this.setStatus(Objects.requireNonNullElse(prevStatus, Status.IDLE), true);
                        else this.setStatus(Status.FOLLOW, true);
                        return InteractionResult.SUCCESS;
                    }
                }
                else if (this.isTame() && !player.getUUID().equals(this.getOwnerUUID())) {
                    this.tellPlayer(player, TEXT_HELLO_OWNED(this.getProfessionName(), this.getOwnerName()));
                    if (!player.isCrouching()) {
                        openTradeGUI(player);
                        return InteractionResult.SUCCESS;
                    }
                }
                else if (!this.isTame()) {
                    this.tellPlayer(player, TEXT_HELLO(this.getProfessionName()));
                    this.openHireGUI(player);
                    this.navigation.stop();
                    return InteractionResult.SUCCESS;
                }
            }
        }
        return InteractionResult.PASS;
    }

    public void setFollow(boolean bool) {
        //super.setFollow(bool);
        if (!this.getReturning() && !this.getTraveling() && !bool && this.getVehicle() instanceof Boat boat && boat.getEncodeId().contains("smallships")) {
            this.setSmallShipsSailState(boat, 0);
            this.setSailPos(null);
        }
    }

    public void openTradeGUI(Player player) {
        if (player instanceof ServerPlayer) {
            NetworkHooks.openGui((ServerPlayer) player, new MenuProvider() {
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

    public void openWaypointsGUI(Player player) {
        if (player instanceof ServerPlayer) {
            NetworkHooks.openGui((ServerPlayer) player, new MenuProvider() {
                @Override
                public @NotNull Component getDisplayName() {
                    return getName();
                }

                @Override
                public @NotNull AbstractContainerMenu createMenu(int i, @NotNull Inventory playerInventory, @NotNull Player playerEntity) {
                    return new MerchantWaypointContainer(i, player, MerchantEntity.this, playerInventory);
                }
            }, packetBuffer -> {
                packetBuffer.writeUUID(getUUID());
            });
        } else {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageOpenGuiMerchant(player, this.getUUID()));
        }

        if (player instanceof ServerPlayer) {
            Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player), new MessageToClientUpdateMerchantScreen(this.WAYPOINTS, this.WAYPOINT_ITEMS, getCurrentTrades(), getTradeLimits(), this.getTraveling(), this.getReturning()));
        }
    }

    @Override
    public void openGUI(Player player) {
        if (player instanceof ServerPlayer) {
            NetworkHooks.openGui((ServerPlayer) player, new MenuProvider() {
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

        if (player instanceof ServerPlayer) {
            Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player), new MessageToClientUpdateMerchantScreen(this.WAYPOINTS, this.WAYPOINT_ITEMS, getCurrentTrades(), getTradeLimits(), this.getTraveling(), this.getReturning()));
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
        this.goalSelector.addGoal(0, new ControlBoatAI(this));
        //this.goalSelector.addGoal(1, new FloatGoal(this));
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
        this.populateDefaultEquipmentEnchantments(difficultyInstance);
        this.initSpawn();

        return ilivingentitydata;
    }

    @Override
    public void initSpawn() {
        super.initSpawn();
        TextComponent name = new TextComponent("Merchant");

        this.setProfessionName(name.getString());
        this.setCustomName(name);

        this.heal(100);
    }

    @Override
    public boolean isRequiredMainTool(ItemStack tool) {
        return false;
    }
    public boolean hasAMainTool(){
        return false;
    }
    @Override
    public boolean isRequiredSecondTool(ItemStack tool) {
        return false;
    }
    public boolean hasASecondTool(){
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

        if(this.getHorseUUID() != null){
            nbt.putUUID("HorseUUID", this.getHorseUUID());
        }

        if(this.getBoatUUID() != null){
            nbt.putUUID("BoatUUID", this.getBoatUUID());
        }

        nbt.putBoolean("Traveling", this.getTraveling());
        nbt.putBoolean("AutoStartTravel", this.getAutoStartTravel());
        nbt.putBoolean("Returning", this.getReturning());
        nbt.putInt("CurrentWayPointIndex", this.getCurrentWayPointIndex());
        nbt.putInt("ReturningTime", this.getReturningTime());
        nbt.putInt("CurrentReturningTime", this.getCurrentReturningTime());
        nbt.putBoolean("isCreative", this.isCreative());
        nbt.putBoolean("isDayCounted", this.isDayCounted());

        BlockPos currentWayPoint = this.getCurrentWayPoint();
        if (currentWayPoint != null) this.setNbtPosition(nbt, "CurrentWayPoint", currentWayPoint);


        ListTag waypointItems = new ListTag();
        for (int i = 0; i < WAYPOINT_ITEMS.size(); ++i) {
            ItemStack itemstack = WAYPOINT_ITEMS.get(i);
            if (!itemstack.isEmpty()) {
                CompoundTag compoundnbt = new CompoundTag();
                compoundnbt.putByte("WaypointItem", (byte) i);
                itemstack.save(compoundnbt);
                waypointItems.add(compoundnbt);
            }
        }
        nbt.put("WaypointItems", waypointItems);

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

        ListTag limits = new ListTag();
        for(int i = 0; i < 4; i++) {
            CompoundTag compoundnbt = new CompoundTag();
            compoundnbt.putByte("TradeLimit_" + i, (byte) i);

            int limit = getTradeLimits().get(i);
            compoundnbt.putInt("Limit", limit);

            limits.add(compoundnbt);
        }
        nbt.put("TradeLimits", limits);


        ListTag trades = new ListTag();
        for(int i = 0; i < 4; i++) {
            CompoundTag compoundnbt = new CompoundTag();
            compoundnbt.putByte("Trade_" + i, (byte) i);
            int trade = getCurrentTrades().get(i);
            compoundnbt.putInt("Trade", trade);

            trades.add(compoundnbt);
        }
        nbt.put("Trades", trades);

        nbt.putInt("State", this.getState());
        nbt.putInt("TravelSpeedState", this.getTravelSpeedState());
        nbt.putBoolean("InfoTravel", this.getSendInfo());
    }

    public void readAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        ListTag tradeList = nbt.getList("TradeInventory", 10);
        for (int i = 0; i < tradeList.size(); ++i) {
            CompoundTag compoundnbt = tradeList.getCompound(i);
            int j = compoundnbt.getByte("TradeSlot") & 255;

            this.tradeInventory.setItem(j, ItemStack.of(compoundnbt));
        }

        if (nbt.contains("HorseUUID")){
            Optional<UUID> uuid = Optional.of(nbt.getUUID("HorseUUID"));
            this.setHorseUUID(uuid);
        }

        if (nbt.contains("BoatUUID")){
            Optional<UUID> uuid = Optional.of(nbt.getUUID("BoatUUID"));
            this.setBoatUUID(uuid);
        }

        this.setTraveling(nbt.getBoolean("Traveling"));
        this.setAutoStartTravel(nbt.getBoolean("AutoStartTravel"));
        this.setReturning(nbt.getBoolean("Returning"));
        this.setCurrentWayPointIndex(nbt.getInt("CurrentWayPointIndex"));
        this.setReturningTime(nbt.getInt("ReturningTime"));
        this.setCurrentReturningTime(nbt.getInt("CurrentReturningTime"));

        this.setCreative(nbt.getBoolean("isCreative"));
        this.setIsDayCounted(nbt.getBoolean("isDayCounted"));

        BlockPos startPos = this.getNbtPosition(nbt, "CurrentWayPoint");
        if (startPos != null) this.setCurrentWayPoint(startPos);

        ListTag waypointItems = nbt.getList("WaypointItems", 10);
        for (int i = 0; i < waypointItems.size(); ++i) {
            CompoundTag compoundnbt = waypointItems.getCompound(i);

            ItemStack itemStack = ItemStack.of(compoundnbt);
            this.WAYPOINT_ITEMS.add(itemStack);
        }

        ListTag waypoints = nbt.getList("Waypoints", 10);
        for (int i = 0; i < waypoints.size(); ++i) {
            CompoundTag compoundnbt = waypoints.getCompound(i);
            BlockPos pos = new BlockPos(
                    compoundnbt.getDouble("PosX"),
                    compoundnbt.getDouble("PosY"),
                    compoundnbt.getDouble("PosZ"));
            this.WAYPOINTS.add(pos);
        }

        ListTag limits = nbt.getList("TradeLimits", 10);
        if(!limits.isEmpty()){
            for (int i = 0; i < 4; ++i) {
                CompoundTag compoundnbt = limits.getCompound(i);
                int limit = compoundnbt.getInt("Limit");

                this.getTradeLimits().set(i, limit);
            }
        }


        ListTag trades = nbt.getList("Trades", 10);
        if(!trades.isEmpty()) {
            for (int i = 0; i < 4; ++i) {
                CompoundTag compoundnbt = trades.getCompound(i);
                int trade = compoundnbt.getInt("Trade");

                this.getCurrentTrades().set(i, trade);
            }
        }

        this.setState(nbt.getInt("State"));
        this.setTravelSpeedState(nbt.getInt("TravelSpeedState"));
        this.setSendInfo(nbt.getBoolean("InfoTravel"));
    }

    public boolean isReturnTimeElapsed(){
        int maxDays = this.getReturningTime();
        boolean isDayCounted = this.isDayCounted();

        if(this.level.isNight() && !isDayCounted){
            this.setCurrentReturningTime(this.getCurrentReturningTime() + 1);
            this.setIsDayCounted(true);
        }
        else if(this.level.isDay())
            this.setIsDayCounted(false);

        int currentDays = this.getCurrentReturningTime();
        return currentDays >= maxDays && this.level.isDay();
    }

    public boolean isCreative() {
        return this.entityData.get(IS_CREATIVE);
    }

    public void setCreative(boolean creative){
        this.entityData.set(IS_CREATIVE, creative);
    }

    public boolean isDayCounted() {
        return this.entityData.get(IS_DAY_COUNTED);
    }

    public void setIsDayCounted(boolean counted){
        this.entityData.set(IS_DAY_COUNTED, counted);
    }
    @Override
    public void setStartPos(BlockPos pos) {
        ItemStack itemStack = this.getItemStackToRender(pos);

        if(pos != null && !WAYPOINTS.isEmpty()){
            BlockPos prevPos = this.WAYPOINTS.get(WAYPOINTS.size() -1);
            double distance = pos.distSqr(prevPos);

            boolean notNearFromCoastToWater = !isWaterBlockPos(prevPos) && isWaterBlockPos(pos) && distance >= 75F;
            boolean notNearInRiverWater = this.getLevel().getBiome(pos).is(BiomeTags.IS_RIVER) && isWaterBlockPos(prevPos) && isWaterBlockPos(pos) && distance >= 5000F;
            boolean notNearFromWaterToCoast = isWaterBlockPos(prevPos) && !isWaterBlockPos(pos) && distance >= 75F;

            if(notNearFromCoastToWater || notNearInRiverWater || notNearFromWaterToCoast){
                if(this.getOwner() != null) this.tellPlayer(this.getOwner(), Translatable.TEXT_WAYPOINT_NOT_NEAR_TO_PREV);
            }
            else{
                WAYPOINT_ITEMS.add(itemStack);
                WAYPOINTS.add(pos);
            }
        }
        else{
            WAYPOINT_ITEMS.add(itemStack);
            WAYPOINTS.add(pos);
        }

    }

    public ItemStack getItemStackToRender(BlockPos pos){
        BlockState state = this.level.getBlockState(pos);
        ItemStack itemStack;
        if (state.is(Blocks.WATER) || state.is(Blocks.KELP) || state.is(Blocks.KELP_PLANT)){
            //TODO: add item when smallships is installed
            itemStack = new ItemStack(Items.OAK_BOAT);
        }
        else if (state.is(Blocks.AIR) || state.is(Blocks.CAVE_AIR)) itemStack = new ItemStack(Items.GRASS_BLOCK);
        else itemStack = new ItemStack(state.getBlock().asItem());

        return itemStack;
    }

    public boolean getAutoStartTravel() {
        return this.entityData.get(AUTO_START_TRAVEL);
    }

    public void setAutoStartTravel(boolean bool){
        this.entityData.set(AUTO_START_TRAVEL, bool);
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

    public boolean getReturning() {
        return this.entityData.get(RETURNING);
    }

    public void setReturning(boolean returning){
        this.entityData.set(RETURNING, returning);
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

    public int getCurrentReturningTime() { //MC Days
        return entityData.get(CURRENT_RETURNING_TIME);
    }

    public void setCurrentReturningTime(int x) {
        entityData.set(CURRENT_RETURNING_TIME, x);
    }

    @Nullable
    public UUID getHorseUUID() {
        return this.entityData.get(HORSE_ID).orElse(null);
    }

    public void setHorseUUID(Optional<UUID> id) {
        this.entityData.set(HORSE_ID, id);
    }

    @Nullable
    public UUID getBoatUUID() {
        return this.entityData.get(BOAT_ID).orElse(null);
    }

    public void setBoatUUID(Optional<UUID> id) {
        this.entityData.set(BOAT_ID, id);
    }

    @Override
    @Nullable
    public BlockPos getSailPos() {
        return entityData.get(SAIL_POS).orElse(null);
    }

    @Override
    public void setSailPos(BlockPos pos) {
        this.entityData.set(SAIL_POS, Optional.ofNullable(pos));
    }

    public int  getState() {
        return entityData.get(STATE);
    }

    public void setState(int x) {
        entityData.set(STATE, x);
    }

    public int  getTravelSpeedState() {
        return entityData.get(TRAVEL_SPEED_STATE);
    }

    public void setTravelSpeedState(int x) {
        entityData.set(TRAVEL_SPEED_STATE, x);
    }

    public void setTradeLimit(int index, int limit) {
        this.getTradeLimits().set(index, limit);
    }

    public int getTradeLimit(int index){
        return this.getTradeLimits().get(index);
    }

    public int getCurrentTrades(int index){
        return this.getCurrentTrades().get(index);
    }

    public List<Integer> getTradeLimits(){
        if(TRADE_LIMITS.isEmpty()){
            initTradeLimits();
        }
        return this.TRADE_LIMITS;
    }

    public List<Integer> getCurrentTrades(){
        if(CURRENT_TRADES.isEmpty()){
            initCurrentTrades();
        }
        return this.CURRENT_TRADES;
    }


    public void setCurrentTrades(int index, int current) {
        this.getCurrentTrades().set(index, current);
    }

    public void die(@NotNull DamageSource dmg) {
        super.die(dmg);
        if(!isCreative()){
            for (int i = 0; i < this.tradeInventory.getContainerSize(); i++)
                Containers.dropItemStack(this.level, getX(), getY(), getZ(), this.tradeInventory.getItem(i));
        }
    }
    @Override
    public boolean hurt(DamageSource dmg, float amt) {
        if(!isCreative()) return super.hurt(dmg, amt);
        else return false;
    }

    private static final Set<Block> ALLOWED_WATER_BLOCKS = ImmutableSet.of(
            Blocks.WATER, Blocks.KELP, Blocks.KELP_PLANT, Blocks.SEAGRASS, Blocks.TALL_SEAGRASS);

    public boolean isWaterBlockPos(BlockPos pos){
        BlockState state = this.level.getBlockState(pos);

        return ALLOWED_WATER_BLOCKS.contains(state.getBlock());
    }

    public boolean  isFreeWater(double x, double y , double z){
        for(int i = -2; i <= 2; i++) {
            for (int k = -2; k <= 2; k++) {
                BlockPos pos = new BlockPos(x, y, z).offset(i, 0, k);
                BlockState state = this.level.getBlockState(pos);

                if(!state.is(Blocks.WATER) && !state.is(Blocks.KELP) && !state.is(Blocks.KELP_PLANT) && !state.is(Blocks.SEAGRASS) && !state.is(Blocks.TALL_SEAGRASS))
                    return false;
            }
        }
        return true;
    }
    //-1721 67 -902
    public float getPrecisionMin(){
        int base = 50;
        if(this.getVehicle().getEncodeId().contains("smallships")){
            base = 100;
        }
        return base;
    }

    public float getPrecisionMax(){
        int base = 150;
        if(this.getVehicle().getEncodeId().contains("smallships")){
            base = 200;
        }

        return base;
    }

    public enum State{
        IDLE(0),
        HOME(1),
        MOVE_TO_BOAT(2),
        TRAVELING_GROUND(3),
        SAILING(4),
        PAUSING(5),
        ARRIVED(6);

        private final int index;
        State(int index){
            this.index = index;
        }

        public int getIndex(){
            return this.index;
        }

        public static State fromIndex(int index) {
            for (State state : State.values()) {
                if (state.getIndex() == index) {
                    return state;
                }
            }
            throw new IllegalArgumentException("Invalid State index: " + index);
        }
    }

    public boolean getSendInfo(){
        return entityData.get(INFO);
    }

    public void setSendInfo(boolean send){
        entityData.set(INFO, send);
    }

    @Override
    public List<Item> inventoryInputHelp() {
        return null;
    }
}