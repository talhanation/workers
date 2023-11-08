package com.talhanation.workers.network;

import com.talhanation.workers.entities.MerchantEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;

public class MessageMerchantSetSendInfo implements Message<MessageMerchantSetSendInfo> {
    private UUID worker;
    private boolean sendInfo;
    public MessageMerchantSetSendInfo() {
    }

    public MessageMerchantSetSendInfo(UUID recruit, boolean sendInfo) {
        this.worker = recruit;
        this.sendInfo = sendInfo;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {

        ServerPlayer player = context.getSender();
        player.getCommandSenderWorld().getEntitiesOfClass(MerchantEntity.class, player.getBoundingBox()
                        .inflate(16.0D), v -> v
                        .getUUID()
                        .equals(this.worker))
                .stream()
                .filter(MerchantEntity::isAlive)
                .findAny()
                .ifPresent(merchant -> merchant.setSendInfo(sendInfo));

    }

    public MessageMerchantSetSendInfo fromBytes(FriendlyByteBuf buf) {
        this.worker = buf.readUUID();
        this.sendInfo = buf.readBoolean();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.worker);
        buf.writeBoolean(this.sendInfo);
    }

}