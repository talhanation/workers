package com.talhanation.workers.network;

import com.talhanation.workers.Main;
import com.talhanation.workers.entities.MerchantEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;

public class MessageMerchantResetCurrentTradeCounts implements Message<MessageMerchantResetCurrentTradeCounts> {
    private UUID worker;
    private int index;

    public MessageMerchantResetCurrentTradeCounts() {
    }

    public MessageMerchantResetCurrentTradeCounts(UUID worker, int index) {
        this.worker = worker;
        this.index = index;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {

        ServerPlayer player = context.getSender();
        player.level.getEntitiesOfClass(MerchantEntity.class, player.getBoundingBox()
                        .inflate(16.0D), v -> v
                        .getUUID()
                        .equals(this.worker))
                .stream()
                .filter(MerchantEntity::isAlive)
                .findAny()
                .ifPresent(merchant -> this.resetCurrent(player, merchant));
    }

    private void resetCurrent(ServerPlayer player, MerchantEntity merchant){
        merchant.setCurrentTrades(index, 0);
        Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player), new MessageToClientUpdateMerchantScreen(merchant.WAYPOINTS, merchant.WAYPOINT_ITEMS, merchant.getCurrentTrades(), merchant.getTradeLimits(),merchant.getTraveling(), merchant.getReturning()));
    }

    public MessageMerchantResetCurrentTradeCounts fromBytes(FriendlyByteBuf buf) {
        this.worker = buf.readUUID();
        this.index = buf.readInt();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.worker);
        buf.writeInt(this.index);
    }

}