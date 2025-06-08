package com.talhanation.workers;

import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class VillagerEvents {
    /*
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
    */

    @SubscribeEvent
    public void WanderingVillagerTrades(VillagerTradesEvent event) {
        
    }

    @SubscribeEvent
    public void villagerTrades(VillagerTradesEvent event) {
        /*
        if (event.getType() == VillagerProfession.MASON) {
            Trade block_trade = new Trade(Items.EMERALD, 30, ModBlocks.MINER_BLOCK.get(), 1, 4,
                    20);
            List<ItemListing> list = event.getTrades().get(2);
            list.add(block_trade);
            event.getTrades().put(2, list);
        }
        if (event.getType() == VillagerProfession.FARMER) {
            Trade block_trade = new Trade(Items.EMERALD, 23, ModBlocks.LUMBERJACK_BLOCK.get(), 1,
                    4, 20);
            List<ItemListing> list = event.getTrades().get(2);
            list.add(block_trade);
            event.getTrades().put(2, list);
        }

        if (event.getType() == VillagerProfession.FISHERMAN) {
            Trade block_trade = new Trade(Items.EMERALD, 32, ModBlocks.FISHER_BLOCK.get(), 1, 4,
                    20);
            List<ItemListing> list = event.getTrades().get(2);
            list.add(block_trade);
            event.getTrades().put(2, list);
        }

        if (event.getType() == VillagerProfession.BUTCHER) {
            Trade block_trade = new Trade(Items.EMERALD, 35, ModBlocks.SHEPHERD_BLOCK.get(), 1, 4,
                    20);
            List<ItemListing> list = event.getTrades().get(2);
            list.add(block_trade);
            event.getTrades().put(2, list);
        }

        if (event.getType() == VillagerProfession.SHEPHERD) {
            Trade block_trade = new Trade(Items.EMERALD, 25, ModBlocks.SHEPHERD_BLOCK.get(), 1, 4,
                    20);
            List<ItemListing> list = event.getTrades().get(2);
            list.add(block_trade);
            event.getTrades().put(2, list);
        }

        if (event.getType() == VillagerProfession.LIBRARIAN) {
            Trade block_trade = new Trade(Items.EMERALD, 45, ModBlocks.MERCHANT_BLOCK.get(), 1, 4,
                    20);
            List<ItemListing> list = event.getTrades().get(2);
            list.add(block_trade);
            event.getTrades().put(2, list);
        }

        if (event.getType() == VillagerProfession.FARMER) {
            Trade block_trade = new Trade(Items.EMERALD, 28, ModBlocks.FARMER_BLOCK.get(), 1, 4,
                    20);
            List<ItemListing> list = event.getTrades().get(2);
            list.add(block_trade);
            event.getTrades().put(2, list);
        }

        if (event.getType() == VillagerProfession.BUTCHER) {
            Trade block_trade = new Trade(Items.EMERALD, 40, ModBlocks.CATTLE_FARMER_BLOCK.get(),
                    1, 4, 20);
            List<ItemListing> list = event.getTrades().get(2);
            list.add(block_trade);
            event.getTrades().put(2, list);
        }

        if (event.getType() == VillagerProfession.BUTCHER) {
            Trade block_trade = new Trade(Items.EMERALD, 32, ModBlocks.CHICKEN_FARMER_BLOCK.get(),
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
         */
    }
}
