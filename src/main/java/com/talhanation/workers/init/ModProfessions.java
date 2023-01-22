package com.talhanation.workers.init;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.ImmutableSet;
import com.talhanation.workers.Main;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModProfessions {
    private static final Logger logger = LogManager.getLogger(Main.MOD_ID);
    public static final DeferredRegister<VillagerProfession> PROFESSIONS =
            DeferredRegister.create(ForgeRegistries.VILLAGER_PROFESSIONS, Main.MOD_ID);

    public static final RegistryObject<VillagerProfession> MINER = makeProfession("miner", ModPois.POI_MINER);
    public static final RegistryObject<VillagerProfession> LUMBERJACK =
            makeProfession("lumberjack", ModPois.POI_LUMBERJACK);
    public static final RegistryObject<VillagerProfession> FARMER = makeProfession("farmer", ModPois.POI_FARMER);
    public static final RegistryObject<VillagerProfession> MERCHANT = makeProfession("merchant", ModPois.POI_MERCHANT);
    public static final RegistryObject<VillagerProfession> SHEPHERD = makeProfession("shepherd", ModPois.POI_SHEPHERD);
    public static final RegistryObject<VillagerProfession> FISHER = makeProfession("fisherman", ModPois.POI_FISHER);
    public static final RegistryObject<VillagerProfession> CATTLE_FARMER =
            makeProfession("cattle_farmer", ModPois.POI_CATTLE_FARMER);
    public static final RegistryObject<VillagerProfession> CHICKEN_FARMER =
            makeProfession("chicken_farmer", ModPois.POI_CHICKEN_FARMER);
    public static final RegistryObject<VillagerProfession> SWINEHERD =
            makeProfession("swineherd", ModPois.POI_SWINEHERD);

    private static final RegistryObject<VillagerProfession> makeProfession(String name,
            RegistryObject<PoiType> pointOfInterest) {
        logger.info("Registering profession for {} with POI {}", name, pointOfInterest);
        return PROFESSIONS.register(name,
                () -> new VillagerProfession(name, poi -> poi.get() == pointOfInterest.get(),
                        poi -> poi.get() == pointOfInterest.get(), ImmutableSet.of(), ImmutableSet.of(),
                        SoundEvents.VILLAGER_CELEBRATE));
    }
}
