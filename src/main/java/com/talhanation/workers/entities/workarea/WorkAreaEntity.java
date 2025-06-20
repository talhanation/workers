package com.talhanation.workers.entities.workarea;

import com.talhanation.workers.entities.FarmerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public abstract class WorkAreaEntity extends Entity {
    public static final EntityDataAccessor<String> PLAYER_NAME = SynchedEntityData.defineId(WorkAreaEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<String> PLAYER_UUID = SynchedEntityData.defineId(WorkAreaEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Integer> SIZE = SynchedEntityData.defineId(WorkAreaEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> HEIGHT = SynchedEntityData.defineId(WorkAreaEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<String> TEAM_STRING_ID = SynchedEntityData.defineId(WorkAreaEntity.class, EntityDataSerializers.STRING);
    public boolean isDone;
    public boolean isBeingWorkedOn;

    public WorkAreaEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.setInvulnerable(true);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(PLAYER_NAME, "");
        this.entityData.define(PLAYER_UUID, "");
        this.entityData.define(SIZE, 0);
        this.entityData.define(HEIGHT, 0);
        this.entityData.define(TEAM_STRING_ID, "");
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        this.isDone = tag.getBoolean("isDone");
        this.resetTimer = tag.getInt("resetTimer");
        this.isBeingWorkedOn = tag.getBoolean("isBeingWorkedOn");
        this.setPlayerUUID(UUID.fromString(tag.getString("playerUUID")));
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
        tag.putInt("resetTimer", resetTimer);
        tag.putBoolean("isBeingWorkedOn", isBeingWorkedOn);
        tag.putInt("size", getSize());
        tag.putInt("height", getHeight());
        if(!this.getTeamStringID().isEmpty()){
            tag.putString("teamStringID", getTeamStringID());
        }
    }

    int resetTimer;
    @Override
    public void tick() {
        super.tick();
        if(isDone && resetTimer++ > 20*60*3){
            resetTimer = 0;
            this.setDone(false);
        }
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

    public boolean canWorkHere(FarmerEntity farmer) {
        return farmer.isOwned() && (farmer.getOwnerUUID().equals(this.getPlayerUUID()) || (farmer.getTeam() != null && this.getTeamStringID() != null && this.getTeamStringID().equals(farmer.getTeam().getName())));
    }

    public void setDone(boolean b) {
        this.isDone = b;
    }

    public boolean isDone(){
        return this.isDone;
    }

    public void setBeingWorkedOn(boolean b) {
        this.isBeingWorkedOn = b;
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
        this.entityData.set(PLAYER_UUID, playerUUID.toString());
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
        return UUID.fromString(this.entityData.get(PLAYER_UUID));
    }

    public abstract Item getRenderItem();
}
