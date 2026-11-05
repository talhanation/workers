package com.talhanation.workers.network;


import com.talhanation.workers.entities.MerchantEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public class MessageOpenMerchantTradeScreen implements Message<MessageOpenMerchantTradeScreen> {
    private UUID player;
    private UUID merchantUuid;
    public MessageOpenMerchantTradeScreen() {
        this.player = new UUID(0L, 0L);
    }

    public MessageOpenMerchantTradeScreen(Player player, UUID merchant) {
        this.player = player.getUUID();
        this.merchantUuid = merchant;
    }
    @Override
    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }
    @Override
    public void executeServerSide(IPayloadContext context) {
        ServerPlayer player = ((ServerPlayer) context.player());
        player.getCommandSenderWorld().getEntitiesOfClass(MerchantEntity.class, player.getBoundingBox()
                        .inflate(32.0D), v -> v
                        .getUUID()
                        .equals(this.merchantUuid))
                .stream()
                .findAny()
                .ifPresent(merchant -> merchant.openTradeGUI(player));
    }
    @Override
    public MessageOpenMerchantTradeScreen fromBytes(RegistryFriendlyByteBuf buf) {
        this.player = buf.readUUID();
        this.merchantUuid = buf.readUUID();
        return this;
    }
    @Override
    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(this.player);
        buf.writeUUID(this.merchantUuid);
    }

    public static final CustomPacketPayload.Type<MessageOpenMerchantTradeScreen> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("workers", "message_open_merchant_trade_screen"));

    @Override
    public CustomPacketPayload.Type<MessageOpenMerchantTradeScreen> type() {
        return TYPE;
    }
}

