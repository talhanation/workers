package com.talhanation.workers.network;

import com.talhanation.workers.WorkersMain;
import com.talhanation.workers.entities.MerchantEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.UUID;

/** The owner opens the list of markets the merchant knows. */
public class MessageRequestMerchantMemory implements Message<MessageRequestMerchantMemory> {

    private UUID merchantUuid;

    public MessageRequestMerchantMemory() {}

    public MessageRequestMerchantMemory(UUID merchantUuid) {
        this.merchantUuid = merchantUuid;
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
                .ifPresent(merchant -> WorkersMain.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                        new MessageToClientMerchantMemory(merchant.getUUID(), new ArrayList<>(merchant.getMemory().getEntries()))));
    }

    @Override
    public MessageRequestMerchantMemory fromBytes(FriendlyByteBuf buf) {
        this.merchantUuid = buf.readUUID();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(merchantUuid);
    }
}
