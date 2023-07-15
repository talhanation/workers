package com.talhanation.workers.entities;

import com.talhanation.workers.Main;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.world.ForgeChunkManager;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public abstract class AbstractChunkLoaderEntity extends AbstractInventoryEntity {
    protected abstract boolean shouldLoadChunk();
    public AbstractChunkLoaderEntity(EntityType<? extends AbstractChunkLoaderEntity> entityType, Level world) {
        super(entityType, world);
    }
	public WorkersChunk currentChunk;
	public WorkersChunk prevChunk;
    public boolean initChunkLoad;

    ///////////////////////////////////TICK/////////////////////////////////////////

    public void tick() {
        super.tick();
        updateChunkLoading();
    }

    public void updateChunkLoading() {
        if (this.shouldLoadChunk() && isAlive() && !this.level.isClientSide) {
            this.currentChunk = new WorkersChunk(this.chunkPosition());

            if(!initChunkLoad){
                this.setForceChunk(currentChunk, true);
                initChunkLoad = true;
            }

            if (!currentChunk.isSame(prevChunk)) {
                if(prevChunk != null) {
                    this.setForceChunk(prevChunk, false);
                    initChunkLoad = false;
                }
            }

            prevChunk = currentChunk;

        }
        if(!this.level.isClientSide)
            Main.LOGGER.info(ForgeChunkManager.hasForcedChunks((ServerLevel) this.level));
    }

    ////////////////////////////////////SET////////////////////////////////////

    private void setForceChunk(WorkersChunk chunk, boolean add) {
        ForgeChunkManager.forceChunk((ServerLevel) this.level, Main.MOD_ID, this, chunk.x, chunk.z, add, false);
    }

    public void die(@NotNull DamageSource dmg) {
        super.die(dmg);
        if(!this.level.isClientSide){
            this.setForceChunk(currentChunk, false);
            this.setForceChunk(prevChunk, false);
        }
    }

	public static class WorkersChunk{
		int x;
		int z;
		
		public WorkersChunk(ChunkPos chunkPosition){
			this(chunkPosition.x, chunkPosition.z);
		}
		
		public WorkersChunk(int x, int z){
			this.x = x;
			this.z = z;
		}
		
		public boolean isSame(WorkersChunk otherChunk){
			if(otherChunk != null) return this.x == otherChunk.x && this.z == otherChunk.z;
			else return false;
		}
	}
}
