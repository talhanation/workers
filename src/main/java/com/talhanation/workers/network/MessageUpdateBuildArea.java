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
    public int xSize;
    public int ySize;
    public int zSize;
    public boolean build;
    public MessageUpdateBuildArea() {}
    public MessageUpdateBuildArea(UUID uuid, int xSize, int ySize, int zSize, CompoundTag structureNBT, boolean build) {
        this.uuid = uuid;
        this.xSize = xSize;
        this.ySize = ySize;
        this.zSize = zSize;
        this.structureNBT = structureNBT;
        this.build = build;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context){
        ServerPlayer player = context.getSender();
        if(player == null) return;

        player.getCommandSenderWorld().getEntitiesOfClass(BuildArea.class, player.getBoundingBox()
                        .inflate(32.0D), v -> v
                        .getUUID()
                        .equals(this.uuid))
                .stream()
                .findAny()
                .ifPresent(this::update);

    }

    public void update(BuildArea buildArea){
        buildArea.setXSize(this.xSize);
        buildArea.setYSize(this.ySize);
        buildArea.setZSize(this.zSize);
        buildArea.setStructureNBT(structureNBT);

        if(build){
            buildArea.setStartBuild();
        }
    }

    public MessageUpdateBuildArea fromBytes(FriendlyByteBuf buf) {
        this.uuid = buf.readUUID();
        this.xSize = buf.readInt();
        this.ySize = buf.readInt();
        this.zSize = buf.readInt();
        this.structureNBT = buf.readNbt();
        this.build = buf.readBoolean();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(uuid);
        buf.writeInt(xSize);
        buf.writeInt(ySize);
        buf.writeInt(zSize);
        buf.writeNbt(structureNBT);
        buf.writeBoolean(build);
    }
}
