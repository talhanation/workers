package com.talhanation.workers.entities;

import com.talhanation.workers.Main;
import com.talhanation.workers.config.WorkersModConfig;
import com.talhanation.workers.entities.ai.ControlBoatAI;
import com.talhanation.workers.inventory.WorkerInventoryContainer;
import com.talhanation.workers.entities.ai.FishermanAI;
import com.talhanation.workers.entities.ai.WorkerPickupWantedItemGoal;
import com.talhanation.workers.network.MessageOpenGuiWorker;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Predicate;

public class FishermanEntity extends AbstractWorkerEntity implements IBoatController{

    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(FishermanEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<BlockPos>> SAIL_POS = SynchedEntityData.defineId(FishermanEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);

    private final Predicate<ItemEntity> ALLOWED_ITEMS = (item) -> {
        return (
            !item.hasPickUpDelay() && 
            item.isAlive() && 
            this.wantsToPickUp(item.getItem())
        );
    };

    public FishermanEntity(EntityType<? extends AbstractWorkerEntity> entityType, Level world) {
        super(entityType, world);
        this.initSpawn();
    }

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(STATE, 0);
        this.entityData.define(SAIL_POS, Optional.empty());
    }

    public void addAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("State", this.getState());
    }

    //Boat
    public void readAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.setState(nbt.getInt("State"));
    }

    public void setState(int state) {
        this.entityData.set(STATE, state);
    }

    public int getState() {
        return this.entityData.get(STATE);
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public int workerCosts() {
        return WorkersModConfig.FishermanCost.get();
    }

    @Override
    public Predicate<ItemEntity> getAllowedItems() {
        return ALLOWED_ITEMS;
    }

    // ATTRIBUTES
    public static AttributeSupplier.Builder setAttributes() {
        return createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new ControlBoatAI(this));
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new WorkerPickupWantedItemGoal(this));
        this.goalSelector.addGoal(4, new FishermanAI(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.3D));
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(@NotNull ServerLevel p_241840_1_, @NotNull AgeableMob p_241840_2_) {
        return null;
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor world, @NotNull DifficultyInstance difficultyInstance,
                                        @NotNull MobSpawnType reason, @Nullable SpawnGroupData data, @Nullable CompoundTag nbt) {
        SpawnGroupData ilivingentitydata = super.finalizeSpawn(world, difficultyInstance, reason, data, nbt);
        this.populateDefaultEquipmentEnchantments(random, difficultyInstance);

        this.initSpawn();

        return ilivingentitydata;
    }

    @Override
    public void initSpawn() {
        super.initSpawn();
        Component name = Component.literal("Fisherman");

        this.setProfessionName(name.getString());
        this.setCustomName(name);
    }
    @Override
    public boolean wantsToPickUp(ItemStack itemStack) {
        return !itemStack.isEmpty();
    }
    
    @Override
    public boolean wantsToKeep(ItemStack itemStack) {
        return super.wantsToKeep(itemStack) || itemStack.is(Items.FISHING_ROD);
    }

    @Override
    public void setEquipment() {
        ItemStack initialTool = new ItemStack(Items.FISHING_ROD);
        this.updateInventory(0, initialTool);
        this.equipTool(initialTool);
    }

    @Override
    public void openGUI(Player player) {
        if (player instanceof ServerPlayer) {
            NetworkHooks.openScreen((ServerPlayer) player, new MenuProvider() {
                @Override
                public @NotNull Component getDisplayName() {
                    return getName();
                }

                @Override
                public @NotNull AbstractContainerMenu createMenu(int i, @NotNull Inventory playerInventory, @NotNull Player playerEntity) {
                    return new WorkerInventoryContainer(i, FishermanEntity.this, playerInventory);
                }
            }, packetBuffer -> {
                packetBuffer.writeUUID(getUUID());
            });
        } else {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageOpenGuiWorker(player, this.getUUID()));
        }
    }

    @Override
    @Nullable
    public BlockPos getSailPos() {
        return entityData.get(SAIL_POS).orElse(null);
    }

    @Override
    public void setSailPos(BlockPos pos) {
        this.entityData.set(SAIL_POS, Optional.ofNullable(pos));
    }

    @Override
    public double getBoatControlAccuracy() {
        return 3.5D;
    }

    public enum State{
        IDLE(0),
        MOVING_COAST(0),
        MOVING_TO_BOAT(0),
        SAILING(0), // fährt zum fishing pos
        FISHING(0),
        STOPPING(0), // Wenn boat dann coast und eject
        STOP(0); //


        private final int index;
        State(int index){
            this.index = index;
        }

        public int getIndex(){
            return this.index;
        }

        public static State fromIndex(int index) {
            for (State state : State.values()) {
                if (state.getIndex() == index) {
                    return state;
                }
            }
            throw new IllegalArgumentException("Invalid State index: " + index);
        }
    }

}