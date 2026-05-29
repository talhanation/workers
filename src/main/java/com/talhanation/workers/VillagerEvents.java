package com.talhanation.workers;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import com.talhanation.workers.entities.ai.RecruitStorageUpkeepGoal;
import com.talhanation.workers.entities.ai.VillagerPickupFoodGoal;
import com.talhanation.workers.entities.ai.VillagerRespondToInvitationGoal;
import com.talhanation.workers.entities.ai.animals.WorkerTemptGoal;
import com.talhanation.workers.entities.workarea.AbstractWorkAreaEntity;
import com.talhanation.workers.entities.workarea.IPermissionArea;
import com.talhanation.workers.init.ModEntityTypes;
import com.talhanation.recruits.world.RecruitsHireTradesRegistry;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.ai.RecruitUpkeepEntityGoal;
import com.talhanation.workers.network.MessageToClientUpdateConfig;
import com.talhanation.recruits.world.RecruitsHireTrade;
import com.talhanation.workers.config.WorkersServerConfig;
import com.talhanation.workers.world.VillagerInviteRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.RegistryObject;

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
    public static final Component TITLE_COURIER = Component.translatable("description.workers.title.courier");
    public static final Component TITLE_COOK    = Component.translatable("description.workers.title.cook");
    public static final Component DESCRIPTION_FARMER = Component.translatable("description.workers.farmer");
    public static final Component DESCRIPTION_MINER = Component.translatable("description.workers.miner");
    public static final Component DESCRIPTION_LUMBERJACK = Component.translatable("description.workers.lumberjack");
    public static final Component DESCRIPTION_BUILDER = Component.translatable("description.workers.builder");
    public static final Component DESCRIPTION_MERCHANT = Component.translatable("description.workers.merchant");
    public static final Component DESCRIPTION_FISHERMAN = Component.translatable("description.workers.fisherman");
    public static final Component DESCRIPTION_ANIMAL_FARMER = Component.translatable("description.workers.animalFarmer");
    public static final Component DESCRIPTION_COURIER = Component.translatable("description.workers.courier");
    public static final Component DESCRIPTION_COOK    = Component.translatable("description.workers.cook");

    @SubscribeEvent
    public void onPlayerJoinWorld(EntityJoinLevelEvent event) {
        if(event.getLevel().isClientSide()) return;

        if(event.getEntity() instanceof ServerPlayer player){
                WorkersMain.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                        new MessageToClientUpdateConfig(
                                WorkersServerConfig.ShouldWorkAreaOnlyBeInFactionClaim.get(),
                                WorkersServerConfig.ShouldOnlyPlacingBuildingsBePossible.get(),
                                WorkersServerConfig.BuildModeConfig.get()
                        ));
        }
    }
    @SubscribeEvent
    public void onServerStart(ServerStartedEvent event) {
        registerWorkerTrades();
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void onPlayerLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        registerWorkerTrades();
    }

    public static void registerWorkerTrades(){
        RecruitsHireTrade FARMER = new RecruitsHireTrade(ModEntityTypes.FARMER.getId(), WorkersServerConfig.FarmerCost.get(), TITLE_FARMER, DESCRIPTION_FARMER);
        RecruitsHireTrade LUMBERJACK = new RecruitsHireTrade(ModEntityTypes.LUMBERJACK.getId(), WorkersServerConfig.LumberjackCost.get(), TITLE_LUMBERJACK, DESCRIPTION_LUMBERJACK);
        RecruitsHireTrade MINER = new RecruitsHireTrade(ModEntityTypes.MINER.getId(), WorkersServerConfig.MinerCost.get(), TITLE_MINER, DESCRIPTION_MINER);
        RecruitsHireTrade MERCHANT = new RecruitsHireTrade(ModEntityTypes.MERCHANT.getId(), WorkersServerConfig.MerchantCost.get(), TITLE_MERCHANT, DESCRIPTION_MERCHANT);
        RecruitsHireTrade BUILDER = new RecruitsHireTrade(ModEntityTypes.BUILDER.getId(), WorkersServerConfig.BuilderCost.get(), TITLE_BUILDER, DESCRIPTION_BUILDER);

        RecruitsHireTrade FISHERMAN = new RecruitsHireTrade(ModEntityTypes.FISHERMAN.getId(), WorkersServerConfig.BuilderCost.get(), TITLE_FISHERMAN, DESCRIPTION_FISHERMAN);

        RecruitsHireTrade ANIMAL_FARMER = new RecruitsHireTrade(ModEntityTypes.ANIMAL_FARMER.getId(), WorkersServerConfig.BuilderCost.get(), TITLE_ANIMAL_FARMER, DESCRIPTION_ANIMAL_FARMER);
        RecruitsHireTrade COURIER = new RecruitsHireTrade(ModEntityTypes.COURIER.getId(), WorkersServerConfig.CourierCost.get(), TITLE_COURIER, DESCRIPTION_COURIER);
        RecruitsHireTrade COOK    = new RecruitsHireTrade(ModEntityTypes.COOK.getId(), WorkersServerConfig.CookCost.get(), TITLE_COOK, DESCRIPTION_COOK);


        RecruitsHireTradesRegistry.addTrade("workers", 1, FARMER, LUMBERJACK);
        RecruitsHireTradesRegistry.addTrade("workers", 2, COURIER, COOK);
        RecruitsHireTradesRegistry.addTrade("workers", 3, MERCHANT, BUILDER);

        RecruitsHireTradesRegistry.addTrade("workers2", 1, FARMER, MINER);
        RecruitsHireTradesRegistry.addTrade("workers2", 2, COURIER);
        RecruitsHireTradesRegistry.addTrade("workers2", 3, MERCHANT, BUILDER);

        RecruitsHireTradesRegistry.addTrade("workers3", 1, FARMER, ANIMAL_FARMER);
        RecruitsHireTradesRegistry.addTrade("workers3", 2, COURIER, COOK);
        RecruitsHireTradesRegistry.addTrade("workers3", 3, MERCHANT, BUILDER);

        RecruitsHireTradesRegistry.addTrade("workers4", 1, FARMER, FISHERMAN);
        RecruitsHireTradesRegistry.addTrade("workers4", 2, COURIER, COOK);
        RecruitsHireTradesRegistry.addTrade("workers4", 3, MERCHANT, BUILDER);

        RecruitsHireTradesRegistry.addTrade("workers5", 1, MINER, LUMBERJACK);
        RecruitsHireTradesRegistry.addTrade("workers5", 2, BUILDER, COURIER);
        RecruitsHireTradesRegistry.addTrade("workers5", 3, MERCHANT);
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        VillagerInviteRegistry.clear();
    }

    @SubscribeEvent
    public void onAnimalJoinWorld(EntityJoinLevelEvent event) {
        if(event.getLevel().isClientSide()) return;

        Entity entity = event.getEntity();

        // Recruits (and workers, which extend AbstractRecruitEntity) can use a Workers StorageArea
        // as their upkeep source. We REPLACE the stock entity-upkeep goal with our subclass: it
        // defers to super for normal targets, but for a StorageArea it walks chest-to-chest.
        // Replacing (rather than adding a second goal) stops the stock goal from grabbing the
        // storage — which, being a Container, it would otherwise do and abort with "no food".
        if (entity instanceof AbstractRecruitEntity recruit) {
            recruit.goalSelector.removeAllGoals(g -> g instanceof RecruitUpkeepEntityGoal);
            recruit.goalSelector.addGoal(6, new RecruitStorageUpkeepGoal(recruit));
        }

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
        else if(entity instanceof Villager villager) {
            villager.goalSelector.addGoal(4, new VillagerRespondToInvitationGoal(villager));
            villager.goalSelector.addGoal(4, new VillagerPickupFoodGoal(villager));
        }
        else if(entity instanceof Allay allay){
            if (WorkersServerConfig.WorkersReplaceAllay.get()){
                createWorkerFromAllay(allay);
            }

        }
    }

    private void createWorkerFromAllay(Allay allay) {
        List<RegistryObject<EntityType<?>>> types = ModEntityTypes.WORKER_TYPES.getEntries().stream().toList();

        int rnd = allay.getRandom().nextInt(types.size() -1);
        var type = types.get(rnd).get().create(allay.getCommandSenderWorld());

        if(type instanceof AbstractWorkerEntity worker){
            worker.copyPosition(allay);

            worker.initSpawn();

            allay.remove(Entity.RemovalReason.DISCARDED);
            worker.getInventory().setItem(8, Items.BREAD.getDefaultInstance());
            allay.remove(Entity.RemovalReason.DISCARDED);
            allay.getCommandSenderWorld().addFreshEntity(worker);
        }
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent event) {
        if (event.getLevel() == null || event.getEntity() == null) return;

        if(!(event instanceof PlayerInteractEvent.RightClickBlock) && !(event instanceof PlayerInteractEvent.LeftClickBlock)) {
            return;
        }

        if(disableInteractionInPermissionArea(event.getEntity(), event.getLevel(), event.getPos())){
            event.setCanceled(true);
        }
    }

    public static boolean disableInteractionInPermissionArea(Player player, Level level, BlockPos pos) {
        List<AbstractWorkAreaEntity> areas = level.getEntitiesOfClass(AbstractWorkAreaEntity.class, new AABB(pos).inflate(8));
        if (areas.isEmpty()) return false;

        areas.removeIf(workArea -> !(workArea instanceof IPermissionArea));

        areas.removeIf(permissionArea -> {
            AABB area = permissionArea.getArea();
            return !(pos.getX() >= area.minX && pos.getX() <= area.maxX
                    && pos.getY() >= area.minY && pos.getY() <= area.maxY
                    && pos.getZ() >= area.minZ && pos.getZ() <= area.maxZ);
        });
        if (areas.isEmpty()) return false;

        boolean isContainer = level.getBlockEntity(pos) instanceof Container;
        if (!isContainer) return false;

        IPermissionArea permissionArea = (IPermissionArea) areas.get(0);
        UUID ownerUUID = permissionArea.getPlayerUUID();

        boolean isOwner  = ownerUUID != null && player.getUUID().equals(ownerUUID);
        boolean isTeamMate = permissionArea.getTeamAccess() && player.getTeam() != null && permissionArea.getTeamStringID().equals(player.getTeam().getName());
        boolean isAdmin  = player.isCreative() && player.hasPermissions(2);

        return !isOwner && !isTeamMate && !isAdmin;
    }
}