package com.talhanation.workers.network;

import com.talhanation.workers.entities.workarea.MiningArea;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;

public class MessageUpdateMiningArea implements Message<MessageUpdateMiningArea> {

    public UUID uuid;
    public int xSize;
    public int ySize;
    public int zSize;
    public int yOffset;
    public boolean closeFloor;
    public boolean closeFluids;
    public boolean mineWallOres;
    public net.minecraft.world.item.ItemStack fillItem = net.minecraft.world.item.ItemStack.EMPTY;
    public int mode;
    public boolean keepOn;
    public MessageUpdateMiningArea() {}

    public MessageUpdateMiningArea(UUID uuid, int xSize, int ySize, int zSize, int yOffset, boolean closeFloor, boolean closeFluids, boolean mineWallOres, net.minecraft.world.item.ItemStack fillItem, int mode, boolean keepOn) {
        this.uuid = uuid;
        this.xSize = xSize;
        this.ySize = ySize;
        this.zSize = zSize;
        this.yOffset = yOffset;
        this.closeFloor = closeFloor;
        this.closeFluids = closeFluids;
        this.mineWallOres = mineWallOres;
        this.fillItem = fillItem;
        this.mode = mode;
        this.keepOn = keepOn;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context){
        ServerPlayer player = context.getSender();
        if(player == null) return;

        player.getCommandSenderWorld().getEntitiesOfClass(MiningArea.class, player.getBoundingBox()
                        .inflate(32.0D), v -> v
                        .getUUID()
                        .equals(this.uuid))
                .stream()
                .findAny()
                .ifPresent(this::update);
    }

    public void update(MiningArea miningArea){
        miningArea.setWidthSize(this.xSize);
        miningArea.setHeightSize(this.ySize);
        miningArea.setDepthSize(this.zSize);
        miningArea.setHeightOffset(this.yOffset);
        miningArea.setCloseFloor(this.closeFloor);
        miningArea.setCloseFluids(this.closeFluids);
        miningArea.setMineWallOres(this.mineWallOres);
        miningArea.setFillItem(this.fillItem);
        miningArea.setMode(this.mode);
        miningArea.setKeepOn(this.keepOn);
        miningArea.resetWork();
    }

    public MessageUpdateMiningArea fromBytes(FriendlyByteBuf buf) {
        this.uuid = buf.readUUID();
        this.xSize = buf.readInt();
        this.ySize = buf.readInt();
        this.zSize = buf.readInt();
        this.yOffset = buf.readInt();
        this.closeFloor = buf.readBoolean();
        this.closeFluids = buf.readBoolean();
        this.mineWallOres = buf.readBoolean();
        this.fillItem = buf.readItem();
        this.mode = buf.readInt();
        this.keepOn = buf.readBoolean();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(uuid);
        buf.writeInt(xSize);
        buf.writeInt(ySize);
        buf.writeInt(zSize);
        buf.writeInt(yOffset);
        buf.writeBoolean(closeFloor);
        buf.writeBoolean(closeFluids);
        buf.writeBoolean(mineWallOres);
        buf.writeItem(fillItem);
        buf.writeInt(mode);
        buf.writeBoolean(keepOn);
    }
}