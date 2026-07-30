package com.talhanation.workers.network;


import com.talhanation.workers.entities.MerchantEntity;
import com.talhanation.workers.network.compat.WorkersMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.workers.network.compat.WorkersNetworkContext;

import java.util.UUID;

public class MessageOpenMerchantTradeScreen implements WorkersMessage<MessageOpenMerchantTradeScreen> {
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
    public void executeServerSide(WorkersNetworkContext context) {
        ServerPlayer player = context.getSender();
        player.getCommandSenderWorld().getEntitiesOfClass(MerchantEntity.class, player.getBoundingBox()
                        .inflate(32.0D), v -> v
                        .getUUID()
                        .equals(this.merchantUuid))
                .stream()
                .findAny()
                .ifPresent(merchant -> merchant.openTradeGUI(player));
    }
    @Override
    public MessageOpenMerchantTradeScreen fromBytes(FriendlyByteBuf buf) {
        this.player = buf.readUUID();
        this.merchantUuid = buf.readUUID();
        return this;
    }
    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.player);
        buf.writeUUID(this.merchantUuid);
    }
}

