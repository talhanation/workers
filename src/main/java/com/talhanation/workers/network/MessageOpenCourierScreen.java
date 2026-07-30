package com.talhanation.workers.network;

import com.talhanation.workers.entities.CourierEntity;
import com.talhanation.workers.network.compat.WorkersMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.workers.network.compat.WorkersNetworkContext;

import java.util.UUID;

public class MessageOpenCourierScreen implements WorkersMessage<MessageOpenCourierScreen> {

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
    public void executeServerSide(WorkersNetworkContext context){
        ServerPlayer player = context.getSender();
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
    public MessageOpenCourierScreen fromBytes(FriendlyByteBuf buf){
        this.courierUuid = buf.readUUID();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf){
        buf.writeUUID(courierUuid);
    }
}
