package com.talhanation.workers.network;

import com.talhanation.workers.entities.workarea.MarketArea;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public class MessageUpdateMarketArea implements Message<MessageUpdateMarketArea> {

    public UUID uuid;
    public boolean isOpen;
    public String marketName;
    public boolean useStorage;
    public int currencyReserve;
    public boolean acceptForeign;

    public MessageUpdateMarketArea() {}

    public MessageUpdateMarketArea(UUID uuid, boolean isOpen, String marketName, boolean useStorage, int currencyReserve, boolean acceptForeign) {
        this.uuid = uuid;
        this.isOpen = isOpen;
        this.marketName = marketName;
        this.useStorage = useStorage;
        this.currencyReserve = currencyReserve;
        this.acceptForeign = acceptForeign;
    }

    @Override
    public PacketFlow getExecutingSide() { return PacketFlow.SERVERBOUND; }

    @Override
    public void executeServerSide(IPayloadContext context) {
        ServerPlayer player = ((ServerPlayer) context.player());
        if (player == null) return;

        player.getCommandSenderWorld()
                .getEntitiesOfClass(MarketArea.class, player.getBoundingBox().inflate(64),
                        v -> v.getUUID().equals(this.uuid))
                .stream().findAny()
                .ifPresent(market -> {
                    market.setOpen(isOpen);
                    market.setMarketName(marketName);
                    market.setUseStorage(useStorage);
                    market.setCurrencyReserve(currencyReserve);
                    market.setAcceptForeignMerchants(acceptForeign);
                });
    }

    @Override
    public MessageUpdateMarketArea fromBytes(RegistryFriendlyByteBuf buf) {
        this.uuid = buf.readUUID();
        this.isOpen = buf.readBoolean();
        this.marketName = buf.readUtf();
        this.useStorage = buf.readBoolean();
        this.currencyReserve = buf.readInt();
        this.acceptForeign = buf.readBoolean();
        return this;
    }

    @Override
    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(uuid);
        buf.writeBoolean(isOpen);
        buf.writeUtf(marketName);
        buf.writeBoolean(useStorage);
        buf.writeInt(currencyReserve);
        buf.writeBoolean(acceptForeign);
    }

    public static final CustomPacketPayload.Type<MessageUpdateMarketArea> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("workers", "message_update_market_area"));

    @Override
    public CustomPacketPayload.Type<MessageUpdateMarketArea> type() {
        return TYPE;
    }
}
