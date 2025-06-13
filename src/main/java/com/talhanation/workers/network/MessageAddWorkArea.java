package com.talhanation.workers.network;

import com.talhanation.workers.entities.WorkAreaEntity;
import com.talhanation.workers.init.ModEntityTypes;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

public class MessageAddWorkArea implements Message<MessageAddWorkArea> {
    public float x;
    public float y;
    public float z;
    public MessageAddWorkArea() {

    }

    public MessageAddWorkArea(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context){
        ServerPlayer player = context.getSender();
        if(player == null) return;

        WorkAreaEntity workArea = new WorkAreaEntity(ModEntityTypes.WORKAREA.get(), player.level());
        workArea.moveTo(this.x, this.y, this.z);
        workArea.playerName = player.getName().getString();
        workArea.playerUUID = player.getUUID();
        workArea.name = "";

        player.level().addFreshEntity(workArea);
    }
    public MessageAddWorkArea fromBytes(FriendlyByteBuf buf) {
        this.x = buf.readFloat();
        this.y = buf.readFloat();
        this.z = buf.readFloat();
        //this.recruit = buf.readUUID();

        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeFloat(x);
        buf.writeFloat(y);
        buf.writeFloat(z);
    }

}
