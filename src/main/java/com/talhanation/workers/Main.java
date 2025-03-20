package com.talhanation.workers;

import com.google.common.collect.ImmutableSet;
import com.talhanation.workers.client.gui.*;
import com.talhanation.workers.commands.MerchantResetCommand;
import com.talhanation.workers.config.WorkersModConfig;
import com.talhanation.workers.entities.AbstractWorkerEntity;
import com.talhanation.workers.entities.MerchantEntity;
import com.talhanation.workers.entities.MinerEntity;
import com.talhanation.workers.inventory.*;
import com.talhanation.workers.network.*;
import de.maxhenkel.corelib.ClientRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.IContainerFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.talhanation.workers.client.events.KeyEvents;
import com.talhanation.workers.init.ModBlocks;
import com.talhanation.workers.init.ModEntityTypes;
import com.talhanation.workers.init.ModItems;

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
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

@Mod(Main.MOD_ID)
public class Main {
    public static final String MOD_ID = "workers";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    public static SimpleChannel SIMPLE_CHANNEL;
    public static VillagerProfession MINER;
    public static VillagerProfession LUMBERJACK;
    public static VillagerProfession FARMER;
    public static VillagerProfession MERCHANT;
    public static VillagerProfession SHEPHERD;
    public static VillagerProfession FISHER;
    public static VillagerProfession CATTLE_FARMER;
    public static VillagerProfession CHICKEN_FARMER;
    public static VillagerProfession SWINEHERD;
    public static PoiType POI_MINER;
    public static PoiType POI_LUMBERJACK;
    public static PoiType POI_FARMER;
    public static PoiType POI_MERCHANT;
    public static PoiType POI_SHEPHERD;
    public static PoiType POI_FISHER;
    public static PoiType POI_CATTLE_FARMER;
    public static PoiType POI_CHICKEN_FARMER;
    public static PoiType POI_SWINEHERD;

    public static KeyMapping OPEN_COMMAND_SCREEN;
    public static MenuType<CommandMenu> COMMAND_CONTAINER_TYPE;
    public static MenuType<WorkerHireContainer> HIRE_CONTAINER_TYPE;
    public static MenuType<WorkerInventoryContainer> MINER_CONTAINER_TYPE;
    public static MenuType<WorkerInventoryContainer> ANIMAL_FARMER_CONTAINER_TYPE;
    public static MenuType<WorkerInventoryContainer> WORKER_CONTAINER_TYPE;
    public static MenuType<MerchantTradeContainer> MERCHANT_TRADE_CONTAINER_TYPE;
    public static MenuType<MerchantOwnerContainer> MERCHANT_OWNER_CONTAINER_TYPE;
    public static MenuType<MerchantWaypointContainer> MERCHANT_WAYPOINT_CONTAINER_TYPE;
    public static MenuType<MerchantInventoryContainer> MERCHANT_INVENTORY_CONTAINER_TYPE;
    public static boolean isSmallShipsInstalled;

