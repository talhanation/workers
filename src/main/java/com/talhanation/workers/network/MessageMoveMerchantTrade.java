package com.talhanation.workers.network;

import com.talhanation.workers.entities.MerchantEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public class MessageMoveMerchantTrade implements Message<MessageMoveMerchantTrade> {

    public UUID merchantUuid;
    public UUID tradeUuid;
    public boolean moveUp;

    public MessageMoveMerchantTrade() {}
    public MessageMoveMerchantTrade(UUID merchantUuid, UUID tradeUuid, boolean moveUp) {
        this.merchantUuid = merchantUuid;
        this.tradeUuid    = tradeUuid;
        this.moveUp       = moveUp;
    }

    @Override
    public PacketFlow getExecutingSide() { return PacketFlow.SERVERBOUND; }

    @Override
    public void executeServerSide(IPayloadContext context) {
        ServerPlayer player = ((ServerPlayer) context.player());
        if (player == null) return;

        player.getCommandSenderWorld()
                .getEntitiesOfClass(MerchantEntity.class,
                        player.getBoundingBox().inflate(32.0D),
                        v -> v.getUUID().equals(this.merchantUuid))
                .stream().findAny()
                .ifPresent(merchant -> {
                    if (!player.getUUID().equals(merchant.getOwnerUUID()) && !player.hasPermissions(2)) return;
                    if (moveUp) merchant.moveTradeUp(tradeUuid);
                    else        merchant.moveTradeDown(tradeUuid);
                });
    }

    @Override
    public MessageMoveMerchantTrade fromBytes(RegistryFriendlyByteBuf buf) {
        this.merchantUuid = buf.readUUID();
        this.tradeUuid    = buf.readUUID();
        this.moveUp       = buf.readBoolean();
        return this;
    }

    @Override
    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(merchantUuid);
        buf.writeUUID(tradeUuid);
        buf.writeBoolean(moveUp);
    }

    public static final CustomPacketPayload.Type<MessageMoveMerchantTrade> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("workers", "message_move_merchant_trade"));

    @Override
    public CustomPacketPayload.Type<MessageMoveMerchantTrade> type() {
        return TYPE;
    }
}
