package com.talhanation.workers;

import com.google.common.collect.ImmutableSet;
import com.talhanation.workers.client.events.KeyEvents;
import com.talhanation.workers.client.gui.MinerInventoryScreen;
import com.talhanation.workers.client.gui.WorkerInventoryScreen;
import com.talhanation.workers.entities.*;
import com.talhanation.workers.init.ModBlocks;
import com.talhanation.workers.init.ModEntityTypes;
import com.talhanation.workers.init.ModItems;
import com.talhanation.workers.network.*;
import de.maxhenkel.corelib.ClientRegistry;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.ai.attributes.GlobalEntityTypeAttributes;
import net.minecraft.entity.merchant.villager.VillagerProfession;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.village.PointOfInterestType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DeferredWorkQueue;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.IContainerFactory;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import javax.annotation.Nullable;
import java.util.UUID;

@Mod(Main.MOD_ID)
public class Main {
    public static final String MOD_ID = "workers";
    public static SimpleChannel SIMPLE_CHANNEL;
    public static VillagerProfession MINER;
    public static VillagerProfession LUMBERJACK;
    public static VillagerProfession FARMER;
    public static VillagerProfession FISCHER;
    public static PointOfInterestType POI_MINER;
    public static PointOfInterestType POI_LUMBERJACK;
    public static PointOfInterestType POI_FARMER;
    public static PointOfInterestType POI_FISCHER;
    public static KeyBinding R_KEY;
    public static KeyBinding X_KEY;
    public static KeyBinding C_KEY;
    public static KeyBinding Y_KEY;
    public static KeyBinding V_KEY;
    public static ContainerType<WorkerInventoryContainer> MINER_CONTAINER_TYPE;
    public static ContainerType<WorkerInventoryContainer> WORKER_CONTAINER_TYPE;

