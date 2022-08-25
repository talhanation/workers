package com.talhanation.workers.entities;

import com.google.common.collect.ImmutableSet;
import com.talhanation.workers.Main;
import com.talhanation.workers.entities.ai.*;
import com.talhanation.workers.inventory.MinerInventoryContainer;
import com.talhanation.workers.network.MessageOpenGuiMiner;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public class MinerEntity extends AbstractWorkerEntity {

    private final Predicate<ItemEntity> ALLOWED_ITEMS = (item) -> !item.hasPickUpDelay() && item.isAlive() && this.wantsToPickUp(item.getItem());

    private static final EntityDataAccessor<Direction> DIRECTION = SynchedEntityData.defineId(MinerEntity.class, EntityDataSerializers.DIRECTION);
    private static final EntityDataAccessor<Integer> MINE_TYPE = SynchedEntityData.defineId(MinerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DEPTH = SynchedEntityData.defineId(MinerEntity.class, EntityDataSerializers.INT);
    /*
    MINE TYPES:
    0 = nothing
    1 = 1x1 Tunel
    2 = 3x3 Tunel
    3 = 8x8x8 Pit
    4 = 8x8x1 flat
    */

    private static final Set<Item> WANTED_ITEMS = ImmutableSet.of(
        Items.COAL,
        Items.COPPER_ORE,
        Items.IRON_ORE,
        Items.GOLD_ORE,
        Items.DIAMOND,
        Items.EMERALD,
        Items.STONE,
        Items.COBBLESTONE,
        Items.ANDESITE,
        Items.GRANITE,
        Items.GRAVEL,
        Items.SAND,
        Items.SANDSTONE,
        Items.RED_SAND,
        Items.REDSTONE,
        Items.DIRT,
        Items.DIORITE,
        Items.COARSE_DIRT,
        Items.RAW_COPPER,
        Items.RAW_IRON,
        Items.RAW_GOLD
    );

    public static final Set<Block> IGNORING_BLOCKS = ImmutableSet.of(
            Blocks.CAVE_AIR,
            Blocks.AIR,
            Blocks.TORCH,
            Blocks.WALL_TORCH,
            Blocks.SOUL_WALL_TORCH,
            Blocks.REDSTONE_WIRE,
            Blocks.CAMPFIRE,
            Blocks.CAKE,
            Blocks.ACACIA_SIGN,
            Blocks.SPRUCE_SIGN,
            Blocks.BIRCH_SIGN,
            Blocks.DARK_OAK_SIGN,
            Blocks.JUNGLE_SIGN,
            Blocks.OAK_SIGN,
            Blocks.ACACIA_WALL_SIGN,
            Blocks.SPRUCE_WALL_SIGN,
            Blocks.BIRCH_WALL_SIGN,
            Blocks.DARK_OAK_WALL_SIGN,
            Blocks.JUNGLE_WALL_SIGN,
            Blocks.OAK_WALL_SIGN,
            Blocks.SOUL_LANTERN,
            Blocks.LANTERN,
            Blocks.DETECTOR_RAIL,
            Blocks.RAIL
    );

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DIRECTION, Direction.NORTH);
        this.entityData.define(MINE_TYPE, 0);
        this.entityData.define(DEPTH, 16);
    }

    public MinerEntity(EntityType<? extends AbstractWorkerEntity> entityType, Level world) {
        super(entityType, world);

    }

    //ATTRIBUTES
    public static AttributeSupplier.Builder setAttributes() {
        return createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new WorkerPickupWantedItemGoal(this));
        this.goalSelector.addGoal(2, new MinerMineTunnelGoal(this, 0.5D, 10D));
        this.goalSelector.addGoal(2, new MinerMine3x3TunnelGoal(this, 0.5D, 10D));
        this.goalSelector.addGoal(2, new MinerMine8x8PitGoal(this, 0.5D, 15D));
        this.goalSelector.addGoal(2, new MinerMine8x8x1FlatGoal(this, 0.5D, 15D));

        this.goalSelector.addGoal(1, new PanicGoal(this, 1.3D));

        this.goalSelector.addGoal(9, new MoveBackToVillageGoal(this, 0.6D, false));
        this.goalSelector.addGoal(10, new GolemRandomStrollInVillageGoal(this, 0.6D));
        this.goalSelector.addGoal(10, new WaterAvoidingRandomStrollGoal(this, 1.0D, 0F));
        this.goalSelector.addGoal(11, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(12, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, LivingEntity.class, 8.0F));
    }

    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficultyInstance, MobSpawnType reason, @Nullable SpawnGroupData data, @Nullable CompoundTag nbt) {
        SpawnGroupData ilivingentitydata = super.finalizeSpawn(world, difficultyInstance, reason, data, nbt);
        ((GroundPathNavigation)this.getNavigation()).setCanOpenDoors(true);
        this.populateDefaultEquipmentEnchantments(difficultyInstance);

        this.initSpawn();

        return ilivingentitydata;
    }

    @Override
    public void initSpawn() {
        String name = new TranslatableComponent("entity.workers.miner").getString();

        this.setProfessionName(name);
        this.setCustomName(new TextComponent(name));
        this.setEquipment();
        this.getNavigation().setCanFloat(true);
        this.setDropEquipment();
        this.setRandomSpawnBonus();
        this.setPersistenceRequired();
        this.setCanPickUpLoot(true);
    }

    @Override
    public boolean shouldDirectNavigation() {
        return this.getMineType() != 3;
    }

    protected void pickUpItem(ItemEntity itemEntity) {
        ItemStack itemstack = itemEntity.getItem();
        if (this.wantsToPickUp(itemstack)) {
            SimpleContainer inventory = this.getInventory();
            boolean flag = inventory.canAddItem(itemstack);
            if (!flag) {
                return;
            }

            this.onItemPickup(itemEntity);
            this.take(itemEntity, itemstack.getCount());
            ItemStack itemstack1 = inventory.addItem(itemstack);
            if (itemstack1.isEmpty()) {
                itemEntity.remove(RemovalReason.DISCARDED);
            } else {
                itemstack.setCount(itemstack1.getCount());
            }
        }

    }

    @Override
    public boolean wantsToPickUp(ItemStack itemStack) {
        Item item = itemStack.getItem();
        return (WANTED_ITEMS.contains(item));
    }

    public boolean shouldIgnorBlock(Block block) {
        return (IGNORING_BLOCKS.contains(block));
    }

    @Override
    public void setEquipment() {
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_PICKAXE));
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel world, AgeableMob ageable) {
        return null;
    }

    @Override
    public int workerCosts() {
        return 10;
    }

    public int getMaxMineDepth(){
        return 16;
    }

    @Override
    public Predicate<ItemEntity> getAllowedItems(){
        return ALLOWED_ITEMS;
    }


    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("MineType", this.getMineType());
        nbt.putInt("Depth", this.getMineDepth());
    }

    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.setMineType(nbt.getInt("MineType"));
        this.setMineDepth(nbt.getInt("Depth"));
    }

    @Override
    protected boolean shouldLoadChunk() {
        return true;
    }

    public void setMineDirection(Direction dir) {
        entityData.set(DIRECTION, dir);
    }

    public Direction getMineDirection() {
        return entityData.get(DIRECTION);
    }

    public void setMineType(int x){
        this.clearStartPos();
        entityData.set(MINE_TYPE, x);
    }

    public int getMineType(){
       return entityData.get(MINE_TYPE);
    }

    public void setMineDepth(int x){
        entityData.set(DEPTH, x);
    }

    public int getMineDepth(){
        return entityData.get(DEPTH);
    }

    public void changeTool(BlockState blockState) {
        if (blockState != null){
            if (blockState.is(BlockTags.MINEABLE_WITH_SHOVEL)){
                this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_SHOVEL));
            }
            else if (blockState.is(BlockTags.MINEABLE_WITH_PICKAXE)){
                this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_PICKAXE));
            }
            else
                this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ItemStack.EMPTY.getItem()));
        }
    }

    public void openGUI(Player player) {
        if (player instanceof ServerPlayer) {
            NetworkHooks.openGui((ServerPlayer) player, new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return getName();
                }
                @Nullable
                @Override
                public AbstractContainerMenu createMenu(int i, Inventory playerInventory, Player playerEntity) {
                    return new MinerInventoryContainer(i, MinerEntity.this, playerInventory);
                }
            }, packetBuffer -> {packetBuffer.writeUUID(getUUID());});
        } else {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageOpenGuiMiner(player, this.getUUID()));
        }
    }

    @Override
    public void setIsWorking(boolean bool) {
        if (this.getMineType() == 0) {
            bool = false;
            entityData.set(IS_WORKING, bool);
        }
        else
            super.setIsWorking(bool);
    }

    public void tick(){
        super.tick();
    }
}
