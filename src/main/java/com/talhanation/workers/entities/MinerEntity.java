package com.talhanation.workers.entities;


import com.talhanation.workers.entities.ai.MinerMineTunnelGoal;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.AgeableEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ILivingEntityData;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.PanicGoal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.GroundPathNavigator;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.ForgeEventFactory;

import javax.annotation.Nullable;
import java.util.Optional;

public class MinerEntity extends AbstractWorkerEntity {

    private static final DataParameter<Integer> breakingTime = EntityDataManager.defineId(MinerEntity.class, DataSerializers.INT);
    private static final DataParameter<Integer> currentTimeBreak = EntityDataManager.defineId(MinerEntity.class, DataSerializers.INT);
    private static final DataParameter<Integer> previousTimeBreak = EntityDataManager.defineId(MinerEntity.class, DataSerializers.INT);

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(breakingTime, 0);
        this.entityData.define(currentTimeBreak, -1);
        this.entityData.define(previousTimeBreak, -1);
    }

    public MinerEntity(EntityType<? extends AbstractWorkerEntity> entityType, World world) {
        super(entityType, world);

    }

    //ATTRIBUTES
    public static AttributeModifierMap.MutableAttribute setAttributes() {
        return createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);

    }

    protected void registerGoals() {
        this.goalSelector.addGoal(2, new MinerMineTunnelGoal(this, 0.5D, 10D));
    }

    @Nullable
    public ILivingEntityData finalizeSpawn(IServerWorld world, DifficultyInstance difficultyInstance, SpawnReason reason, @Nullable ILivingEntityData data, @Nullable CompoundNBT nbt) {
        ILivingEntityData ilivingentitydata = super.finalizeSpawn(world, difficultyInstance, reason, data, nbt);
        ((GroundPathNavigator)this.getNavigation()).setCanOpenDoors(true);
        this.populateDefaultEquipmentEnchantments(difficultyInstance);
        this.setEquipment();
        this.setDropEquipment();
        this.setCanPickUpLoot(true);
        return ilivingentitydata;
    }
    @Override
    public void setEquipment() {
        int i = this.random.nextInt(9);
        if (i == 0) {
            this.setItemSlot(EquipmentSlotType.MAINHAND, new ItemStack(Items.STONE_PICKAXE));
        }else{
            this.setItemSlot(EquipmentSlotType.MAINHAND, new ItemStack(Items.WOODEN_PICKAXE));
        }
    }

    @Nullable
    @Override
    public AgeableEntity getBreedOffspring(ServerWorld world, AgeableEntity ageable) {
        return null;
    }

    @Override
    public int workerCosts() {
        return 10;
    }

    @Override
    public String workerName() {
        return "Miner";
    }

    public int getBreakingTime() {
        return this.entityData.get(breakingTime);
    }

    public void setBreakingTime(int value) {
        this.entityData.set(breakingTime, value);
    }

    public int getCurrentTimeBreak() {
        return this.entityData.get(currentTimeBreak);
    }

    public void setCurrentTimeBreak(int value) {
        this.entityData.set(currentTimeBreak, value);
    }

    public int getPreviousTimeBreak() {
        return this.entityData.get(previousTimeBreak);
    }

    public void setPreviousTimeBreak(int value) {
        this.entityData.set(previousTimeBreak, value);
    }

    public void addAdditionalSaveData(CompoundNBT compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("breakTime", this.getBreakingTime());
        compound.putInt("currentTimeBreak", this.getCurrentTimeBreak());
        compound.putInt("previousTimeBreak", this.getPreviousTimeBreak());
    }

    public void readAdditionalSaveData(CompoundNBT compound) {
        super.readAdditionalSaveData(compound);
        this.setBreakingTime(compound.getInt("breakTime"));
        this.setCurrentTimeBreak(compound.getInt("currentTimeBreak"));
        this.setPreviousTimeBreak(compound.getInt("previousTimeBreak"));
    }

    public Direction getMineDirectrion() {
        return Direction.EAST;
    }

    public void mineBlock(BlockPos blockPos){
        if (!this.dead && ForgeEventFactory.getMobGriefingEvent(this.level, this) && !getFollow()) {
            boolean flag = false;
            BlockPos blockpos2 = blockPos.above();
            BlockState blockstate = this.level.getBlockState(blockPos);
            Block block = blockstate.getBlock();

            BlockState blockstate2 = this.level.getBlockState(blockPos.above());
            Block block2 = blockstate2.getBlock();

            if (block != Blocks.AIR) {

                if (this.getCurrentTimeBreak() % 5 == 4) {
                    level.playLocalSound(blockPos.getX(), blockPos.getY(), blockPos.getZ(), blockstate.getSoundType().getHitSound(), SoundCategory.BLOCKS, 1F, 0.75F, false);
                }

                //set max destroy speed
                int bp = (int) (blockstate.getDestroySpeed(this.level, blockPos) * 100);
                this.setBreakingTime(bp);

                //increase current
                this.setCurrentTimeBreak(this.getCurrentTimeBreak() + (int) (1 * (this.getUseItem().getDestroySpeed(blockstate))));
                float f = (float) this.getCurrentTimeBreak() / (float) this.getBreakingTime();

                int i = (int) (f * 10);

                if (i != this.getPreviousTimeBreak()) {
                    this.level.destroyBlockProgress(1, blockPos, i);
                    this.setPreviousTimeBreak(i);
                }

                if (this.getCurrentTimeBreak() >= this.getBreakingTime()) {
                    flag = this.level.destroyBlock(blockPos, true, this) || flag;
                    this.setCurrentTimeBreak(-1);
                    this.setBreakingTime(0);
                }
                if (this.getRandom().nextInt(5) == 0) {
                    if (!this.swinging) {
                        this.swing(this.getUsedItemHand());
                    }
                }
            } else if (block2 != Blocks.AIR) {

                if (this.getCurrentTimeBreak() % 5 == 4) {
                    level.playLocalSound(blockpos2.getX(), blockpos2.getY(), blockpos2.getZ(), blockstate2.getSoundType().getHitSound(), SoundCategory.BLOCKS, 1F, 0.75F, false);
                }

                //set max destroy speed
                int bp = (int) (blockstate2.getDestroySpeed(this.level, blockpos2.above()) * 100);
                this.setBreakingTime(bp);

                //increase current
                this.setCurrentTimeBreak(this.getCurrentTimeBreak() + (int) (1 * (this.getUseItem().getDestroySpeed(blockstate2))));
                float f = (float) this.getCurrentTimeBreak() / (float) this.getBreakingTime();

                int i = (int) (f * 10);

                if (i != this.getPreviousTimeBreak()) {
                    this.level.destroyBlockProgress(1, blockpos2, i);
                    this.setPreviousTimeBreak(i);
                }

                if (this.getCurrentTimeBreak() >= this.getBreakingTime()) {
                    flag = this.level.destroyBlock(blockpos2, true, this) || flag;
                    this.setCurrentTimeBreak(-1);
                    this.setBreakingTime(0);
                }
                if (this.getRandom().nextInt(5) == 0) {
                    if (!this.swinging) {
                        this.swing(this.getUsedItemHand());
                    }
                }

            }
        }

    }

}
