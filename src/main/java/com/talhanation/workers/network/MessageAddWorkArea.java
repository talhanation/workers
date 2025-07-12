package com.talhanation.workers.network;

import com.talhanation.workers.entities.workarea.BuildArea;
import com.talhanation.workers.entities.workarea.CropArea;
import com.talhanation.workers.entities.workarea.AbstractWorkAreaEntity;
import com.talhanation.workers.entities.workarea.LumberArea;
import com.talhanation.workers.init.ModEntityTypes;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

public class MessageAddWorkArea implements Message<MessageAddWorkArea> {
    public float x;
    public float y;
    public float z;
    public int type;
    public MessageAddWorkArea() {

    }

    public MessageAddWorkArea(float x, float y, float z, int type) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.type = type;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context){
        ServerPlayer player = context.getSender();
        if(player == null) return;
        String teamStringID = "";
        if(player.getTeam() != null){
            teamStringID = player.getTeam().getName();
        }
        AbstractWorkAreaEntity workArea;
        switch (type){
            case 2 -> {
                workArea = new BuildArea(ModEntityTypes.BUILDAREA.get(), player.level());
                workArea.setWidthSize(4);
                workArea.setHeightSize(4);
                workArea.setDepthSize(4);

            }
            case 1 -> {
                workArea = new LumberArea(ModEntityTypes.LUMBERAREA.get(), player.level());
                workArea.setWidthSize(16);
                workArea.setHeightSize(12);
                workArea.setDepthSize(16);
            }
            default -> {
                workArea = new CropArea(ModEntityTypes.CROPAREA.get(), player.level());
                workArea.setWidthSize(8);
                workArea.setHeightSize(2);
                workArea.setDepthSize(8);

            }
        }
        workArea.setFacing(player.getDirection());
        workArea.createArea();
        workArea.setTeamStringID(teamStringID);
        workArea.setDone(false);
        workArea.setPlayerName(player.getName().getString());
        workArea.setPlayerUUID(player.getUUID());
        workArea.setCustomName(Component.literal(""));

        workArea.moveTo(this.x, this.y, this.z);
        player.level().addFreshEntity(workArea);
    }
    public MessageAddWorkArea fromBytes(FriendlyByteBuf buf) {
        this.x = buf.readFloat();
        this.y = buf.readFloat();
        this.z = buf.readFloat();
        this.type = buf.readInt();
        //this.recruit = buf.readUUID();

        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeFloat(x);
        buf.writeFloat(y);
        buf.writeFloat(z);
        buf.writeInt(type);
    }

}
