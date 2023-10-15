package com.talhanation.workers.network;

import com.talhanation.workers.entities.MerchantEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;

public class MessageMerchantSetCreative implements Message<MessageMerchantSetCreative> {
    private UUID worker;
    private boolean creative;
    public MessageMerchantSetCreative() {
    }

    public MessageMerchantSetCreative(UUID recruit, boolean creative) {
        this.worker = recruit;
        this.creative = creative;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {

        ServerPlayer player = context.getSender();
        player.level.getEntitiesOfClass(MerchantEntity.class, player.getBoundingBox()
                        .inflate(16.0D), v -> v
                        .getUUID()
                        .equals(this.worker))
                .stream()
                .filter(MerchantEntity::isAlive)
                .findAny()
                .ifPresent(merchant -> this.setCreative(player, merchant, creative));

    }

    private void setCreative(ServerPlayer player, MerchantEntity merchant, boolean creative){
        merchant.setCreative(creative);
        if(creative) merchant.tellPlayer(player, new TextComponent("Im now a Creative Merchant."));
        else merchant.tellPlayer(player, new TextComponent("Im back a Survival Merchant."));
    }

    public MessageMerchantSetCreative fromBytes(FriendlyByteBuf buf) {
        this.worker = buf.readUUID();
        this.creative = buf.readBoolean();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.worker);
        buf.writeBoolean(this.creative);
    }

}