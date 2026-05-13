package com.talhanation.workers.client.events;

import com.talhanation.recruits.config.RecruitsClientConfig;
import com.talhanation.workers.WorkersMain;
import com.talhanation.workers.client.render.FishingBobberRenderer;
import com.talhanation.workers.client.render.WorkerAreaRenderer;
import com.talhanation.workers.client.render.WorkerVillagerRenderer;
import com.talhanation.workers.init.ModEntityTypes;
import com.talhanation.workers.client.render.WorkerHumanRenderer;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = WorkersMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD , value = Dist.CLIENT)
public class ClientEvent {
    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void entityRenderersEvent(EntityRenderersEvent.RegisterRenderers event) {
        EntityRenderers.register(ModEntityTypes.CROPAREA.get(), WorkerAreaRenderer::new);
        EntityRenderers.register(ModEntityTypes.LUMBERAREA.get(), WorkerAreaRenderer::new);
        EntityRenderers.register(ModEntityTypes.BUILDAREA.get(), WorkerAreaRenderer::new);
        EntityRenderers.register(ModEntityTypes.MININGAREA.get(), WorkerAreaRenderer::new);
        EntityRenderers.register(ModEntityTypes.STORAGEAREA.get(), WorkerAreaRenderer::new);
        EntityRenderers.register(ModEntityTypes.MARKETAREA.get(), WorkerAreaRenderer::new);
        EntityRenderers.register(ModEntityTypes.FISHINGAREA.get(), WorkerAreaRenderer::new);
        EntityRenderers.register(ModEntityTypes.ANIMAL_PEN_AREA.get(), WorkerAreaRenderer::new);
        EntityRenderers.register(ModEntityTypes.HOMEAREA.get(), WorkerAreaRenderer::new);
        EntityRenderers.register(ModEntityTypes.KITCHEN_AREA.get(), WorkerAreaRenderer::new);

        EntityRenderers.register(ModEntityTypes.FISHING_BOBBER.get(), FishingBobberRenderer::new);


        if (RecruitsClientConfig.RecruitsLookLikeVillagers.get()) {
            EntityRenderers.register(ModEntityTypes.FARMER.get(), WorkerVillagerRenderer::new);
            EntityRenderers.register(ModEntityTypes.LUMBERJACK.get(), WorkerVillagerRenderer::new);
            EntityRenderers.register(ModEntityTypes.MINER.get(), WorkerVillagerRenderer::new);
            EntityRenderers.register(ModEntityTypes.BUILDER.get(), WorkerVillagerRenderer::new);
            EntityRenderers.register(ModEntityTypes.MERCHANT.get(), WorkerVillagerRenderer::new);
            EntityRenderers.register(ModEntityTypes.FISHERMAN.get(), WorkerVillagerRenderer::new);
            EntityRenderers.register(ModEntityTypes.ANIMAL_FARMER.get(), WorkerVillagerRenderer::new);
            EntityRenderers.register(ModEntityTypes.COURIER.get(), WorkerVillagerRenderer::new);
            EntityRenderers.register(ModEntityTypes.COOK.get(), WorkerVillagerRenderer::new);
        }
        else{
            EntityRenderers.register(ModEntityTypes.FARMER.get(), WorkerHumanRenderer::new);
            EntityRenderers.register(ModEntityTypes.LUMBERJACK.get(), WorkerHumanRenderer::new);
            EntityRenderers.register(ModEntityTypes.MINER.get(), WorkerHumanRenderer::new);
            EntityRenderers.register(ModEntityTypes.BUILDER.get(), WorkerHumanRenderer::new);
            EntityRenderers.register(ModEntityTypes.MERCHANT.get(), WorkerHumanRenderer::new);
            EntityRenderers.register(ModEntityTypes.FISHERMAN.get(), WorkerHumanRenderer::new);
            EntityRenderers.register(ModEntityTypes.ANIMAL_FARMER.get(), WorkerHumanRenderer::new);
            EntityRenderers.register(ModEntityTypes.COURIER.get(), WorkerHumanRenderer::new);
            EntityRenderers.register(ModEntityTypes.COOK.get(), WorkerHumanRenderer::new);
        }
    }
}
