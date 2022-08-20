package com.talhanation.workers.init;

import com.talhanation.workers.Main;
import com.talhanation.workers.client.render.ChickenFarmerRenderer;
import com.talhanation.workers.entities.*;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

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


    public static final RegistryObject<EntityType<CattleFarmerEntity>> CATTLE_FARMER = ENTITY_TYPES.register("cattle_farmer",
            () -> EntityType.Builder.of(CattleFarmerEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F)
                    .canSpawnFarFromPlayer()
                    .setTrackingRange(32)
                    .setShouldReceiveVelocityUpdates(true)
                    .build(new ResourceLocation(Main.MOD_ID, "cattle_farmer").toString()));

    public static final RegistryObject<EntityType<ChickenFarmerEntity>> CHICKEN_FARMER = ENTITY_TYPES.register("chicken_farmer",
            () -> EntityType.Builder.of(ChickenFarmerEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F)
                    .canSpawnFarFromPlayer()
                    .setTrackingRange(32)
                    .setShouldReceiveVelocityUpdates(true)
                    .build(new ResourceLocation(Main.MOD_ID, "chicken_farmer").toString()));

}
