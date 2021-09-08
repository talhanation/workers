package com.talhanation.workers.client.models;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import net.minecraft.client.renderer.entity.model.PlayerModel;

public class ModelRecruitEntity extends PlayerModel<AbstractWorkerEntity>{
    public ModelRecruitEntity(float modelSize, boolean smallArms) {
        super(modelSize, smallArms);
    }
}
