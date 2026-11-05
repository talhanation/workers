package com.talhanation.workers.network;

import com.talhanation.workers.entities.workarea.AbstractWorkAreaEntity;
import com.talhanation.workers.entities.workarea.MiningArea;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public class MessageRotateWorkArea implements Message<MessageRotateWorkArea> {

    public UUID uuid;
    public boolean clockwise;

    public MessageRotateWorkArea() {}

    public MessageRotateWorkArea(UUID uuid, boolean clockwise) {
        this.uuid = uuid;
        this.clockwise = clockwise;
    }

    @Override
    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    @Override
    public void executeServerSide(IPayloadContext context) {
        ServerPlayer player = ((ServerPlayer) context.player());
        if (player == null) return;

        player.getCommandSenderWorld().getEntitiesOfClass(AbstractWorkAreaEntity.class,
                        player.getBoundingBox().inflate(32.0D),
                        v -> v.getUUID().equals(this.uuid))
                .stream()
                .findAny()
                .ifPresent(this::rotate);
    }

    public void rotate(AbstractWorkAreaEntity workArea) {
        Direction current = workArea.getFacing();
        Direction next = clockwise ? current.getClockWise() : current.getCounterClockWise();

        // Test the rotated area before committing
        workArea.setFacing(next);
        workArea.createArea();

        if (AbstractWorkAreaEntity.isAreaOverlapping(workArea.level(), workArea, workArea.getArea())) {
            // Revert to original facing
            workArea.setFacing(current);
            workArea.createArea();
            return;
        }

        if (workArea instanceof MiningArea miningArea) {
            miningArea.resetWork();
        }
    }

    @Override
    public MessageRotateWorkArea fromBytes(RegistryFriendlyByteBuf buf) {
        this.uuid = buf.readUUID();
        this.clockwise = buf.readBoolean();
        return this;
    }

    @Override
    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(uuid);
        buf.writeBoolean(clockwise);
    }

    public static final CustomPacketPayload.Type<MessageRotateWorkArea> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("workers", "message_rotate_work_area"));

    @Override
    public CustomPacketPayload.Type<MessageRotateWorkArea> type() {
        return TYPE;
    }
}