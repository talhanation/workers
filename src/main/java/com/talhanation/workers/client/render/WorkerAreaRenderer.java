package com.talhanation.workers.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.talhanation.workers.entities.WorkAreaEntity;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WorkerAreaRenderer extends EntityRenderer<WorkAreaEntity> {
    private final ItemRenderer itemRenderer;
    public WorkerAreaRenderer(EntityRendererProvider.Context mgr) {
        super(mgr);
        this.itemRenderer = mgr.getItemRenderer();
        this.shadowRadius = 0.15F;
        this.shadowStrength = 0.75F;
    }
    @Override
    public ResourceLocation getTextureLocation(WorkAreaEntity p_115034_) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
    //ItemEntityRenderer
    @Override
    public void render(WorkAreaEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();

        // Rotation wie bei ItemEntity
        float rotation = (entity.tickCount + partialTicks) * 3.0F;

        ItemStack itemstack = entity.getRenderItem().getDefaultInstance();

        poseStack.translate(0, 1, 0);
        poseStack.scale(2.30f, 2.30f, 2.30f);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));

        BakedModel bakedmodel = this.itemRenderer.getModel(itemstack, entity.level(), null, entity.getId());
        this.itemRenderer.render(itemstack, ItemDisplayContext.GROUND, false, poseStack, bufferSource, packedLight, OverlayTexture.NO_OVERLAY, bakedmodel);

        poseStack.popPose();

        double x = Mth.lerp(partialTicks, entity.xOld, entity.getX());
        double y = Mth.lerp(partialTicks, entity.yOld, entity.getY());
        double z = Mth.lerp(partialTicks, entity.zOld, entity.getZ());

        AABB worldBox = new AABB(
                entity.getOnPos().getX() - entity.radius,
                entity.getOnPos().getY(),
                entity.getOnPos().getZ() - entity.radius,
                entity.getOnPos().getX() + entity.radius + 1,
                entity.getOnPos().getY() + entity.height,
                entity.getOnPos().getZ() + entity.radius + 1
        );

        AABB relativeBox = worldBox.move(-x, -y, -z);


        poseStack.pushPose();
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.lines());
        LevelRenderer.renderLineBox(poseStack, vertexConsumer, relativeBox, 1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
    }


}
