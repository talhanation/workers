package com.talhanation.workers.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;


public interface IRenderWorkArea {
    default void renderWorkArea(PoseStack poseStack, VertexConsumer vertexConsumer, AABB aabb){
        LevelRenderer.renderLineBox(poseStack, vertexConsumer, aabb, 1F, 1F, 1F, 1F);
    }

    default void drawLine(PoseStack stack, VertexConsumer buffer, Vec3 from, Vec3 to, float r, float g, float b, float a) {
        Matrix4f matrix = stack.last().pose();
        buffer.vertex(matrix, (float) from.x, (float) from.y, (float) from.z)
                .color(r, g, b, a)
                .normal(0, 1, 0) // Dummy normal – Forge verlangt sie manchmal
                .endVertex();
        buffer.vertex(matrix, (float) to.x, (float) to.y, (float) to.z)
                .color(r, g, b, a)
                .normal(0, 1, 0)
                .endVertex();
    }

}
