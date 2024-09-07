package com.talhanation.workers.network;

import com.talhanation.workers.entities.MerchantEntity;
import com.talhanation.workers.init.ModItems;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.Team;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MessageWriteSpawnEgg implements Message<MessageWriteSpawnEgg> {

    private UUID merchant;

    public MessageWriteSpawnEgg(){
    }

    public MessageWriteSpawnEgg(UUID merchant) {
        this.merchant = merchant;

    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer serverPlayer = context.getSender();
        List<MerchantEntity> merchantlist = Objects.requireNonNull(context.getSender()).level.getEntitiesOfClass(MerchantEntity.class, context.getSender().getBoundingBox().inflate(64.0D));

        for (MerchantEntity merchant : merchantlist) {
            if(merchant.getUUID().equals(this.merchant)) {
                ItemStack itemStack = new ItemStack(ModItems.MERCHANT_SPAWN_EGG.get());

                CompoundTag entityTag = new CompoundTag();
                String name = merchant.getName().getString();
                Team team = merchant.getTeam();
                if(team != null){
                    entityTag.putString("Team", team.getName());
                }
                entityTag.putString("Name", name);

                ListTag list = new ListTag();
                for (int i = 0; i < merchant.getTradeInventory().getContainerSize(); ++i) {
                    ItemStack itemstack = merchant.getTradeInventory().getItem(i);
                    if (!itemstack.isEmpty()) {
                        CompoundTag compoundnbt = new CompoundTag();
                        compoundnbt.putByte("TradeSlot", (byte) i);
                        itemstack.save(compoundnbt);
                        list.add(compoundnbt);
                    }
                }
                entityTag.put("TradeInventory", list);

                if(merchant.getHorseUUID() != null){
                    entityTag.putUUID("HorseUUID", merchant.getHorseUUID());
                }

                if(merchant.getBoatUUID() != null){
                    entityTag.putUUID("BoatUUID", merchant.getBoatUUID());
                }

                entityTag.putBoolean("Traveling", merchant.getTraveling());
                entityTag.putBoolean("AutoStartTravel", merchant.getAutoStartTravel());
                entityTag.putBoolean("Returning", merchant.getReturning());
                entityTag.putInt("CurrentWayPointIndex", merchant.getCurrentWayPointIndex());
                entityTag.putInt("ReturningTime", merchant.getReturningTime());
                entityTag.putInt("CurrentReturningTime", merchant.getCurrentReturningTime());
                entityTag.putBoolean("isCreative", merchant.isCreative());
                entityTag.putBoolean("isDayCounted", merchant.isDayCounted());

                BlockPos currentWayPoint = merchant.getCurrentWayPoint();
                if (currentWayPoint != null) merchant.setNbtPosition(entityTag, "CurrentWayPoint", currentWayPoint);


                ListTag waypointItems = new ListTag();
                for (int i = 0; i < merchant.WAYPOINT_ITEMS.size(); ++i) {
                    ItemStack itemstack = merchant.WAYPOINT_ITEMS.get(i);
                    if (!itemstack.isEmpty()) {
                        CompoundTag compoundnbt = new CompoundTag();
                        compoundnbt.putByte("WaypointItem", (byte) i);
                        itemstack.save(compoundnbt);
                        waypointItems.add(compoundnbt);
                    }
                }
                entityTag.put("WaypointItems", waypointItems);

                ListTag waypoints = new ListTag();
                for(int i = 0; i < merchant.WAYPOINTS.size(); i++){
                    CompoundTag compoundnbt = new CompoundTag();
                    compoundnbt.putByte("Waypoint", (byte) i);
                    BlockPos pos = merchant.WAYPOINTS.get(i);
                    compoundnbt.putDouble("PosX", pos.getX());
                    compoundnbt.putDouble("PosY", pos.getY());
                    compoundnbt.putDouble("PosZ", pos.getZ());

                    waypoints.add(compoundnbt);
                }
                entityTag.put("Waypoints", waypoints);

                ListTag limits = new ListTag();
                for(int i = 0; i < 4; i++) {
                    CompoundTag compoundnbt = new CompoundTag();
                    compoundnbt.putByte("TradeLimit_" + i, (byte) i);

                    int limit = merchant.getTradeLimits().get(i);
                    compoundnbt.putInt("Limit", limit);

                    limits.add(compoundnbt);
                }
                entityTag.put("TradeLimits", limits);


                ListTag trades = new ListTag();
                for(int i = 0; i < 4; i++) {
                    CompoundTag compoundnbt = new CompoundTag();
                    compoundnbt.putByte("Trade_" + i, (byte) i);
                    int trade = merchant.getCurrentTrades().get(i);
                    compoundnbt.putInt("Trade", trade);

                    trades.add(compoundnbt);
                }
                entityTag.put("Trades", trades);

                entityTag.putInt("State", merchant.getState());
                entityTag.putInt("TravelSpeedState", merchant.getTravelSpeedState());
                entityTag.putBoolean("InfoTravel", merchant.getSendInfo());


                ListTag listnbt = new ListTag();
                for (int i = 0; i < merchant.getInventory().getContainerSize(); ++i) {
                    ItemStack itemstack = merchant.getInventory().getItem(i);
                    if (!itemstack.isEmpty()) {
                        CompoundTag compoundnbt = new CompoundTag();
                        compoundnbt.putByte("Slot", (byte) i);
                        itemstack.save(compoundnbt);
                        listnbt.add(compoundnbt);
                    }
                }
                entityTag.put("Items", listnbt);

                CompoundTag itemTag = new CompoundTag();
                itemTag.put("EntityTag", entityTag);

                if (itemStack != null && serverPlayer != null && serverPlayer.getMainHandItem().isEmpty()) {
                    itemStack.setTag(itemTag);
                    serverPlayer.setItemInHand(InteractionHand.MAIN_HAND, itemStack);
                }
                break;
            }
        }
    }
    public MessageWriteSpawnEgg fromBytes(FriendlyByteBuf buf) {
        this.merchant = buf.readUUID();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.merchant);
    }

}