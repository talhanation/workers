package com.talhanation.workers.util;


import com.talhanation.workers.Main;
import com.talhanation.workers.client.render.*;
import com.talhanation.workers.init.ModEntityTypes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Main.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD , value = Dist.CLIENT)
public class ClientEventBusSub {

    @SubscribeEvent
    public static void clientSetup(EntityRenderersEvent.RegisterRenderers event){
        event.registerEntityRenderer(ModEntityTypes.MINER.get(), MinerRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.LUMBERJACK.get(), LumberjackRenderer::new );
        event.registerEntityRenderer(ModEntityTypes.SHEPHERD.get(), WorkersRenderer::new );
        event.registerEntityRenderer(ModEntityTypes.FARMER.get(), WorkersRenderer::new );
        event.registerEntityRenderer(ModEntityTypes.FISHERMAN.get(), FishermanRenderer::new );
        event.registerEntityRenderer(ModEntityTypes.MERCHANT.get(), MerchantRenderer::new );

    }
}