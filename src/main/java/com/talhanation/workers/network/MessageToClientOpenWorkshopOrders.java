package com.talhanation.workers.network;

import com.talhanation.workers.client.gui.WorkshopOrderScreen;
import com.talhanation.workers.entities.workarea.WorkshopArea;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;

/** Opens the order list of the workshop the clicked craftsman works in. */
public class MessageToClientOpenWorkshopOrders implements Message<MessageToClientOpenWorkshopOrders> {

    private UUID uuid;

    public MessageToClientOpenWorkshopOrders() {

    }

    public MessageToClientOpenWorkshopOrders(UUID uuid) {
        this.uuid = uuid;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.CLIENT;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void executeClientSide(NetworkEvent.Context context) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        player.getCommandSenderWorld().getEntitiesOfClass(WorkshopArea.class, player.getBoundingBox()
                        .inflate(64.0D), v -> v
                        .getUUID()
                        .equals(this.uuid))
                .stream()
                .findAny()
                .ifPresent(area -> Minecraft.getInstance().setScreen(new WorkshopOrderScreen(area, player)));
    }

    @Override
    public MessageToClientOpenWorkshopOrders fromBytes(FriendlyByteBuf buf) {
        this.uuid = buf.readUUID();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(uuid);
    }
}
