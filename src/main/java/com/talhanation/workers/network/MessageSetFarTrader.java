package com.talhanation.workers.network;

import com.talhanation.workers.entities.MerchantEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;

public class MessageSetFarTrader implements Message<MessageSetFarTrader> {

    private UUID merchantUuid;
    private boolean farTrader;

    public MessageSetFarTrader() {}

    public MessageSetFarTrader(UUID merchantUuid, boolean farTrader) {
        this.merchantUuid = merchantUuid;
        this.farTrader = farTrader;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    @Override
    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player == null) return;

        player.getCommandSenderWorld().getEntitiesOfClass(MerchantEntity.class, player.getBoundingBox().inflate(32.0D),
                        merchant -> merchant.getUUID().equals(this.merchantUuid) && player.getUUID().equals(merchant.getOwnerUUID()))
                .stream()
                .findAny()
                .ifPresent(merchant -> merchant.setFarTrader(farTrader));
    }

    @Override
    public MessageSetFarTrader fromBytes(FriendlyByteBuf buf) {
        this.merchantUuid = buf.readUUID();
        this.farTrader = buf.readBoolean();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(merchantUuid);
        buf.writeBoolean(farTrader);
    }
}
