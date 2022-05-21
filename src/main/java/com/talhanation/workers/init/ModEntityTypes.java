package com.talhanation.workers.init;

import com.talhanation.workers.Main;
import com.talhanation.workers.entities.*;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fmllegacy.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModEntityTypes {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITIES, Main.MOD_ID);


    public static final RegistryObject<EntityType<MinerEntity>> MINER = ENTITY_TYPES.register("miner",
            () -> EntityType.Builder.of(MinerEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F)
                    .canSpawnFarFromPlayer()
                    .setTrackingRange(32)
                    .setShouldReceiveVelocityUpdates(true)
                    .build(new ResourceLocation(Main.MOD_ID, "miner").toString()));

    public static final RegistryObject<EntityType<LumberjackEntity>> LUMBERJACK = ENTITY_TYPES.register("lumberjack",
            () -> EntityType.Builder.of(LumberjackEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F)
                    .canSpawnFarFromPlayer()
                    .setTrackingRange(32)
                    .setShouldReceiveVelocityUpdates(true)
                    .build(new ResourceLocation(Main.MOD_ID, "lumberjack").toString()));

    public static final RegistryObject<EntityType<ShepherdEntity>> SHEPHERD = ENTITY_TYPES.register("shepherd",
            () -> EntityType.Builder.of(ShepherdEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F)
                    .canSpawnFarFromPlayer()
                    .setTrackingRange(32)
                    .setShouldReceiveVelocityUpdates(true)
                    .build(new ResourceLocation(Main.MOD_ID, "shepherd").toString()));

    public static final RegistryObject<EntityType<FarmerEntity>> FARMER = ENTITY_TYPES.register("farmer",
            () -> EntityType.Builder.of(FarmerEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F)
                    .canSpawnFarFromPlayer()
                    .setTrackingRange(32)
                    .setShouldReceiveVelocityUpdates(true)
                    .build(new ResourceLocation(Main.MOD_ID, "farmer").toString()));

    public static final RegistryObject<EntityType<FishermanEntity>> FISHERMAN = ENTITY_TYPES.register("fisherman",
            () -> EntityType.Builder.of(FishermanEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F)
                    .canSpawnFarFromPlayer()
                    .setTrackingRange(32)
                    .setShouldReceiveVelocityUpdates(true)
                    .build(new ResourceLocation(Main.MOD_ID, "fisherman").toString()));

    public static final RegistryObject<EntityType<MerchantEntity>> MERCHANT = ENTITY_TYPES.register("merchant",
            () -> EntityType.Builder.of(MerchantEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F)
                    .canSpawnFarFromPlayer()
                    .setTrackingRange(32)
                    .setShouldReceiveVelocityUpdates(true)
                    .build(new ResourceLocation(Main.MOD_ID, "merchant").toString()));
}
