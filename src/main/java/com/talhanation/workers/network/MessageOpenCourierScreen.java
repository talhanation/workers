package com.talhanation.workers.network;

import com.talhanation.workers.entities.CourierEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public class MessageOpenCourierScreen implements Message<MessageOpenCourierScreen> {

    private UUID courierUuid;

    public MessageOpenCourierScreen(){}

    public MessageOpenCourierScreen(UUID courierUuid){
        this.courierUuid = courierUuid;
    }

    @Override
    public PacketFlow getExecutingSide(){
        return PacketFlow.SERVERBOUND;
    }

    @Override
    public void executeServerSide(IPayloadContext context){
        ServerPlayer player = ((ServerPlayer) context.player());
        if (player == null) return;

        player.getCommandSenderWorld()
                .getEntitiesOfClass(CourierEntity.class,
                        player.getBoundingBox().inflate(10.0D),
                        c -> c.getUUID().equals(this.courierUuid) && c.isAlive())
                .stream()
                .findAny()
                .ifPresent(c -> c.openSpecialGUI(player));
    }

    @Override
    public MessageOpenCourierScreen fromBytes(RegistryFriendlyByteBuf buf){
        this.courierUuid = buf.readUUID();
        return this;
    }

    @Override
    public void toBytes(RegistryFriendlyByteBuf buf){
        buf.writeUUID(courierUuid);
    }

    public static final CustomPacketPayload.Type<MessageOpenCourierScreen> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("workers", "message_open_courier_screen"));

    @Override
    public CustomPacketPayload.Type<MessageOpenCourierScreen> type() {
        return TYPE;
    }
}
