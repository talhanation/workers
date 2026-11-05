package com.talhanation.workers.network;

import com.talhanation.workers.CommandEvents;
import com.talhanation.workers.entities.MerchantEntity;
import com.talhanation.workers.world.WorkersMerchantTrade;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public class MessageOpenMerchantEditTradeScreen implements Message<MessageOpenMerchantEditTradeScreen> {
    private UUID player;
    private UUID merchantUuid;
    private CompoundTag nbt;
    public MessageOpenMerchantEditTradeScreen() {
        this.player = new UUID(0L, 0L);
    }

    public MessageOpenMerchantEditTradeScreen(Player player, UUID merchantUuid, WorkersMerchantTrade trade) {
        this.player = player.getUUID();
        this.merchantUuid = merchantUuid;
        this.nbt = trade.toNbt(player.registryAccess());
    }
    @Override
    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }
    @Override
    public void executeServerSide(IPayloadContext context) {
        if (!((ServerPlayer) context.player()).getUUID().equals(player)) {
            return;
        }
        ServerPlayer player = ((ServerPlayer) context.player());
        player.getCommandSenderWorld().getEntitiesOfClass(MerchantEntity.class, player.getBoundingBox()
                        .inflate(32.0D), v -> v
                        .getUUID()
                        .equals(this.merchantUuid))
                .stream()
                .findAny()
                .ifPresent(merchant -> merchant.openAddEditTradeGUI(player, WorkersMerchantTrade.fromNbt(nbt, player.registryAccess())));
    }
    @Override
    public MessageOpenMerchantEditTradeScreen fromBytes(RegistryFriendlyByteBuf buf) {
        this.player = buf.readUUID();
        this.merchantUuid = buf.readUUID();
        this.nbt = buf.readNbt();
        return this;
    }
    @Override
    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(this.player);
        buf.writeUUID(this.merchantUuid);
        buf.writeNbt(this.nbt);
    }

    public static final CustomPacketPayload.Type<MessageOpenMerchantEditTradeScreen> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("workers", "message_open_merchant_edit_trade_screen"));

    @Override
    public CustomPacketPayload.Type<MessageOpenMerchantEditTradeScreen> type() {
        return TYPE;
    }
}

