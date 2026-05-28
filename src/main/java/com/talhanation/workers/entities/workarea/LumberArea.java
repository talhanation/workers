package com.talhanation.workers.entities.workarea;

import com.talhanation.workers.WorkersMain;
import com.talhanation.workers.client.gui.LumberAreaScreen;
import com.talhanation.workers.compat.DynamicTrees;
import com.talhanation.workers.entities.LumberjackEntity;
import com.talhanation.workers.world.Tree;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.*;

public class LumberArea extends AbstractWorkAreaEntity {

    public static final EntityDataAccessor<ItemStack> SAPLING_STACK = SynchedEntityData.defineId(LumberArea.class, EntityDataSerializers.ITEM_STACK);
    public static final EntityDataAccessor<Boolean> REPLANT = SynchedEntityData.defineId(LumberArea.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> SHEAR_LEAVES = SynchedEntityData.defineId(LumberArea.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> STRIP_LOGS = SynchedEntityData.defineId(LumberArea.class, EntityDataSerializers.BOOLEAN);
    public Stack<BlockPos> stackToPlant = new Stack<>();
    public Stack<BlockPos> stackToBoneMeal = new Stack<>();
    public Stack<Tree> stackOfTrees = new Stack<>();


    public LumberArea(EntityType<?> type, Level level) {
        super(type, level);
    }

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SAPLING_STACK, ItemStack.EMPTY);
        this.entityData.define(REPLANT, true);
        this.entityData.define(SHEAR_LEAVES, false);
        this.entityData.define(STRIP_LOGS, false);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if(tag.contains("saplingItem")){
            ItemStack stack = ItemStack.of(tag.getCompound("saplingItem"));
            this.setSaplingStack(stack);
        }
        this.setReplant(tag.getBoolean("replantTrees"));
        this.setStripLogs(tag.getBoolean("stripLogs"));
        this.setShearLeaves(tag.getBoolean("shearLeaves"));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        CompoundTag nbt = new CompoundTag();
        this.getSaplingStack().save(nbt);
        tag.put("saplingItem", nbt);
        tag.putBoolean("replantTrees", getReplant());
        tag.putBoolean("shearLeaves", getShearLeaves());
        tag.putBoolean("stripLogs", getStripLogs());
    }

