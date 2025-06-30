package com.talhanation.workers.entities;

import com.talhanation.recruits.config.RecruitsClientConfig;
import com.talhanation.recruits.entities.AbstractChunkLoaderEntity;
import com.talhanation.workers.entities.ai.DepositItemsInChestsGoal;
import com.talhanation.workers.entities.ai.GetNeededItemsFromChestsGoal;
import com.talhanation.workers.world.NeededItem;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;


public abstract class AbstractWorkerEntity extends AbstractChunkLoaderEntity {
    public IWorkerController workController;
    public AbstractWorkerEntity(EntityType<? extends AbstractWorkerEntity> entityType, Level world) {
        super(entityType, world);
    }
    public List<BlockPos> chestPositions = new ArrayList<>();
    public List<NeededItem> neededItems = new ArrayList<>();
    public int farmedItems;
    public boolean forcedDeposit;
    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new DepositItemsInChestsGoal(this));
        this.goalSelector.addGoal(0, new GetNeededItemsFromChestsGoal(this));
    }

    /////////////////////////////////// TICK/////////////////////////////////////////


    @Override
    public void aiStep() {
        super.aiStep();
        if(this.getCommandSenderWorld().isClientSide()) return;

        if(workController != null) this.workController.tick();

        this.getCommandSenderWorld().getProfiler().push("looting");
        if (this.canPickUpLoot() && this.isAlive() && !this.dead) {
            List<ItemEntity> nearbyItems = this.getCommandSenderWorld().getEntitiesOfClass(
                ItemEntity.class,
                this.getBoundingBox().inflate(5.5D, 5.5D, 5.5D)
            );

            for (ItemEntity itementity : nearbyItems) {
                if (!itementity.isRemoved() && !itementity.getItem().isEmpty() && !itementity.hasPickUpDelay() && this.wantsToPickUp(itementity.getItem())) {
                    this.pickUpItem(itementity);
                }
            }
        }
    }

    public boolean canAddItem(ItemStack itemToAdd) {
        boolean flag = false;
        List<ItemStack> inventorySlots = new ArrayList<>();
        for(int i = 6; i < this.inventory.getContainerSize(); i++){
            inventorySlots.add(inventory.items.get(i));
        }

        for(ItemStack itemstack : inventorySlots) {
            if (itemstack.isEmpty() || ItemStack.isSameItemSameTags(itemstack, itemToAdd) && itemstack.getCount() < itemstack.getMaxStackSize()) {
                flag = true;
                break;
            }
        }

        return flag;
    }

    @Override
    protected void pickUpItem(ItemEntity itemEntity) {
        ItemStack itemstack = itemEntity.getItem();
        if (this.wantsToPickUp(itemstack)) {
            if (!this.canAddItem(itemstack)) return;

            this.onItemPickup(itemEntity);
            this.take(itemEntity, itemstack.getCount());
            ItemStack itemstack1 = this.addItem(itemstack);

            this.farmedItems += itemstack.getCount() - itemstack1.getCount();
            NeededItem.applyToNeededItems(itemstack, neededItems);

            if (itemstack1.isEmpty()) {
                itemEntity.remove(RemovalReason.DISCARDED);
            } else {
                itemstack.setCount(itemstack1.getCount());
            }
        }
    }

    public ItemStack addItem(ItemStack itemStackToAdd) {
        if (itemStackToAdd.isEmpty()) {
            return ItemStack.EMPTY;
        } else {
            ItemStack itemstack = itemStackToAdd.copy();
            this.moveItemToOccupiedSlotsWithSameType(itemstack);
            if (itemstack.isEmpty()) {
                return ItemStack.EMPTY;
            } else {
                this.moveItemToEmptySlots(itemstack);
                return itemstack.isEmpty() ? ItemStack.EMPTY : itemstack;
            }
        }
    }

    private void moveItemToOccupiedSlotsWithSameType(ItemStack itemStackToMove) {
        for(int i = 6; i < this.getInventory().getContainerSize(); ++i) {
            ItemStack itemstack = this.getInventory().getItem(i);
            if (ItemStack.isSameItemSameTags(itemstack, itemStackToMove)) {
                this.moveItemsBetweenStacks(itemStackToMove, itemstack);
                if (itemStackToMove.isEmpty()) {
                    return;
                }
            }
        }
    }

    private void moveItemToEmptySlots(ItemStack itemStack) {
        for(int i = 6; i < this.getInventory().getContainerSize(); ++i) {
            ItemStack itemstack = this.getInventory().getItem(i);
            if (itemstack.isEmpty()) {
                this.getInventory().setItem(i, itemStack.copyAndClear());
                return;
            }
        }

    }

    private void moveItemsBetweenStacks(ItemStack p_19186_, ItemStack p_19187_) {
        int i = Math.min(64, p_19187_.getMaxStackSize());
        int j = Math.min(p_19186_.getCount(), i - p_19187_.getCount());
        if (j > 0) {
            p_19187_.grow(j);
            p_19186_.shrink(j);
            this.getInventory().setChanged();
        }
    }

    @Nullable
    public SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor world, @NotNull DifficultyInstance diff, @NotNull MobSpawnType reason, @Nullable SpawnGroupData spawnData, @Nullable CompoundTag nbt) {
        return spawnData;
    }
    public void setDropEquipment()  {
        this.dropEquipment();
    }


    //////////////////////////////////// REGISTER////////////////////////////////////

    protected void defineSynchedData() {
        super.defineSynchedData();
    }

    public void addAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        if (this.chestPositions != null && !this.chestPositions.isEmpty()) {
            ListTag listTag = new ListTag();
            for (BlockPos pos : this.chestPositions) {
                CompoundTag compoundTag = new CompoundTag();
                compoundTag.putInt("x", pos.getX());
                compoundTag.putInt("y", pos.getY());
                compoundTag.putInt("z", pos.getZ());
                listTag.add(compoundTag);
            }

            nbt.put("ChestPositions", listTag);
        }
        nbt.putInt("farmedItems", farmedItems);
    }

    public void readAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        if (nbt.contains("ChestPositions", Tag.TAG_LIST)) {
            ListTag listTag = nbt.getList("ChestPositions", Tag.TAG_COMPOUND);
            this.chestPositions = new ArrayList<>();
            for (Tag t : listTag) {
                CompoundTag compoundTag = (CompoundTag) t;
                int x = compoundTag.getInt("x");
                int y = compoundTag.getInt("y");
                int z = compoundTag.getInt("z");
                this.chestPositions.add(new BlockPos(x, y, z));
            }
        }
        this.farmedItems = nbt.getInt("farmedItems");
    }

    @OnlyIn(Dist.CLIENT)
    public SoundEvent getHurtSound(DamageSource ds) {
        if(RecruitsClientConfig.RecruitsLookLikeVillagers.get()){
            return SoundEvents.VILLAGER_HURT;
        }
        else
            return SoundEvents.PLAYER_HURT;
    }

    @OnlyIn(Dist.CLIENT)
    protected SoundEvent getDeathSound() {
        if(RecruitsClientConfig.RecruitsLookLikeVillagers.get()){
            return SoundEvents.VILLAGER_DEATH;
        }
        else
            return SoundEvents.PLAYER_DEATH;
    }

    protected float getSoundVolume() {
        return 0.4F;
    }

    /**
     * This is used to determine whether the worker should store an ItemStack
     * in a chest or keep it in its inventory.
     * 
     * For example, lumberjacks need saplings to replant trees, farmers need seeds, etc.
     * 
     * @param itemStack The ItemStack to compare against
     * @return true if the ItemStack will be kept in inventory, false if it will be stored in a chest.
     */
    public boolean wantsToKeep(ItemStack itemStack) {
        return (itemStack.isEdible() && itemStack.getFoodProperties(this).getNutrition() > 4);
    }

    //////////////////////////////////// SET////////////////////////////////////

    public void addDepositPosition(BlockPos pos){
        if(chestPositions.contains(pos)) return;

        this.chestPositions.add(pos);
    }

    public void removeDepositPosition(BlockPos pos){
        this.chestPositions.remove(pos);
    }

    public void setEquipment() {
    }

    public boolean needsToSleep() {
        return !this.getCommandSenderWorld().isDay();
    }

    public abstract Predicate<ItemEntity> getAllowedItems();

    public void initSpawn(){
        this.setEquipment();
        this.setDropEquipment();
        this.setPersistenceRequired();
        this.setCanPickUpLoot(true);
    }

    public double getDistanceToOwner(){
        return this.getOwner() != null ? this.distanceToSqr(this.getOwner()) : 1D;
    }

    public void tick() {
        super.tick();
        if(this.getCommandSenderWorld().isClientSide()) return;
    }

    public abstract List<Item> inventoryInputHelp();

    public boolean needsToGetToChest() {
        return this.needsToGetFood() || needsToDeposit() || needsToGetItems();
    }

    public boolean needsToDeposit() {
        return forcedDeposit || farmedItems > 128;
    }

    @Nullable
    public ItemStack getMatchingItem(Predicate<ItemStack> predicate) {
        for (ItemStack stack : this.getInventory().items) {
            if (!stack.isEmpty() && predicate.test(stack)) {
                return stack;
            }
        }
        return null;
    }

    int currentTimeBreak;
    int breakingTime;
    int previousTimeBreak;

    public void mineBlock(BlockPos pos) {
        if (!this.isAlive()) return;

        Level level = this.getCommandSenderWorld();
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return;

        // Laubblöcke: mit Schere manuell abbauen und droppen
        if (state.getBlock() instanceof LeavesBlock && this.getMainHandItem().getItem() instanceof ShearsItem) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            List<ItemStack> drops = Block.getDrops(state, (ServerLevel) level, pos, blockEntity, this, this.getMainHandItem());

            for (ItemStack drop : drops) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), drop);
            }

            state.onRemove(level, pos, Blocks.AIR.defaultBlockState(), false); // z.B. für Sounds/Particles
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            level.playSound(null, pos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 1.0F, 1.0F); // optionaler Scher-Sound

            return;
        }

        // Normaler Abbau
        if (currentTimeBreak % 5 == 4) {
            level.playLocalSound(pos.getX(), pos.getY(), pos.getZ(),
                    state.getSoundType().getHitSound(), SoundSource.BLOCKS, 1F, 0.75F, false);
        }

        if (breakingTime == 0) {
            breakingTime = (int) (state.getDestroySpeed(level, pos) * 30);
        }

        float destroySpeed = this.getUseItem().getDestroySpeed(state) * 2;
        currentTimeBreak += (int) destroySpeed;

        int stage = (int) ((float) currentTimeBreak / breakingTime * 10);
        if (stage != previousTimeBreak) {
            level.destroyBlockProgress(1, pos, stage);
            previousTimeBreak = stage;
        }

        if (currentTimeBreak >= breakingTime) {
            level.destroyBlock(pos, true, this); // funktioniert für normale Blöcke
            currentTimeBreak = 0;
            breakingTime = 0;
            previousTimeBreak = 0;
        }

        this.swing(InteractionHand.MAIN_HAND);
    }


    public boolean needsToGetItems() {
        return !neededItems.isEmpty();
    }

    public void addNeededItem(NeededItem neededItem) {
        if(neededItems.contains(neededItem)) return;

        neededItems.add(neededItem);
    }

    public void onItemAddedToInventory(ItemStack itemStack){
        for(NeededItem neededItem : neededItems){
            if(neededItem.matches(itemStack)){
                NeededItem.applyToNeededItems(itemStack, neededItems);;
                break;
            }
        }
    }

    public void switchMainHandItem(Predicate<ItemStack> predicate) {
        if (!this.isAlive() || predicate == null) return;

        SimpleContainer inventory = this.getInventory();
        ItemStack mainHand = this.getMainHandItem();
        if (predicate.test(mainHand)) return;

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (predicate.test(stack)) {

                inventory.setItem(i, mainHand);
                this.setItemInHand(InteractionHand.MAIN_HAND, stack);
                return;
            }
        }
    }

}
