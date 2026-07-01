package com.talhanation.workers.entities;

import com.talhanation.recruits.compat.workers.IVillagerWorker;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.pathfinding.AsyncGroundPathNavigation;
import com.talhanation.workers.config.WorkersServerConfig;
import com.talhanation.workers.entities.ai.CourierWorkGoal;
import com.talhanation.workers.entities.ai.navigation.WorkersGroundPathNavigation;
import com.talhanation.workers.entities.workarea.AbstractWorkAreaEntity;
import com.talhanation.workers.inventory.CourierContainer;
import com.talhanation.workers.world.CourierRoute;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Predicate;

public class CourierEntity extends AbstractWorkerEntity implements IVillagerWorker {

    private static final EntityDataAccessor<CompoundTag> ROUTE_DATA = SynchedEntityData.defineId(CourierEntity.class, EntityDataSerializers.COMPOUND_TAG);
    @Nullable public CourierRoute currentRoute = null;
    public int currentWaypointIndex = 0;
    public boolean returning = false;
    public boolean useVehicleInventory = false;
    public boolean shouldCycle = false;
    public boolean pendingShouldCycle = false;

    private final Predicate<ItemEntity> ALLOWED_ITEMS =
            item -> !item.hasPickUpDelay() && item.isAlive()
                    && getInventory().canAddItem(item.getItem())
                    && this.wantsToPickUp(item.getItem());

    // ── Constructor ────────────────────────────────────────────────────────────

    public CourierEntity(EntityType<? extends AbstractWorkerEntity> entityType, Level world) {
        super(entityType, world);
    }

