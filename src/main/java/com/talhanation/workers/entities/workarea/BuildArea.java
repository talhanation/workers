package com.talhanation.workers.entities.workarea;

import com.talhanation.workers.Main;
import com.talhanation.workers.client.gui.BuildAreaScreen;
import com.talhanation.workers.network.MessageToClientOpenWorkAreaScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.network.PacketDistributor;

import java.util.Stack;

public class BuildArea extends AbstractWorkAreaEntity {
    public static final EntityDataAccessor<CompoundTag> STRUCTURE = SynchedEntityData.defineId(BuildArea.class, EntityDataSerializers.COMPOUND_TAG);
    public Stack<BlockPos> stackToBreak = new Stack<>();

    public BuildArea(EntityType<?> type, Level level) {
        super(type, level);
    }

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(STRUCTURE, new CompoundTag());
    }
    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
    }
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
    }

    @Override
    public Item getRenderItem() {
        return Items.IRON_SHOVEL;
    }

    @Override
    public Screen getScreen(Player player) {
        return new BuildAreaScreen(this, player);
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
    public void tick() {
        super.tick();
        if(isDone()) this.remove(RemovalReason.DISCARDED);
    }

    public void setStructureNBT(CompoundTag tag){
        this.entityData.set(STRUCTURE, tag);
    }

    public CompoundTag getStructureNBT(){
        return this.entityData.get(STRUCTURE);
    }

    public void setStartBuild() {
        CompoundTag tag = getStructureNBT();
        if (tag == null || !tag.contains("blocks", Tag.TAG_LIST)) return;
        int width = tag.getInt("width");
        String dir = tag.getString("facing");
        Direction scanFacing = Direction.byName(dir);
        ListTag blockList = tag.getList("blocks", Tag.TAG_COMPOUND);
        BlockPos origin = this.getOriginPos();
        Direction facing = this.getFacing();
        Direction right = facing.getClockWise();
        int rotationSteps = (4 + facing.get2DDataValue() - scanFacing.get2DDataValue()) % 4;

        for (Tag t : blockList) {
            CompoundTag blockTag = (CompoundTag) t;

            int relX = blockTag.getInt("x");
            int relY = blockTag.getInt("y");
            int relZ = blockTag.getInt("z");

            BlockPos worldPos = origin
                    .relative(facing, relZ)
                    .relative(right, width - 1 - relX)
                    .above(relY);

            CompoundTag stateTag = blockTag.getCompound("state");
            BlockState state = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), stateTag);
            BlockState rotatedState = rotateBlockState(state, rotationSteps);

            this.getCommandSenderWorld().setBlock(worldPos, rotatedState, 3);
        }
    }

    public static BlockState rotateBlockState(BlockState state, int steps) {
        Property<?> facingProp = state.getProperties().stream()
                .filter(p -> p.getName().equals("facing") || p.getName().equals("rotation") || p.getName().equals("axis") || p.getName().equals("horizontal_facing"))
                .findFirst().orElse(null);

        if (facingProp instanceof DirectionProperty dirProp) {
            Direction current = state.getValue(dirProp);
            if (current.getAxis().isHorizontal()) {
                Direction rotated = Direction.from2DDataValue((current.get2DDataValue() + steps) % 4);
                return state.setValue(dirProp, rotated);
            }
        }

        if (facingProp instanceof EnumProperty<?> enumProp && enumProp.getName().equals("rotation")) {
            // For blocks like signs with integer rotation from 0-15 (0 = south)
            IntegerProperty rotationProp = (IntegerProperty) facingProp;
            int current = state.getValue(rotationProp);
            int rotated = (current + steps * 4) % 16; // 4 steps per 90°
            return state.setValue(rotationProp, rotated);
        }

        // Axis (e.g. logs, pistons)
        if (facingProp instanceof EnumProperty<?> axisProp && axisProp.getName().equals("axis")) {
            Direction.Axis axis = state.getValue((EnumProperty<Direction.Axis>) axisProp);
            if (steps % 2 == 1) {
                // Swap X <-> Z if 90° or 270°
                Direction.Axis rotated = axis == Direction.Axis.X ? Direction.Axis.Z : axis == Direction.Axis.Z ? Direction.Axis.X : axis;
                return state.setValue((EnumProperty<Direction.Axis>) axisProp, rotated);
            }
        }

        if (state.hasProperty(StairBlock.SHAPE)) {
            StairsShape shape = state.getValue(StairBlock.SHAPE);
            if (shape == StairsShape.INNER_LEFT) shape = StairsShape.INNER_RIGHT;
            else if (shape == StairsShape.INNER_RIGHT) shape = StairsShape.INNER_LEFT;
            else if (shape == StairsShape.OUTER_LEFT) shape = StairsShape.OUTER_RIGHT;
            else if (shape == StairsShape.OUTER_RIGHT) shape = StairsShape.OUTER_LEFT;
            state = state.setValue(StairBlock.SHAPE, shape);
        }

        return state;
    }


    public void scanBreakArea(){
        stackToBreak.clear();
        Level level = this.getCommandSenderWorld();

        Fluid centerPosFluid = level.getFluidState(this.getOnPos()).getType();
        if(!(centerPosFluid == Fluids.WATER)|| (centerPosFluid == Fluids.FLOWING_WATER)) {
            this.stackToBreak.push(this.getOnPos());
        }

        BlockPos.betweenClosedStream(area).forEach(pos -> {
            BlockState state = level.getBlockState(pos);
            /*if (!state.isAir()) {
                this.stackToBreak.push(pos.immutable());

            }
             */
        });
    }
}
