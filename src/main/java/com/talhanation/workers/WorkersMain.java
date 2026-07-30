package com.talhanation.workers;

import com.talhanation.recruits.client.events.CommandCategoryManager;
import com.talhanation.workers.client.events.ScreenEvents;
import com.talhanation.workers.client.gui.WorkerCommandScreen;
import com.talhanation.workers.config.WorkersServerConfig;
import com.talhanation.workers.init.ModBlocks;
import com.talhanation.workers.init.ModEntityTypes;
import com.talhanation.workers.init.ModItems;
import com.talhanation.workers.init.ModMenuTypes;
import com.talhanation.workers.init.ModPois;
import com.talhanation.workers.init.ModProfessions;
import com.talhanation.workers.network.*;
import com.talhanation.workers.network.compat.WorkersChannel;
import com.talhanation.workers.world.StructureManager;
import de.maxhenkel.corelib.CommonRegistry;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(WorkersMain.MOD_ID)
public class WorkersMain {
    public static final String MOD_ID = "workers";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    public static final WorkersChannel SIMPLE_CHANNEL = new WorkersChannel();

    public static boolean isDynamicTreesInstalled;
    public static boolean isFarmersDelightInstalled;

    public WorkersMain(IEventBus modEventBus, Dist dist, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, (IConfigSpec) WorkersServerConfig.SERVER);

        modEventBus.addListener(this::setup);
        modEventBus.addListener(this::registerPayloads);
        ModBlocks.BLOCKS.register(modEventBus);
        ModPois.POIS.register(modEventBus);
        ModProfessions.PROFESSIONS.register(modEventBus);
        ModMenuTypes.MENU_TYPES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModEntityTypes.ENTITY_TYPES.register(modEventBus);
        ModEntityTypes.WORKER_TYPES.register(modEventBus);
        modEventBus.addListener(this::addCreativeTabs);

        if (dist == Dist.CLIENT) {
            modEventBus.addListener(this::clientSetup);
        }

        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        // MerchantResetCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        if (WorkersServerConfig.BuildModeConfig.get() == com.talhanation.workers.config.BuildMode.PRESET_FACTIONS) {
            java.io.File factionsDir = event.getServer().getServerDirectory()
                    .resolve("workers").resolve("scan").resolve("factions").toFile();
            if (!factionsDir.exists()) {
                factionsDir.mkdirs();
                LOGGER.info("[Workers] Created factions scan folder: {}", factionsDir.getAbsolutePath());
            }
        }
    }

    private void setup(final FMLCommonSetupEvent event) {
        NeoForge.EVENT_BUS.register(new VillagerEvents());
        NeoForge.EVENT_BUS.register(new WorkerClaimEvents());
        NeoForge.EVENT_BUS.register(new UpdateChecker());

        ModList modList = ModList.get();
        isDynamicTreesInstalled = modList.isLoaded("dynamictrees");
        isFarmersDelightInstalled = modList.isLoaded("farmersdelight");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(MOD_ID);
        Class[] messages = {
                MessageAddWorkArea.class,
                MessageToClientOpenWorkAreaScreen.class,
                MessageUpdateWorkArea.class,
                MessageUpdateCropArea.class,
                MessageUpdateLumberArea.class,
                MessageUpdateBuildArea.class,
                MessageUpdateMiningArea.class,
                MessageUpdateMerchantTrade.class,
                MessageUpdateMerchant.class,
                MessageDoTradeWithMerchant.class,
                MessageOpenMerchantEditTradeScreen.class,
                MessageOpenMerchantTradeScreen.class,
                MessageToClientUpdateConfig.class,
                MessageUpdateStorageArea.class,
                MessageUpdateAnimalPenArea.class,
                MessageRotateWorkArea.class,
                MessageMoveMerchantTrade.class,
                MessageUpdateMarketArea.class,
                MessageUpdateKitchenArea.class,
                MessageUpdateOwner.class,
                MessageCourierSetRoute.class,
                MessageOpenCourierScreen.class,
                MessageRequestPresetList.class,
                MessageToClientPresetList.class,
                MessageRequestPresetContent.class,
                MessageToClientPresetContent.class,
                MessageUpdateHomeArea.class,
                MessageOpenMerchantVillagerTradeScreen.class
        };
        for (Class message : messages) {
            CommonRegistry.registerMessage(registrar, message);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(ModMenuTypes::registerMenus);
        event.enqueueWork(StructureManager::copyDefaultStructuresIfMissing);
        CommandCategoryManager.register(new WorkerCommandScreen());
        NeoForge.EVENT_BUS.register(new ScreenEvents());
    }

    private void addCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.SPAWN_EGGS)) {
            event.accept(ModItems.FARMER_SPAWN_EGG.get());
            event.accept(ModItems.LUMBERJACK_SPAWN_EGG.get());
            event.accept(ModItems.MINER_SPAWN_EGG.get());
            event.accept(ModItems.MERCHANT_SPAWN_EGG.get());
            event.accept(ModItems.BUILDER_SPAWN_EGG.get());
            event.accept(ModItems.FISHERMAN_SPAWN_EGG.get());
            event.accept(ModItems.ANIMAL_FARMER_SPAWN_EGG.get());
            event.accept(ModItems.COURIER_SPAWN_EGG.get());
            event.accept(ModItems.COOK_SPAWN_EGG.get());
        }
    }
}
