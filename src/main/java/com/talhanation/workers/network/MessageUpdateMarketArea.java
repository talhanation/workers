package com.talhanation.workers.network;

import com.talhanation.workers.entities.workarea.MarketArea;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

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
    public Dist getExecutingSide() { return Dist.DEDICATED_SERVER; }

    @Override
    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
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
    public MessageUpdateMarketArea fromBytes(FriendlyByteBuf buf) {
        this.uuid = buf.readUUID();
        this.isOpen = buf.readBoolean();
        this.marketName = buf.readUtf();
        this.useStorage = buf.readBoolean();
        this.currencyReserve = buf.readInt();
        this.acceptForeign = buf.readBoolean();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(uuid);
        buf.writeBoolean(isOpen);
        buf.writeUtf(marketName);
        buf.writeBoolean(useStorage);
        buf.writeInt(currencyReserve);
        buf.writeBoolean(acceptForeign);
    }
}
