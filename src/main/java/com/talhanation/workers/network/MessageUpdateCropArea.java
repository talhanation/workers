package com.talhanation.workers.network;

import com.talhanation.workers.entities.workarea.CropArea;
import com.talhanation.workers.network.compat.WorkersMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.workers.network.compat.WorkersNetworkContext;

import java.util.UUID;

import static com.talhanation.workers.entities.workarea.AbstractWorkAreaEntity.DONE_TIME;

public class MessageUpdateCropArea implements WorkersMessage<MessageUpdateCropArea> {

    public UUID uuid;
    public ItemStack cropItem = ItemStack.EMPTY;
    public MessageUpdateCropArea() {

    }

    public MessageUpdateCropArea(UUID uuid, ItemStack cropItem) {
        this.uuid = uuid;

        this.cropItem = cropItem.copy();
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(WorkersNetworkContext context){
        ServerPlayer player = context.getSender();
        if(player == null) return;

        player.getCommandSenderWorld().getEntitiesOfClass(CropArea.class, player.getBoundingBox()
                        .inflate(16.0D), v -> v
                        .getUUID()
                        .equals(this.uuid))
                .stream()
                .findAny()
                .ifPresent(this::update);

    }

    public void update(CropArea cropArea){
        cropArea.setSeedStack(this.cropItem);
        cropArea.updateType();
        cropArea.setTime(cropArea.getTime() + DONE_TIME);
    }

    public MessageUpdateCropArea fromBytes(FriendlyByteBuf buf) {
        this.uuid = buf.readUUID();
        this.cropItem = ItemStack.OPTIONAL_STREAM_CODEC.decode((RegistryFriendlyByteBuf) buf);
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(uuid);
        ItemStack.OPTIONAL_STREAM_CODEC.encode((RegistryFriendlyByteBuf) buf, cropItem);

    }

}
