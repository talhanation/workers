package com.talhanation.workers;

import com.talhanation.workers.entities.*;
import com.talhanation.workers.init.ModEntityTypes;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Main.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class AttributeEvent {

    @SubscribeEvent
    public static void entityAttributeEvent(final EntityAttributeCreationEvent event) {
        event.put(ModEntityTypes.FARMER.get(), FarmerEntity.setAttributes().build());
        event.put(ModEntityTypes.FISHERMAN.get(), FishermanEntity.setAttributes().build());
        event.put(ModEntityTypes.LUMBERJACK.get(), LumberjackEntity.setAttributes().build());
        event.put(ModEntityTypes.MERCHANT.get(), MerchantEntity.setAttributes().build());
        event.put(ModEntityTypes.MINER.get(), MinerEntity.setAttributes().build());
        event.put(ModEntityTypes.SHEPHERD.get(), ShepherdEntity.setAttributes().build());
    }
}