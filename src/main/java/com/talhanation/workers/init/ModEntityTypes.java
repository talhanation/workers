package com.talhanation.workers.init;

import com.talhanation.workers.WorkersMain;

import com.talhanation.workers.entities.*;
import com.talhanation.workers.entities.workarea.*;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntityTypes {

        public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister
                        .create(ForgeRegistries.ENTITY_TYPES, WorkersMain.MOD_ID);

        public static final RegistryObject<EntityType<CropArea>> CROPAREA = ENTITY_TYPES.register("croparea",
                () -> EntityType.Builder.of(CropArea::new, MobCategory.MISC)
                        .sized(1.2F, 2.00F)
                        .fireImmune().noSummon()
                        .build(new ResourceLocation(WorkersMain.MOD_ID, "croparea").toString()));
        public static final RegistryObject<EntityType<LumberArea>> LUMBERAREA = ENTITY_TYPES.register("lumberarea",
                () -> EntityType.Builder.of(LumberArea::new, MobCategory.MISC)
                        .sized(1.2F, 2.00F)
                        .fireImmune().noSummon()
                        .build(new ResourceLocation(WorkersMain.MOD_ID, "lumberarea").toString()));
        public static final RegistryObject<EntityType<BuildArea>> BUILDAREA = ENTITY_TYPES.register("buildarea",
                () -> EntityType.Builder.of(BuildArea::new, MobCategory.MISC)
                        .sized(1.2F, 2.00F)
                        .fireImmune().noSummon()
                        .build(new ResourceLocation(WorkersMain.MOD_ID, "buildarea").toString()));
        public static final RegistryObject<EntityType<MiningArea>> MININGAREA = ENTITY_TYPES.register("miningarea",
                () -> EntityType.Builder.of(MiningArea::new, MobCategory.MISC)
                        .sized(1.2F, 2.00F)
                        .fireImmune().noSummon()
                        .build(new ResourceLocation(WorkersMain.MOD_ID, "miningarea").toString()));
        public static final RegistryObject<EntityType<StorageArea>> STORAGEAREA = ENTITY_TYPES.register("storagearea",
                () -> EntityType.Builder.of(StorageArea::new, MobCategory.MISC)
                        .sized(1.2F, 2.00F)
                        .fireImmune().noSummon()
                        .build(new ResourceLocation(WorkersMain.MOD_ID, "storagearea").toString()));

        public static final RegistryObject<EntityType<MarketArea>> MARKETAREA = ENTITY_TYPES.register("marketarea",
                () -> EntityType.Builder.of(MarketArea::new, MobCategory.MISC)
                        .sized(1.2F, 2.00F)
                        .fireImmune().noSummon()
                        .build(new ResourceLocation(WorkersMain.MOD_ID, "marketarea").toString()));

        public static final RegistryObject<EntityType<KitchenArea>> KITCHEN_AREA = ENTITY_TYPES.register("kitchenarea",
                () -> EntityType.Builder.of(KitchenArea::new, MobCategory.MISC)
                        .sized(1.2F, 2.00F)
                        .fireImmune().noSummon()
                        .build(new ResourceLocation(WorkersMain.MOD_ID, "kitchenarea").toString()));

        public static final RegistryObject<EntityType<FishingArea>> FISHINGAREA = ENTITY_TYPES.register("fishingarea",
                () -> EntityType.Builder.of(FishingArea::new, MobCategory.MISC)
                        .sized(1.2F, 2.00F)
                        .fireImmune().noSummon()
                        .build(new ResourceLocation(WorkersMain.MOD_ID, "fishingarea").toString()));

    public static final RegistryObject<EntityType<AnimalPenArea>> ANIMAL_PEN_AREA = ENTITY_TYPES.register("animalpenarea",
            () -> EntityType.Builder.of(AnimalPenArea::new, MobCategory.MISC)
                    .sized(1.2F, 2.00F)
                    .fireImmune().noSummon()
                    .build(new ResourceLocation(WorkersMain.MOD_ID, "animalpenarea").toString()));

    public static final RegistryObject<EntityType<HomeArea>> HOMEAREA = ENTITY_TYPES.register("homearea",
            () -> EntityType.Builder.of(HomeArea::new, MobCategory.MISC)
                    .sized(1.2F, 2.00F)
                    .fireImmune().noSummon()
                    .build(new ResourceLocation(WorkersMain.MOD_ID, "homearea").toString()));


    public static final RegistryObject<EntityType<AnimalFarmerEntity>> ANIMAL_FARMER = ENTITY_TYPES.register("animal_farmer",
                        () -> EntityType.Builder.of(AnimalFarmerEntity::new, MobCategory.CREATURE)
                                        .sized(0.6F, 1.95F)
                                        .canSpawnFarFromPlayer()
                                        .setTrackingRange(32)
                                        .setShouldReceiveVelocityUpdates(true)
                                        .build(new ResourceLocation(WorkersMain.MOD_ID, "animal_farmer").toString()));

        public static final RegistryObject<EntityType<LumberjackEntity>> LUMBERJACK = ENTITY_TYPES.register("lumberjack",
                () -> EntityType.Builder.of(LumberjackEntity::new, MobCategory.CREATURE)
                        .sized(0.6F, 1.95F)
                        .canSpawnFarFromPlayer()
                        .setTrackingRange(32)
                        .setShouldReceiveVelocityUpdates(true)
                        .build(new ResourceLocation(WorkersMain.MOD_ID, "lumberjack").toString()));

        public static final RegistryObject<EntityType<FarmerEntity>> FARMER = ENTITY_TYPES.register("farmer",
                () -> EntityType.Builder.of(FarmerEntity::new, MobCategory.CREATURE)
                        .sized(0.6F, 1.95F)
                        .canSpawnFarFromPlayer()
                        .setTrackingRange(32)
                        .setShouldReceiveVelocityUpdates(true)
                        .build(new ResourceLocation(WorkersMain.MOD_ID, "farmer").toString()));

        public static final RegistryObject<EntityType<MinerEntity>> MINER = ENTITY_TYPES.register("miner",
                () -> EntityType.Builder.of(MinerEntity::new, MobCategory.CREATURE)
                        .sized(0.6F, 1.95F)
                        .canSpawnFarFromPlayer()
                        .setTrackingRange(32)
                        .setShouldReceiveVelocityUpdates(true)
                        .build(new ResourceLocation(WorkersMain.MOD_ID, "miner").toString()));

        public static final RegistryObject<EntityType<BuilderEntity>> BUILDER = ENTITY_TYPES.register("builder",
                () -> EntityType.Builder.of(BuilderEntity::new, MobCategory.CREATURE)
                        .sized(0.6F, 1.95F)
                        .canSpawnFarFromPlayer()
                        .setTrackingRange(32)
                        .setShouldReceiveVelocityUpdates(true)
                        .build(new ResourceLocation(WorkersMain.MOD_ID, "builder").toString()));

       public static final RegistryObject<EntityType<MerchantEntity>> MERCHANT = ENTITY_TYPES.register("merchant",
                () -> EntityType.Builder.of(MerchantEntity::new, MobCategory.CREATURE)
                        .sized(0.6F, 1.95F)
                        .canSpawnFarFromPlayer()
                        .setTrackingRange(32)
                        .setShouldReceiveVelocityUpdates(true)
                        .build(new ResourceLocation(WorkersMain.MOD_ID, "merchant").toString()));


       public static final RegistryObject<EntityType<FishermanEntity>> FISHERMAN = ENTITY_TYPES.register("fisherman",
                () -> EntityType.Builder.of(FishermanEntity::new, MobCategory.CREATURE)
                        .sized(0.6F, 1.95F)
                        .canSpawnFarFromPlayer()
                        .setTrackingRange(32)
                        .setShouldReceiveVelocityUpdates(true)
                        .build(new ResourceLocation(WorkersMain.MOD_ID, "fisherman").toString()));

       public static final RegistryObject<EntityType<CookEntity>> COOK = ENTITY_TYPES.register("cook",
                () -> EntityType.Builder.of(CookEntity::new, MobCategory.CREATURE)
                        .sized(0.6F, 1.95F)
                        .canSpawnFarFromPlayer()
                        .setTrackingRange(32)
                        .setShouldReceiveVelocityUpdates(true)
                        .build(new ResourceLocation(WorkersMain.MOD_ID, "cook").toString()));

       public static final RegistryObject<EntityType<FishingBobberEntity>> FISHING_BOBBER = ENTITY_TYPES.register("fishing_bobber",
                        () -> EntityType.Builder.<FishingBobberEntity>of(FishingBobberEntity::new, MobCategory.MISC)
                                .sized(0.25F, 0.25F)
                                .clientTrackingRange(4)
                                .updateInterval(5)
                                .build(new ResourceLocation(WorkersMain.MOD_ID, "fishing_bobber").toString()));

        public static final RegistryObject<EntityType<CourierEntity>> COURIER = ENTITY_TYPES.register("courier",
                () -> EntityType.Builder.of(CourierEntity::new, MobCategory.MISC)
                        .sized(0.6F, 1.95F)
                        .canSpawnFarFromPlayer()
                        .setTrackingRange(32)
                        .build(new ResourceLocation(WorkersMain.MOD_ID, "courier").toString()));
}
