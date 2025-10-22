package com.talhanation.workers;

import com.talhanation.recruits.RecruitsHireTradesRegistry;
import com.talhanation.workers.network.MessageToClientUpdateConfig;
import com.talhanation.recruits.world.RecruitsHireTrade;
import com.talhanation.workers.config.WorkersServerConfig;
import com.talhanation.workers.init.ModEntityTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;

public class VillagerEvents {

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
        RecruitsHireTradesRegistry.register(new RecruitsHireTrade(ModEntityTypes.FARMER.getId(), WorkersServerConfig.FarmerCost.get(), 1, 50, TITLE_FARMER, DESCRIPTION_FARMER, List.of(RecruitsHireTrade.RecruitsTradeTag.FARMER, RecruitsHireTrade.RecruitsTradeTag.MELEE)));
        RecruitsHireTradesRegistry.register(new RecruitsHireTrade(ModEntityTypes.LUMBERJACK.getId(), WorkersServerConfig.LumberjackCost.get(), 1, 50, TITLE_LUMBERJACK, DESCRIPTION_LUMBERJACK, List.of(RecruitsHireTrade.RecruitsTradeTag.FARMER, RecruitsHireTrade.RecruitsTradeTag.MELEE)));
        RecruitsHireTradesRegistry.register(new RecruitsHireTrade(ModEntityTypes.MINER.getId(), WorkersServerConfig.MinerCost.get(), 1, 50, TITLE_MINER, DESCRIPTION_MINER, List.of(RecruitsHireTrade.RecruitsTradeTag.FARMER, RecruitsHireTrade.RecruitsTradeTag.MELEE)));
        RecruitsHireTradesRegistry.register(new RecruitsHireTrade(ModEntityTypes.BUILDER.getId(), WorkersServerConfig.BuilderCost.get(), 1, 50, TITLE_BUILDER, DESCRIPTION_BUILDER, List.of(RecruitsHireTrade.RecruitsTradeTag.WORKER, RecruitsHireTrade.RecruitsTradeTag.MELEE)));
        //RecruitsHireTradesRegistry.register(new RecruitsHireTrade(ModEntityTypes.MERCHANT.getId(), WorkersServerConfig.MerchantCost.get(),1, 50));
        /*
        RecruitsHireTradesRegistry.register(new RecruitsHireTrade(ModEntityTypes.FISHERMAN.getId(), WorkersServerConfig.FishermanCost.get(),3, 50));
        RecruitsHireTradesRegistry.register(new RecruitsHireTrade(ModEntityTypes.CATTLE_FARMER.getId(), WorkersServerConfig.CattleFarmerCost.get(),3, 50));
        RecruitsHireTradesRegistry.register(new RecruitsHireTrade(ModEntityTypes.CHICKEN_FARMER.getId(), WorkersServerConfig.ChickenFarmer.get(),3, 50));
        RecruitsHireTradesRegistry.register(new RecruitsHireTrade(ModEntityTypes.SWINEHERD.getId(), WorkersServerConfig.SwineFarmerCost.get(),3, 50));
        RecruitsHireTradesRegistry.register(new RecruitsHireTrade(ModEntityTypes.RABBIT_FARMER.getId(), WorkersServerConfig.RabbitFarmerCost.get(),3, 50));
        RecruitsHireTradesRegistry.register(new RecruitsHireTrade(ModEntityTypes.BEE_KEEPER.getId(), WorkersServerConfig.BeeKeeperCost.get(),3, 50));

        RecruitsHireTradesRegistry.register(new RecruitsHireTrade(ModEntityTypes.CHEF.getId(), WorkersServerConfig.ChefCost.get(),4, 50));
        */
    }

    private static final Component TITLE_FARMER = Component.translatable("description.workers.title.farmer");
    private static final Component TITLE_MINER = Component.translatable("description.workers.title.miner");
    private static final Component TITLE_LUMBERJACK = Component.translatable("description.workers.title.lumberjack");
    private static final Component TITLE_BUILDER = Component.translatable("description.workers.title.builder");

    private static final Component DESCRIPTION_FARMER = Component.translatable("description.workers.farmer");
    private static final Component DESCRIPTION_MINER = Component.translatable("description.workers.miner");
    private static final Component DESCRIPTION_LUMBERJACK = Component.translatable("description.workers.lumberjack");
    private static final Component DESCRIPTION_BUILDER = Component.translatable("description.workers.builder");
}
