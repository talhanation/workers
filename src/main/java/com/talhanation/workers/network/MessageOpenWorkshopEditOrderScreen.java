package com.talhanation.workers.network;

import com.talhanation.workers.entities.workarea.WorkshopArea;
import com.talhanation.workers.world.CraftOrder;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;

public class MessageOpenWorkshopEditOrderScreen implements Message<MessageOpenWorkshopEditOrderScreen> {

    private UUID workshopUuid;
    private CompoundTag nbt;

    public MessageOpenWorkshopEditOrderScreen() {}

    public MessageOpenWorkshopEditOrderScreen(UUID workshopUuid, CraftOrder order) {
        this.workshopUuid = workshopUuid;
        this.nbt = order.toNbt();
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    @Override
    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player == null || nbt == null) return;

        player.getCommandSenderWorld().getEntitiesOfClass(WorkshopArea.class, player.getBoundingBox().inflate(64.0D),
                        area -> area.getUUID().equals(this.workshopUuid))
                .stream()
                .findAny()
                .ifPresent(workshop -> {
                    if (!workshop.canPlayerSee(player)) return;
                    workshop.openEditOrderGUI(player, CraftOrder.fromNbt(nbt));
                });
    }

    @Override
    public MessageOpenWorkshopEditOrderScreen fromBytes(FriendlyByteBuf buf) {
        this.workshopUuid = buf.readUUID();
        this.nbt = buf.readNbt();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.workshopUuid);
        buf.writeNbt(this.nbt);
    }
}
