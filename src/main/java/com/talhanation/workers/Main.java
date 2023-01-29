package com.talhanation.workers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mojang.blaze3d.platform.InputConstants;
import com.talhanation.workers.client.events.KeyEvents;
import com.talhanation.workers.init.ModBlocks;
import com.talhanation.workers.init.ModEntityTypes;
import com.talhanation.workers.init.ModItems;
import com.talhanation.workers.init.ModMenuTypes;
import com.talhanation.workers.init.ModPois;
import com.talhanation.workers.init.ModProfessions;
import com.talhanation.workers.init.ModShortcuts;
import com.talhanation.workers.network.MessageAnimalCount;
import com.talhanation.workers.network.MessageBed;
import com.talhanation.workers.network.MessageChest;
import com.talhanation.workers.network.MessageHire;
import com.talhanation.workers.network.MessageHireGui;
import com.talhanation.workers.network.MessageHomePos;
import com.talhanation.workers.network.MessageMineDepth;
import com.talhanation.workers.network.MessageMineType;
import com.talhanation.workers.network.MessageOpenGuiAnimalFarmer;
import com.talhanation.workers.network.MessageOpenGuiMerchant;
import com.talhanation.workers.network.MessageOpenGuiMiner;
import com.talhanation.workers.network.MessageOpenGuiWorker;
import com.talhanation.workers.network.MessageStartPos;
import com.talhanation.workers.network.MessageTradeButton;

import de.maxhenkel.corelib.CommonRegistry;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
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

    public Main() {
        // ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON,
        // workersModConfig.CONFIG);
        // workersModConfig.loadConfig(workersModConfig.CONFIG,
        // FMLPaths.CONFIGDIR.get().resolve("workers-common.toml"));

        final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::setup);
        ModBlocks.BLOCKS.register(modEventBus);
        ModPois.POIS.register(modEventBus);
        ModProfessions.PROFESSIONS.register(modEventBus);
        ModMenuTypes.MENU_TYPES.register(modEventBus);
        // ModSounds.SOUNDS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModEntityTypes.ENTITY_TYPES.register(modEventBus);

        modEventBus.addListener(this::clientSetup);
        modEventBus.addListener(ModShortcuts::registerBindings);

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new KeyEvents());
    }

    private void setup(final FMLCommonSetupEvent event) {
        LOGGER.info("FMLCommonSetupEvent triggered");
        MinecraftForge.EVENT_BUS.register(new VillagerEvents());
        MinecraftForge.EVENT_BUS.register(new CommandEvents());
        MinecraftForge.EVENT_BUS.register(this);

        SIMPLE_CHANNEL = CommonRegistry.registerChannel(Main.MOD_ID, "default");


        // CommonRegistry.registerMessage(SIMPLE_CHANNEL, 0, MessageStartPos.class);
        // CommonRegistry.registerMessage(SIMPLE_CHANNEL, 1, MessageOpenGuiMiner.class);
        // CommonRegistry.registerMessage(SIMPLE_CHANNEL, 2, MessageMineType.class);
        // CommonRegistry.registerMessage(SIMPLE_CHANNEL, 3, MessageMineDepth.class);
        // CommonRegistry.registerMessage(SIMPLE_CHANNEL, 4, MessageOpenGuiWorker.class);
        // CommonRegistry.registerMessage(SIMPLE_CHANNEL, 5, MessageHomePos.class);
        // CommonRegistry.registerMessage(SIMPLE_CHANNEL, 6, MessageOpenGuiMerchant.class);
        // CommonRegistry.registerMessage(SIMPLE_CHANNEL, 7, MessageTradeButton.class);
        // CommonRegistry.registerMessage(SIMPLE_CHANNEL, 8, MessageOpenGuiAnimalFarmer.class);
        // CommonRegistry.registerMessage(SIMPLE_CHANNEL, 9, MessageAnimalCount.class);
        // CommonRegistry.registerMessage(SIMPLE_CHANNEL, 10, MessageHire.class);
        // CommonRegistry.registerMessage(SIMPLE_CHANNEL, 11, MessageHireGui.class);
        Class[] messages = {
            MessageStartPos.class,
            MessageOpenGuiMiner.class,
            MessageMineType.class,
            MessageMineDepth.class,
            MessageOpenGuiWorker.class,
            MessageHomePos.class,
            MessageOpenGuiMerchant.class,
            MessageTradeButton.class,
            MessageOpenGuiAnimalFarmer.class,
            MessageAnimalCount.class,
            MessageHire.class,
            MessageHireGui.class,
            MessageChest.class,
            MessageBed.class
        };
        for (int i = 0; i < messages.length; i++) {
            Class<Message> message = messages[i];
            CommonRegistry.registerMessage(SIMPLE_CHANNEL, i, message);
        }

        LOGGER.info("Messages registered");
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ModMenuTypes.registerMenus());
    }
}
