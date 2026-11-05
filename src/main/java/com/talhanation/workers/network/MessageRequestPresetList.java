package com.talhanation.workers.network;

import com.talhanation.workers.config.BuildMode;
import com.talhanation.workers.config.WorkersServerConfig;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Team;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.talhanation.workers.WorkersMain;
import net.neoforged.neoforge.network.PacketDistributor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class MessageRequestPresetList implements Message<MessageRequestPresetList> {

    public MessageRequestPresetList(){}

    @Override
    public PacketFlow getExecutingSide(){
        return PacketFlow.SERVERBOUND;
    }

    @Override
    public void executeServerSide(IPayloadContext context){
        ServerPlayer player = ((ServerPlayer) context.player());
        if (player == null) return;

        BuildMode mode = WorkersServerConfig.BuildModeConfig.get();
        if (mode == BuildMode.FREE) return;

        Path scanRoot = Path.of(player.server.getServerDirectory().toAbsolutePath().toString(), "workers", "scan");

        String teamId = null;
        if (mode == BuildMode.PRESET_FACTIONS) {

            try {
                Team playerTeam =  player.getTeam();
                teamId = playerTeam != null ? playerTeam.getName() : null;

            }
            catch (Exception ignored) {}

            if (teamId == null) {
                PacketDistributor.sendToPlayer(player, new MessageToClientPresetList(new ArrayList<>()));
                return;
            }
            scanRoot = scanRoot.resolve("factions").resolve(teamId);
        }

        List<String> names = collectNbtNames(scanRoot);

        PacketDistributor.sendToPlayer(player, new MessageToClientPresetList(names));
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
    public MessageRequestPresetList fromBytes(RegistryFriendlyByteBuf buf){
        return this;
    }

    @Override
    public void toBytes(RegistryFriendlyByteBuf buf){

    }

    public static final CustomPacketPayload.Type<MessageRequestPresetList> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("workers", "message_request_preset_list"));

    @Override
    public CustomPacketPayload.Type<MessageRequestPresetList> type() {
        return TYPE;
    }
}
