package com.talhanation.workers.entities.workarea;

import com.talhanation.workers.Main;
import com.talhanation.workers.client.gui.LumberAreaScreen;
import com.talhanation.workers.entities.AbstractWorkerEntity;
import com.talhanation.workers.entities.LumberjackEntity;
import com.talhanation.workers.network.MessageToClientOpenWorkAreaScreen;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;

public class LumberArea extends AbstractWorkAreaEntity {

    public static final EntityDataAccessor<ItemStack> SAPLING_STACK = SynchedEntityData.defineId(LumberArea.class, EntityDataSerializers.ITEM_STACK);
    public static final EntityDataAccessor<Boolean> REPLANT = SynchedEntityData.defineId(LumberArea.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> SHEAR_LEAVES = SynchedEntityData.defineId(LumberArea.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> STRIP_LOGS = SynchedEntityData.defineId(LumberArea.class, EntityDataSerializers.BOOLEAN);
    public Stack<BlockPos> stackToPlant = new Stack<>();
    public Stack<Tree> stackOfTrees = new Stack<>();


    public LumberArea(EntityType<?> type, Level level) {
        super(type, level);
    }

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SAPLING_STACK, ItemStack.EMPTY);
        this.entityData.define(REPLANT, false);
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
    public Screen getScreen(Player player) {
        return new LumberAreaScreen(this, player);
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


    public void scanForTrees() {
        Set<BlockPos> visited = new HashSet<>();

        for (BlockPos pos : BlockPos.betweenClosed(this.getOnPos().offset(-getSize(), -getHeight(), -getSize()), this.getOnPos().offset(getSize(), getHeight(), getSize()))) {
            if (!visited.contains(pos) && isLog(this.getCommandSenderWorld(), pos)) {
                BlockState state = this.getCommandSenderWorld().getBlockState(pos);
                if (isLog(state)) {
                    String treeType = getTreeType(state);

                    Tree tempTree = new Tree(treeType, pos);
                    scanTree(this.getCommandSenderWorld(), pos.immutable(), visited, tempTree);

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
        }
    }

    private BlockPos getLowestLog(Stack<BlockPos> logStack) {
        return logStack.stream().min(Comparator.comparingInt(BlockPos::getY)).orElse(null);
    }

    private String getTreeType(BlockState state) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return id != null ? id.getPath().replace("_log", "") : "unknown";
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
                if (AxeItem.STRIPPABLES.containsKey(state.getBlock())) {
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

    public void scanPlantArea(){
        this.stackToPlant.clear();
        BlockPos center = this.getOnPos();
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            stackToPlant.add(center.relative(dir, 5).above());
        }

        stackToPlant.add(center.offset(-5, 1, -5));
        stackToPlant.add(center.offset(-5, 1, 5));
        stackToPlant.add(center.offset(5, 1, -5));
        stackToPlant.add(center.offset(5, 1, 5));
    }

    public Stack<BlockPos> getStackToPlant() {
        return stackToPlant;
    }

    private boolean isLog(Level level, BlockPos pos) {
        return isLog(level.getBlockState(pos));
    }

    private boolean isLog(BlockState state) {
        return state.is(BlockTags.LOGS);
    }

    private boolean isLeaf(BlockState state) {
        return state.is(BlockTags.LEAVES);
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
