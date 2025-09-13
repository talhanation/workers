package com.talhanation.workers.entities.workarea;

import com.talhanation.workers.Main;
import com.talhanation.workers.client.gui.MiningAreaScreen;
import com.talhanation.workers.entities.MinerEntity;
import com.talhanation.workers.network.MessageToClientOpenWorkAreaScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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

import java.util.Stack;
import java.util.stream.Stream;

public class MiningArea extends AbstractWorkAreaEntity {

    public static final EntityDataAccessor<Integer> MINING_MODE = SynchedEntityData.defineId(MiningArea.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> HEIGHT_OFFSET = SynchedEntityData.defineId(MiningArea.class, EntityDataSerializers.INT);

    public Stack<BlockPos> stackToPlace = new Stack<>();
    public Stack<BlockPos> stackToBreak = new Stack<>();

    public MiningArea(EntityType<?> type, Level level) {
        super(type, level);
    }
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(MINING_MODE, 0);
        this.entityData.define(HEIGHT_OFFSET, 1);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setMiningMode(MiningMode.fromIndex(tag.getInt("miningMode")));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("miningMode", this.getMiningMode().getIndex());
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

    public void scanBreakArea() {
        if (area == null) area = this.getArea();
        stackToBreak.clear();

        Level level = this.getCommandSenderWorld();

        BlockPos.betweenClosedStream(area).forEach(pos -> {
            BlockState state = level.getBlockState(pos);
            FluidState fluidState = level.getFluidState(pos);
            if (!state.isAir() && fluidState.isEmpty()) {

                if (!isAir(state)) {
                    stackToBreak.push(pos.immutable());
                }
            }
        });
    }

    public void scanForOresOnWalls() {
        if (area == null) area = this.getArea();
        stackToBreak.clear(); // optional: oder eigene stackToOres

        Level level = this.getCommandSenderWorld();

        getWallBlocks(area, 3).forEach(pos -> {
            BlockState state = level.getBlockState(pos);

            if (isOre(state)) {
                // check ob an Luft grenzt (frei sichtbar)
                for (Direction dir : Direction.values()) {
                    BlockPos adj = pos.relative(dir);
                    if (level.getBlockState(adj).isAir()) {
                        stackToBreak.push(pos.immutable());
                        break;
                    }
                }
            }
        });
    }

    public void scanClosingArea() {
        if (area == null) area = this.getArea();
        stackToPlace.clear();

        Level level = this.getCommandSenderWorld();
        getWallBlocks(area, 1).forEach(pos -> {
            BlockState state = level.getBlockState(pos);
            FluidState fluidState = level.getFluidState(pos);
            if (!state.isAir() && fluidState.isEmpty()) {
                for (Direction dir : Direction.values()) {
                    BlockPos adj = pos.relative(dir);
                    if (level.getBlockState(adj).isSolid()) {
                        stackToPlace.push(pos.immutable());
                        break;
                    }
                }
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

    public boolean isOre(BlockState state){
        return state.is(Tags.Blocks.ORES);
    }

    public boolean isAir(BlockState state){
        return state.isAir() || state.is(Blocks.AIR) || state.is(Blocks.CAVE_AIR);
    }

    public void setMiningMode(MiningMode mode) {
        this.entityData.set(MINING_MODE, mode.getIndex());
    }

    public MiningMode getMiningMode() {
        return MiningMode.fromIndex(this.entityData.get(MINING_MODE));
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
