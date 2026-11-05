package com.talhanation.workers.network;

import com.talhanation.workers.entities.workarea.MiningArea;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

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
        this.fillItem = fillItem.copy(); // NeoForge encodes payloads later on the netty thread: snapshot mutable data here
        this.mode = mode;
        this.keepOn = keepOn;
    }

    @Override
    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(IPayloadContext context){
        ServerPlayer player = ((ServerPlayer) context.player());
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

    public MessageUpdateMiningArea fromBytes(RegistryFriendlyByteBuf buf) {
        this.uuid = buf.readUUID();
        this.xSize = buf.readInt();
        this.ySize = buf.readInt();
        this.zSize = buf.readInt();
        this.yOffset = buf.readInt();
        this.closeFloor = buf.readBoolean();
        this.closeFluids = buf.readBoolean();
        this.mineWallOres = buf.readBoolean();
        this.fillItem = net.minecraft.world.item.ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        this.mode = buf.readInt();
        this.keepOn = buf.readBoolean();
        return this;
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(uuid);
        buf.writeInt(xSize);
        buf.writeInt(ySize);
        buf.writeInt(zSize);
        buf.writeInt(yOffset);
        buf.writeBoolean(closeFloor);
        buf.writeBoolean(closeFluids);
        buf.writeBoolean(mineWallOres);
        net.minecraft.world.item.ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, fillItem);
        buf.writeInt(mode);
        buf.writeBoolean(keepOn);
    }

    public static final CustomPacketPayload.Type<MessageUpdateMiningArea> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("workers", "message_update_mining_area"));

    @Override
    public CustomPacketPayload.Type<MessageUpdateMiningArea> type() {
        return TYPE;
    }
}