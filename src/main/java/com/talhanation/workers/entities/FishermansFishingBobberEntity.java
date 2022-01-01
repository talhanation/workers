package com.talhanation.workers.entities;

import java.util.Random;

import com.talhanation.workers.init.ModEntityTypes;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileHelper;
import net.minecraft.entity.projectile.ProjectileItemEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.server.SSpawnObjectPacket;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.RegistryObject;

public class FishermansFishingBobberEntity{}
        /*extends ProjectileItemEntity {
    private final Random syncronizedRandom = new Random();
    private int outOfWaterTime;
    private int life;
    private int nibble;
    private int timeUntilHooked;
    private float fishAngle;
    private boolean openWater = true;
    private State currentState = State.FLYING;


    private FishermansFishingBobberEntity(World world, LivingEntity owner) {
        super(ModEntityTypes.FISHING_BOBBER.get(), owner, world);
        this.noCulling = true;
        this.setOwner(owner);
    }

    @OnlyIn(Dist.CLIENT)
    public FishermansFishingBobberEntity(World world, LivingEntity owner, double x, double y, double z) {
        this(world);
        this.setPos(x, y, z);
        this.xo = this.getX();
        this.yo = this.getY();
        this.zo = this.getZ();
    }

    public FishermansFishingBobberEntity(World world, LivingEntity owner) {
        this(world, owner);
        float f = owner.xRot;
        float f1 = owner.yRot;
        float f2 = MathHelper.cos(-f1 * ((float) Math.PI / 180F) - (float) Math.PI);
        float f3 = MathHelper.sin(-f1 * ((float) Math.PI / 180F) - (float) Math.PI);
        float f4 = -MathHelper.cos(-f * ((float) Math.PI / 180F));
        float f5 = MathHelper.sin(-f * ((float) Math.PI / 180F));
        double d0 = owner.getX() - (double) f3 * 0.3D;
        double d1 = owner.getEyeY();
        double d2 = owner.getZ() - (double) f2 * 0.3D;
        this.moveTo(d0, d1, d2, f1, f);
        Vector3d vector3d = new Vector3d((double) (-f3), (double) MathHelper.clamp(-(f5 / f4), -5.0F, 5.0F), (double) (-f2));
        double d3 = vector3d.length();
        vector3d = vector3d.multiply(0.6D / d3 + 0.5D + this.random.nextGaussian() * 0.0045D, 0.6D / d3 + 0.5D + this.random.nextGaussian() * 0.0045D, 0.6D / d3 + 0.5D + this.random.nextGaussian() * 0.0045D);
        this.setDeltaMovement(vector3d);
        this.yRot = (float) (MathHelper.atan2(vector3d.x, vector3d.z) * (double) (180F / (float) Math.PI));
        this.xRot = (float) (MathHelper.atan2(vector3d.y, (double) MathHelper.sqrt(getHorizontalDistanceSqr(vector3d))) * (double) (180F / (float) Math.PI));
        this.yRotO = this.yRot;
        this.xRotO = this.xRot;
    }

    @OnlyIn(Dist.CLIENT)
    public boolean shouldRenderAtSqrDistance(double p_70112_1_) {
        double d0 = 64.0D;
        return p_70112_1_ < 4096.0D;
    }

    @OnlyIn(Dist.CLIENT)
    public void lerpTo(double p_180426_1_, double p_180426_3_, double p_180426_5_, float p_180426_7_, float p_180426_8_, int p_180426_9_, boolean p_180426_10_) {
    }

    public void tick() {
        this.syncronizedRandom.setSeed(this.getUUID().getLeastSignificantBits() ^ this.level.getGameTime());
        super.tick();
        Entity owner = this.getOwner();
        if (owner == null) {
            this.remove();
        } else if (this.level.isClientSide) {
            if (this.onGround) {
                ++this.life;
                if (this.life >= 1200) {
                    this.remove();
                    return;
                }
            } else {
                this.life = 0;
            }

            float f = 0.0F;
            BlockPos blockpos = this.blockPosition();
            FluidState fluidstate = this.level.getFluidState(blockpos);
            if (fluidstate.is(FluidTags.WATER)) {
                f = fluidstate.getHeight(this.level, blockpos);
            }

            boolean flag = f > 0.0F;
            if (this.currentState == State.FLYING) {

                if (flag) {
                    this.setDeltaMovement(this.getDeltaMovement().multiply(0.3D, 0.2D, 0.3D));
                    this.currentState = State.BOBBING;
                    return;
                }

                this.checkCollision();
            } else {

                if (this.currentState == State.BOBBING) {
                    Vector3d vector3d = this.getDeltaMovement();
                    double d0 = this.getY() + vector3d.y - (double)blockpos.getY() - (double)f;
                    if (Math.abs(d0) < 0.01D) {
                        d0 += Math.signum(d0) * 0.1D;
                    }

                    this.setDeltaMovement(vector3d.x * 0.9D, vector3d.y - d0 * (double)this.random.nextFloat() * 0.2D, vector3d.z * 0.9D);
                    if (this.nibble <= 0 && this.timeUntilHooked <= 0) {
                        this.openWater = true;
                    } else {
                        this.openWater = this.openWater && this.outOfWaterTime < 10 && this.calculateOpenWater(blockpos);
                    }

                    if (flag) {
                        this.outOfWaterTime = Math.max(0, this.outOfWaterTime - 1);

                    } else {
                        this.outOfWaterTime = Math.min(10, this.outOfWaterTime + 1);
                    }
                }
            }

            if (!fluidstate.is(FluidTags.WATER)) {
                this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.03D, 0.0D));
            }

            this.move(MoverType.SELF, this.getDeltaMovement());
            this.updateRotation();
            if (this.currentState == State.FLYING && (this.onGround || this.horizontalCollision)) {
                this.setDeltaMovement(Vector3d.ZERO);
            }

            double d1 = 0.92D;
            this.setDeltaMovement(this.getDeltaMovement().scale(0.92D));
            this.reapplyPosition();
        }
    }

    private boolean shouldStopFishing(PlayerEntity p_234600_1_) {
        ItemStack itemstack = p_234600_1_.getMainHandItem();
        ItemStack itemstack1 = p_234600_1_.getOffhandItem();
        boolean flag = itemstack.getItem() == Items.FISHING_ROD;
        boolean flag1 = itemstack1.getItem() == Items.FISHING_ROD;
        if (!p_234600_1_.removed && p_234600_1_.isAlive() && (flag || flag1) && !(this.distanceToSqr(p_234600_1_) > 1024.0D)) {
            return false;
        } else {
            this.remove();
            return true;
        }
    }

    private void checkCollision() {
        RayTraceResult raytraceresult = ProjectileHelper.getHitResult(this, this::canHitEntity);
        this.onHit(raytraceresult);
    }

    protected boolean canHitEntity(Entity entity) {
        return false;
    }


    protected void onHitBlock(BlockRayTraceResult p_230299_1_) {
        super.onHitBlock(p_230299_1_);
        this.setDeltaMovement(this.getDeltaMovement().normalize().scale(p_230299_1_.distanceTo(this)));
    }


    private boolean calculateOpenWater(BlockPos p_234603_1_) {
        WaterType fishingbobberentity$watertype = WaterType.INVALID;

        for(int i = -1; i <= 2; ++i) {
            WaterType fishingbobberentity$watertype1 = this.getOpenWaterTypeForArea(p_234603_1_.offset(-2, i, -2), p_234603_1_.offset(2, i, 2));
            switch(fishingbobberentity$watertype1) {
                case INVALID:
                    return false;
                case ABOVE_WATER:
                    if (fishingbobberentity$watertype == WaterType.INVALID) {
                        return false;
                    }
                    break;
                case INSIDE_WATER:
                    if (fishingbobberentity$watertype == WaterType.ABOVE_WATER) {
                        return false;
                    }
            }

            fishingbobberentity$watertype = fishingbobberentity$watertype1;
        }

        return true;
    }

    private WaterType getOpenWaterTypeForArea(BlockPos p_234602_1_, BlockPos p_234602_2_) {
        return BlockPos.betweenClosedStream(p_234602_1_, p_234602_2_).map(this::getOpenWaterTypeForBlock).reduce((p_234601_0_, p_234601_1_) -> {
            return p_234601_0_ == p_234601_1_ ? p_234601_0_ : WaterType.INVALID;
        }).orElse(WaterType.INVALID);
    }

    private WaterType getOpenWaterTypeForBlock(BlockPos p_234604_1_) {
        BlockState blockstate = this.level.getBlockState(p_234604_1_);
        if (!blockstate.isAir() && !blockstate.is(Blocks.LILY_PAD)) {
            FluidState fluidstate = blockstate.getFluidState();
            return fluidstate.is(FluidTags.WATER) && fluidstate.isSource() && blockstate.getCollisionShape(this.level, p_234604_1_).isEmpty() ? WaterType.INSIDE_WATER : WaterType.INVALID;
        } else {
            return WaterType.ABOVE_WATER;
        }
    }

    public boolean isOpenWaterFishing() {
        return this.openWater;
    }

    public void addAdditionalSaveData(CompoundNBT p_213281_1_) {
    }

    public void readAdditionalSaveData(CompoundNBT p_70037_1_) {
    }



    protected boolean isMovementNoisy() {
        return false;
    }


    @Override
    protected Item getDefaultItem() {
        return null;
    }

    protected void defineSynchedData() {
    }

    @Override
    public void remove(boolean keepData) {
        Entity owner = this.getOwner();
        if (owner == null) {
            super.remove(keepData);
        }
    }

    public boolean canChangeDimensions() {
        return false;
    }

    public IPacket<?> getAddEntityPacket() {
        Entity entity = this.getOwner();
        return new SSpawnObjectPacket(this, entity == null ? this.getId() : entity.getId());
    }

    enum State {
        FLYING,
        BOBBING;
    }

    enum WaterType {
        ABOVE_WATER,
        INSIDE_WATER,
        INVALID;
    }
}
*/