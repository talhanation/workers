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

    public MessageMerchantResetCurrentTradeCounts() {
    }

    public MessageMerchantResetCurrentTradeCounts(UUID worker) {
        this.worker = worker;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {

        ServerPlayer player = context.getSender();
        player.getCommandSenderWorld().getEntitiesOfClass(MerchantEntity.class, player.getBoundingBox()
                        .inflate(16.0D), v -> v
                        .getUUID()
                        .equals(this.worker))
                .stream()
                .filter(MerchantEntity::isAlive)
                .findAny()
                .ifPresent(merchant -> this.resetCurrent(player, merchant));
    }

    private void resetCurrent(ServerPlayer player, MerchantEntity merchant){
        for(int i = 0; i < 4; i++)
            merchant.setCurrentTrades(i, 0);

        Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player), new MessageToClientUpdateMerchantScreen(merchant.WAYPOINTS, merchant.WAYPOINT_ITEMS, merchant.getCurrentTrades(), merchant.getTradeLimits(),merchant.getTraveling(), merchant.getReturning()));
    }

    public MessageMerchantResetCurrentTradeCounts fromBytes(FriendlyByteBuf buf) {
        this.worker = buf.readUUID();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.worker);
    }

}