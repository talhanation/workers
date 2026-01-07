package com.talhanation.workers.entities;

import com.talhanation.workers.init.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

public class FishingBobberEntity extends Projectile {
    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID = SynchedEntityData.defineId(FishingBobberEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private int life;
    @Nullable
    private FishermanEntity owner;

    public FishingBobberEntity(EntityType<? extends FishingBobberEntity> type, Level level) {
        super(type, level);
    }

    public FishingBobberEntity(Level level, FishermanEntity owner) {
        this(ModEntityTypes.FISHING_BOBBER.get(), level);
        this.setOwner(owner);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_UUID, Optional.empty());
    }

    @Override
    protected boolean canHitEntity(Entity p_36842_) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if(!this.level().isClientSide()) {
            this.life++;
            if (this.life > 3000 || this.getOwner() == null) {
                this.discard();
            }
        }

        float f = 0.0F;
        BlockPos blockpos = this.blockPosition();
        FluidState fluidstate = this.level().getFluidState(blockpos);
        if (fluidstate.is(FluidTags.WATER)) {
            f = fluidstate.getHeight(this.level(), blockpos);
        }
        if (this.isInWater()) {
            Vec3 vec3 = this.getDeltaMovement();
            double d0 = this.getY() + vec3.y - (double)blockpos.getY() - (double)f;
            if (Math.abs(d0) < 0.01D) {
                d0 += Math.signum(d0) * 0.1D;
            }

            this.setDeltaMovement(vec3.x * 0.9D, vec3.y - d0 * (double)this.random.nextFloat() * 0.2D, vec3.z * 0.9D);
        }
        else{
            if(!this.onGround()) this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.03D, 0.0D));
        }

        this.move(MoverType.SELF, this.getDeltaMovement());
        this.updateRotation();

        this.reapplyPosition();

        this.setDeltaMovement(this.getDeltaMovement().scale(0.92D));
    }

    @Nullable
    public UUID getOwnerUUID() {
        return this.entityData.get(OWNER_UUID).orElse(null);
    }

    public void setOwner(FishermanEntity owner) {
        super.setOwner(owner);
        if (owner == null) return;

        this.owner = owner;
        this.setOwnerUUID(owner.getUUID());
    }

    public FishermanEntity getOwner() {
        if (this.owner == null && this.getOwnerUUID() != null) {
            Optional<FishermanEntity> owner = this.getCommandSenderWorld().getEntitiesOfClass(FishermanEntity.class, this.getBoundingBox().inflate(16))
                    .stream()
                    .filter(fisherman -> fisherman.getUUID().equals(this.getOwnerUUID()))
                    .findFirst();

            owner.ifPresent(this::setOwner);
        }
        return this.owner;
    }

    public int getWaterDepth() {
        if (!isInWater()) {
            return 0;
        }

        BlockPos pos = this.blockPosition();
        int depth = 0;

        while (this.level().getBlockState(pos.below(depth + 1)).is(Blocks.WATER)) {
            depth++;
            if (depth > 64) break;
        }

        return depth + 1;
    }

    public void setOwnerUUID(UUID owner) {
        this.entityData.set(OWNER_UUID, Optional.of(owner));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        this.life = nbt.getInt("life");
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        nbt.putInt("life", this.life);
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}