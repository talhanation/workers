package com.talhanation.workers.network;

import com.talhanation.workers.entities.workarea.AbstractWorkAreaEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

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
    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    @Override
    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
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
        }
    }

    @Override
    public MessageRotateWorkArea fromBytes(FriendlyByteBuf buf) {
        this.uuid = buf.readUUID();
        this.clockwise = buf.readBoolean();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(uuid);
        buf.writeBoolean(clockwise);
    }
}
