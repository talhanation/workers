package com.talhanation.workers;

import com.talhanation.workers.entities.*;
import com.talhanation.workers.init.ModEntityTypes;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = WorkersMain.MOD_ID)
public class AttributeEvent {

    @SubscribeEvent
    public static void entityAttributeEvent(final EntityAttributeCreationEvent event) {
        event.put(ModEntityTypes.FARMER.get(), FarmerEntity.setAttributes().build());
        event.put(ModEntityTypes.LUMBERJACK.get(), LumberjackEntity.setAttributes().build());
        event.put(ModEntityTypes.MINER.get(), MinerEntity.setAttributes().build());
        event.put(ModEntityTypes.BUILDER.get(), BuilderEntity.setAttributes().build());
        event.put(ModEntityTypes.MERCHANT.get(), MerchantEntity.setAttributes().build());
        event.put(ModEntityTypes.FISHERMAN.get(), FishermanEntity.setAttributes().build());
        event.put(ModEntityTypes.ANIMAL_FARMER.get(), AnimalFarmerEntity.setAttributes().build());
        event.put(ModEntityTypes.COURIER.get(), AnimalFarmerEntity.setAttributes().build());
        event.put(ModEntityTypes.COOK.get(), CookEntity.setAttributes().build());
    }
}
