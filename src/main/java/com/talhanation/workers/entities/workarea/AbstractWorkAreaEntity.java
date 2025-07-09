package com.talhanation.workers.entities.workarea;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public abstract class AbstractWorkAreaEntity extends Entity {
    public static final EntityDataAccessor<String> PLAYER_NAME = SynchedEntityData.defineId(AbstractWorkAreaEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Optional<UUID>> PLAYER_UUID = SynchedEntityData.defineId(AbstractWorkAreaEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    public static final EntityDataAccessor<Integer> X_SIZE = SynchedEntityData.defineId(AbstractWorkAreaEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> Z_SIZE = SynchedEntityData.defineId(AbstractWorkAreaEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> Y_SIZE = SynchedEntityData.defineId(AbstractWorkAreaEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<String> TEAM_STRING_ID = SynchedEntityData.defineId(AbstractWorkAreaEntity.class, EntityDataSerializers.STRING);
    public boolean isDone;
    public boolean isBeingWorkedOn;
    public static int DONE_TIME =  20*60;
    public boolean showBox;

    public AbstractWorkAreaEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.setInvulnerable(true);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(PLAYER_NAME, "");
        this.entityData.define(PLAYER_UUID, Optional.empty());
        this.entityData.define(X_SIZE, 0);
        this.entityData.define(Y_SIZE, 0);
        this.entityData.define(Z_SIZE, 0);
        this.entityData.define(TEAM_STRING_ID, "");
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        this.isDone = tag.getBoolean("isDone");
        this.timeSinceLastVisit = tag.getInt("timeSinceLastVisit");
        this.isBeingWorkedOn = tag.getBoolean("isBeingWorkedOn");
        this.setPlayerUUID(tag.getUUID("playerUUID"));
        this.setXSize(tag.getInt("Xsize"));
        this.setYSize(tag.getInt("Ysize"));
        this.setZSize(tag.getInt("Zsize"));
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
        tag.putInt("size", getXSize());
        tag.putInt("height", getYSize());
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


    public static List<AbstractWorkAreaEntity> getNearbyAreas(Level level, BlockPos center, int radius) {
        List<AbstractWorkAreaEntity> nearby = new ArrayList<>();

        for (AbstractWorkAreaEntity area : level.getEntitiesOfClass(AbstractWorkAreaEntity.class, new AABB(center).inflate(radius))) {
            if (!area.getOnPos().equals(center)) {
                nearby.add(area);
            }
        }

        return nearby;
    }

    public static boolean isAreaOverlapping(Level level, AbstractWorkAreaEntity currentArea, AABB targetBox) {
        for (AbstractWorkAreaEntity other : level.getEntitiesOfClass(AbstractWorkAreaEntity.class, targetBox.inflate(1))) {
            if (other != currentArea && other.getBoundingBox().intersects(targetBox)) {
                return true;
            }
        }
        return false;
    }

    public boolean isBeingWorkedOn(){
        return this.isBeingWorkedOn;
    }
    public void setXSize(int size) {
        this.entityData.set(X_SIZE, size);
    }
    public void setYSize(int height) {
        this.entityData.set(Y_SIZE, height);
    }
    public void setZSize(int size) {
        this.entityData.set(Z_SIZE, size);
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

    public int getYSize() {
        return this.entityData.get(Y_SIZE);
    }

    public int getXSize() {
        return this.entityData.get(X_SIZE);
    }
    public int getZSize() {
        return this.entityData.get(Z_SIZE);
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
