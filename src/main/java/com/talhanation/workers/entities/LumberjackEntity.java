package com.talhanation.workers.entities;

import com.talhanation.workers.Main;
import com.talhanation.workers.config.WorkersModConfig;
import com.talhanation.workers.inventory.WorkerInventoryContainer;
import com.talhanation.workers.entities.ai.*;
import com.talhanation.workers.network.MessageOpenGuiWorker;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
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

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Predicate;

import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import org.jetbrains.annotations.NotNull;

public class LumberjackEntity extends AbstractWorkerEntity {

    public final Predicate<ItemEntity> ALLOWED_ITEMS = (
            item) -> (!item.hasPickUpDelay() && item.isAlive() && this.wantsToPickUp(item.getItem()));

    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(LumberjackEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> REPLANT = SynchedEntityData.defineId(LumberjackEntity.class, EntityDataSerializers.BOOLEAN);

    public LumberjackEntity(EntityType<? extends AbstractWorkerEntity> entityType, Level world) {
        super(entityType, world);
        this.initSpawn();
    }

    public Predicate<ItemEntity> getAllowedItems() {
        return ALLOWED_ITEMS;
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public int workerCosts() {
        return WorkersModConfig.LumberjackCost.get();
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
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new WorkerPickupWantedItemGoal(this));
        this.goalSelector.addGoal(3, new LumberjackAI(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.3D));
    }

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(STATE, 0);
        this.entityData.define(REPLANT, true);
    }

    public void addAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("State", this.getState());
        nbt.putBoolean("Replant", this.getReplantSaplings());
    }

    //Boat
    public void readAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.setState(nbt.getInt("State"));
        this.setReplantSaplings(nbt.getBoolean("Replant"));
    }

    @Override
    public AgeableMob getBreedOffspring(@NotNull ServerLevel p_241840_1_, @NotNull AgeableMob p_241840_2_) {
        return null;
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficultyInstance,
        MobSpawnType reason, @Nullable SpawnGroupData data, @Nullable CompoundTag nbt) {
        SpawnGroupData ilivingentitydata = super.finalizeSpawn(world, difficultyInstance, reason, data, nbt);
        this.populateDefaultEquipmentEnchantments(random, difficultyInstance);

        this.initSpawn();

        return ilivingentitydata;
    }

    @Override
    public void initSpawn() {
        super.initSpawn();
        Component name = Component.literal("Lumberjack");

        this.setProfessionName(name.getString());
        this.setCustomName(name);
    }

    @Override
    public boolean isRequiredMainTool(ItemStack tool) {
        return tool.getItem() instanceof AxeItem;
    }

    @Override
    public boolean isRequiredSecondTool(ItemStack tool) {
        return false;
    }

    @Override
    public boolean wantsToPickUp(ItemStack itemStack) {
        return (
            itemStack.is(ItemTags.LOGS) ||
            itemStack.is(ItemTags.SAPLINGS) ||
            itemStack.is(Items.STICK) ||
            itemStack.is(Items.APPLE)
        );
    }

    @Override
    public boolean wantsToKeep(ItemStack itemStack) {
        return super.wantsToKeep(itemStack) || itemStack.is(ItemTags.SAPLINGS);
    }

    public void setState(int state) {
        this.entityData.set(STATE, state);
    }

    public int getState() {
        return this.entityData.get(STATE);
    }


    public boolean wantsToBreak(Block block) {
        //TODO: Maybe actually find a structure that is a tree?
        BlockState blockState = block.defaultBlockState();
        return blockState.is(BlockTags.LOGS);
    }

    @Override
    public void setEquipment() {
        ItemStack initialTool = new ItemStack(Items.STONE_AXE);
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
                public AbstractContainerMenu createMenu(int i, @NotNull Inventory playerInventory, @NotNull Player playerEntity) {
                    return new WorkerInventoryContainer(i, LumberjackEntity.this, playerInventory);
                }
            }, packetBuffer -> packetBuffer.writeUUID(getUUID()));
        } else {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageOpenGuiWorker(player, this.getUUID()));
        }
    }

    public void setReplantSaplings(boolean replant) {
        this.entityData.set(REPLANT, replant);
    }

    public boolean getReplantSaplings() {
        return entityData.get(REPLANT);
    }

    public enum State{
        IDLE(0),
        CALC_WORK_POS(1),
        WORKING(2),
        STOPPING(3),
        DEPOSIT(7),
        UPKEEP(8),

        SLEEP(9),
        STOP(10);



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
            return IDLE;
        }
    }
}