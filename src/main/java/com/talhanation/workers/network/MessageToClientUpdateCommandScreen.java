package com.talhanation.workers.network;

import com.talhanation.workers.client.gui.CommandScreen;
import com.talhanation.workers.entities.AbstractWorkerEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.UUID;

public class MessageToClientUpdateCommandScreen implements Message<MessageToClientUpdateCommandScreen> {
    public List<UUID> ids;
    public List<String> names;
    //public List<AbstractWorkerEntity> workers;

    public MessageToClientUpdateCommandScreen() {
    }

    public MessageToClientUpdateCommandScreen(List<UUID> workers, List<String> names) {
        this.ids = workers;
        this.names = names;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.CLIENT;
    }

    @Override
    public void executeClientSide(NetworkEvent.Context context) {
        CommandScreen.worker_ids = this.ids;
        CommandScreen.worker_names = this.names;
    }

    @Override
    public MessageToClientUpdateCommandScreen fromBytes(FriendlyByteBuf buf) {
        //this.ids = buf.readList(FriendlyByteBuf::readNbt);
        this.ids = buf.readList(FriendlyByteBuf::readUUID);
        this.names = buf.readList(FriendlyByteBuf::readUtf);
        // TODO: NBT read for List<AbstractWorker>
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        //buf.writeCollection(ids, FriendlyByteBuf::writeNbt);
        buf.writeCollection(ids, FriendlyByteBuf::writeUUID);
        buf.writeCollection(names, FriendlyByteBuf::writeUtf);
        // TODO: NBT write for List<AbstractWorker>
    }
}
