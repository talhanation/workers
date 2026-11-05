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

public class MessageDoTradeWithMerchant implements Message<MessageDoTradeWithMerchant> {

    public UUID merchantUuid;
    public UUID trade;
    public MessageDoTradeWithMerchant() {}
    public MessageDoTradeWithMerchant(UUID merchantUuid, UUID trade) {
        this.merchantUuid = merchantUuid;
        this.trade = trade;
    }

    @Override
    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(IPayloadContext context){
        ServerPlayer player = ((ServerPlayer) context.player());
        if(player == null) return;

        player.getCommandSenderWorld().getEntitiesOfClass(MerchantEntity.class, player.getBoundingBox()
                        .inflate(32.0D), v -> v
                        .getUUID()
                        .equals(this.merchantUuid))
                .stream()
                .findAny()
                .ifPresent(merchant -> merchant.doTrade(trade, player));

    }

    public MessageDoTradeWithMerchant fromBytes(RegistryFriendlyByteBuf buf) {
        this.merchantUuid = buf.readUUID();
        this.trade = buf.readUUID();
        return this;
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(merchantUuid);
        buf.writeUUID(this.trade);
    }

    public static final CustomPacketPayload.Type<MessageDoTradeWithMerchant> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("workers", "message_do_trade_with_merchant"));

    @Override
    public CustomPacketPayload.Type<MessageDoTradeWithMerchant> type() {
        return TYPE;
    }
}
