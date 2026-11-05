package com.talhanation.workers.network;

import com.talhanation.workers.entities.workarea.AnimalPenArea;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

import static com.talhanation.workers.entities.workarea.AbstractWorkAreaEntity.DONE_TIME;

public class MessageUpdateAnimalPenArea implements Message<MessageUpdateAnimalPenArea> {

    public UUID uuid;
    public boolean slaughter;
    public boolean breed;
    public boolean special;
    public boolean gather;
    public int maxAnimals;
    public int animalTypeIndex;
    public MessageUpdateAnimalPenArea() {
    }
    public MessageUpdateAnimalPenArea(UUID uuid, AnimalPenArea.AnimalTypes animalType, boolean breed, boolean slaughter, boolean special, boolean gather, int maxAnimals) {
        this.uuid = uuid;
        this.animalTypeIndex = animalType.getIndex();
        this.breed = breed;
        this.slaughter = slaughter;
        this.special = special;
        this.gather = gather;
        this.maxAnimals = maxAnimals;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(IPayloadContext context){
        ServerPlayer player = ((ServerPlayer) context.player());
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
        animalPenArea.setGather(this.gather);
        animalPenArea.setAnimalType(AnimalPenArea.AnimalTypes.fromIndex(this.animalTypeIndex));

        animalPenArea.time += DONE_TIME;
    }

    public MessageUpdateAnimalPenArea fromBytes(RegistryFriendlyByteBuf buf) {
        this.uuid = buf.readUUID();
        this.slaughter = buf.readBoolean();
        this.breed = buf.readBoolean();
        this.special = buf.readBoolean();
        this.gather = buf.readBoolean();
        this.maxAnimals = buf.readInt();
        this.animalTypeIndex = buf.readInt();
        return this;
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(uuid);
        buf.writeBoolean(slaughter);
        buf.writeBoolean(breed);
        buf.writeBoolean(special);
        buf.writeBoolean(gather);
        buf.writeInt(maxAnimals);
        buf.writeInt(animalTypeIndex);
    }

    public static final CustomPacketPayload.Type<MessageUpdateAnimalPenArea> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("workers", "message_update_animal_pen_area"));

    @Override
    public CustomPacketPayload.Type<MessageUpdateAnimalPenArea> type() {
        return TYPE;
    }
}
