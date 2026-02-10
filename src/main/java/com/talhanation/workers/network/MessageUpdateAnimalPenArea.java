package com.talhanation.workers.network;

import com.talhanation.workers.entities.workarea.AnimalPenArea;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;

import static com.talhanation.workers.entities.workarea.AbstractWorkAreaEntity.DONE_TIME;

public class MessageUpdateAnimalPenArea implements Message<MessageUpdateAnimalPenArea> {

    public UUID uuid;
    public boolean slaughter;
    public boolean breed;
    public boolean special;
    public int maxAnimals;
    public int animalTypeIndex;
    public MessageUpdateAnimalPenArea() {
    }
    public MessageUpdateAnimalPenArea(UUID uuid, AnimalPenArea.AnimalTypes animalType, boolean breed, boolean slaughter, boolean special, int maxAnimals) {
        this.uuid = uuid;
        this.animalTypeIndex = animalType.getIndex();
        this.breed = breed;
        this.slaughter = slaughter;
        this.special = special;
        this.maxAnimals = maxAnimals;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context){
        ServerPlayer player = context.getSender();
        if(player == null) return;

        player.getCommandSenderWorld().getEntitiesOfClass(AnimalPenArea.class, player.getBoundingBox()
                        .inflate(16.0D), v -> v
                        .getUUID()
                        .equals(this.uuid))
                .stream()
                .findAny()
                .ifPresent(this::update);

    }

    public void update(AnimalPenArea animalPenArea){
        animalPenArea.setMaxAnimals(this.maxAnimals);
        animalPenArea.setSlaughter(this.slaughter);
        animalPenArea.setBreed(this.breed);
        animalPenArea.setSpecial(this.special);
        animalPenArea.setAnimalType(AnimalPenArea.AnimalTypes.fromIndex(this.animalTypeIndex));

        animalPenArea.time += DONE_TIME;
    }

    public MessageUpdateAnimalPenArea fromBytes(FriendlyByteBuf buf) {
        this.uuid = buf.readUUID();
        this.slaughter = buf.readBoolean();
        this.breed = buf.readBoolean();
        this.special = buf.readBoolean();
        this.maxAnimals = buf.readInt();
        this.animalTypeIndex = buf.readInt();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(uuid);
        buf.writeBoolean(slaughter);
        buf.writeBoolean(breed);
        buf.writeBoolean(special);
        buf.writeInt(maxAnimals);
        buf.writeInt(animalTypeIndex);
    }
}
