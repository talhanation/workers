package com.talhanation.workers.entities;

import com.google.common.collect.ImmutableSet;
import com.talhanation.workers.Main;
import com.talhanation.workers.config.WorkersModConfig;
import com.talhanation.workers.entities.ai.*;
import com.talhanation.workers.inventory.MinerInventoryContainer;
import com.talhanation.workers.network.MessageOpenGuiMiner;
import de.maxhenkel.corelib.tag.ItemTag;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.TierSortingRegistry;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class MinerEntity extends AbstractWorkerEntity {

    private final Predicate<ItemEntity> ALLOWED_ITEMS =
            (item) -> !item.hasPickUpDelay() && item.isAlive() && this.wantsToPickUp(item.getItem());

    private static final EntityDataAccessor<Direction> DIRECTION = SynchedEntityData.defineId(MinerEntity.class, EntityDataSerializers.DIRECTION);
    private static final EntityDataAccessor<Integer> MINE_TYPE = SynchedEntityData.defineId(MinerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DEPTH = SynchedEntityData.defineId(MinerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> CHECKED = SynchedEntityData.defineId(MinerEntity.class, EntityDataSerializers.BOOLEAN);

    /*
     * MINE TYPES:
     *  0 = nothing
     *  1 = 1x1 Tunnel
     *  2 = 3x3 Tunnel
     *  3 = 8x8x8 Pit
     *  4 = 8x8x1 Flat
     *  5 = 8x8x3 Room
     *  6 = 16x16x16 Pit
     *  7 = 16x16x1 Flat
     *  8 = 16x16x3 Room
     */

    private static final Set<Item> WANTED_ITEMS = ImmutableSet.of(Items.COAL, Items.COPPER_ORE, Items.IRON_ORE,
            Items.GOLD_ORE, Items.DIAMOND, Items.EMERALD, Items.STONE, Items.COBBLESTONE, Items.ANDESITE, Items.GRANITE,
            Items.GRAVEL, Items.SAND, Items.SANDSTONE, Items.RED_SAND, Items.REDSTONE, Items.DIRT, Items.DIORITE,
            Items.COARSE_DIRT, Items.RAW_COPPER, Items.RAW_IRON, Items.RAW_GOLD, Items.TUFF, Items.BLACKSTONE,
            Items.DEEPSLATE, Items.DEEPSLATE_BRICKS, Items.BASALT, Items.TORCH);

    public static final Set<Block> IGNORING_BLOCKS = ImmutableSet.of(Blocks.CAVE_AIR, Blocks.AIR, Blocks.TORCH,
            Blocks.WALL_TORCH, Blocks.SOUL_WALL_TORCH, Blocks.REDSTONE_WIRE, Blocks.CAMPFIRE, Blocks.CAKE,
            Blocks.ACACIA_SIGN, Blocks.SPRUCE_SIGN, Blocks.BIRCH_SIGN, Blocks.DARK_OAK_SIGN, Blocks.JUNGLE_SIGN,
            Blocks.OAK_SIGN, Blocks.ACACIA_WALL_SIGN, Blocks.SPRUCE_WALL_SIGN, Blocks.BIRCH_WALL_SIGN,
            Blocks.DARK_OAK_WALL_SIGN, Blocks.JUNGLE_WALL_SIGN, Blocks.OAK_WALL_SIGN, Blocks.SOUL_LANTERN,
            Blocks.LANTERN, Blocks.DETECTOR_RAIL, Blocks.RAIL, Blocks.WATER);

    public int blocks;
    public int side;
    public int depth;
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DIRECTION, Direction.NORTH);
        this.entityData.define(MINE_TYPE, 1);
        this.entityData.define(DEPTH, 16);
        this.entityData.define(CHECKED, false);
    }

    public MinerEntity(EntityType<? extends AbstractWorkerEntity> entityType, Level world) {
        super(entityType, world);
    }

    public boolean canWorkWithoutTool(){
        return false;
    }

    // ATTRIBUTES
    public static AttributeSupplier.Builder setAttributes() {
        return createMobAttributes().add(Attributes.MAX_HEALTH, 20.0D).add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 1.0D).add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.3D));
        this.goalSelector.addGoal(2, new MinerAI(this));
    }

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
        Component name = Component.literal("Miner");
        this.setProfessionName(name.getString());
        this.setCustomName(name);
    }

    @Override
    public boolean wantsToPickUp(ItemStack itemStack) {
        Item item = itemStack.getItem();
        return (WANTED_ITEMS.contains(item)) || itemStack.is(Tags.Items.ORES) || itemStack.is(Tags.Items.STONE);
    }

    @Override
    public boolean wantsToKeep(ItemStack itemStack) {
        return super.wantsToKeep(itemStack) || itemStack.is(Items.TORCH) || itemStack.getItem() instanceof ShovelItem || itemStack.getItem() instanceof PickaxeItem;
    }

    public boolean shouldIgnoreBlock(BlockState blockState) {
        return (IGNORING_BLOCKS.contains(blockState.getBlock()) || !canBreakBlock(blockState));
    }

    @Override
    public void setEquipment() {
        ItemStack initialTool = new ItemStack(Items.STONE_PICKAXE);
        this.updateInventory(0, initialTool);
        this.equipTool(initialTool);

        ItemStack initialTool2 = new ItemStack(Items.STONE_SHOVEL);
        this.updateInventory(1, initialTool2);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(@NotNull ServerLevel world, @NotNull AgeableMob ageable) {
        return null;
    }

    @Override
    public int workerCosts() {
        return WorkersModConfig.MinerCost.get();
    }

    public int getMaxMineDepth() {
        return 64;
    }

    public boolean getChecked(){
        return this.entityData.get(CHECKED);
    }

    @Override
    public Predicate<ItemEntity> getAllowedItems() {
        return ALLOWED_ITEMS;
    }

    public void addAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("MineType", this.getMineType());
        nbt.putInt("Depth", this.getMineDepth());
        nbt.putBoolean("Checked", this.getChecked());
        nbt.putString("MineDirection", this.getMineDirection().getName());

        ListTag waypoints = new ListTag();

    }

    public void readAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.setMineType(nbt.getInt("MineType"), false);
        this.setMineDepth(nbt.getInt("Depth"));
        this.setChecked(nbt.getBoolean("Checked"));
        this.setMineDirection(Direction.byName(nbt.getString("MineDirection")));
    }

    public void setMineDirection(Direction dir) {
        entityData.set(DIRECTION, dir);
    }

    public Direction getMineDirection() {
        return entityData.get(DIRECTION);
    }

    public void setChecked(boolean checked){
        this.entityData.set(CHECKED, checked);
    }

    public void setMineType(int x, boolean clear) {
        if(clear)this.clearStartPos();
        this.entityData.set(MINE_TYPE, x);
    }
    @Override
    public void setStartPos(BlockPos pos){
        this.resetCounts();
        super.setStartPos(pos);
    }

    public int getMineType() {
        return entityData.get(MINE_TYPE);
    }

    public void setMineDepth(int x) {
        entityData.set(DEPTH, x);
    }

    public int getMineDepth() {
        return entityData.get(DEPTH);
    }

    public void changeTool(BlockState blockState) {
        if (blockState != null) {
            if (blockState.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
                for (int i = 0; i < getInventory().items.size(); i++){
                    ItemStack stack = getInventory().items.get(i);
                    if(stack.getItem() instanceof ShovelItem){
                        this.equipTool(stack);
                    }
                }

            } else if (blockState.is(BlockTags.MINEABLE_WITH_PICKAXE)) {
                for (int i = 0; i < getInventory().items.size(); i++){
                    ItemStack stack = getInventory().items.get(i);
                    if(stack.getItem() instanceof PickaxeItem){
                        this.equipTool(stack);
                    }
                }
            } else
                this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ItemStack.EMPTY.getItem()));
        }
    }

    public void openGUI(Player player) {
        if (player instanceof ServerPlayer) {
            NetworkHooks.openScreen((ServerPlayer) player, new MenuProvider() {
                @Override
                public @NotNull Component getDisplayName() {
                    return getName();
                }

                @Override
                public @NotNull AbstractContainerMenu createMenu(int i, @NotNull Inventory playerInventory, @NotNull Player playerEntity) {
                    return new MinerInventoryContainer(i, MinerEntity.this, playerInventory);
                }
            }, packetBuffer -> {
                packetBuffer.writeUUID(getUUID());
            });
        } else {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageOpenGuiMiner(player, this.getUUID()));
        }
    }

    public void tick() {
        super.tick();
    }

    public void resetCounts(){
        blocks = 0;
        side = 0;
        depth = 0;
    }
    public boolean isRequiredMainTool(ItemStack tool) {
        return tool.getItem() instanceof PickaxeItem;
    }

    public boolean isRequiredSecondTool(ItemStack tool) {
        return tool.getItem() instanceof ShovelItem;
    }
    public boolean hasAMainTool(){
        return true;
    }
    public boolean hasASecondTool(){
        return true;
    }

    public boolean canBreakBlock(BlockState state){
        ItemStack tool = this.getMainHandItem();
        if(tool.getItem() instanceof DiggerItem diggerItem){
            return TierSortingRegistry.isCorrectTierForDrops(diggerItem.getTier(), state);
        }
        else
            return false;
    }

    public int getFarmedItemsDepositAmount(){
        return 128;
    }

    @Override
    public List<Item> inventoryInputHelp() {
        return Arrays.asList(Items.IRON_PICKAXE, Items.IRON_SHOVEL, Items.TORCH);
    }
}
