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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class SailorNodeEvaluator extends WalkNodeEvaluator {
    private float oldWaterMalus;
    private float oldWalkableMalus;
    private float oldWaterBorderMalus;
    private float oldBreachMalus;

    public void prepare(@NotNull PathNavigationRegion region, @NotNull Mob mob) {
        super.prepare(region, mob);
        this.oldWaterMalus = mob.getPathfindingMalus(BlockPathTypes.WATER);
        mob.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
        this.oldWalkableMalus = mob.getPathfindingMalus(BlockPathTypes.WALKABLE);
        mob.setPathfindingMalus(BlockPathTypes.WALKABLE, -1F);
        this.oldWaterBorderMalus = mob.getPathfindingMalus(BlockPathTypes.WATER_BORDER);
        mob.setPathfindingMalus(BlockPathTypes.WATER_BORDER, 32F);
        this.oldBreachMalus = mob.getPathfindingMalus(BlockPathTypes.BREACH);
        mob.setPathfindingMalus(BlockPathTypes.BREACH, -1F);
    }

    public void done() {
        this.mob.setPathfindingMalus(BlockPathTypes.WATER, this.oldWaterMalus);
        this.mob.setPathfindingMalus(BlockPathTypes.WALKABLE, this.oldWalkableMalus);
        this.mob.setPathfindingMalus(BlockPathTypes.WATER_BORDER, this.oldWaterBorderMalus);
        this.mob.setPathfindingMalus(BlockPathTypes.BREACH, this.oldBreachMalus);
        super.done();
    }
    @Override
    public boolean canReachWithoutCollision(Node p_77625_) {
        AABB aabb = this.mob.getBoundingBox().inflate(15);
        Vec3 vec3 = new Vec3((double)p_77625_.x - this.mob.getX() + aabb.getXsize() / 2.0D, (double)p_77625_.y - this.mob.getY() + aabb.getYsize() / 2.0D, (double)p_77625_.z - this.mob.getZ() + aabb.getZsize() / 2.0D);
        int i = Mth.ceil(vec3.length() / aabb.getSize());
        vec3 = vec3.scale((double)(1.0F / (float)i));

        for(int j = 1; j <= i; ++j) {
            aabb = aabb.move(vec3);
            if (this.hasCollisions(aabb)) {
                return false;
            }
        }

        return true;
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