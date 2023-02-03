package com.talhanation.workers.client.render.human;

import com.talhanation.workers.Main;
import com.talhanation.workers.client.render.AbstractWorkersHumanRenderer;
import com.talhanation.workers.entities.AbstractWorkerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class FishermanHumanRenderer extends AbstractWorkersHumanRenderer<AbstractWorkerEntity> {

    private static final ResourceLocation[] TEXTURE = {
            new ResourceLocation(Main.MOD_ID, "textures/entity/human/fisherman.png"),
    };

    public FishermanHumanRenderer(EntityRendererProvider.Context mgr) {
        super(mgr);
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractWorkerEntity p_110775_1_) {
        return TEXTURE[0];
    }
}
