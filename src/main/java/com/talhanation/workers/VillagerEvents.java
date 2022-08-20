package com.talhanation.workers;

import com.talhanation.workers.entities.*;
import com.talhanation.workers.init.ModBlocks;
import com.talhanation.workers.init.ModEntityTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;
import java.util.Random;

public class VillagerEvents {

    @SubscribeEvent
    public void onVillagerLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        Entity entity = event.getEntityLiving();
        if (entity instanceof Villager) {
            Villager villager = (Villager) entity;
            VillagerProfession profession = villager.getVillagerData().getProfession();

            if (profession == Main.MINER) {
                createMiner(villager);
            }

            if (profession == Main.LUMBERJACK) {
                createLumber(villager);
            }

            if (profession == Main.FISHER) {
                createFisher(villager);
            }

            if (profession == Main.SHEPHERD) {
                createShepherd(villager);
            }

            if (profession == Main.FARMER) {
                createFarmer(villager);
            }

            if (profession == Main.MERCHANT) {
                createMerchant(villager);
            }

        }

    }
    private static void createMiner(LivingEntity entity){
        MinerEntity miner = ModEntityTypes.MINER.get().create(entity.level);
        Villager villager = (Villager) entity;
        miner.copyPosition(villager);

        miner.initSpawn();

        villager.remove(Entity.RemovalReason.DISCARDED);
        villager.level.addFreshEntity(miner);
    }

    private static void createLumber(LivingEntity entity){
        LumberjackEntity lumberjack = ModEntityTypes.LUMBERJACK.get().create(entity.level);
        Villager villager = (Villager) entity;
        lumberjack.copyPosition(villager);

        lumberjack.initSpawn();

        villager.remove(Entity.RemovalReason.DISCARDED);
        villager.level.addFreshEntity(lumberjack);
    }

    private static void createFisher(LivingEntity entity){
        FishermanEntity fisher = ModEntityTypes.FISHERMAN.get().create(entity.level);
        Villager villager = (Villager) entity;
        fisher.copyPosition(villager);

        fisher.initSpawn();

        villager.remove(Entity.RemovalReason.DISCARDED);
        villager.level.addFreshEntity(fisher);
    }

    private static void createMerchant(LivingEntity entity){
        MerchantEntity merchant = ModEntityTypes.MERCHANT.get().create(entity.level);
        Villager villager = (Villager) entity;
        merchant.copyPosition(villager);

        merchant.initSpawn();

        villager.remove(Entity.RemovalReason.DISCARDED);
        villager.level.addFreshEntity(merchant);
    }

    private static void createShepherd(LivingEntity entity){
        ShepherdEntity shepherd = ModEntityTypes.SHEPHERD.get().create(entity.level);
        Villager villager = (Villager) entity;
        shepherd.copyPosition(villager);

        shepherd.initSpawn();

        villager.remove(Entity.RemovalReason.DISCARDED);
        villager.level.addFreshEntity(shepherd);
    }

    private static void createFarmer(LivingEntity entity){
        FarmerEntity farmer = ModEntityTypes.FARMER.get().create(entity.level);
        Villager villager = (Villager) entity;
        farmer.copyPosition(villager);

        farmer.initSpawn();

        villager.remove(Entity.RemovalReason.DISCARDED);
        villager.level.addFreshEntity(farmer);
    }

    @SubscribeEvent
    public void WanderingVillagerTrades(VillagerTradesEvent event) {

    }

    @SubscribeEvent
    public void villagerTrades(VillagerTradesEvent event) {

        if (event.getType() == VillagerProfession.MASON) {
            VillagerTrades.ItemListing block_trade = new Trade(Items.EMERALD, 30, ModBlocks.MINER_BLOCK.get(), 1, 4, 20);
            List list = event.getTrades().get(2);
            list.add(block_trade);
            event.getTrades().put(2, list);
        }
        if (event.getType() == VillagerProfession.FARMER) {
            VillagerTrades.ItemListing block_trade = new Trade(Items.EMERALD, 15, ModBlocks.LUMBERJACK_BLOCK.get(), 1, 4, 20);
            List list = event.getTrades().get(2);
            list.add(block_trade);
            event.getTrades().put(2, list);
        }

        if (event.getType() == VillagerProfession.FISHERMAN) {
            VillagerTrades.ItemListing block_trade = new Trade(Items.EMERALD, 25, ModBlocks.FISHER_BLOCK.get(), 1, 4, 20);
            List list = event.getTrades().get(2);
            list.add(block_trade);
            event.getTrades().put(2, list);
        }

        if (event.getType() == VillagerProfession.BUTCHER) {
            VillagerTrades.ItemListing block_trade = new Trade(Items.EMERALD, 35, ModBlocks.SHEPHERD_BLOCK.get(), 1, 4, 20);
            List list = event.getTrades().get(2);
            list.add(block_trade);
            event.getTrades().put(2, list);
        }

        if (event.getType() == VillagerProfession.LIBRARIAN) {
            VillagerTrades.ItemListing block_trade = new Trade(Items.EMERALD, 45, ModBlocks.MERCHANT_BLOCK.get(), 1, 4, 20);
            List list = event.getTrades().get(2);
            list.add(block_trade);
            event.getTrades().put(2, list);
        }

        if (event.getType() == VillagerProfession.FARMER) {
            VillagerTrades.ItemListing block_trade = new Trade(Items.EMERALD, 28, ModBlocks.FARMER_BLOCK.get(), 1, 4, 20);
            List list = event.getTrades().get(2);
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

        public Trade(ItemLike buyingItem, int buyingAmount, ItemLike sellingItem, int sellingAmount, int maxUses, int givenExp) {
            this.buyingItem = buyingItem.asItem();
            this.buyingAmount = buyingAmount;
            this.sellingItem = sellingItem.asItem();
            this.sellingAmount = sellingAmount;
            this.maxUses = maxUses;
            this.givenExp = givenExp;
            this.priceMultiplier = 0.05F;
        }

        public MerchantOffer getOffer(Entity entity, Random random) {
            return new MerchantOffer(new ItemStack(this.buyingItem, this.buyingAmount), new ItemStack(sellingItem, sellingAmount), maxUses, givenExp, priceMultiplier);
        }
    }

    static class ItemsForEmeraldsTrade implements VillagerTrades.ItemListing {
        private final ItemStack itemStack;
        private final int emeraldCost;
        private final int numberOfItems;
        private final int maxUses;
        private final int villagerXp;
        private final float priceMultiplier;

        public ItemsForEmeraldsTrade(Block p_i50528_1_, int p_i50528_2_, int p_i50528_3_, int p_i50528_4_, int p_i50528_5_) {
            this(new ItemStack(p_i50528_1_), p_i50528_2_, p_i50528_3_, p_i50528_4_, p_i50528_5_);
        }

        public ItemsForEmeraldsTrade(Item p_i50529_1_, int p_i50529_2_, int p_i50529_3_, int p_i50529_4_) {
            this(new ItemStack(p_i50529_1_), p_i50529_2_, p_i50529_3_, 12, p_i50529_4_);
        }

        public ItemsForEmeraldsTrade(Item p_i50530_1_, int p_i50530_2_, int p_i50530_3_, int p_i50530_4_, int p_i50530_5_) {
            this(new ItemStack(p_i50530_1_), p_i50530_2_, p_i50530_3_, p_i50530_4_, p_i50530_5_);
        }

        public ItemsForEmeraldsTrade(ItemStack p_i50531_1_, int p_i50531_2_, int p_i50531_3_, int p_i50531_4_, int p_i50531_5_) {
            this(p_i50531_1_, p_i50531_2_, p_i50531_3_, p_i50531_4_, p_i50531_5_, 0.05F);
        }

        public ItemsForEmeraldsTrade(ItemStack p_i50532_1_, int p_i50532_2_, int p_i50532_3_, int p_i50532_4_, int p_i50532_5_, float p_i50532_6_) {
            this.itemStack = p_i50532_1_;
            this.emeraldCost = p_i50532_2_;
            this.numberOfItems = p_i50532_3_;
            this.maxUses = p_i50532_4_;
            this.villagerXp = p_i50532_5_;
            this.priceMultiplier = p_i50532_6_;
        }

        public MerchantOffer getOffer(Entity p_221182_1_, Random p_221182_2_) {
            return new MerchantOffer(new ItemStack(Items.EMERALD, this.emeraldCost), new ItemStack(this.itemStack.getItem(), this.numberOfItems), this.maxUses, this.villagerXp, this.priceMultiplier);
        }
    }

}
