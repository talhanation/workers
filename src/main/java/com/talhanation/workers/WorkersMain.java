package com.talhanation.workers;

import com.talhanation.recruits.client.events.CommandCategoryManager;
import com.talhanation.workers.client.events.ScreenEvents;
import com.talhanation.workers.client.gui.WorkerCommandScreen;
import com.talhanation.workers.config.WorkersServerConfig;
import com.talhanation.workers.network.*;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.talhanation.workers.init.ModBlocks;
import com.talhanation.workers.init.ModEntityTypes;
import com.talhanation.workers.init.ModItems;
import com.talhanation.workers.init.ModMenuTypes;
import com.talhanation.workers.init.ModPois;
import com.talhanation.workers.init.ModProfessions;
import com.talhanation.workers.init.ModShortcuts;

import de.maxhenkel.corelib.CommonRegistry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.simple.SimpleChannel;
import com.talhanation.workers.world.StructureManager;

@Mod(WorkersMain.MOD_ID)
public class WorkersMain {
    public static final String MOD_ID = "workers";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    public static SimpleChannel SIMPLE_CHANNEL;

    public WorkersMain() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, WorkersServerConfig.SERVER);

        final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::setup);
        ModBlocks.BLOCKS.register(modEventBus);
        ModPois.POIS.register(modEventBus);
        ModProfessions.PROFESSIONS.register(modEventBus);
        ModMenuTypes.MENU_TYPES.register(modEventBus);
        // ModSounds.SOUNDS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModEntityTypes.ENTITY_TYPES.register(modEventBus);

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::addCreativeTabs);

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            FMLJavaModLoadingContext.get().getModEventBus().addListener(WorkersMain.this::clientSetup);
            FMLJavaModLoadingContext.get().getModEventBus().addListener(ModShortcuts::registerBindings);
        });

        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        //MerchantResetCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        if (WorkersServerConfig.BuildModeConfig.get() == com.talhanation.workers.config.BuildMode.PRESET_FACTIONS) {
            java.io.File factionsDir = event.getServer().getServerDirectory()
                    .toPath().resolve("workers").resolve("scan").resolve("factions").toFile();
            if (!factionsDir.exists()) {
                factionsDir.mkdirs();
                LOGGER.info("[Workers] Created factions scan folder: {}", factionsDir.getAbsolutePath());
            }
        }
    }


    @SuppressWarnings({"unchecked", "rawtypes"})
    private void setup(final FMLCommonSetupEvent event) {
        MinecraftForge.EVENT_BUS.register(new VillagerEvents());
        MinecraftForge.EVENT_BUS.register(new CommandEvents());
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new UpdateChecker());

        SIMPLE_CHANNEL = CommonRegistry.registerChannel(WorkersMain.MOD_ID, "default");

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
                MessageUpdateOwner.class,
                MessageCourierSetRoute.class,
                MessageOpenCourierScreen.class,
                MessageRequestPresetList.class,
                MessageToClientPresetList.class,
                MessageRequestPresetContent.class,
                MessageToClientPresetContent.class
        };
        for (int i = 0; i < messages.length; i++) CommonRegistry.registerMessage(SIMPLE_CHANNEL, i, messages[i]);
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(ModMenuTypes::registerMenus);
        event.enqueueWork(StructureManager::copyDefaultStructuresIfMissing);
        CommandCategoryManager.register(new WorkerCommandScreen());
        MinecraftForge.EVENT_BUS.register(new ScreenEvents());
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
        }
    }
}