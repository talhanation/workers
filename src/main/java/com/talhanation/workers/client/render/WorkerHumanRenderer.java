package com.talhanation.workers.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.recruits.client.render.RecruitHumanRenderer;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class WorkerHumanRenderer extends RecruitHumanRenderer {
    public WorkerHumanRenderer(EntityRendererProvider.Context mgr) {
        super(mgr);
        //this.addLayer(new WorkersProfessionLayer(this));
    }


    @Override
    public void render(AbstractRecruitEntity recruit, float p_117789_, float p_117790_, PoseStack p_117791_, MultiBufferSource p_117792_, int p_117793_) {
        super.render(recruit, p_117789_, p_117790_, p_117791_, p_117792_, p_117793_);


    }
}
