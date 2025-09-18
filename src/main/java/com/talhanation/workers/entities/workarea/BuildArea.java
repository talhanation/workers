package com.talhanation.workers.entities.workarea;

import com.talhanation.workers.Main;
import com.talhanation.workers.client.gui.BuildAreaScreen;
import com.talhanation.workers.network.MessageToClientOpenWorkAreaScreen;
import com.talhanation.workers.world.BuildBlock;
import com.talhanation.workers.world.ScannedBlock;
import com.talhanation.workers.world.StructureManager;
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
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;

public class BuildArea extends AbstractWorkAreaEntity {
    public static final EntityDataAccessor<CompoundTag> STRUCTURE = SynchedEntityData.defineId(BuildArea.class, EntityDataSerializers.COMPOUND_TAG);
    public Stack<BlockPos> stackToBreak = new Stack<>();
    public Stack<BuildBlock> stackToPlace = new Stack<>();
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
        this.setStructureNBT(tag.getCompound("structureNBT"));
    }
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("structureNBT", this.getStructureNBT());
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
        stackToPlace.clear();

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

            this.stackToPlace.push(new BuildBlock(worldPos, rotatedState));
        }
    }

    public static BlockState rotateBlockState(BlockState state, int steps) {
        steps = steps % 4;
        if (steps == 0) return state;
        
        Property<?> pN = state.getBlock().getStateDefinition().getProperty("north");
        Property<?> pE = state.getBlock().getStateDefinition().getProperty("east");
        Property<?> pS = state.getBlock().getStateDefinition().getProperty("south");
        Property<?> pW = state.getBlock().getStateDefinition().getProperty("west");

        if (pN instanceof BooleanProperty north &&
                pE instanceof BooleanProperty east &&
                pS instanceof BooleanProperty south &&
                pW instanceof BooleanProperty west) {

            Map<Direction, BooleanProperty> connectionProps = Map.of(
                    Direction.NORTH, north,
                    Direction.EAST,  east,
                    Direction.SOUTH, south,
                    Direction.WEST,  west
            );

            Map<Direction, Boolean> values = new EnumMap<>(Direction.class);
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BooleanProperty prop = connectionProps.get(dir);
                values.put(dir, state.getValue(prop));
            }

            // Neue rotierte Werte
            Map<Direction, Boolean> rotatedValues = new EnumMap<>(Direction.class);
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                Direction rotated = Direction.from2DDataValue((dir.get2DDataValue() + steps) % 4);
                rotatedValues.put(rotated, values.get(dir));
            }

            // Setze neue Verbindungen
            for (Map.Entry<Direction, Boolean> entry : rotatedValues.entrySet()) {
                BooleanProperty prop = connectionProps.get(entry.getKey());
                state = state.setValue(prop, entry.getValue());
            }
        }


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


    public void scanBreakArea() {
        stackToBreak.clear();
        Level level = this.getCommandSenderWorld();
        List<ScannedBlock> structure = StructureManager.parseStructureFromNBT(getStructureNBT());
        Map<BlockPos, BlockState> expectedBlocks = new HashMap<>();
        for (ScannedBlock scanned : structure) {
            BlockPos worldPos = getOriginPos().offset(scanned.relativePos());
            expectedBlocks.put(worldPos, scanned.state());
        }

        BlockPos.betweenClosedStream(getArea()).forEach(pos -> {
            BlockState currentState = level.getBlockState(pos);

            BlockState expectedState = expectedBlocks.get(pos);

            if (expectedState == null) {
                // Kein Block war dort vorgesehen → komplett fremder Block
                stackToBreak.push(pos.immutable());
            } else if (!statesMatch(currentState, expectedState)) {
                // Block stimmt nicht mit erwartetem überein → z.B. anderer Typ, Orientierung etc.
                stackToBreak.push(pos.immutable());
            }
        });
    }

    private boolean statesMatch(BlockState current, BlockState expected) {
        if (current.getBlock() != expected.getBlock()) return false;

        for (Property<?> prop : expected.getProperties()) {
            if (!current.hasProperty(prop)) return false;
            if (!current.getValue(prop).equals(expected.getValue(prop))) return false;
        }

        return true;
    }

    public List<ItemStack> getRequiredMaterials(CompoundTag tag) {
        Map<Item, Integer> materialMap = new HashMap<>();
        List<ItemStack> stacks = new ArrayList<>();

        if (tag == null || !tag.contains("blocks", Tag.TAG_LIST)) return stacks;

        ListTag blockList = tag.getList("blocks", Tag.TAG_COMPOUND);
        for (Tag t : blockList) {
            CompoundTag blockTag = (CompoundTag) t;

            CompoundTag stateTag = blockTag.getCompound("state");
            BlockState state = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), stateTag);

            Block block = state.getBlock();
            Item item = Item.BY_BLOCK.get(block);

            if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
                DoubleBlockHalf half = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);
                if (half != DoubleBlockHalf.LOWER) {
                    continue;
                }
            }

            if (item instanceof BlockItem) {
                materialMap.merge(item, 1, Integer::sum);
            }
        }

        for (Map.Entry<Item, Integer> entry : materialMap.entrySet()) {
            ItemStack stack = new ItemStack(entry.getKey());
            if (stack.isEmpty()) continue;

            stack.setCount(entry.getValue());
            stacks.add(stack);
        }

        return stacks;
    }
}

