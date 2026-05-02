package com.talhanation.workers;

import com.talhanation.workers.entities.ai.animals.WorkerTemptGoal;
import com.talhanation.workers.entities.workarea.MarketArea;
import com.talhanation.workers.init.ModEntityTypes;
import com.talhanation.recruits.world.RecruitsHireTradesRegistry;
import com.talhanation.workers.network.MessageToClientUpdateConfig;
import com.talhanation.recruits.world.RecruitsHireTrade;
import com.talhanation.workers.config.WorkersServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;
import java.util.UUID;

public class VillagerEvents {
    public static final Component TITLE_FARMER = Component.translatable("description.workers.title.farmer");
    public static final Component TITLE_MINER = Component.translatable("description.workers.title.miner");
    public static final Component TITLE_LUMBERJACK = Component.translatable("description.workers.title.lumberjack");
    public static final Component TITLE_BUILDER = Component.translatable("description.workers.title.builder");
    public static final Component TITLE_MERCHANT = Component.translatable("description.workers.title.merchant");
    public static final Component TITLE_FISHERMAN = Component.translatable("description.workers.title.fisherman");
    public static final Component TITLE_ANIMAL_FARMER = Component.translatable("description.workers.title.animalFarmer");
    public static final Component DESCRIPTION_FARMER = Component.translatable("description.workers.farmer");
    public static final Component DESCRIPTION_MINER = Component.translatable("description.workers.miner");
    public static final Component DESCRIPTION_LUMBERJACK = Component.translatable("description.workers.lumberjack");
    public static final Component DESCRIPTION_BUILDER = Component.translatable("description.workers.builder");
    public static final Component DESCRIPTION_MERCHANT = Component.translatable("description.workers.merchant");
    public static final Component DESCRIPTION_FISHERMAN = Component.translatable("description.workers.fisherman");
    public static final Component DESCRIPTION_ANIMAL_FARMER = Component.translatable("description.workers.animalFarmer");

