package com.talhanation.workers.entities;

import com.google.common.collect.ImmutableSet;
import com.talhanation.workers.entities.ai.*;
import net.minecraft.entity.AgeableEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ILivingEntityData;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.LookAtGoal;
import net.minecraft.entity.ai.goal.LookRandomlyGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WaterAvoidingRandomWalkingGoal;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.pathfinding.GroundPathNavigator;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.function.Predicate;

public class ShepherdEntity extends AbstractWorkerEntity{

    private final Predicate<ItemEntity> ALLOWED_ITEMS = (item) -> {
        return !item.hasPickUpDelay() && item.isAlive() && this.wantsToPickUp(item.getItem());
    };

    private static final Set<Item> WANTED_ITEMS = ImmutableSet.of(
            Items.BLACK_WOOL,
            Items.WHITE_WOOL,
            Items.RED_WOOL,
            Items.BLUE_WOOL,
            Items.ORANGE_WOOL,
            Items.GREEN_WOOL,
            Items.LIGHT_BLUE_WOOL,
            Items.GRAY_WOOL,
            Items.LIGHT_GRAY_WOOL,
            Items.BROWN_WOOL,
            Items.CYAN_WOOL,
            Items.MAGENTA_WOOL,
            Items.YELLOW_WOOL,
            Items.PINK_WOOL,
            Items.LIME_WOOL,
            Items.MUTTON
    );

    public ShepherdEntity(EntityType<? extends AbstractWorkerEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public int workerCosts() {
        return 12;
    }

    @Override
    public String workerName() {
        return "Shepherd";
    }

    //ATTRIBUTES
    public static AttributeModifierMap.MutableAttribute setAttributes() {
        return createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new SwimGoal(this));
        this.goalSelector.addGoal(4, new WorkerPickupWantedItemGoal(this, ALLOWED_ITEMS));
        this.goalSelector.addGoal(2, new WorkerFollowOwnerGoal(this, 1.2D, 7.F, 4.0F));
        this.goalSelector.addGoal(3, new SheerSheepGoal<>(this, SheepEntity.class, false));
        this.goalSelector.addGoal(3, new SloughterAnimalGoal<>(this, SheepEntity.class, false));
        this.goalSelector.addGoal(3, new BreedAnimalGoal(this, SheepEntity.class, false));
        this.goalSelector.addGoal(4, new LookAtGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.addGoal(5, new LookRandomlyGoal(this));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomWalkingGoal(this, 1.0D, 0F));
    }

    @Nullable
    @Override
    public AgeableEntity getBreedOffspring(ServerWorld p_241840_1_, AgeableEntity p_241840_2_) {
        return null;
    }

    @Override
    @Nullable
    public ILivingEntityData finalizeSpawn(IServerWorld world, DifficultyInstance difficultyInstance, SpawnReason reason, @Nullable ILivingEntityData data, @Nullable CompoundNBT nbt) {
        ILivingEntityData ilivingentitydata = super.finalizeSpawn(world, difficultyInstance, reason, data, nbt);
        ((GroundPathNavigator)this.getNavigation()).setCanOpenDoors(true);
        this.populateDefaultEquipmentEnchantments(difficultyInstance);
        this.setEquipment();
        this.setDropEquipment();
        this.getNavigation().setCanFloat(true);
        this.setCanPickUpLoot(true);
        return ilivingentitydata;
    }

    protected void pickUpItem(ItemEntity itemEntity) {
        ItemStack itemstack = itemEntity.getItem();
        if (this.wantsToPickUp(itemstack)) {
            Inventory inventory = this.getInventory();
            boolean flag = inventory.canAddItem(itemstack);
            if (!flag) {
                return;
            }

            this.onItemPickup(itemEntity);
            this.take(itemEntity, itemstack.getCount());
            ItemStack itemstack1 = inventory.addItem(itemstack);
            if (itemstack1.isEmpty()) {
                itemEntity.remove();
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

    @Override
    public void setEquipment() {
        int i = this.random.nextInt(9);
        this.setItemSlot(EquipmentSlotType.MAINHAND, new ItemStack(Items.SHEARS));
        this.setItemSlot(EquipmentSlotType.OFFHAND, new ItemStack(Items.WOODEN_HOE));
    }

    public ActionResultType mobInteract(PlayerEntity player, Hand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        Item item = itemstack.getItem();
        if (this.level.isClientSide) {
            boolean flag = this.isOwnedBy(player) || this.isTame() || isInSittingPose() || item == Items.BONE && !this.isTame();
            return flag ? ActionResultType.CONSUME : ActionResultType.PASS;
        } else {
            if (this.isTame() && player.getUUID().equals(this.getOwnerUUID())) {

                if (player.isCrouching()) {
                    this.setIsWorking(true);
                    //this.setStartPos(java.util.Optional.of(this.getOnPos()));

                }
                if(!player.isCrouching()) {
                    setFollow(!getFollow());
                    return ActionResultType.SUCCESS;
                }

            } else if (item == Items.EMERALD && !this.isTame() && playerHasEnoughEmeralds(player)) {
                if (!player.abilities.instabuild) {
                    if (!player.isCreative()) {
                        itemstack.shrink(workerCosts());
                    }
                    return ActionResultType.SUCCESS;
                }
                this.tame(player);
                this.navigation.stop();
                this.setTarget(null);
                this.setOrderedToSit(false);
                this.setIsWorking(false);
                return ActionResultType.SUCCESS;
            }
            else if (item == Items.EMERALD  && !this.isTame() && !playerHasEnoughEmeralds(player)) {
                player.sendMessage(new StringTextComponent("You need " + workerCosts() + " Emeralds to hire me!"), player.getUUID());
            }
            else if (!this.isTame() && item != Items.EMERALD ) {
                player.sendMessage(new StringTextComponent("I am a " + workerName()), player.getUUID());

            }
            return super.mobInteract(player, hand);
        }
    }
}
