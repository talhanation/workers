package com.talhanation.workers.entities.ai.navigation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Target;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class SailorNodeEvaluator extends WalkNodeEvaluator {

    private float oldWalkableCost;
    private float oldWaterBorderCost;

    public void prepare(@NotNull PathNavigationRegion region, @NotNull Mob mob) {
        super.prepare(region, mob);
        mob.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
        this.oldWalkableCost = mob.getPathfindingMalus(BlockPathTypes.WALKABLE);
        mob.setPathfindingMalus(BlockPathTypes.WALKABLE, -1F);
        this.oldWaterBorderCost = mob.getPathfindingMalus(BlockPathTypes.WATER_BORDER);
        mob.setPathfindingMalus(BlockPathTypes.WATER_BORDER, 16.0F);
    }

    public void done() {
        this.mob.setPathfindingMalus(BlockPathTypes.WALKABLE, this.oldWalkableCost);
        this.mob.setPathfindingMalus(BlockPathTypes.WATER_BORDER, this.oldWaterBorderCost);
        super.done();
    }

    @Nullable
    public Node getStart() {
        return this.getStartNode(new BlockPos(Mth.floor(this.mob.getBoundingBox().minX), Mth.floor(this.mob.getBoundingBox().minY + 0.5D), Mth.floor(this.mob.getBoundingBox().minZ)));
    }

    @Nullable
    public Target getGoal(double p_164662_, double p_164663_, double p_164664_) {
        return this.getTargetFromNode(this.getNode(Mth.floor(p_164662_), Mth.floor(p_164663_ + 0.5D), Mth.floor(p_164664_)));
    }

    public int getNeighbors(Node @NotNull [] nodes, Node nodeIn) {
        int i = 0;
        int j = 0;
        BlockPathTypes cachedBlockType = this.getCachedBlockType(this.mob, nodeIn.x, nodeIn.y, nodeIn.z);

        double d0 = this.getFloorLevel(new BlockPos(nodeIn.x, nodeIn.y, nodeIn.z));
        Node node = this.findAcceptedNode(nodeIn.x, nodeIn.y, nodeIn.z + 1, j, d0, Direction.SOUTH, cachedBlockType);
        if (this.isNeighborValid(node, nodeIn)) {
            nodes[i++] = node;
        }

        Node node1 = this.findAcceptedNode(nodeIn.x - 1, nodeIn.y, nodeIn.z, j, d0, Direction.WEST, cachedBlockType);
        if (this.isNeighborValid(node1, nodeIn)) {
            nodes[i++] = node1;
        }

        Node node2 = this.findAcceptedNode(nodeIn.x + 1, nodeIn.y, nodeIn.z, j, d0, Direction.EAST, cachedBlockType);
        if (this.isNeighborValid(node2, nodeIn)) {
            nodes[i++] = node2;
        }

        Node node3 = this.findAcceptedNode(nodeIn.x, nodeIn.y, nodeIn.z - 1, j, d0, Direction.NORTH, cachedBlockType);
        if (this.isNeighborValid(node3, nodeIn)) {
            nodes[i++] = node3;
        }

        Node node4 = this.findAcceptedNode(nodeIn.x - 1, nodeIn.y, nodeIn.z - 1, j, d0, Direction.NORTH, cachedBlockType);
        if (this.isDiagonalValid(nodeIn, node1, node3, node4)) {
            nodes[i++] = node4;
        }

        Node node5 = this.findAcceptedNode(nodeIn.x + 1, nodeIn.y, nodeIn.z - 1, j, d0, Direction.NORTH, cachedBlockType);
        if (this.isDiagonalValid(nodeIn, node2, node3, node5)) {
            nodes[i++] = node5;
        }

        Node node6 = this.findAcceptedNode(nodeIn.x - 1, nodeIn.y, nodeIn.z + 1, j, d0, Direction.SOUTH, cachedBlockType);
        if (this.isDiagonalValid(nodeIn, node1, node, node6)) {
            nodes[i++] = node6;
        }

        Node node7 = this.findAcceptedNode(nodeIn.x + 1, nodeIn.y, nodeIn.z + 1, j, d0, Direction.SOUTH, cachedBlockType);
        if (this.isDiagonalValid(nodeIn, node2, node, node7)) {
            nodes[i++] = node7;
        }

        return i;
    }

    protected double getFloorLevel(@NotNull BlockPos p_164674_) {
        return this.mob.getLevel().getSeaLevel();
    }

    protected boolean isAmphibious() {
        return true;
    }

    public @NotNull BlockPathTypes getBlockPathType(@NotNull BlockGetter blockGetter, int x, int y, int z) {
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
        BlockPathTypes blockpathtypes = getBlockPathTypeRaw(blockGetter, mutableBlockPos.set(x, y, z));
        if (blockpathtypes == BlockPathTypes.WATER) {
            for(Direction direction : Direction.values()) {
                BlockPathTypes blockPathTypes = getBlockPathTypeRaw(blockGetter, mutableBlockPos.set(x, y, z).move(direction));
                if (blockPathTypes == BlockPathTypes.BLOCKED) {
                    return BlockPathTypes.WATER_BORDER;
                }
            }

            return BlockPathTypes.WATER;
        } else {
            return getBlockPathTypeStatic(blockGetter, mutableBlockPos);
        }
    }
}