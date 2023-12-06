package com.talhanation.workers;

import com.talhanation.workers.config.WorkersModConfig;
import com.talhanation.workers.entities.AbstractWorkerEntity;
import com.talhanation.workers.entities.ai.horse.HorseRiddenByMerchantGoal;
import com.talhanation.workers.init.ModBlocks;
import com.talhanation.workers.init.ModEntityTypes;
import com.talhanation.workers.init.ModProfessions;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class VillagerEvents {


    @SubscribeEvent
    public void attackWorkers(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();

        if (WorkersModConfig.PillagerAttackWorkers.get() && entity instanceof AbstractIllager illager) {
            illager.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(illager, AbstractWorkerEntity.class, true));
        }

        if (WorkersModConfig.MonsterAttackWorkers.get() && entity instanceof Monster && !(entity instanceof EnderMan)) {
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
        AbstractWorkerEntity worker = workerType.create(villager.level);
        if (worker != null) {

            worker.copyPosition(villager);
            worker.initSpawn();

            for(ItemStack itemStack : villager.getInventory().items){
                worker.getInventory().addItem(itemStack);
            }

            Component name = villager.getCustomName();
            if(name  != null)worker.setCustomName(name);

            if(WorkersModConfig.WorkersTablesPOIReleasing.get()) villager.releasePoi(MemoryModuleType.JOB_SITE);
            villager.releasePoi(MemoryModuleType.HOME);
            villager.releasePoi(MemoryModuleType.MEETING_POINT);

            villager.discard();
            villager.level.addFreshEntity(worker);
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

    @SubscribeEvent
    public void onProfessionBlockBreak(BlockEvent.BreakEvent event){
        if(event.getState().getBlock().getDescriptionId().contains("workers")){
            BlockState blockState = event.getState();
            Block block = blockState.getBlock();
            BlockPos blockPos = event.getPos();
            ServerLevel level = Objects.requireNonNull(event.getLevel().getServer()).overworld();
            if(level != null && WorkersModConfig.ProfessionBlocksDrop.get()){
                ItemEntity itementity = new ItemEntity(level, blockPos.getX(), blockPos.getY() + 0.5, blockPos.getZ(), block.asItem().getDefaultInstance());
                float f = 0.05F;
                itementity.setDeltaMovement(level.random.triangle(0.0D, 0.11485000171139836D), level.random.triangle(0.2D, 0.11485000171139836D), level.random.triangle(0.0D, 0.11485000171139836D));
                level.addFreshEntity(itementity);
            }
        }
    }
}
