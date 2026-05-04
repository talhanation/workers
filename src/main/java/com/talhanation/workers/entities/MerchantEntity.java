package com.talhanation.workers.entities;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.ICanTradeEmbargo;
import com.talhanation.recruits.pathfinding.AsyncGroundPathNavigation;
import com.talhanation.workers.WorkersMain;
import com.talhanation.workers.config.WorkersServerConfig;
import com.talhanation.workers.entities.ai.MerchantWorkGoal;
import com.talhanation.workers.entities.workarea.AbstractWorkAreaEntity;
import com.talhanation.workers.entities.workarea.MarketArea;
import com.talhanation.workers.inventory.MerchantAddEditTradeContainer;
import com.talhanation.workers.inventory.MerchantTradeContainer;
import com.talhanation.workers.network.MessageOpenMerchantEditTradeScreen;
import com.talhanation.workers.network.MessageOpenMerchantTradeScreen;
import com.talhanation.workers.world.VillagerInviteRegistry;
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
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.item.trading.MerchantOffer;
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

public class MerchantEntity extends AbstractWorkerEntity implements ICanTradeEmbargo {
    private static final EntityDataAccessor<CompoundTag> TRADES = SynchedEntityData.defineId(MerchantEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<Integer> TRADER_PROGRESS = SynchedEntityData.defineId(MerchantEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TRADER_LEVEL = SynchedEntityData.defineId(MerchantEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> IS_TRADING = SynchedEntityData.defineId(MerchantEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_CREATIVE = SynchedEntityData.defineId(MerchantEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> MARKET_NAME = SynchedEntityData.defineId(MerchantEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DAILY_REFRESH = SynchedEntityData.defineId(MerchantEntity.class, EntityDataSerializers.BOOLEAN);

    private boolean lastWasDawn = false;

    private final Predicate<ItemEntity> ALLOWED_ITEMS = (item) ->
            (!item.hasPickUpDelay() && item.isAlive() && getInventory().canAddItem(item.getItem()) && this.wantsToPickUp(item.getItem()));

    public boolean needsNewTrades;
    public MarketArea currentMarketArea;

    @Nullable public Villager activeTradingVillager;
    @Nullable public WorkersMerchantTrade activeVillagerTrade;
    @Nullable public MerchantOffer activeVillagerOffer;
    public int villagerTradeTimeout;

    public MerchantEntity(EntityType<? extends AbstractWorkerEntity> entityType, Level world) {
        super(entityType, world);
    }

    @Override
    public boolean shouldLoadChunk() {
        return true;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(TRADES, new CompoundTag());
        this.entityData.define(TRADER_PROGRESS, 0);
        this.entityData.define(TRADER_LEVEL, 1);
        this.entityData.define(IS_TRADING, false);
        this.entityData.define(IS_CREATIVE, false);
        this.entityData.define(MARKET_NAME, "");
        this.entityData.define(DAILY_REFRESH, false);
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

            if (this.isCreative() && this.isDailyRefresh()) {
                long timeOfDay = this.level().getDayTime() % 24000L;
                boolean isDawn = timeOfDay < 2000L;
                if (isDawn && !lastWasDawn) {
                    refreshAllTrades();
                }
                lastWasDawn = isDawn;
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
        this.goalSelector.addGoal(3, new MerchantWorkGoal(this));
    }

    @Override
    public AbstractWorkAreaEntity getCurrentWorkArea() { return currentMarketArea; }


    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.put("Trades", WorkersMerchantTrade.listToNbt(getTrades()));
        nbt.putInt("TraderProgress", this.getTraderProgress());
        nbt.putInt("TraderLevel", this.getTraderLevel());
        nbt.putBoolean("isCreative", this.isCreative());
        nbt.putBoolean("dailyRefresh", this.isDailyRefresh());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.setTrades(WorkersMerchantTrade.listFromNbt(nbt.getCompound("Trades")));
        this.setTraderProgress(nbt.getInt("TraderProgress"));
        this.setTraderLevel(nbt.getInt("TraderLevel"));
        this.setCreative(nbt.getBoolean("isCreative"));
        this.setDailyRefresh(nbt.getBoolean("dailyRefresh"));
    }

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
        ((AsyncGroundPathNavigation) this.getNavigation()).setCanOpenDoors(true);
        this.populateDefaultEquipmentEnchantments(randomsource, difficultyInstance);
        this.initSpawn();
        return ilivingentitydata;
    }


    @Override
    public InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        if (this.isTrading()) return InteractionResult.PASS;
        if (this.getCommandSenderWorld().isClientSide()) return InteractionResult.SUCCESS;
        if (!player.isCrouching()) {
            openTradeGUI(player);
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean canBeHired() {
        return !isCreative();
    }

    @Override
    public void initSpawn() {
        this.setCustomName(Component.literal("Merchant"));
        this.setCost(WorkersServerConfig.MerchantCost.get());
        this.setEquipment();
        this.setDropEquipment();
        this.setRandomSpawnBonus();
        this.setPersistenceRequired();
        this.setFollowState(2);
        AbstractRecruitEntity.applySpawnValues(this);
    }

    @Override
    public boolean wantsToPickUp(ItemStack itemStack) {
        if ((itemStack.getItem() instanceof SwordItem && this.getMainHandItem().isEmpty()) ||
                (itemStack.getItem() instanceof ShieldItem) && this.getOffhandItem().isEmpty())
            return !hasSameTypeOfItem(itemStack);
        return super.wantsToPickUp(itemStack);
    }

    public Predicate<ItemEntity> getAllowedItems() {
        return ALLOWED_ITEMS;
    }

    @Override
    public boolean canHoldItem(ItemStack itemStack) {
        return !(itemStack.getItem() instanceof CrossbowItem || itemStack.getItem() instanceof BowItem);
    }


    public void openTradeGUI(Player player) {
        this.setTrading(true);
        if (player instanceof ServerPlayer sp) {
            NetworkHooks.openScreen(sp, new MenuProvider() {
                public @NotNull Component getDisplayName() { return MerchantEntity.this.getName(); }
                public @NotNull AbstractContainerMenu createMenu(int i, @NotNull Inventory inv, @NotNull Player p) {
                    return new MerchantTradeContainer(i, MerchantEntity.this, inv);
                }
            }, buf -> buf.writeUUID(this.getUUID()));
        } else {
            WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageOpenMerchantTradeScreen(player, this.getUUID()));
        }
    }

    public void openAddEditTradeGUI(Player player, WorkersMerchantTrade trade) {
        if (player instanceof ServerPlayer sp) {
            this.setTrading(true);
            NetworkHooks.openScreen(sp, new MenuProvider() {
                public @NotNull Component getDisplayName() { return Component.literal("trade_edit_screen"); }
                public @NotNull AbstractContainerMenu createMenu(int i, @NotNull Inventory inv, @NotNull Player p) {
                    return new MerchantAddEditTradeContainer(i, MerchantEntity.this, inv, trade);
                }
            }, buf -> { buf.writeUUID(this.getUUID()); buf.writeNbt(trade.toNbt()); });
        } else {
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
    public void setTraderProgress(int x){
        this.entityData.set(TRADER_PROGRESS, x);
    }
    public void setTraderLevel(int x) {
        this.entityData.set(TRADER_LEVEL, x);
    }

    public void addTraderProgress(int x) {
        if (x == 0) return;
        int newProgress = getTraderProgress() + x;
        if (newProgress >= 100) {
            newProgress -= 100;
            setTraderLevel(getTraderLevel() + 1);
            needsNewTrades = true;
        }
        setTraderProgress(newProgress);
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
        if(!trade.enabled) return;

        Inventory playerInv = player.getInventory();

        int playerEmeralds = 0;
        int merchantEmeralds = 0;
        int playerTradeItem = 0;
        int merchantTradeItemCount = 0;

        ItemStack currencyItem = trade.currencyItem;
        int price = currencyItem.getCount();
        ItemStack tradeItemStack = trade.tradeItem;
        int tradeCount = tradeItemStack.getCount();

        // checkPlayerMoney
        for (int i = 0; i < playerInv.getContainerSize(); i++) {
            ItemStack itemStackInSlot = playerInv.getItem(i);

            if (areItemStacksEqual(itemStackInSlot, currencyItem, trade.allowDamagedCurrency)) {
                playerEmeralds = playerEmeralds + itemStackInSlot.getCount();
            }
        }

        // checkMerchantMoney
        merchantEmeralds = this.countMerchantItemStack(currencyItem, trade.allowDamagedCurrency);

        // checkPlayerTradeGood
        for (int i = 0; i < playerInv.getContainerSize(); i++) {
            ItemStack itemStackInSlot = playerInv.getItem(i);

            if (areItemStacksEqual(itemStackInSlot, tradeItemStack, trade.allowDamagedCurrency)) {
                playerTradeItem = playerTradeItem + itemStackInSlot.getCount();
            }
        }

        // checkMerchantTradeGood
        merchantTradeItemCount = this.countMerchantItemStack(tradeItemStack, trade.allowDamagedCurrency);


        boolean merchantHasItems = merchantTradeItemCount >= tradeCount;
        boolean playerCanPay = playerEmeralds >= price;
        boolean canAddItemToInv = canAddItemToMerchant(currencyItem);

        if(!canAddItemToInv) {
            player.sendSystemMessage(TEXT_NO_SPACE());
            if(this.getOwner() != null) this.getOwner().sendSystemMessage(TEXT_NO_SPACE_OWNER());
            return;
        }

        if(!merchantHasItems && !this.isCreative()) {
            player.sendSystemMessage(TEXT_NO_ITEM_LEFT(tradeItemStack.getItem().toString()));
            if(this.getOwner() != null) this.getOwner().sendSystemMessage(TEXT_NO_ITEM_LEFT_OWNER(tradeItemStack.getItem().toString()));
            return;
        }

        if (!playerCanPay) {
            player.sendSystemMessage(TEXT_NOT_ENOUGH_ITEMS(currencyItem.getItem().toString()));
            return;
        }

        if(playerInv.getFreeSlot() == -1){
            player.sendSystemMessage(TEXT_CUSTOMER_NO_SPACE());
            return;
        }

        if(trade.maxTrades != -1 && trade.currentTrades +1 >= trade.maxTrades){
            player.sendSystemMessage(TEXT_LIMIT_REACHED());
            if(this.getOwner() != null) this.getOwner().sendSystemMessage(TEXT_LIMIT_REACHED_OWNER(tradeItemStack.getItem().toString(), currencyItem.getItem().toString()));
        }

        // shrink merchant tradeItem
        if(!this.isCreative()){
            shrinkMerchantItemStack(tradeItemStack, tradeCount, trade.allowDamagedCurrency);
        }

        int toRemove = price;
        for (int i = 0; i < playerInv.getContainerSize() && toRemove > 0; i++) {
            ItemStack itemStackInSlot = playerInv.getItem(i);
            if (areItemStacksEqual(itemStackInSlot, currencyItem, trade.allowDamagedCurrency)) {
                int amount = Math.min(toRemove, itemStackInSlot.getCount());


                if(!this.isCreative()){
                    addItemToMerchant(itemStackInSlot, amount);
                }
                itemStackInSlot.shrink(amount);

                toRemove -= amount;
            }
        }

        ItemStack tradeGood = tradeItemStack.copy();
        addItemWithMaxStackCount(playerInv, tradeGood, tradeCount);

        trade.currentTrades++;
        this.setTrades(currents);
    }

    public boolean tryExecuteVillagerTrade(WorkersMerchantTrade trade, ItemStack costItem, ItemStack reward) {
        if(countMerchantItemStack(trade.tradeItem, false) < costItem.getCount()) return false;
        if(!canAddItemToMerchant(reward)) return false;

        shrinkMerchantItemStack(trade.tradeItem, costItem.getCount(), false);
        addItemToMerchant(reward, reward.getCount());

        trade.currentTrades++;
        List<WorkersMerchantTrade> list = getTrades();
        setTrades(list);

        return true;
    }

    public void clearVillagerTrade() {
        if(this.activeTradingVillager != null) {
            VillagerInviteRegistry.release(this.activeTradingVillager.getUUID());
        }
        this.activeTradingVillager = null;
        this.activeVillagerTrade = null;
        this.activeVillagerOffer = null;
        this.villagerTradeTimeout = 0;
    }


    public boolean canAddItemToMerchant(ItemStack itemStack){
        boolean can;
        if(this.currentMarketArea != null) {
            can = this.currentMarketArea.canAddItem(itemStack);
        }
        else{
            can = this.getInventory().canAddItem(itemStack);
        }

        return can;
    }

    public void shrinkMerchantItemStack(ItemStack itemStack, int amount, boolean allowDamagedCurrency) {
        if(this.currentMarketArea != null) {
            this.currentMarketArea.shrinkItemFromContainers(itemStack, amount, allowDamagedCurrency);
        }
        else{
            int toRemove = amount;
            for (int i = 0; i < this.getInventory().getContainerSize() && toRemove > 0; i++) {
                ItemStack itemStackInSlot = this.getInventory().getItem(i);
                if (areItemStacksEqual(itemStackInSlot, itemStack, allowDamagedCurrency)) {
                    int removeCount = Math.min(toRemove, itemStackInSlot.getCount());
                    itemStackInSlot.shrink(removeCount);
                    toRemove -= removeCount;
                }
            }
        }
    }

    public int countMerchantItemStack(ItemStack itemStack, boolean allowDamaged) {
        int x = -1;
        if(this.currentMarketArea != null) {
            x = this.currentMarketArea.countItemInContainers(itemStack, allowDamaged);
        }
        else{
            for (int i = 0; i < this.getInventory().getContainerSize(); i++) {
                ItemStack itemStackInSlot = this.getInventory().getItem(i);

                if (areItemStacksEqual(itemStackInSlot, itemStack, allowDamaged)) {
                    x = x + itemStackInSlot.getCount();
                }
            }
        }

        return x;
    }

    public void addItemToMerchant(ItemStack itemStack, int amount) {
        if(this.currentMarketArea != null) {
            this.currentMarketArea.depositItemToContainers(itemStack, amount);
        }
        else{
            addItemWithMaxStackCount(this.getInventory(), itemStack, amount);
        }
    }

    private static void addItemWithMaxStackCount(SimpleContainer inv, ItemStack stack, int count) {
        int maxStackCount = stack.getMaxStackSize();

        while (count > 0) {

            int currentStackCount = Math.min(maxStackCount, count);

            ItemStack newStack = stack.copy();
            newStack.setCount(currentStackCount);

            inv.addItem(newStack);

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

    public static boolean areItemStacksEqual(ItemStack a, ItemStack b, boolean allowDamagedCurrency) {
        if (a.isEmpty() && b.isEmpty()) return true;
        if (a.isEmpty() || b.isEmpty()) return false;

        if (allowDamagedCurrency) {
            return a.getItem() == b.getItem();
        }
        else{
            return ItemStack.isSameItemSameTags(a, b);
        }
    }


    public void moveTradeUp(UUID tradeUuid) {
        List<WorkersMerchantTrade> list = new ArrayList<>(getTrades());
        for (int i = 1; i < list.size(); i++) {
            if (list.get(i).uuid.equals(tradeUuid)) {
                WorkersMerchantTrade tmp = list.get(i - 1);
                list.set(i - 1, list.get(i));
                list.set(i, tmp);
                setTrades(list);
                return;
            }
        }
    }

    public void moveTradeDown(UUID tradeUuid) {
        List<WorkersMerchantTrade> list = new ArrayList<>(getTrades());
        for (int i = 0; i < list.size() - 1; i++) {
            if (list.get(i).uuid.equals(tradeUuid)) {
                WorkersMerchantTrade tmp = list.get(i + 1);
                list.set(i + 1, list.get(i));
                list.set(i, tmp);
                setTrades(list);
                return;
            }
        }
    }

    // ── Synced data getters / setters ─────────────────────────────────────────

    public void setTrading(boolean b){
        this.entityData.set(IS_TRADING, b);
    }
    public boolean isTrading(){
        return this.entityData.get(IS_TRADING);
    }
    public void setCreative(boolean b){
        this.entityData.set(IS_CREATIVE, b);
    }
    public boolean isCreative(){
        return this.entityData.get(IS_CREATIVE);
    }
    public void setCurrentMarketName(String n){
        this.entityData.set(MARKET_NAME, n);
    }
    public String getCurrentMarketName(){
        return this.entityData.get(MARKET_NAME);
    }
    public void setDailyRefresh(boolean b){
        this.entityData.set(DAILY_REFRESH, b);
    }
    public boolean isDailyRefresh(){
        return this.entityData.get(DAILY_REFRESH);
    }

    public void refreshAllTrades() {
        List<WorkersMerchantTrade> list = getTrades();
        list.forEach(t -> t.currentTrades = 0);
        setTrades(list);
    }

    @Override
    public boolean hurt(@NotNull DamageSource dmg, float amt) {
        if(!this.isCreative()){
            return super.hurt(dmg, amt);
        }
        else return false;
    }

    public Component TEXT_NO_ITEM_LEFT(String s){
        return Component.translatable("chat.workers.text.merchantNoItemsLeft", this.getName().getString(), s);
    }

    public Component TEXT_NO_ITEM_LEFT_OWNER(String s){
        return Component.translatable("chat.workers.text.merchantNoItemsLeftOwner", this.getName().getString(), s);
    }

    public Component TEXT_LIMIT_REACHED_OWNER(String currency, String goods){
        return Component.translatable("chat.workers.text.merchantLimitReachedOwner", this.getName().getString(), currency, goods);
    }

    public Component TEXT_LIMIT_REACHED(){
        return Component.translatable("chat.workers.text.merchantLimitReached", this.getName().getString());
    }

    public Component TEXT_NOT_ENOUGH_ITEMS(String s){
        return Component.translatable("chat.workers.text.merchantNotEnoughItems", this.getName().getString(), s);
    }

    public Component TEXT_CUSTOMER_NO_SPACE(){
        return Component.translatable("chat.workers.text.merchantCustomerNoSpace", this.getName().getString());
    }

    public Component TEXT_NO_SPACE(){
        return Component.translatable("chat.workers.text.merchantNoSpace", this.getName().getString());
    }

    public Component TEXT_NO_SPACE_OWNER(){
        return Component.translatable("chat.workers.text.merchantNoSpaceOwner", this.getName().getString());
    }

    @Override
    public String getEmbargoTeamID() {
        return this.getTeam() != null ? this.getTeam().getName() : "";
    }
}
