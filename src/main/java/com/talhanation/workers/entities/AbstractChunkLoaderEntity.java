package com.talhanation.workers.entities;

import com.talhanation.workers.Main;
import com.talhanation.workers.config.WorkersModConfig;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.world.ForgeChunkManager;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public abstract class AbstractChunkLoaderEntity extends AbstractInventoryEntity {
    public AbstractChunkLoaderEntity(EntityType<? extends AbstractChunkLoaderEntity> entityType, Level world) {
        super(entityType, world);
    }
    public WorkersChunk currentChunk;
    public WorkersChunk prevChunk;
	public List<WorkersChunk> currentChunks;
	public List<WorkersChunk> prevChunks;
    public boolean initChunkLoad;

    ///////////////////////////////////TICK/////////////////////////////////////////

    public void tick() {
        super.tick();
        updateChunkLoading();
    }

    public void updateChunkLoading() {
        if (WorkersModConfig.WorkerChunkLoading.get() && isAlive() && !this.level.isClientSide) {
            this.currentChunk = new WorkersChunk(this.chunkPosition());
            this.currentChunks = WorkersChunk.getSurroundingChunks(this.currentChunk);

            if(!initChunkLoad){
                for(WorkersChunk chunk : currentChunks){
                    this.setForceChunk(chunk, true);
                }

                initChunkLoad = true;
            }

            if (!currentChunk.isSame(prevChunk)) {
                if(prevChunk != null) {
                    this.prevChunks = WorkersChunk.getSurroundingChunks(this.prevChunk);
                    List<WorkersChunk> toUnLoad = WorkersChunk.getChunksToUnload(this.currentChunks, this.prevChunks);
                    for(WorkersChunk chunk : toUnLoad) this.setForceChunk(chunk, true);
                    initChunkLoad = false;
                }
            }

            prevChunk = currentChunk;

        }
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

        public WorkersChunk getNextChunk(Direction direction) {
            WorkersChunk workersChunk = null;
            switch (direction){
                case NORTH -> workersChunk = new WorkersChunk(this.x, this.z - 1);
                case EAST -> workersChunk = new WorkersChunk(this.x + 1, this.z);
                case SOUTH -> workersChunk = new WorkersChunk(this.x, this.z + 1);
                case WEST -> workersChunk = new WorkersChunk(this.x - 1, this.z);

                default -> {}
            }

            return workersChunk;
        }

        public static List<WorkersChunk> getSurroundingChunks(@NotNull WorkersChunk currentChunk){
            List<WorkersChunk> list = new ArrayList<>();

            for(int i = -1; i <= 1; i++){
                for(int k = -1; k <= 1; k++){
                    WorkersChunk newChunk = new WorkersChunk(currentChunk.x + i, currentChunk.z + k);
                    list.add(newChunk);
                }
            }
            return list;
        }

        public static List<WorkersChunk> getChunksToUnload(List<WorkersChunk> currentChunks, List<WorkersChunk> prevChunks) {
            List<WorkersChunk> list = new ArrayList<>();

            for(WorkersChunk prevChunk : prevChunks){
                for(WorkersChunk currentChunk : currentChunks) {
                    if(!currentChunk.isSame(prevChunk)) list.add(prevChunk);
                }
            }
            return list;
        }
    }
}