    public Main() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, WorkersModConfig.CONFIG);
        WorkersModConfig.loadConfig(WorkersModConfig.CONFIG, FMLPaths.CONFIGDIR.get().resolve("workers-common.toml"));

        final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::setup);
        modEventBus.addGenericListener(PoiType.class, this::registerPointsOfInterest);
        modEventBus.addGenericListener(VillagerProfession.class, this::registerVillagerProfessions);
        modEventBus.addGenericListener(MenuType.class, this::registerContainers);
        ModItems.ITEMS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModEntityTypes.ENTITY_TYPES.register(modEventBus);
        //ModSounds.SOUNDS.register(modEventBus);
        MinecraftForge.EVENT_BUS.register(this);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> FMLJavaModLoadingContext.get().getModEventBus().addListener(Main.this::clientSetup));
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        MerchantResetCommand.register(event.getDispatcher());
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
            MessageLumberjackReplant.class,
            MessageToClientUpdateHireScreen.class,
            MessageOpenOwnerGuiMerchant.class,
            MessageWriteSpawnEgg.class
        };
        for (int i = 0; i < messages.length; i++) CommonRegistry.registerMessage(SIMPLE_CHANNEL, i, messages[i]);
        LOGGER.info("Villager Workers Messages registered");

        isSmallShipsInstalled = ModList.get().isLoaded("smallships");//Smallships mod
    }
    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void clientSetup(FMLClientSetupEvent event) {

        MinecraftForge.EVENT_BUS.register(new KeyEvents());

        OPEN_COMMAND_SCREEN = ClientRegistry.registerKeyBinding("key.workers.open_command_screen", "category.workers", GLFW.GLFW_KEY_X);

        ClientRegistry.registerScreen(Main.MINER_CONTAINER_TYPE, MinerInventoryScreen::new);
        ClientRegistry.registerScreen(Main.WORKER_CONTAINER_TYPE, WorkerInventoryScreen::new);
        ClientRegistry.registerScreen(Main.MERCHANT_TRADE_CONTAINER_TYPE, MerchantTradeScreen::new);
        ClientRegistry.registerScreen(Main.MERCHANT_OWNER_CONTAINER_TYPE, MerchantOwnerScreen::new);
        ClientRegistry.registerScreen(Main.ANIMAL_FARMER_CONTAINER_TYPE, AnimalFarmerInventoryScreen::new);
        ClientRegistry.registerScreen(Main.HIRE_CONTAINER_TYPE, WorkerHireScreen::new);
        ClientRegistry.registerScreen(Main.COMMAND_CONTAINER_TYPE, CommandScreen::new);
        ClientRegistry.registerScreen(Main.MERCHANT_WAYPOINT_CONTAINER_TYPE, MerchantWaypointScreen::new);
        ClientRegistry.registerScreen(Main.MERCHANT_INVENTORY_CONTAINER_TYPE, MerchantInventoryScreen::new);
    }

    @SubscribeEvent
    public void registerPointsOfInterest(RegistryEvent.Register<PoiType> event) {
        POI_MINER = new PoiType("poi_miner", PoiType.getBlockStates(ModBlocks.MINER_BLOCK.get()), 1, 1);
        POI_MINER.setRegistryName(Main.MOD_ID, "poi_miner");
        POI_LUMBERJACK = new PoiType("poi_lumberjack", PoiType.getBlockStates(ModBlocks.LUMBERJACK_BLOCK.get()), 1, 1);
        POI_LUMBERJACK.setRegistryName(Main.MOD_ID, "poi_lumberjack");
        POI_FISHER = new PoiType("poi_fisher", PoiType.getBlockStates(ModBlocks.FISHER_BLOCK.get()), 1, 1);
        POI_FISHER.setRegistryName(Main.MOD_ID, "poi_fisher");
        POI_FARMER = new PoiType("poi_farmer", PoiType.getBlockStates(ModBlocks.FARMER_BLOCK.get()), 1, 1);
        POI_FARMER.setRegistryName(Main.MOD_ID, "poi_farmer");
        POI_MERCHANT = new PoiType("poi_merchant", PoiType.getBlockStates(ModBlocks.MERCHANT_BLOCK.get()), 1, 1);
        POI_MERCHANT.setRegistryName(Main.MOD_ID, "poi_merchant");
        POI_SHEPHERD = new PoiType("poi_shepherd", PoiType.getBlockStates(ModBlocks.SHEPHERD_BLOCK.get()), 1, 1);
        POI_SHEPHERD.setRegistryName(Main.MOD_ID, "poi_shepherd");

        POI_CATTLE_FARMER = new PoiType("poi_cattle_farmer", PoiType.getBlockStates(ModBlocks.CATTLE_FARMER_BLOCK.get()), 1, 1);
        POI_CATTLE_FARMER.setRegistryName(Main.MOD_ID, "poi_cattle_farmer");
        POI_CHICKEN_FARMER = new PoiType("poi_chicken_farmer", PoiType.getBlockStates(ModBlocks.CHICKEN_FARMER_BLOCK.get()), 1, 1);
        POI_CHICKEN_FARMER.setRegistryName(Main.MOD_ID, "poi_chicken_farmer");
        POI_SWINEHERD = new PoiType("poi_swineherd", PoiType.getBlockStates(ModBlocks.SWINEHERD_BLOCK.get()), 1, 1);
        POI_SWINEHERD.setRegistryName(Main.MOD_ID, "poi_swineherd");

        event.getRegistry().register(POI_MINER);
        event.getRegistry().register(POI_LUMBERJACK);
        event.getRegistry().register(POI_FISHER);
        event.getRegistry().register(POI_FARMER);
        event.getRegistry().register(POI_MERCHANT);
        event.getRegistry().register(POI_SHEPHERD);
        event.getRegistry().register(POI_CATTLE_FARMER);
        event.getRegistry().register(POI_CHICKEN_FARMER);
        event.getRegistry().register(POI_SWINEHERD);
    }

    @SubscribeEvent
    public void registerVillagerProfessions(RegistryEvent.Register<VillagerProfession> event) {
        MINER = new VillagerProfession("miner", POI_MINER, ImmutableSet.of(), ImmutableSet.of(), SoundEvents.VILLAGER_CELEBRATE);
        MINER.setRegistryName(Main.MOD_ID, "miner");
        LUMBERJACK = new VillagerProfession("lumberjack", POI_LUMBERJACK, ImmutableSet.of(), ImmutableSet.of(), SoundEvents.VILLAGER_CELEBRATE);
        LUMBERJACK.setRegistryName(Main.MOD_ID, "lumberjack");
        FISHER = new VillagerProfession("fisher", POI_FISHER, ImmutableSet.of(), ImmutableSet.of(), SoundEvents.VILLAGER_CELEBRATE);
        FISHER.setRegistryName(Main.MOD_ID, "fisher");
        SHEPHERD = new VillagerProfession("shepherd", POI_SHEPHERD, ImmutableSet.of(), ImmutableSet.of(), SoundEvents.VILLAGER_CELEBRATE);
        SHEPHERD.setRegistryName(Main.MOD_ID, "shepherd");
        FARMER = new VillagerProfession("farmer", POI_FARMER, ImmutableSet.of(), ImmutableSet.of(), SoundEvents.VILLAGER_CELEBRATE);
        FARMER.setRegistryName(Main.MOD_ID, "farmer");
        MERCHANT = new VillagerProfession("merchant", POI_MERCHANT, ImmutableSet.of(), ImmutableSet.of(), SoundEvents.VILLAGER_CELEBRATE);
        MERCHANT.setRegistryName(Main.MOD_ID, "merchant");
        CATTLE_FARMER = new VillagerProfession("cattle_farmer", POI_CATTLE_FARMER, ImmutableSet.of(), ImmutableSet.of(), SoundEvents.VILLAGER_CELEBRATE);
        CATTLE_FARMER.setRegistryName(Main.MOD_ID, "cattle_farmer");
        CHICKEN_FARMER = new VillagerProfession("chicken_farmer", POI_CHICKEN_FARMER, ImmutableSet.of(), ImmutableSet.of(), SoundEvents.VILLAGER_CELEBRATE);
        CHICKEN_FARMER.setRegistryName(Main.MOD_ID, "chicken_farmer");
        SWINEHERD = new VillagerProfession("swineherd", POI_SWINEHERD, ImmutableSet.of(), ImmutableSet.of(), SoundEvents.VILLAGER_CELEBRATE);
        SWINEHERD.setRegistryName(Main.MOD_ID, "swineherd");

        event.getRegistry().register(MINER);
        event.getRegistry().register(LUMBERJACK);
        event.getRegistry().register(FISHER);
        event.getRegistry().register(MERCHANT);
        event.getRegistry().register(FARMER);
        event.getRegistry().register(SHEPHERD);
        event.getRegistry().register(CATTLE_FARMER);
        event.getRegistry().register(CHICKEN_FARMER);
        event.getRegistry().register(SWINEHERD);
    }

    @SubscribeEvent
    public void registerContainers(RegistryEvent.Register<MenuType<?>> event) {
        MINER_CONTAINER_TYPE = new MenuType<>((IContainerFactory<WorkerInventoryContainer>) (windowId, inv, data) -> {
            MinerEntity rec = (MinerEntity) getRecruitByUUID(inv.player, data.readUUID());
            if (rec == null) {
                return null;
            }
            return new WorkerInventoryContainer(windowId, rec, inv);
        });
        WORKER_CONTAINER_TYPE = new MenuType<>((IContainerFactory<WorkerInventoryContainer>) (windowId, inv, data) -> {
            AbstractWorkerEntity rec = getRecruitByUUID(inv.player, data.readUUID());
            if (rec == null) {
                return null;
            }
            return new WorkerInventoryContainer(windowId, rec, inv);
        });
        MERCHANT_TRADE_CONTAINER_TYPE = new MenuType<>((IContainerFactory<MerchantTradeContainer>) (windowId, inv, data) -> {
            MerchantEntity rec = (MerchantEntity) getRecruitByUUID(inv.player, data.readUUID());
            if (rec == null) {
                return null;
            }
            return new MerchantTradeContainer(windowId, rec, inv);
        });
        MERCHANT_OWNER_CONTAINER_TYPE = new MenuType<>((IContainerFactory<MerchantOwnerContainer>) (windowId, inv, data) -> {
            MerchantEntity rec = (MerchantEntity) getRecruitByUUID(inv.player, data.readUUID());
            if (rec == null) {
                return null;
            }
            return new MerchantOwnerContainer(windowId, rec, inv);
        });

        MERCHANT_INVENTORY_CONTAINER_TYPE = new MenuType<>((IContainerFactory<MerchantInventoryContainer>) (windowId, inv, data) -> {
            MerchantEntity rec = (MerchantEntity) getRecruitByUUID(inv.player, data.readUUID());
            if (rec == null) {
                return null;
            }
            return new MerchantInventoryContainer(windowId, rec, inv);
        });

        ANIMAL_FARMER_CONTAINER_TYPE = new MenuType<>((IContainerFactory<WorkerInventoryContainer>) (windowId, inv, data) -> {
            AbstractWorkerEntity rec = getRecruitByUUID(inv.player, data.readUUID());
            if (rec == null) {
                return null;
            }
            return new WorkerInventoryContainer(windowId, rec, inv);
        });

        COMMAND_CONTAINER_TYPE = new MenuType<>((IContainerFactory<CommandMenu>) (windowId, inv, data) -> {
            Player playerEntity = inv.player;
            if (playerEntity == null) {
                return null;
            }
            return new CommandMenu(windowId, playerEntity);
        });

        HIRE_CONTAINER_TYPE = new MenuType<>((IContainerFactory<WorkerHireContainer>) (windowId, inv, data) -> {
            AbstractWorkerEntity rec = getRecruitByUUID(inv.player, data.readUUID());
            if (rec == null) {
                return null;
            }
            return new WorkerHireContainer(windowId, inv.player, rec, inv);
        });

        MERCHANT_WAYPOINT_CONTAINER_TYPE = new MenuType<>((IContainerFactory<MerchantWaypointContainer>) (windowId, inv, data) -> {
            AbstractWorkerEntity rec = getRecruitByUUID(inv.player, data.readUUID());
            if (rec == null) {
                return null;
            }
            return new MerchantWaypointContainer(windowId, inv.player, rec, inv);
        });


        MINER_CONTAINER_TYPE.setRegistryName(new ResourceLocation(Main.MOD_ID, "miner_container"));
        event.getRegistry().register(MINER_CONTAINER_TYPE);

        WORKER_CONTAINER_TYPE.setRegistryName(new ResourceLocation(Main.MOD_ID, "worker_container"));
        event.getRegistry().register(WORKER_CONTAINER_TYPE);

        MERCHANT_TRADE_CONTAINER_TYPE.setRegistryName(new ResourceLocation(Main.MOD_ID, "merchant_container"));
        event.getRegistry().register(MERCHANT_TRADE_CONTAINER_TYPE);

        MERCHANT_OWNER_CONTAINER_TYPE.setRegistryName(new ResourceLocation(Main.MOD_ID, "merchant_owner_container"));
        event.getRegistry().register(MERCHANT_OWNER_CONTAINER_TYPE);

        ANIMAL_FARMER_CONTAINER_TYPE.setRegistryName(new ResourceLocation(Main.MOD_ID, "animal_farmer_container"));
        event.getRegistry().register(ANIMAL_FARMER_CONTAINER_TYPE);

        HIRE_CONTAINER_TYPE.setRegistryName(new ResourceLocation(Main.MOD_ID, "hire_container"));
        event.getRegistry().register(HIRE_CONTAINER_TYPE);

        COMMAND_CONTAINER_TYPE.setRegistryName(new ResourceLocation(Main.MOD_ID, "command_container"));
        event.getRegistry().register(COMMAND_CONTAINER_TYPE);

        MERCHANT_WAYPOINT_CONTAINER_TYPE.setRegistryName(new ResourceLocation(Main.MOD_ID, "merchant_waypoint_container"));
        event.getRegistry().register(MERCHANT_WAYPOINT_CONTAINER_TYPE);

        MERCHANT_INVENTORY_CONTAINER_TYPE.setRegistryName(new ResourceLocation(Main.MOD_ID, "merchant_inventory_container"));
        event.getRegistry().register(MERCHANT_INVENTORY_CONTAINER_TYPE);
    }

    @Nullable
    public static AbstractWorkerEntity getRecruitByUUID(Player player, UUID uuid) {
        double distance = 10D;
        return player.level.getEntitiesOfClass(AbstractWorkerEntity.class, new AABB(player.getX() - distance, player.getY() - distance, player.getZ() - distance, player.getX() + distance, player.getY() + distance, player.getZ() + distance), entity -> entity.getUUID().equals(uuid)).stream().findAny().orElse(null);
    }
}
