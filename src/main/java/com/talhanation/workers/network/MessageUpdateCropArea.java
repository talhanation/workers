package com.talhanation.workers.network;

import com.talhanation.workers.entities.workarea.CropArea;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

import static com.talhanation.workers.entities.workarea.AbstractWorkAreaEntity.DONE_TIME;

public class MessageUpdateCropArea implements Message<MessageUpdateCropArea> {

    public UUID uuid;
    public ItemStack cropItem;
    public MessageUpdateCropArea() {

    }

    public MessageUpdateCropArea(UUID uuid, ItemStack cropItem) {
        this.uuid = uuid;

        this.cropItem = cropItem.copy();
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(IPayloadContext context){
        ServerPlayer player = ((ServerPlayer) context.player());
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
        ItemStack itemStack = this.cropItem;
        cropArea.setSeedStack(itemStack);
        cropArea.updateType();
        cropArea.setTime(cropArea.getTime() + DONE_TIME);
    }

    public MessageUpdateCropArea fromBytes(RegistryFriendlyByteBuf buf) {
        this.uuid = buf.readUUID();
        this.cropItem = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        return this;
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(uuid);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, cropItem);

    }

    public static final CustomPacketPayload.Type<MessageUpdateCropArea> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("workers", "message_update_crop_area"));

    @Override
    public CustomPacketPayload.Type<MessageUpdateCropArea> type() {
        return TYPE;
    }
}
