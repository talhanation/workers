package com.talhanation.workers.init;

import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.ImmutableSet;
import com.talhanation.workers.Main;

import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModPois {
    private static final Logger logger = LogManager.getLogger(Main.MOD_ID);
    public static final DeferredRegister<PoiType> POIS =
            DeferredRegister.create(ForgeRegistries.POI_TYPES, Main.MOD_ID);

    public static final RegistryObject<PoiType> POI_MINER = makePoi("miner", ModBlocks.MINER_BLOCK);
    public static final RegistryObject<PoiType> POI_LUMBERJACK = makePoi("lumberjack", ModBlocks.LUMBERJACK_BLOCK);
    public static final RegistryObject<PoiType> POI_FARMER = makePoi("farmer", ModBlocks.FARMER_BLOCK);
    public static final RegistryObject<PoiType> POI_MERCHANT = makePoi("merchant", ModBlocks.MERCHANT_BLOCK);
    public static final RegistryObject<PoiType> POI_SHEPHERD = makePoi("shepherd", ModBlocks.SHEPHERD_BLOCK);
    public static final RegistryObject<PoiType> POI_FISHER = makePoi("fisherman", ModBlocks.FISHER_BLOCK);
    public static final RegistryObject<PoiType> POI_CATTLE_FARMER =
            makePoi("cattle_farmer", ModBlocks.CATTLE_FARMER_BLOCK);
    public static final RegistryObject<PoiType> POI_CHICKEN_FARMER =
            makePoi("chicken_farmer", ModBlocks.CHICKEN_FARMER_BLOCK);
    public static final RegistryObject<PoiType> POI_SWINEHERD = makePoi("swineherd", ModBlocks.SWINEHERD_BLOCK);

    private static RegistryObject<PoiType> makePoi(String name, RegistryObject<Block> block) {
        logger.info("Registering POI for " + block.getKey().toString());
        return POIS.register(name, () -> {
            Set<BlockState> blockStates = ImmutableSet.copyOf(block.get().getStateDefinition().getPossibleStates());
            return new PoiType(blockStates, 1, 1);
        });
    }
}
