package com.talhanation.workers.entities.workarea;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public abstract class AbstractWorkAreaEntity extends Entity {
    public static final EntityDataAccessor<String> PLAYER_NAME = SynchedEntityData.defineId(AbstractWorkAreaEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Optional<UUID>> PLAYER_UUID = SynchedEntityData.defineId(AbstractWorkAreaEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    public static final EntityDataAccessor<Integer> WIDTH = SynchedEntityData.defineId(AbstractWorkAreaEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> DEPTH = SynchedEntityData.defineId(AbstractWorkAreaEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> HEIGHT = SynchedEntityData.defineId(AbstractWorkAreaEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<String> TEAM_STRING_ID = SynchedEntityData.defineId(AbstractWorkAreaEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Direction> FACING = SynchedEntityData.defineId(AbstractWorkAreaEntity.class, EntityDataSerializers.DIRECTION);
    public boolean isDone;
    public boolean isBeingWorkedOn;
    public static int DONE_TIME =  20*60;
    public boolean showBox;
    public AABB area;
    public AbstractWorkAreaEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.setInvulnerable(true);
        this.createArea();
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(PLAYER_NAME, "");
        this.entityData.define(PLAYER_UUID, Optional.empty());
        this.entityData.define(WIDTH, 0);
        this.entityData.define(HEIGHT, 0);
        this.entityData.define(DEPTH, 0);
        this.entityData.define(FACING, Direction.SOUTH);
        this.entityData.define(TEAM_STRING_ID, "");
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        this.isDone = tag.getBoolean("isDone");
        this.time = tag.getInt("time");
        this.isBeingWorkedOn = tag.getBoolean("isBeingWorkedOn");
        this.setPlayerUUID(tag.getUUID("playerUUID"));
        this.setWidthSize(tag.getInt("width"));
        this.setHeightSize(tag.getInt("height"));
        this.setDepthSize(tag.getInt("depth"));
        this.setFacing(Direction.from3DDataValue(tag.getInt("facing")));
        if(tag.contains("teamStringID")){
            this.setTeamStringID(tag.getString("teamStringID"));
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        tag.putUUID("playerUUID", getPlayerUUID());
        tag.putBoolean("isDone", isDone);
        tag.putBoolean("isBeingWorkedOn", isBeingWorkedOn);
        tag.putInt("width", getWidthSize());
        tag.putInt("height", getHeightSize());
        tag.putInt("depth", getDepthSize());
        tag.putInt("facing", this.getFacing().get3DDataValue());
        if(!this.getTeamStringID().isEmpty()){
            tag.putString("teamStringID", getTeamStringID());
        }
        tag.putInt("time", time);
    }

    public int time;
    @Override
    public void tick() {
        super.tick();
        if(tickCount % 20 == 0) time++;
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
        this.isBeingWorkedOn = b;
    }

    public int getTime(){
        return this.time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public AABB getArea() {
        return createArea();
    }

    public AABB createArea() {
        Direction facing = getFacing();
        int width = getWidthSize() - 1;
        int depth = getDepthSize() - 1;
        int height = getHeightSize();

        BlockPos start = this.getOnPos();
        BlockPos end;
        switch (facing) {
            case NORTH -> end = start.offset(width, height, -depth);
            case SOUTH -> end = start.offset(-width, height, depth);
            case WEST  -> end = start.offset(-width, height, -depth);
            default  -> end = start.offset(width, height, depth);//EAST
        }
        return new AABB(start, end);
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
    public void setWidthSize(int size) {
        this.entityData.set(WIDTH, size);
    }
    public void setHeightSize(int height) {
        this.entityData.set(HEIGHT, height);
    }
    public void setDepthSize(int size) {
        this.entityData.set(DEPTH, size);
    }

    public void setFacing(Direction direction) {
        this.entityData.set(FACING, direction);
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

    public int getHeightSize() {
        return this.entityData.get(HEIGHT);
    }

    public int getWidthSize() {
        return this.entityData.get(WIDTH);
    }
    public int getDepthSize() {
        return this.entityData.get(DEPTH);
    }

    public Direction getFacing() {
        return this.entityData.get(FACING);
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
    @OnlyIn(Dist.CLIENT)
    public abstract Screen getScreen(Player player);

    public BlockPos getOriginPos() {
        return this.getOnPos();
    }
}
