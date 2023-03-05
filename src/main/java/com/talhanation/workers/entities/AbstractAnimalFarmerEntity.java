package com.talhanation.workers.entities;

import com.talhanation.workers.Main;
import com.talhanation.workers.inventory.AnimalFarmerInventoryContainer;
import com.talhanation.workers.network.MessageOpenGuiAnimalFarmer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public abstract class AbstractAnimalFarmerEntity extends AbstractWorkerEntity {

    private static final EntityDataAccessor<Integer> MAX_ANIMALS = SynchedEntityData
            .defineId(AbstractAnimalFarmerEntity.class, EntityDataSerializers.INT);

    public AbstractAnimalFarmerEntity(EntityType<? extends AbstractWorkerEntity> entityType, Level world) {
        super(entityType, world);
    }

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(MAX_ANIMALS, 8);
    }

    public void addAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("MaxAnimals", this.getMaxAnimalCount());
    }

    public void readAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.setMaxAnimalCount(nbt.getInt("MaxAnimals"));
    }

    public void setMaxAnimalCount(int x) {
        this.entityData.set(MAX_ANIMALS, x);
    }

    public int getMaxAnimalCount() {
        return entityData.get(MAX_ANIMALS);
    }

    @Override
    public void openGUI(Player player) {
        if (player instanceof ServerPlayer) {
            NetworkHooks.openScreen((ServerPlayer) player, new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return getName();
                }

                @Nullable
                @Override
                public AbstractContainerMenu createMenu(int i, Inventory playerInventory, Player playerEntity) {
                    return new AnimalFarmerInventoryContainer(i, AbstractAnimalFarmerEntity.this, playerInventory);
                }
            }, packetBuffer -> {
                packetBuffer.writeUUID(getUUID());
            });
        } else {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageOpenGuiAnimalFarmer(player, this.getUUID()));
        }
    }
}
