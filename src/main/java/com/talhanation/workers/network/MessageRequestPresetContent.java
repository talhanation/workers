package com.talhanation.workers.network;

import com.talhanation.workers.WorkersMain;
import com.talhanation.workers.config.BuildMode;
import com.talhanation.workers.config.WorkersServerConfig;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Team;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

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
    public Dist getExecutingSide(){
        return Dist.DEDICATED_SERVER;
    }

    @Override
    public void executeServerSide(NetworkEvent.Context context){
        ServerPlayer player = context.getSender();
        if (player == null) return;

        String safe = presetName.replace("..", "").replace("/", "").replace("\\", "");

        BuildMode mode = WorkersServerConfig.BuildModeConfig.get();
        Path scanRoot = Path.of(player.server.getServerDirectory().getAbsolutePath(), "workers", "scan");

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
            CompoundTag nbt = NbtIo.readCompressed(file);
            WorkersMain.SIMPLE_CHANNEL.sendTo(new MessageToClientPresetContent(safe, nbt), player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
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
