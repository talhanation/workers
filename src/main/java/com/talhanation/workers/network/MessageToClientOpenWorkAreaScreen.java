package com.talhanation.workers.network;

import com.talhanation.workers.client.gui.WorkAreaScreenFactory;
import com.talhanation.workers.entities.workarea.AbstractWorkAreaEntity;
import com.talhanation.workers.network.compat.WorkersMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.protocol.PacketFlow;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.api.distmarker.Dist;
import com.talhanation.workers.network.compat.WorkersNetworkContext;

import java.util.UUID;

public class MessageToClientOpenWorkAreaScreen implements WorkersMessage<MessageToClientOpenWorkAreaScreen> {

    private UUID uuid;

    public MessageToClientOpenWorkAreaScreen() {

    }

    public MessageToClientOpenWorkAreaScreen(UUID uuid) {
        this.uuid = uuid;
    }

    @Override
    public PacketFlow getExecutingSide() {
        return PacketFlow.CLIENTBOUND;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void executeClientSide(WorkersNetworkContext context) {
        Player player = Minecraft.getInstance().player;
        player.getCommandSenderWorld().getEntitiesOfClass(AbstractWorkAreaEntity.class, player.getBoundingBox()
                        .inflate(16.0D), v -> v
                        .getUUID()
                        .equals(this.uuid))
                .stream()
                .findAny()
                .ifPresent(areaEntity -> WorkAreaScreenFactory.open(areaEntity, player));
    }

    @Override
    public MessageToClientOpenWorkAreaScreen fromBytes(FriendlyByteBuf buf) {
        this.uuid = buf.readUUID();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(uuid);
    }
}