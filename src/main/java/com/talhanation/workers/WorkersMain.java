package com.talhanation.workers;

import com.talhanation.recruits.client.events.CommandCategoryManager;
import com.talhanation.workers.client.events.ScreenEvents;
import com.talhanation.workers.client.gui.WorkerCommandScreen;
import com.talhanation.workers.config.WorkersServerConfig;
import com.talhanation.workers.network.*;
import com.talhanation.workers.network.MessageUpdateKitchenArea;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.config.ModConfig;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.world.entity.EntityType;
import com.talhanation.recruits.entities.AbstractInventoryEntity;
import com.talhanation.workers.world.StructureManager;
import com.talhanation.workers.world.WorkersRemoteTravel;

@Mod(WorkersMain.MOD_ID)
public class WorkersMain {
    public static final String MOD_ID = "workers";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    public static final SimpleChannel SIMPLE_CHANNEL = new SimpleChannel();

    public static boolean isDynamicTreesInstalled;
    public static boolean isFarmersDelightInstalled;

    public WorkersMain(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, WorkersServerConfig.SERVER);

        modEventBus.addListener(this::setup);
        modEventBus.addListener(this::registerPayloads);
        modEventBus.addListener(this::registerCapabilities);
        ModBlocks.BLOCKS.register(modEventBus);
        ModPois.POIS.register(modEventBus);
        ModProfessions.PROFESSIONS.register(modEventBus);
        ModMenuTypes.MENU_TYPES.register(modEventBus);
        // ModSounds.SOUNDS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModEntityTypes.ENTITY_TYPES.register(modEventBus);
        ModEntityTypes.WORKER_TYPES.register(modEventBus);

        modEventBus.addListener(this::addCreativeTabs);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(WorkersMain.this::clientSetup);
            modEventBus.addListener(ModMenuTypes::registerMenus);
        }

        NeoForge.EVENT_BUS.register(this);


        //Compat
        ModList modList = ModList.get();

        isDynamicTreesInstalled = modList.isLoaded("dynamictrees");
        isFarmersDelightInstalled = modList.isLoaded("farmersdelight");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        //MerchantResetCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        if (WorkersServerConfig.BuildModeConfig.get() == com.talhanation.workers.config.BuildMode.PRESET_FACTIONS) {
            java.io.File factionsDir = event.getServer().getServerDirectory().resolve("workers").resolve("scan").resolve("factions").toFile();
            if (!factionsDir.exists()) {
                factionsDir.mkdirs();
                LOGGER.info("[Workers] Created factions scan folder: {}", factionsDir.getAbsolutePath());
            }
        }
    }


    @SuppressWarnings({"unchecked", "rawtypes"})
    private void setup(final FMLCommonSetupEvent event) {
        NeoForge.EVENT_BUS.register(new VillagerEvents());
        // CommandEvents has no listeners, NeoForge throws when registering a class without @SubscribeEvent methods
        NeoForge.EVENT_BUS.register(new WorkerClaimEvents());
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(new UpdateChecker());
        NeoForge.EVENT_BUS.register(new WorkersRemoteTravel.Events());
        NeoForge.EVENT_BUS.register(new com.talhanation.workers.world.template.TemplateEvents());

    }

    private static final Class[] messages = {
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
                MessageOpenMerchantVillagerTradeScreen.class,
                MessageOpenWorkshopEditOrderScreen.class,
                MessageUpdateWorkshopOrder.class,
                MessageRequestCourierTargets.class,
                MessageToClientCourierTargets.class,
                MessageUpdateWorkAreaName.class,
                MessageUpdateWorkshopArea.class,
                MessageToClientVillagerPrices.class,
                MessageToClientOpenWorkshopOrders.class,
                MessageSetFarTrader.class,
                MessageRequestMerchantMemory.class,
                MessageToClientMerchantMemory.class,
                MessageUpdateMerchantMemory.class,
                MessageSetGhostItem.class,
                MessageSetWorkshopRecipe.class,
                MessageUpdateBuildAreaFoundation.class
        };

    // 1.20.1: AbstractInventoryEntity#getCapability served the item handler for every subclass, NeoForge needs it per entity type
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        for (DeferredHolder<EntityType<?>, ? extends EntityType<?>> holder : ModEntityTypes.WORKER_TYPES.getEntries()) {
            event.registerEntity(Capabilities.ItemHandler.ENTITY, (EntityType) holder.get(), (entity, context) -> ((AbstractInventoryEntity) entity).getItemHandler());
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void registerPayloads(final RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        for (int i = 0; i < messages.length; i++) CommonRegistry.registerMessage(registrar, messages[i]);
    }

    @OnlyIn(Dist.CLIENT)
    public void clientSetup(FMLClientSetupEvent event) {
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
            event.accept(ModItems.CRAFTSMAN_SPAWN_EGG.get());
            event.accept(ModItems.STOREKEEPER_SPAWN_EGG.get());
        }
    }

    // Forge SimpleChannel replacement, so all WorkersMain.SIMPLE_CHANNEL.sendToServer(...) calls stay as they are
    public static class SimpleChannel {
        public void sendToServer(CustomPacketPayload message) {
            PacketDistributor.sendToServer(message);
        }
    }
}