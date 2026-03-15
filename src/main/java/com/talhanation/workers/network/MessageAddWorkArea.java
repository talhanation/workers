package com.talhanation.workers.network;

import com.talhanation.workers.entities.workarea.*;
import com.talhanation.workers.init.ModEntityTypes;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

public class MessageAddWorkArea implements Message<MessageAddWorkArea> {
    public BlockPos pos;
    public int type;
    public MessageAddWorkArea() {

    }

    public MessageAddWorkArea(BlockPos pos, int type) {
        this.pos = pos;
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
            case 7 -> {
                workArea = new MarketArea(ModEntityTypes.MARKETAREA.get(), player.level());
                workArea.setWidthSize(4);
                workArea.setHeightSize(8);
                workArea.setDepthSize(4);
            }
            case 6 -> {
                workArea = new AnimalPenArea(ModEntityTypes.ANIMAL_PEN_AREA.get(), player.level());
                workArea.setWidthSize(12);
                workArea.setHeightSize(4);
                workArea.setDepthSize(12);
            }
            case 5 -> {
                workArea = new FishingArea(ModEntityTypes.FISHINGAREA.get(), player.level());
                workArea.setWidthSize(9);
                workArea.setHeightSize(2);
                workArea.setDepthSize(9);
            }

            case 4 -> {
                workArea = new StorageArea(ModEntityTypes.STORAGEAREA.get(), player.level());
                workArea.setWidthSize(5);
                workArea.setHeightSize(5);
                workArea.setDepthSize(5);

            }
            case 3 -> {
                workArea = new MiningArea(ModEntityTypes.MININGAREA.get(), player.level());
                workArea.setWidthSize(8);
                workArea.setHeightSize(4);
                workArea.setDepthSize(8);

            }
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
                workArea.setWidthSize(9);
                workArea.setHeightSize(2);
                workArea.setDepthSize(9);
            }
        }
        workArea.setFacing(player.getDirection());
        workArea.createArea();
        workArea.setTeamStringID(teamStringID);
        workArea.setDone(false);
        workArea.setPlayerName(player.getName().getString());
        workArea.setPlayerUUID(player.getUUID());
        workArea.setCustomName(Component.literal(""));

        workArea.moveTo(pos.above(), 0, 0);
        player.level().addFreshEntity(workArea);
    }
    public MessageAddWorkArea fromBytes(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.type = buf.readInt();

        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeInt(type);
    }

}
