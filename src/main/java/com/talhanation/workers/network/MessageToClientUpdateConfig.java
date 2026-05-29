package com.talhanation.workers.network;

import com.talhanation.workers.client.WorkersClientManager;
import com.talhanation.workers.config.BuildMode;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

public class MessageToClientUpdateConfig implements Message<MessageToClientUpdateConfig> {
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
    public Dist getExecutingSide() {
        return Dist.CLIENT;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void executeClientSide(NetworkEvent.Context context) {
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
