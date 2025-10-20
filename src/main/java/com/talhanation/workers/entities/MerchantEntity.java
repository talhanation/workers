package com.talhanation.workers.entities;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.MessengerEntity;
import com.talhanation.recruits.pathfinding.AsyncGroundPathNavigation;
import com.talhanation.workers.Translatable;
import com.talhanation.workers.WorkersMain;
import com.talhanation.workers.config.WorkersServerConfig;
import com.talhanation.workers.entities.workarea.AbstractWorkAreaEntity;
import com.talhanation.workers.inventory.MerchantAddEditTradeContainer;
import com.talhanation.workers.inventory.MerchantTradeContainer;
import com.talhanation.workers.network.MessageOpenMerchantEditTradeScreen;
import com.talhanation.workers.network.MessageOpenMerchantTradeScreen;
import com.talhanation.workers.world.WorkersMerchantTrade;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

public class MerchantEntity extends AbstractWorkerEntity {
    private static final EntityDataAccessor<CompoundTag> TRADES = SynchedEntityData.defineId(MerchantEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<Integer> TRADER_PROGRESS = SynchedEntityData.defineId(MerchantEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TRADER_LEVEL = SynchedEntityData.defineId(MerchantEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> IS_TRADING = SynchedEntityData.defineId(MerchantEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_CREATIVE = SynchedEntityData.defineId(MerchantEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> CREATIVE_RESTORE = SynchedEntityData.defineId(MerchantEntity.class, EntityDataSerializers.BOOLEAN);
    private final Predicate<ItemEntity> ALLOWED_ITEMS = (item) ->
            (!item.hasPickUpDelay() && item.isAlive() && getInventory().canAddItem(item.getItem()) && this.wantsToPickUp(item.getItem()));
    public boolean needsNewTrades;
    public MerchantEntity(EntityType<? extends AbstractWorkerEntity> entityType, Level world) {
        super(entityType, world);
    }
    @Override
    public boolean shouldLoadChunk() {
        return false;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(TRADES, new CompoundTag());
        this.entityData.define(TRADER_PROGRESS, 0);
        this.entityData.define(TRADER_LEVEL, 1);
        this.entityData.define(IS_TRADING, false);
        this.entityData.define(IS_CREATIVE, false);
    }

    @Override
    public void tick() {
        super.tick();
        if(level().isClientSide()) return;

        if(tickCount % 20 == 0){
            if(this.getTarget() == null){
                this.switchMainHandItem(ItemStack::isEmpty);
                this.switchOffHandItem(ItemStack::isEmpty);
            }

        }
    }

    @Override
    public List<Item> inventoryInputHelp() {
        return null;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();

    }

    @Override
    public AbstractWorkAreaEntity getCurrentWorkArea() {
        return null;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.put("Trades", WorkersMerchantTrade.listToNbt(getTrades()));
        nbt.putInt("TraderProgress", this.getTraderProgress());
        nbt.putInt("TraderLevel", this.getTraderLevel());
        nbt.putBoolean("isCreative", this.isCreative());
        nbt.putBoolean("creativeRestore", this.shouldRestoreCreative());

    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.setTrades(WorkersMerchantTrade.listFromNbt(nbt.getCompound("Trades")));
        this.setTraderProgress(nbt.getInt("TraderProgress"));
        this.setTraderLevel(nbt.getInt("TraderLevel"));
        this.setCreative(nbt.getBoolean("isCreative"));
        this.setCreativeRestore(nbt.getBoolean("creativeRestore"));
    }

    //ATTRIBUTES
    public static AttributeSupplier.Builder setAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 50.0D)
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


    @Override
    public InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        if(this.isTrading()){
            return InteractionResult.PASS;
        }

        if (this.getCommandSenderWorld().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        else {
            if(this.isOwned()){
                if(!player.isCrouching()){
                    openTradeGUI(player);
                    return InteractionResult.SUCCESS;
                }
            }
            return super.mobInteract(player, hand);
        }
    }

    @Override
    public boolean canBeHired() {
        return true;
    }

    @Override
    public void initSpawn() {
        this.setCustomName(Component.literal("Merchant"));
        this.setCost(WorkersServerConfig.MerchantCost.get());

        this.setEquipment();

        this.setDropEquipment();
        this.setRandomSpawnBonus();
        this.setPersistenceRequired();

        this.setGroup(1);
        this.setFollowState(2);

        AbstractRecruitEntity.applySpawnValues(this);
    }

    @Override
    public boolean wantsToPickUp(ItemStack itemStack) {
        if((itemStack.getItem() instanceof SwordItem && this.getMainHandItem().isEmpty()) ||
                (itemStack.getItem() instanceof ShieldItem) && this.getOffhandItem().isEmpty())
            return !hasSameTypeOfItem(itemStack);

        else return super.wantsToPickUp(itemStack);
    }

    public Predicate<ItemEntity> getAllowedItems(){
        return ALLOWED_ITEMS;
    }

    @Override
    public boolean canHoldItem(ItemStack itemStack){
        return !(itemStack.getItem() instanceof CrossbowItem || itemStack.getItem() instanceof BowItem);
    }
    public void openTradeGUI(Player player){
        this.setTrading(true);

        if (player instanceof ServerPlayer) {
            NetworkHooks.openScreen((ServerPlayer)player, new MenuProvider() {
                public @NotNull Component getDisplayName() {
                    return MerchantEntity.this.getName();
                }

                public @NotNull AbstractContainerMenu createMenu(int i, @NotNull Inventory playerInventory, @NotNull Player playerEntity) {
                    return new MerchantTradeContainer(i, MerchantEntity.this, playerInventory);
                }
            }, (packetBuffer) -> {
                packetBuffer.writeUUID(this.getUUID());
            });
        } else {
            WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageOpenMerchantTradeScreen(player, this.getUUID()));
        }
    }

    public void openAddEditTradeGUI(Player player, WorkersMerchantTrade trade){
        if (player instanceof ServerPlayer) {
            this.setTrading(true);
            NetworkHooks.openScreen((ServerPlayer)player, new MenuProvider() {
                public @NotNull Component getDisplayName() {
                    return Component.literal("trade_edit_screen");
                }
                public @NotNull AbstractContainerMenu createMenu(int i, @NotNull Inventory playerInventory, @NotNull Player playerEntity) {
                    return new MerchantAddEditTradeContainer(i, MerchantEntity.this, playerInventory, trade);
                }
            }, (packetBuffer) -> {
                packetBuffer.writeUUID(this.getUUID());
                packetBuffer.writeNbt(trade.toNbt());
            });
        }
        else {
            WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageOpenMerchantEditTradeScreen(player, this.getUUID(), trade));
        }
    }

    @Override
    public void openGUI(Player player) {
        super.openGUI(player);
    }

    public int getTraderLevel(){
        return entityData.get(TRADER_LEVEL);
    }
    public int getTraderProgress() {
        return entityData.get(TRADER_PROGRESS);
    }

    public void setTraderProgress(int x) {
        this.entityData.set(TRADER_PROGRESS, x);
    }
    public void setTraderLevel(int x) {
        this.entityData.set(TRADER_LEVEL, x);
    }
    public void addTraderProgress(int x) {
        int current = this.getTraderProgress();
        if (x == 0) return;

        int newProgress = current + x;

        //LevelUp
        if(newProgress >= 100){
            newProgress -= 100;

            this.setTraderLevel(getTraderLevel() + 1);
            this.needsNewTrades = true;
        }

        this.setTraderProgress(newProgress);
    }

    public int getMaxAvailableTrades(){
        return this.getTraderLevel() * 3;
    }

    public List<WorkersMerchantTrade> getTrades() {
        return WorkersMerchantTrade.listFromNbt(this.entityData.get(TRADES));
    }

    public void setTrades(List<WorkersMerchantTrade> list) {
        this.entityData.set(TRADES, WorkersMerchantTrade.listToNbt(list));
    }

    public void addOrUpdateTrade(WorkersMerchantTrade trade) {
        if (trade == null) return;

        List<WorkersMerchantTrade> currentTrades = new ArrayList<>(getTrades());
        boolean updated = false;

        for (int i = 0; i < currentTrades.size(); i++) {
            WorkersMerchantTrade existing = currentTrades.get(i);
            if (existing.uuid.equals(trade.uuid)) {
                currentTrades.set(i, trade);
                updated = true;
                break;
            }
        }

        if (!updated) {
            currentTrades.add(trade);
        }

        setTrades(currentTrades);
    }

    public void removeTrade(WorkersMerchantTrade trade) {
        if (trade == null) return;
        List<WorkersMerchantTrade> currents = getTrades();
        boolean removed = currents.removeIf(current -> current.uuid.equals(trade.uuid));
        if (removed) setTrades(currents);
    }

    public void doTrade(UUID uuid, ServerPlayer player) {
        List<WorkersMerchantTrade> currents = getTrades();
        WorkersMerchantTrade trade = null;

        for (WorkersMerchantTrade trade1 : currents){
            if(trade1.uuid.equals(uuid)){
                trade = trade1;
                break;
            }
        }

        if(trade == null) return;

        Inventory playerInv = player.getInventory();
        SimpleContainer merchantInv = this.getInventory();// supply and money

        int playerEmeralds = 0;
        int merchantEmeralds = 0;
        int playerTradeItem = 0;
        int merchantTradeItem = 0;

        ItemStack currencyItem = trade.currencyItem;
        int price = currencyItem.getCount();

        ItemStack tradeItemStack = trade.tradeItem;
        int tradeCount = tradeItemStack.getCount();

        // checkPlayerMoney
        for (int i = 0; i < playerInv.getContainerSize(); i++) {
            ItemStack itemStackInSlot = playerInv.getItem(i);

            if (areItemStacksEqual(itemStackInSlot, currencyItem)) {
                playerEmeralds = playerEmeralds + itemStackInSlot.getCount();
            }
        }

        // checkMerchantMoney
        for (int i = 0; i < merchantInv.getContainerSize(); i++) {
            ItemStack itemStackInSlot = merchantInv.getItem(i);

            if (areItemStacksEqual(itemStackInSlot, currencyItem)) {
                merchantEmeralds = merchantEmeralds + itemStackInSlot.getCount();
            }
        }

        // checkPlayerTradeGood
        for (int i = 0; i < playerInv.getContainerSize(); i++) {
            ItemStack itemStackInSlot = playerInv.getItem(i);

            if (areItemStacksEqual(itemStackInSlot, tradeItemStack)) {
                playerTradeItem = playerTradeItem + itemStackInSlot.getCount();
            }
        }

        // checkMerchantTradeGood
        for (int i = 0; i < merchantInv.getContainerSize(); i++) {
            ItemStack itemStackInSlot = merchantInv.getItem(i);

            if (areItemStacksEqual(itemStackInSlot, tradeItemStack)) {
                merchantTradeItem = merchantTradeItem + itemStackInSlot.getCount();
            }
        }

        boolean merchantHasItems = merchantTradeItem >= tradeCount;
        boolean playerCanPay = playerEmeralds >= price;
        boolean canAddItemToInv = merchantInv.canAddItem(currencyItem);

        if(!canAddItemToInv) {
            return;
        }

        if(!merchantHasItems && !this.isCreative()) {
            return;
        }

        if (!playerCanPay) {
            return;
        }

        merchantTradeItem = merchantTradeItem - tradeCount;

        // remove merchant tradeItem
        if(!this.isCreative()){
            for (int i = 0; i < merchantInv.getContainerSize(); i++) {
                ItemStack itemStackInSlot = merchantInv.getItem(i);

                if (areItemStacksEqual(itemStackInSlot, tradeItemStack)) {
                    merchantInv.removeItemNoUpdate(i);
                }
            }

            // add tradeGoodLeft to merchantInv
            ItemStack tradeGoodLeft = tradeItemStack.copy();
            addItemWithMaxStackCount(merchantInv, tradeGoodLeft, merchantTradeItem);
        }
        // add tradeItem to playerInventory
        ItemStack tradeGood = tradeItemStack.copy();
        addItemWithMaxStackCount(playerInv, tradeGood, tradeCount);

        // give player tradeGood
        // remove playerEmeralds ->add left
        //
        playerEmeralds = playerEmeralds - price;

        // merchantEmeralds = merchantEmeralds + price;

        // remove playerEmeralds
        for (int i = 0; i < playerInv.getContainerSize(); i++) {
            ItemStack itemStackInSlot = playerInv.getItem(i);

            if (areItemStacksEqual(itemStackInSlot, currencyItem)) {
                playerInv.removeItemNoUpdate(i);
            }
        }

        // add emeralds to merchantInventory
        if(!this.isCreative()) {
            ItemStack emeraldsKar = currencyItem.copy();
            addItemWithMaxStackCount(merchantInv, emeraldsKar, price);
        }


        // add leftEmeralds to playerInventory
        ItemStack emeraldsLeft = currencyItem.copy();
        addItemWithMaxStackCount(playerInv, emeraldsLeft, playerEmeralds);

        trade.currentTrades++;
        this.setTrades(currents);
    }

    private static void addItemWithMaxStackCount(SimpleContainer merchantInv, ItemStack stack, int count) {
        int maxStackCount = stack.getMaxStackSize();

        while (count > 0) {

            int currentStackCount = Math.min(maxStackCount, count);

            ItemStack newStack = stack.copy();
            newStack.setCount(currentStackCount);

            merchantInv.addItem(newStack);

            count -= currentStackCount;
        }
    }

    private static void addItemWithMaxStackCount(Inventory inv, ItemStack stack, int count) {
        int maxStackCount = stack.getMaxStackSize();

        while (count > 0) {

            int currentStackCount = Math.min(maxStackCount, count);

            ItemStack newStack = stack.copy();
            newStack.setCount(currentStackCount);

            inv.add(newStack);

            count -= currentStackCount;
        }
    }

    public static boolean areItemStacksEqual(ItemStack a, ItemStack b) {
        if (a.isEmpty() && b.isEmpty()) return true;
        if (a.isEmpty() || b.isEmpty()) return false;
        return ItemStack.isSameItemSameTags(a, b);
    }

    public void setTrading(boolean trading) {
        this.entityData.set(IS_TRADING, trading);
    }

    public boolean isTrading(){
        return this.entityData.get(IS_TRADING);
    }

    public void setCreative(boolean creative) {
        this.entityData.set(IS_CREATIVE, creative);
    }

    public boolean isCreative(){
        return this.entityData.get(IS_CREATIVE);
    }

    public void setCreativeRestore(boolean restore) {
        this.entityData.set(CREATIVE_RESTORE, restore);
    }

    public boolean shouldRestoreCreative(){
        return this.entityData.get(CREATIVE_RESTORE) && isCreative();
    }

    @Override
    public boolean hurt(@NotNull DamageSource dmg, float amt) {
        if(!this.isCreative()){
            return super.hurt(dmg, amt);
        }
        else return false;
    }

}
