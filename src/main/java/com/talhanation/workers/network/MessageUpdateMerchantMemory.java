package com.talhanation.workers.network;

import com.talhanation.workers.entities.MerchantEntity;
import com.talhanation.workers.world.MerchantMemory;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;

/** Favorite / avoid a known market. */
public class MessageUpdateMerchantMemory implements Message<MessageUpdateMerchantMemory> {

    private UUID merchantUuid;
    private UUID marketUuid;
    private boolean favorite;
    private boolean avoid;

    public MessageUpdateMerchantMemory() {}

    public MessageUpdateMerchantMemory(UUID merchantUuid, UUID marketUuid, boolean favorite, boolean avoid) {
        this.merchantUuid = merchantUuid;
        this.marketUuid = marketUuid;
        this.favorite = favorite;
        this.avoid = avoid;
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
                .ifPresent(merchant -> {
                    MerchantMemory.Entry entry = merchant.getMemory().get(marketUuid);
                    if (entry == null) return;
                    // a market is either a favorite or avoided
                    entry.favorite = favorite && !avoid;
                    entry.avoid = avoid;
                });
    }

    @Override
    public MessageUpdateMerchantMemory fromBytes(FriendlyByteBuf buf) {
        this.merchantUuid = buf.readUUID();
        this.marketUuid = buf.readUUID();
        this.favorite = buf.readBoolean();
        this.avoid = buf.readBoolean();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(merchantUuid);
        buf.writeUUID(marketUuid);
        buf.writeBoolean(favorite);
        buf.writeBoolean(avoid);
    }
}
