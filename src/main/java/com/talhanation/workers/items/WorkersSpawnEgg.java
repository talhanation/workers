package com.talhanation.workers.items;

import com.talhanation.workers.Main;
import com.talhanation.workers.entities.MerchantEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.PlayerTeam;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public class WorkersSpawnEgg extends SpawnEggItem {
    private final Supplier<EntityType<?>> entityType;

    public WorkersSpawnEgg(Supplier<EntityType<?>> entityType, int primaryColor, int secondaryColor, Properties properties){
        super(null, primaryColor, secondaryColor, properties);
        this.entityType = entityType;
    }
    @Override
    public @NotNull EntityType<?> getType(CompoundTag compound){
        if(compound != null && compound.contains("EntityTag", 10)) {
            CompoundTag entityTag = compound.getCompound("EntityTag");

            if(entityTag.contains("id", 8)) {
                return EntityType.byString(entityTag.getString("id")).orElse(this.entityType.get());
            }


        }
        return this.entityType.get();
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        else{
            ItemStack stack = context.getItemInHand();
            BlockPos pos = context.getClickedPos();
            EntityType<?> entitytype = this.getType(stack.getTag());
            Entity entity = entitytype.create(world);


            CompoundTag entityTag = stack.getTag();
            if(entity instanceof MerchantEntity merchant && entityTag != null) {
                CompoundTag nbt = entityTag.getCompound("EntityTag");


                if (nbt.contains("Team")) {
                    String s = nbt.getString("Team");
                    PlayerTeam playerteam = merchant.level.getScoreboard().getPlayerTeam(s);
                    boolean flag = playerteam != null && merchant.level.getScoreboard().addPlayerToTeam(merchant.getStringUUID(), playerteam);
                    if (!flag) {
                        Main.LOGGER.warn("Unable to add mob to team \"{}\" (that team probably doesn't exist)", (Object)s);
                    }
                }
                String name = nbt.getString("Name");
                merchant.setCustomName(new TextComponent(name));


                ListTag tradeList = nbt.getList("TradeInventory", 10);
                for (int i = 0; i < tradeList.size(); ++i) {
                    CompoundTag compoundnbt = tradeList.getCompound(i);
                    int j = compoundnbt.getByte("TradeSlot") & 255;

                    merchant.getTradeInventory().setItem(j, ItemStack.of(compoundnbt));
                }

                if (nbt.contains("HorseUUID")){
                    Optional<UUID> uuid = Optional.of(nbt.getUUID("HorseUUID"));
                    merchant.setHorseUUID(uuid);
                }

                if (nbt.contains("BoatUUID")){
                    Optional<UUID> uuid = Optional.of(nbt.getUUID("BoatUUID"));
                    merchant.setBoatUUID(uuid);
                }

                merchant.setTraveling(nbt.getBoolean("Traveling"));
                merchant.setAutoStartTravel(nbt.getBoolean("AutoStartTravel"));
                merchant.setReturning(nbt.getBoolean("Returning"));
                merchant.setCurrentWayPointIndex(nbt.getInt("CurrentWayPointIndex"));
                merchant.setReturningTime(nbt.getInt("ReturningTime"));
                merchant.setCurrentReturningTime(nbt.getInt("CurrentReturningTime"));

                merchant.setCreative(nbt.getBoolean("isCreative"));
                merchant.setIsDayCounted(nbt.getBoolean("isDayCounted"));

                BlockPos startPos = merchant.getNbtPosition(nbt, "CurrentWayPoint");
                if (startPos != null) merchant.setCurrentWayPoint(startPos);

                ListTag waypointItems = nbt.getList("WaypointItems", 10);
                for (int i = 0; i < waypointItems.size(); ++i) {
                    CompoundTag compoundnbt = waypointItems.getCompound(i);

                    ItemStack itemStack = ItemStack.of(compoundnbt);
                    merchant.WAYPOINT_ITEMS.add(itemStack);
                }

                ListTag waypoints = nbt.getList("Waypoints", 10);
                for (int i = 0; i < waypoints.size(); ++i) {
                    CompoundTag compoundnbt = waypoints.getCompound(i);
                    BlockPos pos1 = new BlockPos(
                            compoundnbt.getDouble("PosX"),
                            compoundnbt.getDouble("PosY"),
                            compoundnbt.getDouble("PosZ"));
                    merchant.WAYPOINTS.add(pos1);
                }

                ListTag limits = nbt.getList("TradeLimits", 10);
                if(!limits.isEmpty()){
                    for (int i = 0; i < 4; ++i) {
                        CompoundTag compoundnbt = limits.getCompound(i);
                        int limit = compoundnbt.getInt("Limit");

                        merchant.getTradeLimits().set(i, limit);
                    }
                }


                ListTag trades = nbt.getList("Trades", 10);
                if(!trades.isEmpty()) {
                    for (int i = 0; i < 4; ++i) {
                        CompoundTag compoundnbt = trades.getCompound(i);
                        int trade = compoundnbt.getInt("Trade");

                        merchant.getCurrentTrades().set(i, trade);
                    }
                }

                merchant.setState(nbt.getInt("State"));
                merchant.setTravelSpeedState(nbt.getInt("TravelSpeedState"));
                merchant.setSendInfo(nbt.getBoolean("InfoTravel"));

                ListTag listnbt = nbt.getList("Items", 10);//muss 10 sein amk sonst nix save
                //merchant.createInventory();
                merchant.setPersistenceRequired();

                for (int i = 0; i < listnbt.size(); ++i) {
                    CompoundTag compoundnbt = listnbt.getCompound(i);
                    int j = compoundnbt.getByte("Slot") & 255;
                    if (j < merchant.getInventory().getContainerSize()) {
                        merchant.getInventory().setItem(j, ItemStack.of(compoundnbt));
                    }
                }

                merchant.setPos(pos.getX() + 0.5, pos.getY() + 1 , pos.getZ() + 0.5);
                //if(recruit instanceof BowmanEntity bowman) bowman.reassessWeaponGoal();

                world.addFreshEntity(merchant);

                if (!context.getPlayer().isCreative()) {
                    stack.shrink(1);
                }
                return InteractionResult.SUCCESS;
            }
            else{
                return super.useOn(context);
            }
        }
    }
}