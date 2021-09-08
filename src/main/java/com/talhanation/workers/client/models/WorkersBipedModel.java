package com.talhanation.workers.client.models;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;

import java.util.function.Function;

public class WorkersBipedModel<E extends LivingEntity> extends BipedModel<E> {
    public boolean showChest = false;
    public WorkersBipedModel(float modelSize) {
        super(modelSize);
    }

    public WorkersBipedModel(float modelSize, float yOff, int texW, int texH) {
        super(modelSize, yOff, texW, texH);
    }

    public WorkersBipedModel(Function<ResourceLocation, RenderType> renderType, float modelSize, float yOff, int texW, int texH) {
        super(renderType, modelSize, yOff, texW, texH);
    }

}
