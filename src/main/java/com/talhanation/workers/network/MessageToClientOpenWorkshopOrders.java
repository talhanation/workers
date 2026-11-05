package com.talhanation.workers.network;

import com.talhanation.workers.client.gui.WorkshopOrderScreen;
import com.talhanation.workers.entities.workarea.WorkshopArea;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

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
    public PacketFlow getExecutingSide() {
        return PacketFlow.CLIENTBOUND;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void executeClientSide(IPayloadContext context) {
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
    public MessageToClientOpenWorkshopOrders fromBytes(RegistryFriendlyByteBuf buf) {
        this.uuid = buf.readUUID();
        return this;
    }

    @Override
    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(uuid);
    }

    public static final CustomPacketPayload.Type<MessageToClientOpenWorkshopOrders> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("workers", "message_to_client_open_workshop_orders"));

    @Override
    public CustomPacketPayload.Type<MessageToClientOpenWorkshopOrders> type() {
        return TYPE;
    }
}
