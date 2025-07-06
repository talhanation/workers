package com.talhanation.workers.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.talhanation.recruits.client.events.ClientEvent;
import com.talhanation.workers.entities.workarea.AbstractWorkAreaEntity;
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
public class WorkerAreaRenderer extends EntityRenderer<AbstractWorkAreaEntity> {
    private final ItemRenderer itemRenderer;
    public WorkerAreaRenderer(EntityRendererProvider.Context mgr) {
        super(mgr);
        this.itemRenderer = mgr.getItemRenderer();
        this.shadowRadius = 0.15F;
        this.shadowStrength = 0.75F;
    }
    @Override
    public ResourceLocation getTextureLocation(AbstractWorkAreaEntity p_115034_) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
    //ItemEntityRenderer
    @Override
    public void render(@NotNull AbstractWorkAreaEntity abstractWorkAreaEntity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        super.render(abstractWorkAreaEntity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
        Player player = Minecraft.getInstance().player;
        if(player == null) return;

        if(!player.getUUID().equals(abstractWorkAreaEntity.getPlayerUUID()) || (player.getTeam() != null && !player.getTeam().getName().equals(abstractWorkAreaEntity.getTeamStringID()))) return;

        poseStack.pushPose();

        float rotation = (abstractWorkAreaEntity.tickCount + partialTicks) * 3.0F;

        ItemStack itemstack = abstractWorkAreaEntity.getRenderItem().getDefaultInstance();

        poseStack.translate(0, 1, 0);
        poseStack.scale(2.30f, 2.30f, 2.30f);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));

        BakedModel bakedmodel = this.itemRenderer.getModel(itemstack, abstractWorkAreaEntity.level(), null, abstractWorkAreaEntity.getId());
        this.itemRenderer.render(itemstack, ItemDisplayContext.GROUND, false, poseStack, bufferSource, packedLight, OverlayTexture.NO_OVERLAY, bakedmodel);

        poseStack.popPose();

        Entity looking = ClientEvent.getEntityByLooking();

        if(!abstractWorkAreaEntity.showBox && (looking == null || !looking.equals(abstractWorkAreaEntity))) return;


        double x = Mth.lerp(partialTicks, abstractWorkAreaEntity.xOld, abstractWorkAreaEntity.getX());
        double y = Mth.lerp(partialTicks, abstractWorkAreaEntity.yOld, abstractWorkAreaEntity.getY());
        double z = Mth.lerp(partialTicks, abstractWorkAreaEntity.zOld, abstractWorkAreaEntity.getZ());

        AABB worldBox = new AABB(
                abstractWorkAreaEntity.getOnPos().getX() - abstractWorkAreaEntity.getSize(),
                abstractWorkAreaEntity.getOnPos().getY(),
                abstractWorkAreaEntity.getOnPos().getZ() - abstractWorkAreaEntity.getSize(),
                abstractWorkAreaEntity.getOnPos().getX() + abstractWorkAreaEntity.getSize() + 1,
                abstractWorkAreaEntity.getOnPos().getY() + abstractWorkAreaEntity.getHeight(),
                abstractWorkAreaEntity.getOnPos().getZ() + abstractWorkAreaEntity.getSize() + 1
        );

        AABB relativeBox = worldBox.move(-x, -y, -z);


        for (AbstractWorkAreaEntity neighbor : AbstractWorkAreaEntity.getNearbyAreas(abstractWorkAreaEntity.level(), abstractWorkAreaEntity.getOnPos(), 20)) {
            if (neighbor.getTeamStringID().equals(abstractWorkAreaEntity.getTeamStringID()) &&
                    neighbor.getPlayerUUID().equals(abstractWorkAreaEntity.getPlayerUUID())) {
                AABB neighborBox = new AABB(
                        neighbor.getOnPos().getX() - neighbor.getSize(),
                        neighbor.getOnPos().getY(),
                        neighbor.getOnPos().getZ() - neighbor.getSize(),
                        neighbor.getOnPos().getX() + neighbor.getSize() + 1,
                        neighbor.getOnPos().getY() + neighbor.getHeight(),
                        neighbor.getOnPos().getZ() + neighbor.getSize() + 1
                ).move(-x, -y, -z);

                poseStack.pushPose();
                LevelRenderer.renderLineBox(poseStack, bufferSource.getBuffer(RenderType.lines()), neighborBox, 0.5F, 1.0F, 0.5F, 0.7F);
                poseStack.popPose();
            }
        }

        poseStack.pushPose();
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.lines());
        LevelRenderer.renderLineBox(poseStack, vertexConsumer, relativeBox, 1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
    }


}
