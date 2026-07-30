package com.talhanation.workers.init;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.ImmutableSet;
import com.talhanation.workers.WorkersMain;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModProfessions {
    private static final Logger logger = LogManager.getLogger(WorkersMain.MOD_ID);
    public static final DeferredRegister<VillagerProfession> PROFESSIONS =
            DeferredRegister.create(Registries.VILLAGER_PROFESSION, WorkersMain.MOD_ID);

    private static final DeferredHolder<VillagerProfession, VillagerProfession> makeProfession(String name,
            DeferredHolder<PoiType, PoiType> pointOfInterest) {
        logger.info("Registering profession for {} with POI {}", name, pointOfInterest);
        return PROFESSIONS.register(name,
                () -> new VillagerProfession(name, poi -> poi.value() == pointOfInterest.get(),
                        poi -> poi.value() == pointOfInterest.get(), ImmutableSet.of(), ImmutableSet.of(),
                        SoundEvents.VILLAGER_CELEBRATE));
    }
}
