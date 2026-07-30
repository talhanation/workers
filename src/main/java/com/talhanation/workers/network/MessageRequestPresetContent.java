package com.talhanation.workers.network;

import com.talhanation.workers.WorkersMain;
import com.talhanation.workers.config.BuildMode;
import com.talhanation.workers.config.WorkersServerConfig;
import com.talhanation.workers.network.compat.WorkersMessage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Team;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.workers.network.compat.WorkersNetworkContext;
import com.talhanation.workers.network.compat.WorkersPacketDistributor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class MessageRequestPresetContent implements WorkersMessage<MessageRequestPresetContent> {

    public String presetName;

    public MessageRequestPresetContent(){}
    public MessageRequestPresetContent(String presetName){
        this.presetName = presetName;
    }

    @Override
    public PacketFlow getExecutingSide(){
        return PacketFlow.SERVERBOUND;
    }

    @Override
    public void executeServerSide(WorkersNetworkContext context){
        ServerPlayer player = context.getSender();
        if (player == null) return;

        String safe = presetName.replace("..", "").replace("/", "").replace("\\", "");

        BuildMode mode = WorkersServerConfig.BuildModeConfig.get();
        Path scanRoot = player.server.getServerDirectory().resolve("workers").resolve("scan");

        if (mode == BuildMode.PRESET_FACTIONS) {
            try {
                Team playerTeam = player.getTeam();
                if (playerTeam == null) return;
                scanRoot = scanRoot.resolve("factions").resolve(playerTeam.getName());
            } catch (Exception ignored) {
                return;
            }
        }

        File file = scanRoot.resolve(safe + ".nbt").toFile();

        if (!file.toPath().toAbsolutePath().startsWith(scanRoot.toAbsolutePath())) return;
        if (!file.exists()) return;

        try {
            CompoundTag nbt = NbtIo.readCompressed(file.toPath(), net.minecraft.nbt.NbtAccounter.unlimitedHeap());
            WorkersMain.SIMPLE_CHANNEL.send(
                    WorkersPacketDistributor.PLAYER.with(() -> player),
                    new MessageToClientPresetContent(safe, nbt)
            );
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public MessageRequestPresetContent fromBytes(FriendlyByteBuf buf) {
        this.presetName = buf.readUtf();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(presetName);
    }
}
