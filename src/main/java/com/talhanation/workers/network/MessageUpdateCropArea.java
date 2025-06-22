package com.talhanation.workers.network;

import com.talhanation.workers.entities.workarea.CropArea;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;

import static com.talhanation.workers.entities.workarea.AbstractWorkAreaEntity.DONE_TIME;

public class MessageUpdateCropArea implements Message<MessageUpdateCropArea> {

    public UUID uuid;
    public CompoundTag tag;
    public MessageUpdateCropArea() {

    }

    public MessageUpdateCropArea(UUID uuid, ItemStack cropItem) {
        this.uuid = uuid;

        CompoundTag compoundnbt = new CompoundTag();
        this.tag = cropItem.save(compoundnbt);
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context){
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
        ItemStack itemStack = ItemStack.of(tag);
        cropArea.setSeedStack(itemStack);

        cropArea.resetTimer = DONE_TIME;
    }

    public MessageUpdateCropArea fromBytes(FriendlyByteBuf buf) {
        this.uuid = buf.readUUID();
        this.tag = buf.readNbt();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(uuid);
        buf.writeNbt(tag);

    }

}
