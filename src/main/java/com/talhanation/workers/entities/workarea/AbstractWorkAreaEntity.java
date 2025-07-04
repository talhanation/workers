package com.talhanation.workers.entities.workarea;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.UUID;

public abstract class AbstractWorkAreaEntity extends Entity {
    public static final EntityDataAccessor<String> PLAYER_NAME = SynchedEntityData.defineId(AbstractWorkAreaEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Optional<UUID>> PLAYER_UUID = SynchedEntityData.defineId(AbstractWorkAreaEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    public static final EntityDataAccessor<Integer> SIZE = SynchedEntityData.defineId(AbstractWorkAreaEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> HEIGHT = SynchedEntityData.defineId(AbstractWorkAreaEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<String> TEAM_STRING_ID = SynchedEntityData.defineId(AbstractWorkAreaEntity.class, EntityDataSerializers.STRING);
    public boolean isDone;
    public boolean isBeingWorkedOn;
    public static int DONE_TIME =  20*60;
    public boolean showBox;
    public boolean isBuild;

    public AbstractWorkAreaEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.setInvulnerable(true);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(PLAYER_NAME, "");
        this.entityData.define(PLAYER_UUID, Optional.empty());
        this.entityData.define(SIZE, 0);
        this.entityData.define(HEIGHT, 0);
        this.entityData.define(TEAM_STRING_ID, "");
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        this.isDone = tag.getBoolean("isDone");
        this.timeSinceLastVisit = tag.getInt("timeSinceLastVisit");
        this.isBeingWorkedOn = tag.getBoolean("isBeingWorkedOn");
        this.setPlayerUUID(tag.getUUID("playerUUID"));
        this.setSize(tag.getInt("size"));
        this.setHeight(tag.getInt("height"));

        if(tag.contains("teamStringID")){
            this.setTeamStringID(tag.getString("teamStringID"));
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        tag.putUUID("playerUUID", getPlayerUUID());
        tag.putBoolean("isDone", isDone);
        tag.putInt("timeSinceLastVisit", timeSinceLastVisit);
        tag.putBoolean("isBeingWorkedOn", isBeingWorkedOn);
        tag.putInt("size", getSize());
        tag.putInt("height", getHeight());
        if(!this.getTeamStringID().isEmpty()){
            tag.putString("teamStringID", getTeamStringID());
        }
    }

    public int timeSinceLastVisit;
    @Override
    public void tick() {
        super.tick();
        if(!isBeingWorkedOn()) timeSinceLastVisit++;

    }
    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean hurt(DamageSource damageSource, float a) {
        return false;
    }

    @Override
    protected boolean canAddPassenger(Entity entity) {
        return false;
    }

    @Override
    protected boolean canRide(Entity entity) {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean canBeHitByProjectile() {
        return false;
    }

    @Override
    public boolean canRiderInteract() {
        return false;
    }

    @Override
    public boolean canFreeze() {
        return false;
    }

    @Override
    public boolean isEffectiveAi() {
        return false;
    }

    public boolean canWorkHere(AbstractWorkerEntity worker) {
        return worker.isOwned() && (worker.getOwnerUUID().equals(this.getPlayerUUID()) || (worker.getTeam() != null && this.getTeamStringID() != null && this.getTeamStringID().equals(worker.getTeam().getName())));
    }

    public void setDone(boolean b) {
        this.isDone = b;
    }

    public boolean isDone(){
        return this.isDone;
    }

    public void setBeingWorkedOn(boolean b) {
        if(b) this.timeSinceLastVisit = 0;
        this.isBeingWorkedOn = b;
    }

    public int getTimeSinceLastVisit(){
        return this.timeSinceLastVisit;
    }


    public boolean isBeingWorkedOn(){
        return this.isBeingWorkedOn;
    }
    public void setSize(int size) {
        this.entityData.set(SIZE, size);
    }
    public void setHeight(int height) {
        this.entityData.set(HEIGHT, height);
    }

    public void setPlayerName(String playerName) {
        this.entityData.set(PLAYER_NAME, playerName);
    }

    public void setPlayerUUID(UUID playerUUID) {
        this.entityData.set(PLAYER_UUID, Optional.of(playerUUID));
    }

    public void setTeamStringID(String teamStringID) {
        this.entityData.set(TEAM_STRING_ID, teamStringID);
    }

    public int getHeight() {
        return this.entityData.get(HEIGHT);
    }

    public int getSize() {
        return this.entityData.get(SIZE);
    }

    public String getTeamStringID(){
        return this.entityData.get(TEAM_STRING_ID);
    }

    public String getPlayerName(){
        return this.entityData.get(PLAYER_NAME);
    }

    public UUID getPlayerUUID(){
        return this.entityData.get(PLAYER_UUID).orElse(null);
    }

    public abstract Item getRenderItem();
    public abstract Screen getScreen(Player player);
}
