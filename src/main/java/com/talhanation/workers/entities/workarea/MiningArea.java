package com.talhanation.workers.entities.workarea;

import com.talhanation.workers.Main;
import com.talhanation.workers.client.gui.MiningAreaScreen;
import com.talhanation.workers.config.WorkersServerConfig;
import com.talhanation.workers.entities.MinerEntity;
import com.talhanation.workers.network.MessageToClientOpenWorkAreaScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Stack;
import java.util.stream.Stream;

public class MiningArea extends AbstractWorkAreaEntity {
    public static final EntityDataAccessor<Integer> HEIGHT_OFFSET = SynchedEntityData.defineId(MiningArea.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Boolean> CLOSE_FLOOR = SynchedEntityData.defineId(MiningArea.class, EntityDataSerializers.BOOLEAN);
    public Stack<BlockPos> stackToPlace = new Stack<>();
    public Stack<BlockPos> stackToBreak = new Stack<>();

    public MiningArea(EntityType<?> type, Level level) {
        super(type, level);
    }
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(HEIGHT_OFFSET, 1);
        this.entityData.define(CLOSE_FLOOR, true);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setCloseFloor(tag.getBoolean("closeFloor"));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("closeFloor", this.getCloseFloor());
    }

    @Override
    public void tick() {
        super.tick();
        if(this.isDone()) this.remove(RemovalReason.DISCARDED);
    }

    public Item getRenderItem()  {
        return Items.IRON_PICKAXE;
    }

    @Override
    public Screen getScreen(Player player) {
        return new MiningAreaScreen(this, player);
    }

    public InteractionResult interact(Player player, InteractionHand hand) {
        if(!player.getUUID().equals(this.getPlayerUUID())) return InteractionResult.PASS;

        if (this.getCommandSenderWorld().isClientSide()) {
            return InteractionResult.CONSUME;
        }
        else{
            Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player), new MessageToClientOpenWorkAreaScreen(this.getUUID()));
            return InteractionResult.CONSUME;
        }
    }
    @Override
    public AABB createArea() {
        Direction facing = getFacing();
        int width = getWidthSize() - 1;
        int depth = getDepthSize() - 1;
        int height = getHeightSize();

        BlockPos start = this.getOnPos().offset(0, this.getHeightOffset(), 0);
        BlockPos end;
        switch (facing) {
            case NORTH -> end = start.offset(width, height, -depth);
            case SOUTH -> end = start.offset(-width, height, depth);
            case WEST  -> end = start.offset(-width, height, -depth);
            default  -> end = start.offset(width, height, depth);//EAST
        }
        return new AABB(start, end);
    }

    public int getHeightOffset() {
        return this.entityData.get(HEIGHT_OFFSET);
    }
    public void setHeightOffset(int offset) {
        this.entityData.set(HEIGHT_OFFSET, offset);
        area = this.createArea();
    }

    public boolean getCloseFloor() {
        return this.entityData.get(CLOSE_FLOOR);
    }
    public void setCloseFloor(boolean close) {
        this.entityData.set(CLOSE_FLOOR, close);
    }


    public void scanBreakArea() {
        if (area == null) area = this.getArea();
        stackToBreak.clear();

        Level level = this.getCommandSenderWorld();

        BlockPos.betweenClosedStream(area).forEach(pos -> {
            BlockState state = level.getBlockState(pos);
            FluidState fluidState = level.getFluidState(pos);
            if (pos.getY() != area.maxY && !state.isAir() && fluidState.isEmpty()) {
                if (!isAir(state) && !shouldIgnore(state)) {
                    stackToBreak.push(pos.immutable());
                }
            }
        });
    }

    public boolean shouldIgnore(BlockState state){
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if(id == null) return false;
        return WorkersServerConfig.MINER_IGNORE.contains(id.toString());
    }

    public void scanForOresOnWalls() {
        if (area == null) area = this.getArea();
        stackToBreak.clear();

        Level level = this.getCommandSenderWorld();

        getWallBlocks(area, 3).forEach(pos -> {
            BlockState state = level.getBlockState(pos);

            if (isOre(state) && hasAirNeighbor(pos, level)) {
                stackToBreak.push(pos.immutable());
            }
        });
    }

    public void scanFloorArea() {
        if (area == null) area = this.getArea();
        stackToPlace.clear();

        Level level = this.getCommandSenderWorld();

        getWallBlocks(area, 1).forEach(pos -> {
            BlockState state = level.getBlockState(pos);
            FluidState fluidState = level.getFluidState(pos);

            if(!fluidState.isEmpty()) {
                stackToPlace.push(pos.immutable());
            }

            if (pos.getY() == area.minY - 1 && (state.isAir() || !fluidState.isEmpty())) {
                stackToPlace.push(pos.immutable());
            }
        });

        BlockPos.betweenClosedStream(area).forEach(pos -> {
            FluidState fluidState = level.getFluidState(pos);
            if (fluidState.isSource()) {
                stackToPlace.push(pos.immutable());
            }
        });
    }


    private Stream<BlockPos> getWallBlocks(AABB base, int inflate) {
        AABB expanded = base.inflate(inflate);

        return BlockPos.betweenClosedStream(expanded)
                .filter(pos -> !base.contains(Vec3.atCenterOf(pos)));
    }

    public boolean isWorkerPerfectCandidate(MinerEntity miner) {
        if (miner.getMatchingItem(stack -> stack.getItem() instanceof PickaxeItem) == ItemStack.EMPTY) {
            return false;
        }

        return true;
    }

    public boolean hasAirNeighbor(BlockPos pos, Level level){
        for(Direction direction: Direction.values()){
            BlockState state = level.getBlockState(pos.relative(direction,1));
            if(isAir(state)) return true;
        }
        return false;
    }

    public boolean isOre(BlockState state){
        return state.is(Tags.Blocks.ORES);
    }

    public boolean isAir(BlockState state){
        return state.isAir() || state.is(Blocks.AIR) || state.is(Blocks.CAVE_AIR);
    }

    public enum MiningMode {
        CUSTOM(0),
        MINE(1),
        TUNNEL(2);

        private final int index;
        MiningMode(int index){
            this.index = index;
        }
        public int getIndex(){
            return this.index;
        }

        public static MiningMode fromIndex(int index) {
            for (MiningMode messengerState : MiningMode.values()) {
                if (messengerState.getIndex() == index) {
                    return messengerState;
                }
            }
            throw new IllegalArgumentException("Invalid State index: " + index);
        }


    }
}
