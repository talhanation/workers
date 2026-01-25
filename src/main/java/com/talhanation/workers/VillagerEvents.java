package com.talhanation.workers;


import com.talhanation.workers.init.ModEntityTypes;
import com.talhanation.recruits.world.RecruitsHireTradesRegistry;
import com.talhanation.workers.network.MessageToClientUpdateConfig;
import com.talhanation.recruits.world.RecruitsHireTrade;
import com.talhanation.workers.config.WorkersServerConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;

public class VillagerEvents {
    public static final Component TITLE_FARMER = Component.translatable("description.workers.title.farmer");
    public static final Component TITLE_MINER = Component.translatable("description.workers.title.miner");
    public static final Component TITLE_LUMBERJACK = Component.translatable("description.workers.title.lumberjack");
    public static final Component TITLE_BUILDER = Component.translatable("description.workers.title.builder");
    public static final Component TITLE_MERCHANT = Component.translatable("description.workers.title.merchant");
    public static final Component TITLE_FISHERMAN = Component.translatable("description.workers.title.fisherman");
    public static final Component DESCRIPTION_FARMER = Component.translatable("description.workers.farmer");
    public static final Component DESCRIPTION_MINER = Component.translatable("description.workers.miner");
    public static final Component DESCRIPTION_LUMBERJACK = Component.translatable("description.workers.lumberjack");
    public static final Component DESCRIPTION_BUILDER = Component.translatable("description.workers.builder");
    public static final Component DESCRIPTION_MERCHANT = Component.translatable("description.workers.merchant");
    public static final Component DESCRIPTION_FISHERMAN = Component.translatable("description.workers.fisherman");
    @SubscribeEvent
    public void onPlayerJoinWorld(EntityJoinLevelEvent event) {
        if(event.getLevel().isClientSide()) return;

        if(event.getEntity() instanceof ServerPlayer player){
                WorkersMain.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                        new MessageToClientUpdateConfig(WorkersServerConfig.ShouldWorkAreaOnlyBeInFactionClaim.get()));
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        RecruitsHireTrade FARMER = new RecruitsHireTrade(ModEntityTypes.FARMER.getId(), WorkersServerConfig.FarmerCost.get(), TITLE_FARMER, DESCRIPTION_FARMER);
        RecruitsHireTrade LUMBERJACK = new RecruitsHireTrade(ModEntityTypes.LUMBERJACK.getId(), WorkersServerConfig.LumberjackCost.get(), TITLE_LUMBERJACK, DESCRIPTION_LUMBERJACK);
        RecruitsHireTrade MINER = new RecruitsHireTrade(ModEntityTypes.MINER.getId(), WorkersServerConfig.MinerCost.get(), TITLE_MINER, DESCRIPTION_MINER);
        RecruitsHireTrade MERCHANT = new RecruitsHireTrade(ModEntityTypes.MERCHANT.getId(), WorkersServerConfig.MerchantCost.get(), TITLE_MERCHANT, DESCRIPTION_MERCHANT);
        RecruitsHireTrade BUILDER = new RecruitsHireTrade(ModEntityTypes.BUILDER.getId(), WorkersServerConfig.BuilderCost.get(), TITLE_BUILDER, DESCRIPTION_BUILDER);

        RecruitsHireTrade FISHERMAN = new RecruitsHireTrade(ModEntityTypes.FISHERMAN.getId(), WorkersServerConfig.BuilderCost.get(), TITLE_FISHERMAN, DESCRIPTION_FISHERMAN);

        RecruitsHireTradesRegistry.addTrade("workers", 1, FARMER, LUMBERJACK);
        RecruitsHireTradesRegistry.addTrade("workers", 2, MERCHANT);
        RecruitsHireTradesRegistry.addTrade("workers", 3, BUILDER);

        RecruitsHireTradesRegistry.addTrade("workers2", 1, FARMER, MINER);
        RecruitsHireTradesRegistry.addTrade("workers2", 2, MERCHANT);
        RecruitsHireTradesRegistry.addTrade("workers2", 3, BUILDER);

        RecruitsHireTradesRegistry.addTrade("workers3", 1, FARMER, FISHERMAN);
        RecruitsHireTradesRegistry.addTrade("workers3", 2, MERCHANT);
        RecruitsHireTradesRegistry.addTrade("workers3", 3, BUILDER);

        /*
        RecruitsHireTradesRegistry.addTrade("herd", 1, ANIMAL_FARMER, FISHERMAN);
        RecruitsHireTradesRegistry.addTrade("herd", 2, MERCHANT);
        RecruitsHireTradesRegistry.addTrade("herd", 3, CHEF);

        RecruitsHireTradesRegistry.addTrade("herd", 1, ANIMAL_FARMER, BEE_KEEPER);
        RecruitsHireTradesRegistry.addTrade("herd", 2, MERCHANT);
        RecruitsHireTradesRegistry.addTrade("herd", 3, CHEF);
        */
    }


}
