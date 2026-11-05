package com.talhanation.workers.network;

import com.talhanation.workers.WorkersMain;
import com.talhanation.workers.config.BuildMode;
import com.talhanation.workers.config.WorkersServerConfig;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Team;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class MessageRequestPresetContent implements Message<MessageRequestPresetContent> {

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
    public void executeServerSide(IPayloadContext context){
        ServerPlayer player = ((ServerPlayer) context.player());
        if (player == null) return;

        String safe = presetName.replace("..", "").replace("/", "").replace("\\", "");

        BuildMode mode = WorkersServerConfig.BuildModeConfig.get();
        Path scanRoot = Path.of(player.server.getServerDirectory().toAbsolutePath().toString(), "workers", "scan");

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
            PacketDistributor.sendToPlayer(player, new MessageToClientPresetContent(safe, nbt));
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public MessageRequestPresetContent fromBytes(RegistryFriendlyByteBuf buf) {
        this.presetName = buf.readUtf();
        return this;
    }

    @Override
    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(presetName);
    }

    public static final CustomPacketPayload.Type<MessageRequestPresetContent> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("workers", "message_request_preset_content"));

    @Override
    public CustomPacketPayload.Type<MessageRequestPresetContent> type() {
        return TYPE;
    }
}
