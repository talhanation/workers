package com.talhanation.workers.network;

import com.talhanation.recruits.ClaimEvents;
import com.talhanation.recruits.FactionEvents;
import com.talhanation.recruits.world.RecruitsClaim;
import com.talhanation.recruits.world.RecruitsFaction;
import com.talhanation.workers.WorkAreaTypes;
import com.talhanation.workers.config.WorkersServerConfig;
import com.talhanation.workers.entities.workarea.*;
import com.talhanation.workers.init.ModEntityTypes;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

public class MessageAddWorkArea implements Message<MessageAddWorkArea> {
    public BlockPos pos;
    public int typeIndex;
    public MessageAddWorkArea() {

    }

    public MessageAddWorkArea(BlockPos pos, WorkAreaTypes type) {
        this.pos = pos;
        this.typeIndex = type.getIndex();
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
        WorkAreaTypes type = WorkAreaTypes.fromIndex(typeIndex);

        if(!this.canPlace(player, pos, type)){
            return;
        }

        switch (type){
            case ANIMAL_PEN -> {
                workArea = new AnimalPenArea(ModEntityTypes.ANIMAL_PEN_AREA.get(), player.level());
                workArea.setWidthSize(12);
                workArea.setHeightSize(4);
                workArea.setDepthSize(12);
            }
            case FISHING -> {
                workArea = new FishingArea(ModEntityTypes.FISHINGAREA.get(), player.level());
                workArea.setWidthSize(9);
                workArea.setHeightSize(2);
                workArea.setDepthSize(9);
            }

            case STORAGE -> {
                workArea = new StorageArea(ModEntityTypes.STORAGEAREA.get(), player.level());
                workArea.setWidthSize(5);
                workArea.setHeightSize(5);
                workArea.setDepthSize(5);
                workArea.setTeamAccess(false);

            }
            case MINING -> {
                workArea = new MiningArea(ModEntityTypes.MININGAREA.get(), player.level());
                workArea.setWidthSize(8);
                workArea.setHeightSize(4);
                workArea.setDepthSize(8);

            }
            case BUILDING -> {
                workArea = new BuildArea(ModEntityTypes.BUILDAREA.get(), player.level());
                workArea.setWidthSize(4);
                workArea.setHeightSize(4);
                workArea.setDepthSize(4);

            }
            case LUMBER -> {
                workArea = new LumberArea(ModEntityTypes.LUMBERAREA.get(), player.level());
                workArea.setWidthSize(16);
                workArea.setHeightSize(12);
                workArea.setDepthSize(16);
            }
            case CROPAREA -> {
                workArea = new CropArea(ModEntityTypes.CROPAREA.get(), player.level());
                workArea.setWidthSize(9);
                workArea.setHeightSize(2);
                workArea.setDepthSize(9);
            }
            case MARKET -> {
                workArea = new MarketArea(ModEntityTypes.MARKETAREA.get(), player.level());
                workArea.setWidthSize(5);
                workArea.setHeightSize(5);
                workArea.setDepthSize(5);
                workArea.setTeamAccess(false);
            }

            default -> {
                //IGNORE
                return;
            }
        }
        workArea.setFacing(player.getDirection());
        workArea.moveTo(pos.above(), 0, 0);
        workArea.createArea();
        workArea.setTeamStringID(teamStringID);
        workArea.setDone(false);
        workArea.setPlayerName(player.getName().getString());
        workArea.setPlayerUUID(player.getUUID());
        workArea.setCustomName(Component.literal(""));

        if (AbstractWorkAreaEntity.isAreaOverlapping(player.level(), null, workArea.getArea())) {
            player.sendSystemMessage(Component.translatable("gui.workers.area.overlapping"));
            return;
        }

        player.level().addFreshEntity(workArea);
    }

    public boolean canPlace(ServerPlayer player, BlockPos blockPos, WorkAreaTypes type) {
        if (WorkersServerConfig.ShouldOnlyPlacingBuildingsBePossible.get() && type != WorkAreaTypes.BUILDING) {
            return false;
        }

        if (WorkersServerConfig.ShouldWorkAreaOnlyBeInFactionClaim.get()) {
            if (player.getTeam() == null) return false;

            RecruitsFaction ownFaction = FactionEvents.recruitsFactionManager.getFactionByStringID(player.getTeam().getName());
            if (ownFaction == null) return false;
            if (blockPos == null) return false;

            int chunkX = blockPos.getX() >> 4;
            int chunkZ = blockPos.getZ() >> 4;
            ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
            RecruitsClaim claim = ClaimEvents.recruitsClaimManager.getClaim(chunkPos);
            if (claim == null) return false;

            return claim.containsChunk(chunkPos) && claim.getOwnerFaction().getStringID().equals(ownFaction.getStringID());
        }

        return true;
    }

    public MessageAddWorkArea fromBytes(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.typeIndex = buf.readInt();

        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeInt(typeIndex);
    }

}
