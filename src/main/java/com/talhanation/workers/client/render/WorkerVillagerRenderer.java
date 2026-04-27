package com.talhanation.workers.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.recruits.client.render.RecruitVillagerRenderer;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.workers.client.render.layer.WorkersVillagerProfessionLayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class WorkerVillagerRenderer extends RecruitVillagerRenderer implements IRenderWorkArea{
    public WorkerVillagerRenderer(EntityRendererProvider.Context mgr) {
        super(mgr);
        this.addLayer(new WorkersVillagerProfessionLayer(this));
    }

    @Override
    public void render(AbstractRecruitEntity recruit, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        super.render(recruit, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }
}
