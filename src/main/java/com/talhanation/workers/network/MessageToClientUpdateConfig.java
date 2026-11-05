package com.talhanation.workers.network;

import com.talhanation.workers.client.WorkersClientManager;
import com.talhanation.workers.config.BuildMode;
import com.talhanation.workers.config.BuildPaymentMode;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public class MessageToClientUpdateConfig implements Message<MessageToClientUpdateConfig> {
    private boolean allowWorkAreaOnlyInFactionClaim;
    private boolean allowOnlyBuildings;
    private BuildMode buildMode;
    private BuildPaymentMode buildPaymentMode;
    private ItemStack currency;
    public MessageToClientUpdateConfig() {
    }

    public MessageToClientUpdateConfig(boolean allowWorkAreaOnlyInFactionClaim, boolean allowOnlyBuildings, BuildMode buildMode, BuildPaymentMode buildPaymentMode, ItemStack currency) {
        this.allowWorkAreaOnlyInFactionClaim = allowWorkAreaOnlyInFactionClaim;
        this.allowOnlyBuildings = allowOnlyBuildings;
        this.buildMode = buildMode;
        this.buildPaymentMode = buildPaymentMode;
        this.currency = currency.copy(); // NeoForge encodes payloads later on the netty thread: snapshot mutable data here
    }

    @Override
    public PacketFlow getExecutingSide() {
        return PacketFlow.CLIENTBOUND;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void executeClientSide(IPayloadContext context) {
        WorkersClientManager.configValueWorkAreaOnlyInFactionClaim = this.allowWorkAreaOnlyInFactionClaim;
        WorkersClientManager.configValueOnlyBuildings = this.allowOnlyBuildings;
        WorkersClientManager.buildMode = this.buildMode;
        WorkersClientManager.buildPaymentMode = this.buildPaymentMode;
        WorkersClientManager.currency = this.currency;
    }

    @Override
    public MessageToClientUpdateConfig fromBytes(RegistryFriendlyByteBuf buf) {
        this.allowWorkAreaOnlyInFactionClaim = buf.readBoolean();
        this.allowOnlyBuildings = buf.readBoolean();
        this.buildMode = buf.readEnum(BuildMode.class);
        this.buildPaymentMode = buf.readEnum(BuildPaymentMode.class);
        this.currency = net.minecraft.world.item.ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        return this;
    }

    @Override
    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeBoolean(this.allowWorkAreaOnlyInFactionClaim);
        buf.writeBoolean(this.allowOnlyBuildings);
        buf.writeEnum(this.buildMode);
        buf.writeEnum(this.buildPaymentMode);
        net.minecraft.world.item.ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, this.currency);
    }

    public static final CustomPacketPayload.Type<MessageToClientUpdateConfig> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("workers", "message_to_client_update_config"));

    @Override
    public CustomPacketPayload.Type<MessageToClientUpdateConfig> type() {
        return TYPE;
    }
}
