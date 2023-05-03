package com.talhanation.workers.entities;

import net.minecraft.core.BlockPos;

public interface IBoatController {

    default AbstractWorkerEntity getWorker() {
        return (AbstractWorkerEntity) this;
    }

    BlockPos getSailPos();

    void setSailPos(BlockPos pos);

    double getControlAccuracy();
}
