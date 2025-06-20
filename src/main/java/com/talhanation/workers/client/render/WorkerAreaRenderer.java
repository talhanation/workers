package com.talhanation.workers.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.talhanation.recruits.client.events.ClientEvent;
import com.talhanation.workers.entities.workarea.WorkAreaEntity;
import net.minecraft.client.Minecraft;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

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
    public void render(@NotNull WorkAreaEntity workAreaEntity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        Player player = Minecraft.getInstance().player;
        if(player == null) return;

        if(!player.getUUID().equals(workAreaEntity.getPlayerUUID()) || (player.getTeam() != null && !player.getTeam().getName().equals(workAreaEntity.getTeamStringID()))) return;

        Entity looking = ClientEvent.getEntityByLooking();

        poseStack.pushPose();

        float rotation = (workAreaEntity.tickCount + partialTicks) * 3.0F;

        ItemStack itemstack = workAreaEntity.getRenderItem().getDefaultInstance();

        poseStack.translate(0, 1, 0);
        poseStack.scale(2.30f, 2.30f, 2.30f);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));

        BakedModel bakedmodel = this.itemRenderer.getModel(itemstack, workAreaEntity.level(), null, workAreaEntity.getId());
        this.itemRenderer.render(itemstack, ItemDisplayContext.GROUND, false, poseStack, bufferSource, packedLight, OverlayTexture.NO_OVERLAY, bakedmodel);

        poseStack.popPose();

        if(looking == null || !looking.equals(workAreaEntity)) return;

        double x = Mth.lerp(partialTicks, workAreaEntity.xOld, workAreaEntity.getX());
        double y = Mth.lerp(partialTicks, workAreaEntity.yOld, workAreaEntity.getY());
        double z = Mth.lerp(partialTicks, workAreaEntity.zOld, workAreaEntity.getZ());

        AABB worldBox = new AABB(
                workAreaEntity.getOnPos().getX() - workAreaEntity.getSize(),
                workAreaEntity.getOnPos().getY(),
                workAreaEntity.getOnPos().getZ() - workAreaEntity.getSize(),
                workAreaEntity.getOnPos().getX() + workAreaEntity.getSize() + 1,
                workAreaEntity.getOnPos().getY() + workAreaEntity.getHeight(),
                workAreaEntity.getOnPos().getZ() + workAreaEntity.getSize() + 1
        );

        AABB relativeBox = worldBox.move(-x, -y, -z);


        poseStack.pushPose();
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.lines());
        LevelRenderer.renderLineBox(poseStack, vertexConsumer, relativeBox, 1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
    }


}
