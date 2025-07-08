package com.talhanation.workers.network;

import com.talhanation.workers.entities.workarea.BuildArea;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
public class MessageUpdateBuildArea implements Message<MessageUpdateBuildArea> {

    public UUID uuid;
    public CompoundTag structureNBT;
    public int size;
    public int height;
    public MessageUpdateBuildArea() {}
    public MessageUpdateBuildArea(UUID uuid, int size, int height, CompoundTag structureNBT) {
        this.uuid = uuid;
        this.size = size;
        this.height = height;
        this.structureNBT = structureNBT;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context){
        ServerPlayer player = context.getSender();
        if(player == null) return;

        player.getCommandSenderWorld().getEntitiesOfClass(BuildArea.class, player.getBoundingBox()
                        .inflate(16.0D), v -> v
                        .getUUID()
                        .equals(this.uuid))
                .stream()
                .findAny()
                .ifPresent(this::update);

    }

    public void update(BuildArea buildArea){
        buildArea.setSize(this.size);
        buildArea.setHeight(this.height);
    }

    public MessageUpdateBuildArea fromBytes(FriendlyByteBuf buf) {
        this.uuid = buf.readUUID();
        this.size = buf.readInt();
        this.height = buf.readInt();
        this.structureNBT = buf.readNbt();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(uuid);
        buf.writeInt(size);
        buf.writeInt(height);
        buf.writeNbt(structureNBT);

    }
}