    public Main() {
        //ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, workersModConfig.CONFIG);
        //workersModConfig.loadConfig(workersModConfig.CONFIG, FMLPaths.CONFIGDIR.get().resolve("workers-common.toml"));

        final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::setup);
        modEventBus.addGenericListener(PointOfInterestType.class, this::registerPointsOfInterest);
        modEventBus.addGenericListener(VillagerProfession.class, this::registerVillagerProfessions);
        modEventBus.addGenericListener(ContainerType.class, this::registerContainers);
        ModBlocks.BLOCKS.register(modEventBus);
        //ModSounds.SOUNDS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModEntityTypes.ENTITY_TYPES.register(modEventBus);
        MinecraftForge.EVENT_BUS.register(this);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> FMLJavaModLoadingContext.get().getModEventBus().addListener(Main.this::clientSetup));
    }

    @SuppressWarnings("deprecation")
    private void setup(final FMLCommonSetupEvent event) {
        MinecraftForge.EVENT_BUS.register(new VillagerEvents());
        MinecraftForge.EVENT_BUS.register(new CommandEvents());
        MinecraftForge.EVENT_BUS.register(this);
        SIMPLE_CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(MOD_ID, "default"), () -> "1.0.0", s -> true, s -> true);


        SIMPLE_CHANNEL.registerMessage(0, MessageStartPos.class, MessageStartPos::toBytes,
                buf -> (new MessageStartPos()).fromBytes(buf),
                (msg, fun) -> msg.executeServerSide(fun.get()));

        SIMPLE_CHANNEL.registerMessage(1, MessageOpenGuiMiner.class, MessageOpenGuiMiner::toBytes,
                buf -> (new MessageOpenGuiMiner()).fromBytes(buf),
                (msg, fun) -> msg.executeServerSide(fun.get()));

        SIMPLE_CHANNEL.registerMessage(2, MessageMineType.class, MessageMineType::toBytes,
                buf -> (new MessageMineType()).fromBytes(buf),
                (msg, fun) -> msg.executeServerSide(fun.get()));

        SIMPLE_CHANNEL.registerMessage(3, MessageMineDepth.class, MessageMineDepth::toBytes,
                buf -> (new MessageMineDepth()).fromBytes(buf),
                (msg, fun) -> msg.executeServerSide(fun.get()));

        SIMPLE_CHANNEL.registerMessage(4, MessageOpenGuiWorker.class, MessageOpenGuiWorker::toBytes,
                buf -> (new MessageOpenGuiWorker()).fromBytes(buf),
                (msg, fun) -> msg.executeServerSide(fun.get()));


        DeferredWorkQueue.runLater(() -> {
            GlobalEntityTypeAttributes.put(ModEntityTypes.MINER.get(), MinerEntity.setAttributes().build());
            GlobalEntityTypeAttributes.put(ModEntityTypes.LUMBERJACK.get(), LumberjackEntity.setAttributes().build());
            GlobalEntityTypeAttributes.put(ModEntityTypes.SHEPHERD.get(), ShepherdEntity.setAttributes().build());
            GlobalEntityTypeAttributes.put(ModEntityTypes.FARMER.get(), FarmerEntity.setAttributes().build());
            GlobalEntityTypeAttributes.put(ModEntityTypes.FISHERMAN.get(), FishermanEntity.setAttributes().build());
        });
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void clientSetup(FMLClientSetupEvent event) {

        MinecraftForge.EVENT_BUS.register(new KeyEvents());

        //R_KEY = ClientRegistry.registerKeyBinding("key.r_key", "category.workers", 82);
        //X_KEY = ClientRegistry.registerKeyBinding("key.x_key", "category.workers", 88);
        C_KEY = ClientRegistry.registerKeyBinding("key.c_key", "category.workers", 67);
        //Y_KEY = ClientRegistry.registerKeyBinding("key.y_key", "category.workers", 90);
        //V_KEY = ClientRegistry.registerKeyBinding("key.v_key", "category.workers", 86);

        ClientRegistry.registerScreen(Main.MINER_CONTAINER_TYPE, MinerInventoryScreen::new);
        ClientRegistry.registerScreen(Main.WORKER_CONTAINER_TYPE, WorkerInventoryScreen::new);
    }

    @SubscribeEvent
    public void registerPointsOfInterest(RegistryEvent.Register<PointOfInterestType> event) {
        POI_MINER = new PointOfInterestType("poi_miner", PointOfInterestType.getBlockStates(ModBlocks.MINER_BLOCK.get()), 1, 1);
        POI_MINER.setRegistryName(Main.MOD_ID, "poi_miner");
        POI_LUMBERJACK = new PointOfInterestType("poi_lumberjack", PointOfInterestType.getBlockStates(ModBlocks.LUMBERJACK_BLOCK.get()), 1, 1);
        POI_LUMBERJACK.setRegistryName(Main.MOD_ID, "poi_lumberjack");


        event.getRegistry().register(POI_MINER);
        event.getRegistry().register(POI_LUMBERJACK);

    }

    @SubscribeEvent
    public void registerVillagerProfessions(RegistryEvent.Register<VillagerProfession> event) {
        MINER = new VillagerProfession("miner", POI_MINER, ImmutableSet.of(), ImmutableSet.of(), SoundEvents.VILLAGER_CELEBRATE);
        MINER.setRegistryName(Main.MOD_ID, "miner");
        LUMBERJACK = new VillagerProfession("lumberjack", POI_LUMBERJACK, ImmutableSet.of(), ImmutableSet.of(), SoundEvents.VILLAGER_CELEBRATE);
        LUMBERJACK.setRegistryName(Main.MOD_ID, "lumberjack");



        event.getRegistry().register(MINER);
        event.getRegistry().register(LUMBERJACK);
    }

    @SubscribeEvent
    public void registerContainers(RegistryEvent.Register<ContainerType<?>> event) {
        MINER_CONTAINER_TYPE = new ContainerType<>((IContainerFactory<WorkerInventoryContainer>) (windowId, inv, data) -> {
            MinerEntity rec = (MinerEntity) getRecruitByUUID(inv.player, data.readUUID());
            if (rec == null) {
                return null;
            }
            return new WorkerInventoryContainer(windowId, rec, inv);
        });
        WORKER_CONTAINER_TYPE = new ContainerType<>((IContainerFactory<WorkerInventoryContainer>) (windowId, inv, data) -> {
            LumberjackEntity rec = (LumberjackEntity) getRecruitByUUID(inv.player, data.readUUID());
            if (rec == null) {
                return null;
            }
            return new WorkerInventoryContainer(windowId, rec, inv);
        });

        MINER_CONTAINER_TYPE.setRegistryName(new ResourceLocation(Main.MOD_ID, "miner_container"));
        event.getRegistry().register(MINER_CONTAINER_TYPE);

        WORKER_CONTAINER_TYPE.setRegistryName(new ResourceLocation(Main.MOD_ID, "lumberjack_container"));
        event.getRegistry().register(WORKER_CONTAINER_TYPE);

    }

    @Nullable
    public static AbstractWorkerEntity getRecruitByUUID(PlayerEntity player, UUID uuid) {
        double distance = 10D;
        return player.level.getEntitiesOfClass(AbstractWorkerEntity.class, new AxisAlignedBB(player.getX() - distance, player.getY() - distance, player.getZ() - distance, player.getX() + distance, player.getY() + distance, player.getZ() + distance), entity -> entity.getUUID().equals(uuid)).stream().findAny().orElse(null);
    }
}
