package com.talhanation.workers;

import com.talhanation.workers.config.WorkersModConfig;
import com.talhanation.workers.network.*;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.talhanation.workers.client.events.KeyEvents;
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

@Mod(Main.MOD_ID)
public class Main {
    public static final String MOD_ID = "workers";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    public static SimpleChannel SIMPLE_CHANNEL;
    public static boolean isSmallShipsInstalled;

    public Main() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, WorkersModConfig.CONFIG);
        WorkersModConfig.loadConfig(WorkersModConfig.CONFIG, FMLPaths.CONFIGDIR.get().resolve("workers-common.toml"));

        final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::setup);
        ModBlocks.BLOCKS.register(modEventBus);
        ModPois.POIS.register(modEventBus);
        ModProfessions.PROFESSIONS.register(modEventBus);
        ModMenuTypes.MENU_TYPES.register(modEventBus);
        // ModSounds.SOUNDS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModEntityTypes.ENTITY_TYPES.register(modEventBus);

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            FMLJavaModLoadingContext.get().getModEventBus().addListener(Main.this::clientSetup);
            FMLJavaModLoadingContext.get().getModEventBus().addListener(ModShortcuts::registerBindings);
        });

        MinecraftForge.EVENT_BUS.register(this);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void setup(final FMLCommonSetupEvent event) {
        MinecraftForge.EVENT_BUS.register(new VillagerEvents());
        MinecraftForge.EVENT_BUS.register(new CommandEvents());
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new UpdateChecker());

        SIMPLE_CHANNEL = CommonRegistry.registerChannel(Main.MOD_ID, "default");

        Class[] messages = {
            MessageStartPos.class,
            MessageOpenGuiMiner.class,
            MessageMineType.class,
            MessageMineDepth.class,
            MessageOpenGuiWorker.class,
            MessageOpenGuiMerchant.class,
            MessageMerchantTradeButton.class,
            MessageOpenGuiAnimalFarmer.class,
            MessageAnimalCount.class,
            MessageHire.class,
            MessageHireGui.class,
            MessageChestPos.class,
            MessageBedPos.class,
            MessageOpenCommandScreen.class,
            MessageToClientUpdateCommandScreen.class,
            MessageMerchantTravel.class,
            MessageMerchantAddWayPoint.class,
            MessageMerchantRemoveWayPoint.class,
            MessageMerchantSetCreative.class,
            MessageMerchantReturnTime.class,
            MessageToClientUpdateMerchantScreen.class,
            MessageMerchantHorse.class,
            MessageMerchantResetCurrentTradeCounts.class,
            MessageMerchantTradeLimitButton.class,
            MessageOpenWaypointsGuiMerchant.class,
            MessageMerchantSetTravelSpeed.class,
            MessageMerchantSetAutoStartTravel.class,
            MessageFollow.class,
            MessageChickenFarmerUseEggs.class,
            MessageMerchantSetSendInfo.class,
            MessageLumberjackReplant.class
        };
        for (int i = 0; i < messages.length; i++) CommonRegistry.registerMessage(SIMPLE_CHANNEL, i, messages[i]);
        LOGGER.info("Messages registered");

        isSmallShipsInstalled = ModList.get().isLoaded("smallships");//Smallships mod
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(ModMenuTypes::registerMenus);
        MinecraftForge.EVENT_BUS.register(new KeyEvents());
    }
}
