package com.talhanation.workers.client.render;

import com.talhanation.workers.client.models.ManModel;
import com.talhanation.workers.entities.AbstractWorkerEntity;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;

public abstract class AbstractManRenderer<E extends AbstractWorkerEntity> extends WorkersBipedRenderer<E, ManModel<E>>{
    public AbstractManRenderer(EntityRenderDispatcher mgr) {
        super(mgr, new ManModel(), new ManModel(0.5F, true), new ManModel(1.0F, true), 0.5F);
    }
}
