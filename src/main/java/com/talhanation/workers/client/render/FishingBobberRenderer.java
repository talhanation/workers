package com.talhanation.workers.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.talhanation.workers.entities.FishermanEntity;
import com.talhanation.workers.entities.FishingBobberEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class FishingBobberRenderer extends EntityRenderer<FishingBobberEntity> {
    private static final ResourceLocation TEXTURE_LOCATION = new ResourceLocation("textures/entity/fishing_hook.png");
    private static final RenderType RENDER_TYPE = RenderType.entityCutout(TEXTURE_LOCATION);
    private static final double VIEW_BOBBING_SCALE = 960.0D;

    public FishingBobberRenderer(EntityRendererProvider.Context p_174117_) {
        super(p_174117_);
    }

    public void render(FishingBobberEntity fishingBobber, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource multiBufferSource, int packetLight) {
        FishermanEntity fisherman = fishingBobber.getOwner();
        if (fisherman != null) {
            poseStack.pushPose();
            poseStack.pushPose();
            poseStack.scale(0.5F, 0.5F, 0.5F);
            poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            PoseStack.Pose posestack$pose = poseStack.last();
            Matrix4f matrix4f = posestack$pose.pose();
            Matrix3f matrix3f = posestack$pose.normal();
            VertexConsumer vertexconsumer = multiBufferSource.getBuffer(RENDER_TYPE);
            vertex(vertexconsumer, matrix4f, matrix3f, packetLight, 0.0F, 0, 0, 1);
            vertex(vertexconsumer, matrix4f, matrix3f, packetLight, 1.0F, 0, 1, 1);
            vertex(vertexconsumer, matrix4f, matrix3f, packetLight, 1.0F, 1, 1, 0);
            vertex(vertexconsumer, matrix4f, matrix3f, packetLight, 0.0F, 1, 0, 0);
            poseStack.popPose();
            int i = fisherman.getMainArm() == HumanoidArm.RIGHT ? 1 : -1;
            ItemStack itemstack = fisherman.getMainHandItem();
            if (!itemstack.canPerformAction(net.minecraftforge.common.ToolActions.FISHING_ROD_CAST)) {
                i = -i;
            }

            float f = fisherman.getAttackAnim(partialTicks);
            float f1 = Mth.sin(Mth.sqrt(f) * (float) Math.PI);
            float f2 = Mth.lerp(partialTicks, fisherman.yBodyRotO, fisherman.yBodyRot) * ((float) Math.PI / 180F);
            double d0 = (double) Mth.sin(f2);
            double d1 = (double) Mth.cos(f2);
            double d2 = (double) i * 0.35D;
            double d3 = 0.8D;
            double d4;
            double d5;
            double d6;
            float f3;

            d4 = Mth.lerp((double) partialTicks, fisherman.xo, fisherman.getX()) - d1 * d2 - d0 * 0.8D;
            d5 = fisherman.yo + (double) fisherman.getEyeHeight() + (fisherman.getY() - fisherman.yo) * (double) partialTicks - 0.45D;
            d6 = Mth.lerp((double) partialTicks, fisherman.zo, fisherman.getZ()) - d0 * d2 + d1 * 0.8D;
            f3 = fisherman.isCrouching() ? -0.1875F : 0.0F;


            double d9 = Mth.lerp((double) partialTicks, fishingBobber.xo, fishingBobber.getX());
            double d10 = Mth.lerp((double) partialTicks, fishingBobber.yo, fishingBobber.getY()) + 0.25D;
            double d8 = Mth.lerp((double) partialTicks, fishingBobber.zo, fishingBobber.getZ());
            float f4 = (float) (d4 - d9);
            float f5 = (float) (d5 - d10) + f3;
            float f6 = (float) (d6 - d8);
            VertexConsumer vertexconsumer1 = multiBufferSource.getBuffer(RenderType.lineStrip());
            PoseStack.Pose posestack$pose1 = poseStack.last();
            int j = 16;

            for (int k = 0; k <= 16; ++k) {
                stringVertex(f4, f5, f6, vertexconsumer1, posestack$pose1, fraction(k, 16), fraction(k + 1, 16));
            }

            poseStack.popPose();
            super.render(fishingBobber, entityYaw, partialTicks, poseStack, multiBufferSource, packetLight);
        }
    }

    private static float fraction(int p_114691_, int p_114692_) {
        return (float) p_114691_ / (float) p_114692_;
    }

    private static void vertex(VertexConsumer p_254464_, Matrix4f p_254085_, Matrix3f p_253962_, int p_254296_, float p_253632_, int p_254132_, int p_254171_, int p_254026_) {
        p_254464_.vertex(p_254085_, p_253632_ - 0.5F, (float) p_254132_ - 0.5F, 0.0F).color(255, 255, 255, 255).uv((float) p_254171_, (float) p_254026_).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(p_254296_).normal(p_253962_, 0.0F, 1.0F, 0.0F).endVertex();
    }

    private static void stringVertex(float p_174119_, float p_174120_, float p_174121_, VertexConsumer p_174122_, PoseStack.Pose p_174123_, float p_174124_, float p_174125_) {
        float f = p_174119_ * p_174124_;
        float f1 = p_174120_ * (p_174124_ * p_174124_ + p_174124_) * 0.5F + 0.25F;
        float f2 = p_174121_ * p_174124_;
        float f3 = p_174119_ * p_174125_ - f;
        float f4 = p_174120_ * (p_174125_ * p_174125_ + p_174125_) * 0.5F + 0.25F - f1;
        float f5 = p_174121_ * p_174125_ - f2;
        float f6 = Mth.sqrt(f3 * f3 + f4 * f4 + f5 * f5);
        f3 /= f6;
        f4 /= f6;
        f5 /= f6;
        p_174122_.vertex(p_174123_.pose(), f, f1, f2).color(0, 0, 0, 255).normal(p_174123_.normal(), f3, f4, f5).endVertex();
    }

    public ResourceLocation getTextureLocation(FishingBobberEntity p_114703_) {
        return TEXTURE_LOCATION;
    }
}