    @SubscribeEvent
    public void onPlayerJoinWorld(EntityJoinLevelEvent event) {
        if(event.getLevel().isClientSide()) return;

        if(event.getEntity() instanceof ServerPlayer player){
                WorkersMain.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                        new MessageToClientUpdateConfig(
                                WorkersServerConfig.ShouldWorkAreaOnlyBeInFactionClaim.get(),
                                WorkersServerConfig.BuildModeConfig.get()
                        ));
        }
    }
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        RecruitsHireTrade FARMER = new RecruitsHireTrade(ModEntityTypes.FARMER.getId(), WorkersServerConfig.FarmerCost.get(), TITLE_FARMER, DESCRIPTION_FARMER);
        RecruitsHireTrade LUMBERJACK = new RecruitsHireTrade(ModEntityTypes.LUMBERJACK.getId(), WorkersServerConfig.LumberjackCost.get(), TITLE_LUMBERJACK, DESCRIPTION_LUMBERJACK);
        RecruitsHireTrade MINER = new RecruitsHireTrade(ModEntityTypes.MINER.getId(), WorkersServerConfig.MinerCost.get(), TITLE_MINER, DESCRIPTION_MINER);
        RecruitsHireTrade MERCHANT = new RecruitsHireTrade(ModEntityTypes.MERCHANT.getId(), WorkersServerConfig.MerchantCost.get(), TITLE_MERCHANT, DESCRIPTION_MERCHANT);
        RecruitsHireTrade BUILDER = new RecruitsHireTrade(ModEntityTypes.BUILDER.getId(), WorkersServerConfig.BuilderCost.get(), TITLE_BUILDER, DESCRIPTION_BUILDER);

        RecruitsHireTrade FISHERMAN = new RecruitsHireTrade(ModEntityTypes.FISHERMAN.getId(), WorkersServerConfig.BuilderCost.get(), TITLE_FISHERMAN, DESCRIPTION_FISHERMAN);

        RecruitsHireTrade ANIMAL_FARMER = new RecruitsHireTrade(ModEntityTypes.ANIMAL_FARMER.getId(), WorkersServerConfig.BuilderCost.get(), TITLE_ANIMAL_FARMER, DESCRIPTION_ANIMAL_FARMER);


        RecruitsHireTradesRegistry.addTrade("workers", 1, FARMER, LUMBERJACK);
        RecruitsHireTradesRegistry.addTrade("workers", 2, ANIMAL_FARMER);
        RecruitsHireTradesRegistry.addTrade("workers", 3, BUILDER);

        RecruitsHireTradesRegistry.addTrade("workers2", 1, FARMER, MINER);
        RecruitsHireTradesRegistry.addTrade("workers2", 2, ANIMAL_FARMER);
        RecruitsHireTradesRegistry.addTrade("workers2", 3, BUILDER);

        RecruitsHireTradesRegistry.addTrade("workers3", 1, FARMER, FISHERMAN);
        RecruitsHireTradesRegistry.addTrade("workers3", 2, ANIMAL_FARMER);
        RecruitsHireTradesRegistry.addTrade("workers3", 3, BUILDER);

        /*
        RecruitsHireTradesRegistry.addTrade("herd", 1, ANIMAL_FARMER, FISHERMAN);
        RecruitsHireTradesRegistry.addTrade("herd", 2, MERCHANT);
        RecruitsHireTradesRegistry.addTrade("herd", 3, CHEF);

        RecruitsHireTradesRegistry.addTrade("herd", 1, ANIMAL_FARMER, BEE_KEEPER);
        RecruitsHireTradesRegistry.addTrade("herd", 2, MERCHANT);
        RecruitsHireTradesRegistry.addTrade("herd", 3, CHEF);
        */
    }

    @SubscribeEvent
    public void onAnimalJoinWorld(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();

        if (entity instanceof Chicken chicken) {
            chicken.goalSelector.addGoal(3, new WorkerTemptGoal(chicken,1.0, Chicken.FOOD_ITEMS));
        }
        else if(entity instanceof Cow cow) {
            cow.goalSelector.addGoal(3, new WorkerTemptGoal(cow,1.0, Ingredient.of(Items.WHEAT)));
        }
        else if(entity instanceof Sheep sheep) {
            sheep.goalSelector.addGoal(3, new WorkerTemptGoal(sheep,1.0, Ingredient.of(Items.WHEAT)));
        }
        else if(entity instanceof Pig pig) {
            pig.goalSelector.addGoal(3, new WorkerTemptGoal(pig,1.0, Pig.FOOD_ITEMS));
        }
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent event) {
        if (event.getLevel() == null || event.getEntity() == null) return;

        if(!(event instanceof PlayerInteractEvent.RightClickBlock) && !(event instanceof PlayerInteractEvent.LeftClickBlock)) {
            return;
        }

        if(disableInteractionInMarketArea(event.getEntity(), event.getLevel(), event.getPos())){
            event.setCanceled(true);
        }

    }

    public static boolean disableInteractionInMarketArea(Player player, Level level, BlockPos pos) {
        List<MarketArea> markets = level.getEntitiesOfClass(MarketArea.class, new AABB(pos).inflate(8));
        if (markets.isEmpty()) return false;

        markets.removeIf(marketArea -> {
            AABB area = marketArea.getArea();
            return !(pos.getX() >= area.minX && pos.getX() <= area.maxX
                    && pos.getY() >= area.minY && pos.getY() <= area.maxY
                    && pos.getZ() >= area.minZ && pos.getZ() <= area.maxZ);
        });
        if (markets.isEmpty()) return false;

        boolean isContainer = level.getBlockEntity(pos) instanceof Container;
        if (!isContainer) return false;

        MarketArea market = markets.get(0);
        UUID ownerUUID = market.getPlayerUUID();

        boolean isOwner  = ownerUUID != null && player.getUUID().equals(ownerUUID);
        boolean isAdmin  = player.isCreative() && player.hasPermissions(2);

        return !isOwner && !isAdmin;
    }
}

