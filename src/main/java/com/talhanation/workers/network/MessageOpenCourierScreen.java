package com.talhanation.workers.network;

import com.talhanation.workers.entities.CourierEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;

public class MessageOpenCourierScreen implements Message<MessageOpenCourierScreen> {

    private UUID courierUuid;

    public MessageOpenCourierScreen(){}

    public MessageOpenCourierScreen(UUID courierUuid){
        this.courierUuid = courierUuid;
    }

    @Override
    public Dist getExecutingSide(){
        return Dist.DEDICATED_SERVER;
    }

    @Override
    public void executeServerSide(NetworkEvent.Context context){
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
