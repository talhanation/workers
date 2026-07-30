package com.talhanation.workers.network;

import com.talhanation.workers.config.BuildMode;
import com.talhanation.workers.config.WorkersServerConfig;
import com.talhanation.workers.network.compat.WorkersMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Team;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.workers.network.compat.WorkersNetworkContext;
import com.talhanation.workers.WorkersMain;
import com.talhanation.workers.network.compat.WorkersPacketDistributor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class MessageRequestPresetList implements WorkersMessage<MessageRequestPresetList> {

    public MessageRequestPresetList(){}

    @Override
    public PacketFlow getExecutingSide(){
        return PacketFlow.SERVERBOUND;
    }

    @Override
    public void executeServerSide(WorkersNetworkContext context){
        ServerPlayer player = context.getSender();
        if (player == null) return;

        BuildMode mode = WorkersServerConfig.BuildModeConfig.get();
        if (mode == BuildMode.FREE) return;

        Path scanRoot = player.server.getServerDirectory().resolve("workers").resolve("scan");

        String teamId = null;
        if (mode == BuildMode.PRESET_FACTIONS) {

            try {
                Team playerTeam =  player.getTeam();
                teamId = playerTeam != null ? playerTeam.getName() : null;

            }
            catch (Exception ignored) {}

            if (teamId == null) {
                WorkersMain.SIMPLE_CHANNEL.send(WorkersPacketDistributor.PLAYER.with(() -> player), new MessageToClientPresetList(new ArrayList<>()));
                return;
            }
            scanRoot = scanRoot.resolve("factions").resolve(teamId);
        }

        List<String> names = collectNbtNames(scanRoot);

        WorkersMain.SIMPLE_CHANNEL.send(WorkersPacketDistributor.PLAYER.with(() -> player), new MessageToClientPresetList(names));
    }

    private static List<String> collectNbtNames(Path root) {
        List<String> result = new ArrayList<>();
        if (!Files.exists(root)) return result;
        try (Stream<Path> list = Files.list(root)) {
            list.filter(p -> p.toString().endsWith(".nbt") && Files.isRegularFile(p))
                .forEach(p -> {
                    String filename = p.getFileName().toString().replace(".nbt", "");
                    result.add(filename);
                });
        } catch (IOException e) { e.printStackTrace(); }
        return result;
    }

    @Override
    public MessageRequestPresetList fromBytes(FriendlyByteBuf buf){
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf){

    }
}