    // ── Synced data ────────────────────────────────────────────────────────────

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ROUTE_DATA, new CompoundTag());
    }

    public void syncRouteData() {
        CompoundTag nbt = new CompoundTag();
        nbt.putBoolean("hasRoute", currentRoute != null);
        if (currentRoute != null) {
            nbt.put("route", currentRoute.toNBT());
            nbt.putInt("currentIndex", currentWaypointIndex);
        }
        nbt.putBoolean("useVehicleInventory", useVehicleInventory);
        nbt.putBoolean("shouldCycle", this.shouldCycle);
        nbt.putBoolean("pendingShouldCycle", this.pendingShouldCycle);
        this.entityData.set(ROUTE_DATA, nbt);
    }

    public CompoundTag getRouteData() {
        return this.entityData.get(ROUTE_DATA);
    }

    // ── Route management ──────────────────────────────────────────────────────

    public void loadRoute(CourierRoute route) {
        this.currentRoute         = route;
        this.currentWaypointIndex = 0;
        this.returning            = true;
        syncRouteData();
    }

    public void loadRouteFromNearestWaypoint(CourierRoute route) {
        this.currentRoute         = route;
        this.returning            = true;

        if (route != null && !route.isEmpty()) {
            int    nearestIdx = 0;
            double minDistSq  = Double.MAX_VALUE;
            var    waypoints  = route.getWaypoints();
            for (int i = 0; i < waypoints.size(); i++) {
                net.minecraft.core.BlockPos pos = waypoints.get(i).getPosition();
                double distSq = distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                if (distSq < minDistSq) {
                    minDistSq  = distSq;
                    nearestIdx = i;
                }
            }
            this.currentWaypointIndex = nearestIdx;
        } else {
            this.currentWaypointIndex = 0;
        }

        syncRouteData();
    }

    public void clearRoute() {
        this.currentRoute         = null;
        this.currentWaypointIndex = 0;
        syncRouteData();
    }

    public boolean hasRoute() {
        return currentRoute != null && !currentRoute.isEmpty();
    }

    /**
     * Night-time safety: the courier may only head home once it is back at the
     * start of its route (first waypoint, not on a return leg). Without a route it
     * is free to go immediately. This prevents it from being stranded far out on
     * the route when night falls and then failing to path all the way home.
     */
    @Override
    public boolean canGoHomeNow() {
        if (!hasRoute()) return true;
        return currentWaypointIndex == 0 && !returning;
    }

    public void advanceWaypoint() {
        if(!hasRoute()) return;

        int size = currentRoute.size();
        if(size <= 1){
            currentWaypointIndex = 0;
            returning = false;
            syncRouteData();
            return;
        }

        if(currentWaypointIndex == 0 && returning){
            returning = false;
        }

        if(returning){
            if(currentWaypointIndex > 0)
                currentWaypointIndex--;
        }
        else if(shouldCycle){
            // Cycle mode: ring the route. At the last waypoint wrap straight back
            // to 0 (…→N-1→0→1→…). Previously the index just stopped incrementing at
            // N-1, so the courier got stuck on the final waypoint and never looped.
            currentWaypointIndex = (currentWaypointIndex + 1) % size;
        }
        else{
            // Ping-pong mode: walk to the end, then turn around.
            if(currentWaypointIndex == size - 1){
                returning = true;
                currentWaypointIndex--;
            }
            else{
                currentWaypointIndex++;
            }
        }

        syncRouteData();
    }

    @Nullable
    public CourierRoute.CourierWaypoint getCurrentWaypoint() {
        if (!hasRoute()) return null;
        return currentRoute.getWaypoints().get(currentWaypointIndex);
    }

    // ── AbstractWorkerEntity contract ─────────────────────────────────────────

    @Override
    public AbstractWorkAreaEntity getCurrentWorkArea() {
        return null;
    }

    @Override
    public boolean needsToDeposit() {
        return false;
    }

    @Override
    public boolean needsToGetToChest() {
        return needsToGetFood();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(2, new CourierWorkGoal(this));
    }

    // ── Attributes ────────────────────────────────────────────────────────────

    public static AttributeSupplier.Builder setAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(ForgeMod.SWIM_SPEED.get(), 0.3D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.1D)
                .add(Attributes.ATTACK_DAMAGE, 0.5D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(ForgeMod.ENTITY_REACH.get(), 0D)
                .add(Attributes.ATTACK_SPEED);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor world, @NotNull DifficultyInstance difficulty, @NotNull MobSpawnType reason, @Nullable SpawnGroupData data, @Nullable CompoundTag nbt) {
        RandomSource rand = world.getRandom();
        SpawnGroupData result = super.finalizeSpawn(world, difficulty, reason, data, nbt);

        ((WorkersGroundPathNavigation) this.getNavigation()).setCanOpenDoors(true);

        this.populateDefaultEquipmentEnchantments(rand, difficulty);

        this.initSpawn();

        return result;
    }

    @Override
    public void initSpawn() {
        this.setCustomName(Component.literal("Courier"));
        this.setCost(WorkersServerConfig.CourierCost.get());
        this.setEquipment();
        this.setDropEquipment();
        this.setRandomSpawnBonus();
        this.setPersistenceRequired();
        this.setFollowState(2);
        AbstractRecruitEntity.applySpawnValues(this);
    }
    @Override
    public Predicate<ItemEntity> getAllowedItems() {
        return ALLOWED_ITEMS;
    }

    @Override
    public List<Item> inventoryInputHelp() {
        return null;
    }

    @Override
    public boolean canHoldItem(ItemStack itemStack) {
        return true;
    }

    // ── NBT ───────────────────────────────────────────────────────────────────

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("currentWaypointIndex", currentWaypointIndex);
        nbt.putBoolean("useVehicleInventory", useVehicleInventory);
        nbt.putBoolean("returning", returning);
        nbt.putBoolean("shouldCycle", shouldCycle);
        nbt.putBoolean("pendingShouldCycle", pendingShouldCycle);

        if (currentRoute != null) {
            nbt.put("CourierRoute", currentRoute.toNBT());
        }
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);

        this.currentRoute = null;
        if (nbt.contains("CourierRoute")) {
            this.currentRoute = CourierRoute.fromNBT(nbt.getCompound("CourierRoute"));
        }

        int savedIndex = nbt.getInt("currentWaypointIndex");
        this.currentWaypointIndex = (currentRoute != null && !currentRoute.isEmpty())
                ? Math.max(0, Math.min(savedIndex, currentRoute.size() - 1)) : 0;

        this.useVehicleInventory = nbt.getBoolean("useVehicleInventory");
        this.returning          = nbt.getBoolean("returning");
        this.shouldCycle        = nbt.getBoolean("shouldCycle");
        this.pendingShouldCycle = nbt.getBoolean("pendingShouldCycle");

        syncRouteData();
    }

    public Component TEXT_NO_TARGET_FOUND(String waypointName) {
        return Component.translatable("chat.workers.courier.noTargetFound",
                this.getName().getString(), waypointName);
    }
    public Component TEXT_CANNOT_DEPOSIT_TARGET_FULL(String waypointName) {
        return Component.translatable("chat.workers.courier.cannotDepositTargetFull",
                this.getName().getString(), waypointName);
    }

    public Component TEXT_INVENTORY_FULL(String waypointName) {
        return Component.translatable("chat.workers.courier.inventoryFull",
                this.getName().getString(), waypointName);
    }

    @Override
    public ItemStack getCustomProfessionItem() {
        return null;
    }

    @Override
    public InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        if (this.getCommandSenderWorld().isClientSide()) return InteractionResult.SUCCESS;
        if (!player.isCrouching() && this.getIsOwned() && player.getUUID().equals(this.getOwnerUUID())) {
            openSpecialGUI((ServerPlayer) player);
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public Screen getSpecialScreen(AbstractRecruitEntity abstractRecruitEntity, Player player) {
        return null;
    }

    @Override
    public void openSpecialGUI(ServerPlayer serverPlayer) {
        NetworkHooks.openScreen(serverPlayer, new MenuProvider() {
            @Override
            public @NotNull Component getDisplayName(){
                return CourierEntity.this.getName();
            }

            @Override
            public @NotNull AbstractContainerMenu createMenu(int id, @NotNull Inventory inv, @NotNull Player p) {
                return new CourierContainer(id, CourierEntity.this, inv);
            }
        }, buf -> buf.writeUUID(this.getUUID()));
    }

    @Override
    public boolean hasOnlyScreen() {
        return false;
    }
}