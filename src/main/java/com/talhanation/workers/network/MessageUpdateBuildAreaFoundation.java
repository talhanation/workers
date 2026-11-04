package com.talhanation.workers.network;

import com.talhanation.workers.entities.workarea.BuildArea;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;

/** Foundation mode of a build area: on/off and the fill item (empty = chosen from the ground). */
public class MessageUpdateBuildAreaFoundation implements Message<MessageUpdateBuildAreaFoundation> {

    public UUID uuid;
    public boolean foundation;
    public ItemStack fillItem = ItemStack.EMPTY;

    public MessageUpdateBuildAreaFoundation() {}

    public MessageUpdateBuildAreaFoundation(UUID uuid, boolean foundation, ItemStack fillItem) {
        this.uuid = uuid;
        this.foundation = foundation;
        this.fillItem = fillItem == null ? ItemStack.EMPTY : fillItem;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    @Override
    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player == null) return;

        player.getCommandSenderWorld().getEntitiesOfClass(BuildArea.class,
                        player.getBoundingBox().inflate(32.0D),
                        v -> v.getUUID().equals(this.uuid))
                .stream()
                .findAny()
                .filter(area -> area.canPlayerSee(player))
                .ifPresent(area -> {
                    area.setFoundation(this.foundation);
                    // only auto or a block of the palette
                    area.setFillItem(BuildArea.isValidFillItem(this.fillItem) ? this.fillItem : ItemStack.EMPTY);
                });
    }

    @Override
    public MessageUpdateBuildAreaFoundation fromBytes(FriendlyByteBuf buf) {
        this.uuid = buf.readUUID();
        this.foundation = buf.readBoolean();
        this.fillItem = buf.readItem();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(uuid);
        buf.writeBoolean(foundation);
        buf.writeItem(fillItem);
    }
}
