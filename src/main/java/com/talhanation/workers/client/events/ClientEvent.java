package com.talhanation.workers.client.events;


import com.talhanation.workers.Main;
import com.talhanation.workers.client.models.WorkersVillagerModel;
import com.talhanation.workers.client.render.human.*;
import com.talhanation.workers.client.render.layer.RecruitArmorLayer;
import com.talhanation.workers.client.render.villager.*;
import com.talhanation.workers.config.WorkersModConfig;
import com.talhanation.workers.init.ModEntityTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber(modid = Main.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD , value = Dist.CLIENT)
public class ClientEvent {

    public static ModelLayerLocation WORKER = new ModelLayerLocation(new ResourceLocation(Main.MOD_ID + "worker"), "worker");
    public static ModelLayerLocation RECRUIT_OUTER_ARMOR = new ModelLayerLocation(new ResourceLocation(Main.MOD_ID + "recruit_outer_layer"), "recruit_outer_layer");
    public static ModelLayerLocation RECRUIT_INNER_ARMOR = new ModelLayerLocation(new ResourceLocation(Main.MOD_ID + "recruit_inner_layer"), "recruit_inner_layer");

    @SubscribeEvent
    public static void clientSetup(EntityRenderersEvent.RegisterRenderers event){
        if(WorkersModConfig.WorkersLookLikeVillagers.get()){
            event.registerEntityRenderer(ModEntityTypes.MINER.get(), MinerVillagerRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.LUMBERJACK.get(), LumberjackVillagerRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.SHEPHERD.get(), ShepherdVillagerRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.FARMER.get(), FarmerVillagerRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.FISHERMAN.get(), FishermanVillagerRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.MERCHANT.get(), MerchantVillagerRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.CATTLE_FARMER.get(), CattleFarmerVillagerRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.CHICKEN_FARMER.get(), ChickenFarmerVillagerRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.SWINEHERD.get(), SwineherdVillagerRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.RABBIT_FARMER.get(), RabbitFarmerVillagerRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.BEEKEEPER.get(), BeekeeperVillagerRenderer::new);
        }
        else {
            event.registerEntityRenderer(ModEntityTypes.MINER.get(), MinerHumanRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.LUMBERJACK.get(), LumberjackHumanRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.SHEPHERD.get(), ShepherdHumanRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.FARMER.get(), FarmerHumanRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.FISHERMAN.get(), FishermanHumanRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.MERCHANT.get(), MerchantHumanRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.CATTLE_FARMER.get(), CattleFarmerHumanRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.CHICKEN_FARMER.get(), ChickenFarmerHumanRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.SWINEHERD.get(), SwineherdHumanRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.RABBIT_FARMER.get(), RabbitFarmerHumanRenderer::new);
            event.registerEntityRenderer(ModEntityTypes.BEEKEEPER.get(), BeekeeperHumanRenderer::new);
        }
    }

    @SubscribeEvent
    public static void layerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ClientEvent.WORKER, WorkersVillagerModel::createLayerDefinition);
        event.registerLayerDefinition(ClientEvent.RECRUIT_OUTER_ARMOR, RecruitArmorLayer::createOuterArmorLayer);
        event.registerLayerDefinition(ClientEvent.RECRUIT_INNER_ARMOR, RecruitArmorLayer::createInnerArmorLayer);

    }


    @Nullable
    public static Entity getEntityByLooking() {
        HitResult hit = Minecraft.getInstance().hitResult;

        if (hit instanceof EntityHitResult entityHitResult){
            return entityHitResult.getEntity();
        }
        return null;
    }
}