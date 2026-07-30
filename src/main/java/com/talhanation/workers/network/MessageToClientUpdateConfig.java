package com.talhanation.workers.network;

import com.talhanation.workers.client.WorkersClientManager;
import com.talhanation.workers.config.BuildMode;
import com.talhanation.workers.network.compat.WorkersMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.api.distmarker.Dist;
import com.talhanation.workers.network.compat.WorkersNetworkContext;

public class MessageToClientUpdateConfig implements WorkersMessage<MessageToClientUpdateConfig> {
    private boolean allowWorkAreaOnlyInFactionClaim;
    private boolean allowOnlyBuildings;
    private BuildMode buildMode;
    public MessageToClientUpdateConfig() {
    }

    public MessageToClientUpdateConfig(boolean allowWorkAreaOnlyInFactionClaim, boolean allowOnlyBuildings, BuildMode buildMode) {
        this.allowWorkAreaOnlyInFactionClaim = allowWorkAreaOnlyInFactionClaim;
        this.allowOnlyBuildings = allowOnlyBuildings;
        this.buildMode = buildMode;
    }

    @Override
    public PacketFlow getExecutingSide() {
        return PacketFlow.CLIENTBOUND;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void executeClientSide(WorkersNetworkContext context) {
        WorkersClientManager.configValueWorkAreaOnlyInFactionClaim = this.allowWorkAreaOnlyInFactionClaim;
        WorkersClientManager.configValueOnlyBuildings = this.allowOnlyBuildings;
        WorkersClientManager.buildMode = this.buildMode;
    }

    @Override
    public MessageToClientUpdateConfig fromBytes(FriendlyByteBuf buf) {
        this.allowWorkAreaOnlyInFactionClaim = buf.readBoolean();
        this.allowOnlyBuildings = buf.readBoolean();
        this.buildMode = buf.readEnum(BuildMode.class);
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(this.allowWorkAreaOnlyInFactionClaim);
        buf.writeBoolean(this.allowOnlyBuildings);
        buf.writeEnum(this.buildMode);
    }

}
