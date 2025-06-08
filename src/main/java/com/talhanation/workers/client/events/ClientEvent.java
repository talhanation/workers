package com.talhanation.workers.client.events;

import com.talhanation.recruits.config.RecruitsClientConfig;
import com.talhanation.workers.Main;
import com.talhanation.workers.client.render.WorkerVillagerRenderer;
import com.talhanation.workers.init.ModEntityTypes;
import com.talhanation.workers.client.render.WorkerHumanRenderer;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Main.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD , value = Dist.CLIENT)
public class ClientEvent {
    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void entityRenderersEvent(EntityRenderersEvent.RegisterRenderers event) {
        if (RecruitsClientConfig.RecruitsLookLikeVillagers.get()) {
            EntityRenderers.register(ModEntityTypes.FARMER.get(), WorkerVillagerRenderer::new);
        }
        else{
            EntityRenderers.register(ModEntityTypes.FARMER.get(), WorkerHumanRenderer::new);
        }
    }
}
