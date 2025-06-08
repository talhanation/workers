package com.talhanation.workers.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.talhanation.recruits.client.render.RecruitVillagerRenderer;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.workers.entities.AbstractWorkerEntity;
import com.talhanation.workers.entities.FarmerEntity;
import com.talhanation.workers.world.CropArea;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;


public class WorkerVillagerRenderer extends RecruitVillagerRenderer implements IRenderWorkArea{
    public WorkerVillagerRenderer(EntityRendererProvider.Context mgr) {
        super(mgr);
        //this.addLayer(new WorkersProfessionLayer(this));
    }

    @Override
    public void render(AbstractRecruitEntity recruit, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        super.render(recruit, entityYaw, partialTicks, poseStack, bufferSource, packedLight); // zuerst super call für richtige Reihenfolge

        if (recruit instanceof FarmerEntity farmer && farmer.getCropAreasTag() != null && !farmer.getCropAreasTag().isEmpty()) {
            for(CropArea cropArea : farmer.getCropAreas()){
                BlockPos centerPos = cropArea.getCenterPos();
                AABB worldBox = new AABB(centerPos).inflate(4);
                Vec3 boxCenter = worldBox.getCenter();

                double x = Mth.lerp(partialTicks, farmer.xOld, farmer.getX());
                double y = Mth.lerp(partialTicks, farmer.yOld, farmer.getY());
                double z = Mth.lerp(partialTicks, farmer.zOld, farmer.getZ());

                AABB relativeBox = worldBox.move(-x, -y, -z);

                Vec3 from = new Vec3(0, farmer.getBbHeight() / 2.0, 0); // Mitte der Entity
                Vec3 to = boxCenter.subtract(x, y, z);

                VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.lines());

                this.renderWorkArea(poseStack, vertexConsumer, relativeBox);


                this.drawLine(poseStack, vertexConsumer, from, to, 1f, 1f, 1f, 1f); // cyanfarbene Linie
            }

        }
    }

}
