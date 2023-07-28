package com.talhanation.workers.network;

import com.talhanation.workers.entities.MerchantEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class MessageMerchantHorse implements Message<MessageMerchantHorse> {
    private UUID worker;

    public MessageMerchantHorse() {
    }

    public MessageMerchantHorse(UUID recruit) {
        this.worker = recruit;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {

        ServerPlayer player = context.getSender();
        player.level.getEntitiesOfClass(MerchantEntity.class, player.getBoundingBox()
                        .inflate(16.0D), v -> v
                        .getUUID()
                        .equals(this.worker))
                .stream()
                .filter(MerchantEntity::isAlive)
                .findAny()
                .ifPresent(this::setTraveling);

    }

    private void setTraveling(MerchantEntity merchant) {
        //
        if(merchant.getVehicle() != null){
            merchant.stopRiding();
        }
        else {
            List<AbstractHorse> horseList = merchant.level.getEntitiesOfClass(AbstractHorse.class, merchant.getBoundingBox().inflate(16));
            horseList.sort(Comparator.comparing(horseInList -> horseInList.distanceTo(merchant)));

            if(merchant.getHorseUUID() == null){
                for(int i = 0; i < horseList.size(); i++){
                    merchant.setHorseUUID(Optional.of(horseList.get(i).getUUID()));
                    merchant.startRiding(horseList.get(i));
                }
            }
            else{
                for (AbstractHorse horse : horseList){
                    if(merchant.getHorseUUID() != null && horse.getUUID().equals(merchant.getHorseUUID())){
                        merchant.startRiding(horse);
                    }
                }
            }

        }
    }

    public MessageMerchantHorse fromBytes(FriendlyByteBuf buf) {
        this.worker = buf.readUUID();

        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.worker);
    }
}