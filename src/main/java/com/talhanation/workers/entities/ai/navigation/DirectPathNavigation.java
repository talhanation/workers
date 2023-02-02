package com.talhanation.workers.entities.ai.navigation;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;

public class DirectPathNavigation extends GroundPathNavigation {
    private final Mob mob;
    private float yMobOffset = 0;

    public DirectPathNavigation(Mob mob, Level level) {
        this(mob, level, 3);
    }

    public DirectPathNavigation(Mob mob, Level level, float yMobOffset) {
        super(mob, level);
        this.mob = mob;
        this.yMobOffset = yMobOffset;
    }

    public void tick() {
        ++this.tick;
    }

    public boolean moveTo(double x, double y, double z, double speedIn) {
        Path path = this.path;
        if (path == null) return false;
        Node endNode = path.getEndNode();
        if (endNode == null) return false;
        mob.getMoveControl().setWantedPosition(endNode.x, endNode.y, endNode.z, speedIn);
        return true;
    }
    public boolean moveTo(Entity entityIn, double speedIn) {
        mob.getMoveControl().setWantedPosition(entityIn.getX(), entityIn.getY() + yMobOffset, entityIn.getZ(), speedIn);
        return true;
    }
}
