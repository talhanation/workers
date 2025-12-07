package com.talhanation.workers.network;

import com.talhanation.workers.CommandEvents;
import com.talhanation.workers.entities.AbstractWorkerEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;
import java.util.UUID;

public class MessageAddDepositPos implements Message<MessageAddDepositPos> {

    private UUID player;
    private UUID group;
    private BlockPos pos;

    public MessageAddDepositPos() {
    }

    public MessageAddDepositPos(UUID player, UUID group, BlockPos pos) {
        this.player = player;
        this.group = group;
        this.pos = pos;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = Objects.requireNonNull(context.getSender());
        player.getCommandSenderWorld().getEntitiesOfClass(
                AbstractWorkerEntity.class,
                player.getBoundingBox().inflate(100)
        ).forEach((worker) ->  CommandEvents.onAddDepositCommand(
                this.player,
                worker,
                group,
                pos)
        );
    }

    public MessageAddDepositPos fromBytes(FriendlyByteBuf buf) {
        this.player = buf.readUUID();
        this.group = buf.readUUID();
        this.pos = buf.readBlockPos();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.player);
        buf.writeUUID(this.group);
        buf.writeBlockPos(this.pos);
    }
}
