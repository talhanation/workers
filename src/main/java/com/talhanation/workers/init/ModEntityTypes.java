package com.talhanation.workers.init;

import com.talhanation.workers.Main;
import com.talhanation.workers.entities.*;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModEntityTypes {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITIES, Main.MOD_ID);


    public static final RegistryObject<EntityType<MinerEntity>> MINER = ENTITY_TYPES.register("miner",
            () -> EntityType.Builder.of(MinerEntity::new, EntityClassification.CREATURE)
                    .sized(0.6F, 1.95F)
                    .canSpawnFarFromPlayer()
                    .setTrackingRange(32)
                    .setShouldReceiveVelocityUpdates(true)
                    .build(new ResourceLocation(Main.MOD_ID, "miner").toString()));

    public static final RegistryObject<EntityType<LumberjackEntity>> LUMBERJACK = ENTITY_TYPES.register("lumberjack",
            () -> EntityType.Builder.of(LumberjackEntity::new, EntityClassification.CREATURE)
                    .sized(0.6F, 1.95F)
                    .canSpawnFarFromPlayer()
                    .setTrackingRange(32)
                    .setShouldReceiveVelocityUpdates(true)
                    .build(new ResourceLocation(Main.MOD_ID, "lumberjack").toString()));

    public static final RegistryObject<EntityType<ShepherdEntity>> SHEPHERD = ENTITY_TYPES.register("shepherd",
            () -> EntityType.Builder.of(ShepherdEntity::new, EntityClassification.CREATURE)
                    .sized(0.6F, 1.95F)
                    .canSpawnFarFromPlayer()
                    .setTrackingRange(32)
                    .setShouldReceiveVelocityUpdates(true)
                    .build(new ResourceLocation(Main.MOD_ID, "shepherd").toString()));

    public static final RegistryObject<EntityType<FarmerEntity>> FARMER = ENTITY_TYPES.register("farmer",
            () -> EntityType.Builder.of(FarmerEntity::new, EntityClassification.CREATURE)
                    .sized(0.6F, 1.95F)
                    .canSpawnFarFromPlayer()
                    .setTrackingRange(32)
                    .setShouldReceiveVelocityUpdates(true)
                    .build(new ResourceLocation(Main.MOD_ID, "farmer").toString()));

    public static final RegistryObject<EntityType<FishermanEntity>> FISHERMAN = ENTITY_TYPES.register("fisherman",
            () -> EntityType.Builder.of(FishermanEntity::new, EntityClassification.CREATURE)
                    .sized(0.6F, 1.95F)
                    .canSpawnFarFromPlayer()
                    .setTrackingRange(32)
                    .setShouldReceiveVelocityUpdates(true)
                    .build(new ResourceLocation(Main.MOD_ID, "fisherman").toString()));
}