package com.talhanation.workers.client.render.villager;

import com.talhanation.workers.Main;
import com.talhanation.workers.client.render.AbstractWorkersVillagerRenderer;
import com.talhanation.workers.entities.AbstractInventoryEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class ShepherdVillagerRenderer extends AbstractWorkersVillagerRenderer {

    private static final ResourceLocation[] TEXTURE = {
            new ResourceLocation(Main.MOD_ID,"textures/entity/villager/shepherd.png"),
    };

    public ShepherdVillagerRenderer(EntityRendererProvider.Context mgr) {
        super(mgr);
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractInventoryEntity p_110775_1_) {
        return TEXTURE[0];
    }
}
