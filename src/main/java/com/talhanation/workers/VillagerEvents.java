package com.talhanation.workers;

import com.talhanation.workers.config.WorkersModConfig;
import com.talhanation.workers.entities.AbstractWorkerEntity;
import com.talhanation.workers.entities.ai.horse.HorseRiddenByMerchantGoal;
import com.talhanation.workers.init.ModBlocks;
import com.talhanation.workers.init.ModEntityTypes;
import com.talhanation.workers.init.ModProfessions;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.npc.VillagerTrades.ItemListing;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.util.HashMap;
import java.util.List;

public class VillagerEvents {


    @SubscribeEvent
    public void attackWorkers(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();

        if (WorkersModConfig.PillagerAttackWorkers.get() && entity instanceof AbstractIllager illager) {
            illager.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(illager, AbstractWorkerEntity.class, true));
        }

        if (WorkersModConfig.MonsterAttackWorkers.get() && entity instanceof Monster) {
            Monster monster = (Monster) entity;
            if (!(monster instanceof Creeper))
                monster.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(monster, AbstractWorkerEntity.class, true));
        }

    }
    @SubscribeEvent
    public void onVillagerLivingUpdate(LivingTickEvent event) {
        HashMap<VillagerProfession, EntityType<? extends AbstractWorkerEntity>> entitiesByProfession = new HashMap<>(){{

            put(ModProfessions.MINER.get(), ModEntityTypes.MINER.get());
            put(ModProfessions.LUMBERJACK.get(), ModEntityTypes.LUMBERJACK.get());
            put(ModProfessions.FISHER.get(), ModEntityTypes.FISHERMAN.get());
            put(ModProfessions.SHEPHERD.get(), ModEntityTypes.SHEPHERD.get());
            put(ModProfessions.FARMER.get(), ModEntityTypes.FARMER.get());
            put(ModProfessions.MERCHANT.get(), ModEntityTypes.MERCHANT.get());
            put(ModProfessions.CHICKEN_FARMER.get(), ModEntityTypes.CHICKEN_FARMER.get());
            put(ModProfessions.CATTLE_FARMER.get(), ModEntityTypes.CATTLE_FARMER.get());
            put(ModProfessions.SWINEHERD.get(), ModEntityTypes.SWINEHERD.get());
        }};

        Entity entity = event.getEntity();
        if (entity instanceof Villager villager) {
            VillagerProfession profession = villager.getVillagerData().getProfession();
            
            if (entitiesByProfession.containsKey(profession)) {
                EntityType<? extends AbstractWorkerEntity> workerType = entitiesByProfession.get(profession);
                createWorker(villager, workerType);
            }
        }
    }


    private void createWorker(Villager villager, EntityType<? extends AbstractWorkerEntity> workerType) {
        AbstractWorkerEntity worker = workerType.create(villager.getCommandSenderWorld());
        if (worker != null) {
            worker.copyPosition(villager);
            worker.initSpawn();

            if(WorkersModConfig.WorkersTablesPOIReleasing.get()) villager.releasePoi(MemoryModuleType.JOB_SITE);
            villager.releasePoi(MemoryModuleType.HOME);
            villager.releasePoi(MemoryModuleType.MEETING_POINT);

            villager.discard();
            villager.getCommandSenderWorld().addFreshEntity(worker);
        }
    }

    @SubscribeEvent
    public void onHorseJoinWorld(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();

        if (entity instanceof AbstractHorse horse) {
            horse.goalSelector.addGoal(0, new HorseRiddenByMerchantGoal(horse));
        }
    }

    @SubscribeEvent
    public void WanderingVillagerTrades(VillagerTradesEvent event) {

    }

    @SubscribeEvent
    public void villagerTrades(VillagerTradesEvent event) {

        if (event.getType() == VillagerProfession.MASON) {
            VillagerTrades.ItemListing block_trade = new Trade(Items.EMERALD, 30, ModBlocks.MINER_BLOCK.get(), 1, 4,
                    20);
            List<ItemListing> list = event.getTrades().get(2);
            list.add(block_trade);
            event.getTrades().put(2, list);
        }
        if (event.getType() == VillagerProfession.FARMER) {
            VillagerTrades.ItemListing block_trade = new Trade(Items.EMERALD, 23, ModBlocks.LUMBERJACK_BLOCK.get(), 1,
                    4, 20);
            List<ItemListing> list = event.getTrades().get(2);
            list.add(block_trade);
            event.getTrades().put(2, list);
        }

        if (event.getType() == VillagerProfession.FISHERMAN) {
            VillagerTrades.ItemListing block_trade = new Trade(Items.EMERALD, 32, ModBlocks.FISHER_BLOCK.get(), 1, 4,
                    20);
            List<ItemListing> list = event.getTrades().get(2);
            list.add(block_trade);
            event.getTrades().put(2, list);
        }

        if (event.getType() == VillagerProfession.BUTCHER) {
            VillagerTrades.ItemListing block_trade = new Trade(Items.EMERALD, 35, ModBlocks.SHEPHERD_BLOCK.get(), 1, 4,
                    20);
            List<ItemListing> list = event.getTrades().get(2);
            list.add(block_trade);
            event.getTrades().put(2, list);
        }

        if (event.getType() == VillagerProfession.SHEPHERD) {
            VillagerTrades.ItemListing block_trade = new Trade(Items.EMERALD, 25, ModBlocks.SHEPHERD_BLOCK.get(), 1, 4,
                    20);
            List<ItemListing> list = event.getTrades().get(2);
            list.add(block_trade);
            event.getTrades().put(2, list);
        }

        if (event.getType() == VillagerProfession.LIBRARIAN) {
            VillagerTrades.ItemListing block_trade = new Trade(Items.EMERALD, 45, ModBlocks.MERCHANT_BLOCK.get(), 1, 4,
                    20);
            List<ItemListing> list = event.getTrades().get(2);
            list.add(block_trade);
            event.getTrades().put(2, list);
        }

        if (event.getType() == VillagerProfession.FARMER) {
            VillagerTrades.ItemListing block_trade = new Trade(Items.EMERALD, 28, ModBlocks.FARMER_BLOCK.get(), 1, 4,
                    20);
            List<ItemListing> list = event.getTrades().get(2);
            list.add(block_trade);
            event.getTrades().put(2, list);
        }

        if (event.getType() == VillagerProfession.BUTCHER) {
            VillagerTrades.ItemListing block_trade = new Trade(Items.EMERALD, 40, ModBlocks.CATTLE_FARMER_BLOCK.get(),
                    1, 4, 20);
            List<ItemListing> list = event.getTrades().get(2);
            list.add(block_trade);
            event.getTrades().put(2, list);
        }

        if (event.getType() == VillagerProfession.BUTCHER) {
            VillagerTrades.ItemListing block_trade = new Trade(Items.EMERALD, 32, ModBlocks.CHICKEN_FARMER_BLOCK.get(),
                    1, 4, 20);
            List<ItemListing> list = event.getTrades().get(2);
            list.add(block_trade);
            event.getTrades().put(2, list);
        }

        if (event.getType() == VillagerProfession.BUTCHER) {
            VillagerTrades.ItemListing block_trade = new Trade(Items.EMERALD, 38, ModBlocks.SWINEHERD_BLOCK.get(), 1, 4,
                    20);
            List<ItemListing> list = event.getTrades().get(2);
            list.add(block_trade);
            event.getTrades().put(2, list);
        }

    }

    static class EmeraldForItemsTrade extends Trade {
        public EmeraldForItemsTrade(ItemLike buyingItem, int buyingAmount, int maxUses, int givenExp) {
            super(buyingItem, buyingAmount, Items.EMERALD, 1, maxUses, givenExp);
        }
    }

    static class Trade implements VillagerTrades.ItemListing {
        private final Item buyingItem;
        private final Item sellingItem;
        private final int buyingAmount;
        private final int sellingAmount;
        private final int maxUses;
        private final int givenExp;
        private final float priceMultiplier;

        public Trade(ItemLike buyingItem, int buyingAmount, ItemLike sellingItem, int sellingAmount, int maxUses,
                int givenExp) {
            this.buyingItem = buyingItem.asItem();
            this.buyingAmount = buyingAmount;
            this.sellingItem = sellingItem.asItem();
            this.sellingAmount = sellingAmount;
            this.maxUses = maxUses;
            this.givenExp = givenExp;
            this.priceMultiplier = 0.05F;
        }

        public MerchantOffer getOffer(Entity entity, RandomSource random) {
            return new MerchantOffer(new ItemStack(this.buyingItem, this.buyingAmount),
                    new ItemStack(sellingItem, sellingAmount), maxUses, givenExp, priceMultiplier);
        }
    }

    static class ItemsForEmeraldsTrade implements VillagerTrades.ItemListing {
        private final ItemStack itemStack;
        private final int emeraldCost;
        private final int numberOfItems;
        private final int maxUses;
        private final int villagerXp;
        private final float priceMultiplier;

        public ItemsForEmeraldsTrade(Block p_i50528_1_, int p_i50528_2_, int p_i50528_3_, int p_i50528_4_,
                int p_i50528_5_) {
            this(new ItemStack(p_i50528_1_), p_i50528_2_, p_i50528_3_, p_i50528_4_, p_i50528_5_);
        }

        public ItemsForEmeraldsTrade(Item p_i50529_1_, int p_i50529_2_, int p_i50529_3_, int p_i50529_4_) {
            this(new ItemStack(p_i50529_1_), p_i50529_2_, p_i50529_3_, 12, p_i50529_4_);
        }

        public ItemsForEmeraldsTrade(Item p_i50530_1_, int p_i50530_2_, int p_i50530_3_, int p_i50530_4_,
                int p_i50530_5_) {
            this(new ItemStack(p_i50530_1_), p_i50530_2_, p_i50530_3_, p_i50530_4_, p_i50530_5_);
        }

        public ItemsForEmeraldsTrade(ItemStack p_i50531_1_, int p_i50531_2_, int p_i50531_3_, int p_i50531_4_,
                int p_i50531_5_) {
            this(p_i50531_1_, p_i50531_2_, p_i50531_3_, p_i50531_4_, p_i50531_5_, 0.05F);
        }

        public ItemsForEmeraldsTrade(ItemStack p_i50532_1_, int p_i50532_2_, int p_i50532_3_, int p_i50532_4_,
                int p_i50532_5_, float p_i50532_6_) {
            this.itemStack = p_i50532_1_;
            this.emeraldCost = p_i50532_2_;
            this.numberOfItems = p_i50532_3_;
            this.maxUses = p_i50532_4_;
            this.villagerXp = p_i50532_5_;
            this.priceMultiplier = p_i50532_6_;
        }

        public MerchantOffer getOffer(Entity p_221182_1_, RandomSource p_221182_2_) {
            return new MerchantOffer(new ItemStack(Items.EMERALD, this.emeraldCost),
                    new ItemStack(this.itemStack.getItem(), this.numberOfItems), this.maxUses, this.villagerXp,
                    this.priceMultiplier);
        }
    }

}