    public Item getRenderItem(){
        return Items.IRON_AXE;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public Screen getScreen(Player player) {
        return new LumberAreaScreen(this, player);
    }

    public boolean isWorkerPerfectCandidate(LumberjackEntity lumberjack) {
        boolean needsSapling = this.getReplant();
        boolean needsAxe = this.getStripLogs();
        boolean needsShears = this.getShearLeaves();

        if (needsSapling && lumberjack.getMatchingItem(stack -> stack.is(this.getSaplingStack().getItem())) == ItemStack.EMPTY) {
            return false;
        }

        if (needsAxe && lumberjack.getMatchingItem(stack -> stack.getItem() instanceof AxeItem) == ItemStack.EMPTY) {
            return false;
        }

        if (needsShears && lumberjack.getMatchingItem(stack -> stack.getItem() instanceof ShearsItem) == ItemStack.EMPTY) {
            return false;
        }

        return true;
    }


    public void scanBoneMealArea() {
        if (area == null) this.area = getArea();

        this.stackToBoneMeal.clear();
        Level level = this.getCommandSenderWorld();

        BlockPos.betweenClosedStream(area).forEach(pos -> {
            BlockState state = level.getBlockState(pos);

            if (isSapling(state)) {
                this.stackToBoneMeal.push(pos.immutable());
            }
            else if (WorkersMain.isDynamicTreesInstalled
                    && DynamicTrees.isDynamicTreesRootySoil(state.getBlock())
                    && !DynamicTrees.hasMaxFertility(state)) {
                this.stackToBoneMeal.push(pos.immutable());
            }
        });
    }

    public void scanForTrees() {
        Set<BlockPos> visited = new HashSet<>();
        if (area == null) this.area = getArea();

        BlockPos.betweenClosedStream(area).forEach(pos -> {
            BlockState state = this.getCommandSenderWorld().getBlockState(pos);
            if (isLog(state) && !visited.contains(pos)) {
                boolean isDT = WorkersMain.isDynamicTreesInstalled && DynamicTrees.isDynamicTreesBranch(state.getBlock());
                String treeType = getTreeType(state);

                Tree tempTree = new Tree(treeType, pos, isDT);

                if (isDT) {
                    scanDynamicTree(this.getCommandSenderWorld(), pos.immutable(), visited, tempTree);

                    if (!DynamicTrees.isGrownDynamicTree(new ArrayList<>(tempTree.getStackToBreak()), this.getCommandSenderWorld())) return;

                    // Nur den Basisblock (oder Rooty Soil darunter) in den finalen Stack
                    BlockPos baseBlock = getBaseBlockForDynamicTree(tempTree.getStackToBreak(), this.getCommandSenderWorld());
                    if (baseBlock == null) return;

                    Tree finalTree = new Tree(treeType, baseBlock, true);
                    finalTree.addToBreak(baseBlock);

                    for (BlockPos p : tempTree.getStackToShear()) finalTree.addToShear(p);
                    for (BlockPos p : tempTree.getStackToStrip()) finalTree.addToStrip(p);

                    stackOfTrees.push(finalTree);

                } else {
                    // Vanilla: normaler Scan + Blatt-Validierung
                    scanTree(this.getCommandSenderWorld(), pos.immutable(), visited, tempTree);

                    if (!hasNaturalLeavesConnected(tempTree, this.getCommandSenderWorld())) return;

                    if (!tempTree.isEmpty()) {
                        BlockPos origin = getLowestLog(tempTree.getStackToBreak());

                        Tree finalTree = new Tree(treeType, origin);

                        for (BlockPos p : tempTree.getStackToBreak()) finalTree.addToBreak(p);
                        for (BlockPos p : tempTree.getStackToStrip()) finalTree.addToStrip(p);
                        for (BlockPos p : tempTree.getStackToShear()) finalTree.addToShear(p);

                        stackOfTrees.push(finalTree);
                    }
                }
            }
        });
    }

    private void scanDynamicTree(Level level, BlockPos start, Set<BlockPos> visited, Tree tree) {
        tree.getStackToShear().clear();
        tree.getStackToStrip().clear();
        tree.getStackToBreak().clear();

        Queue<BlockPos> toVisit = new ArrayDeque<>();
        toVisit.add(start);

        while (!toVisit.isEmpty()) {
            BlockPos pos = toVisit.poll();
            if (!visited.add(pos)) continue;

            BlockState state = level.getBlockState(pos);

            if (DynamicTrees.isDynamicTreesBranch(state.getBlock())) {
                if (getStrippedBlock(state) != null) {
                    tree.addToStrip(pos.immutable());
                }

                tree.addToBreak(pos.immutable());

                // Vanilla-Blätter in der Nähe sammeln
                int range = 4;
                for (int x = -range; x < range; x++) {
                    for (int y = -range; y < range; y++) {
                        for (int z = -range; z < range; z++) {
                            BlockPos pos1 = pos.offset(x, y, z);
                            BlockState state1 = level.getBlockState(pos1);
                            if (isLeaf(state1) && !tree.getStackToShear().contains(pos1)) {
                                tree.addToShear(pos1.immutable());
                            }
                        }
                    }
                }

                for (Direction dir : Direction.values()) {
                    toVisit.add(pos.relative(dir));
                }
            }
        }

        tree.getStackToShear().sort(Comparator.reverseOrder());
    }

    // Gibt den untersten Branch-Block zurück – dieser wird gebrochen und triggert DT's Fäll-Logik
    private BlockPos getBaseBlockForDynamicTree(Stack<BlockPos> branches, Level level) {
        return branches.stream()
                .min(Comparator.comparingInt(BlockPos::getY))
                .orElse(null);
    }

    private boolean hasNaturalLeavesConnected(Tree tree, Level level) {
        for (BlockPos logPos : tree.getStackToBreak()) {
            for (BlockPos offset : BlockPos.betweenClosed(
                    logPos.offset(-4, -4, -4),
                    logPos.offset(4, 4, 4))) {

                BlockState state = level.getBlockState(offset);

                if (isLeaf(state)) {
                    if (!state.getOptionalValue(BlockStateProperties.PERSISTENT).orElse(false)) {
                        if (state.getOptionalValue(BlockStateProperties.DISTANCE).orElse(7) < 7) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private BlockPos getLowestLog(Stack<BlockPos> logStack) {
        return logStack.stream().min(Comparator.comparingInt(BlockPos::getY)).orElse(null);
    }

    private String getTreeType(BlockState state) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (id == null) return "unknown";
        return id.getPath().replace("_log", "").replace("_branch", "");
    }

    private void scanTree(Level level, BlockPos start, Set<BlockPos> visited, Tree tree) {
        tree.getStackToShear().clear();
        tree.getStackToStrip().clear();
        tree.getStackToBreak().clear();

        Queue<BlockPos> toVisit = new ArrayDeque<>();
        toVisit.add(start);

        while (!toVisit.isEmpty()) {
            BlockPos pos = toVisit.poll();
            if (!visited.add(pos)) continue;

            BlockState state = level.getBlockState(pos);

            if (isLog(state)) {
                tree.addToBreak(pos);
                if (getStrippedBlock(state) != null) {
                    tree.addToStrip(pos);
                }

                int range = 4;
                for(int x = -range; x < range; x++){
                    for(int y = -range; y < range; y++){
                        for(int z = -range; z < range; z++){
                            BlockPos pos1 = pos.offset(x, y, z);
                            BlockState state1 = level.getBlockState(pos1);
                            if (isLeaf(state1) && !tree.getStackToShear().contains(pos1)) {
                                tree.addToShear(pos1);
                            }
                        }
                    }
                }

                for (Direction dir : Direction.values()) {
                    toVisit.add(pos.relative(dir));
                }
            }
        }
        tree.getStackToShear().sort(Comparator.reverseOrder());
        tree.getStackToStrip().sort(Comparator.reverseOrder());
        tree.getStackToBreak().sort(Comparator.reverseOrder());
    }

    public void scanPlantArea() {
        if (area == null) this.area = getArea();

        this.stackToPlant.clear();
        Level world = this.getCommandSenderWorld();

        List<BlockPos> possiblePositions = new ArrayList<>();

        BlockPos center = new BlockPos((int) area.getCenter().x(), this.getOnPos().getY(), (int) area.getCenter().z());

        for (Direction dir : Direction.Plane.HORIZONTAL) {
            possiblePositions.add(center.relative(dir, 5));
        }

        possiblePositions.add(center);
        possiblePositions.add(center.offset(-5, 0, -5));
        possiblePositions.add(center.offset(-5, 0, 5));
        possiblePositions.add(center.offset(5, 0, -5));
        possiblePositions.add(center.offset(5, 0, 5));

        int minY = (int) area.minY;
        int maxY = (int) (area.maxY);

        for (BlockPos baseXZ : possiblePositions) {
            for (int y = minY; y <= maxY; y++) {
                BlockPos pos = new BlockPos(baseXZ.getX(), y, baseXZ.getZ());
                BlockPos below = pos.below();
                BlockState belowState = world.getBlockState(below);

                if (canSustainSapling(belowState) && world.isEmptyBlock(pos)) {
                    boolean enoughSpace = true;
                    for (int i = 1; i <= 4; i++) {
                        if (!world.isEmptyBlock(pos.above(i))) {
                            enoughSpace = false;
                            break;
                        }
                    }

                    if (enoughSpace) {
                        stackToPlant.add(pos.immutable());
                        break;
                    }
                }
            }
        }
    }


    private boolean canSustainSapling(BlockState belowState) {
        return belowState.is(BlockTags.DIRT);
    }


    public Stack<BlockPos> getStackToPlant() {
        return stackToPlant;
    }

    private boolean isLog(Level level, BlockPos pos) {
        return isLog(level.getBlockState(pos));
    }

    private boolean isLog(BlockState state) {
        if (state.is(BlockTags.LOGS)) return true;
        if (WorkersMain.isDynamicTreesInstalled && DynamicTrees.isDynamicTreesBranch(state.getBlock())) return true;
        return false;
    }

    @Nullable
    public static Block getStrippedBlock(BlockState state) {
        Block strippedBlock = AxeItem.STRIPPABLES.get(state.getBlock());
        if(strippedBlock != null) return strippedBlock;

        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if(id == null) return null;

        ResourceLocation strippedId = new ResourceLocation(id.getNamespace(), "stripped_" + id.getPath());
        Block candidate = BuiltInRegistries.BLOCK.get(strippedId);

        if(candidate.getDescriptionId().contains("dynamictrees")){
            int radius = DynamicTrees.getDynamicTreeRadius(state);
            if(radius < 6) return null;
        }

        if(candidate == Blocks.AIR) return null;

        return candidate;
    }

    private boolean isLeaf(BlockState state) {
        return state.is(BlockTags.LEAVES);
    }

    private boolean isSapling(BlockState state) {
        return state.getBlock() instanceof SaplingBlock;
    }

    public void setSaplingStack(ItemStack seedStack) {
        this.entityData.set(SAPLING_STACK, seedStack);
    }

    public ItemStack getSaplingStack(){
        return entityData.get(SAPLING_STACK);
    }

    public void setReplant(boolean replant) {
        this.entityData.set(REPLANT, replant);
    }

    public boolean getReplant(){
        return entityData.get(REPLANT);
    }

    public void setShearLeaves(boolean shear) {
        this.entityData.set(SHEAR_LEAVES, shear);
    }

    public boolean getShearLeaves(){
        return entityData.get(SHEAR_LEAVES);
    }

    public void setStripLogs(boolean replant) {
        this.entityData.set(STRIP_LOGS, replant);
    }

    public boolean getStripLogs(){
        return entityData.get(STRIP_LOGS);
    }
}