package com.talhanation.workers.network;

import com.talhanation.workers.entities.workarea.LumberArea;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;

import static com.talhanation.workers.entities.workarea.AbstractWorkAreaEntity.DONE_TIME;

public class MessageUpdateLumberArea implements Message<MessageUpdateLumberArea> {

    public UUID uuid;
    public CompoundTag tag;
    public boolean shearLeaves;
    public boolean stripLogs;
    public boolean replant;
    public MessageUpdateLumberArea() {

    }

    public MessageUpdateLumberArea(UUID uuid, ItemStack saplingItem, boolean shearLeaves, boolean stripLogs, boolean replant) {
        this.uuid = uuid;
        CompoundTag compoundnbt = new CompoundTag();
        this.tag = saplingItem.save(compoundnbt);
        this.shearLeaves = shearLeaves;
        this.stripLogs = stripLogs;
        this.replant = replant;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context){
        ServerPlayer player = context.getSender();
        if(player == null) return;

        player.getCommandSenderWorld().getEntitiesOfClass(LumberArea.class, player.getBoundingBox()
                        .inflate(16.0D), v -> v
                        .getUUID()
                        .equals(this.uuid))
                .stream()
                .findAny()
                .ifPresent(this::update);

    }

    public void update(LumberArea lumberArea){
        ItemStack itemStack = ItemStack.of(tag);
        lumberArea.setSaplingStack(itemStack);
        lumberArea.setShearLeaves(this.shearLeaves);
        lumberArea.setStripLogs(this.stripLogs);
        lumberArea.setReplant(this.replant);

        lumberArea.time += DONE_TIME;
    }

    public MessageUpdateLumberArea fromBytes(FriendlyByteBuf buf) {
        this.uuid = buf.readUUID();
        this.tag = buf.readNbt();
        this.shearLeaves = buf.readBoolean();
        this.stripLogs = buf.readBoolean();
        this.replant = buf.readBoolean();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(uuid);
        buf.writeNbt(tag);
        buf.writeBoolean(shearLeaves);
        buf.writeBoolean(stripLogs);
        buf.writeBoolean(replant);
    }

}
