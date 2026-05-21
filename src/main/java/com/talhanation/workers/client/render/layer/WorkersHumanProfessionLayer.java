package com.talhanation.workers.client.render.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.workers.WorkersMain;
import com.talhanation.workers.entities.*;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class WorkersHumanProfessionLayer extends RenderLayer<AbstractRecruitEntity, HumanoidModel<AbstractRecruitEntity>> {

    private static final ResourceLocation[] TEXTURE = {
            new ResourceLocation(WorkersMain.MOD_ID,"textures/entity/human/human_miner_cloth.png"),
            new ResourceLocation(WorkersMain.MOD_ID,"textures/entity/human/human_farmer_cloth.png"),
            new ResourceLocation(WorkersMain.MOD_ID,"textures/entity/human/human_fisherman_cloth.png"),
            new ResourceLocation(WorkersMain.MOD_ID,"textures/entity/human/human_animalfarmer_cloth.png"),
            new ResourceLocation(WorkersMain.MOD_ID,"textures/entity/human/human_lumberjack_cloth.png"),
            new ResourceLocation(WorkersMain.MOD_ID,"textures/entity/human/human_merchant_cloth.png"),
            new ResourceLocation(WorkersMain.MOD_ID,"textures/entity/human/human_courier_cloth.png"),
            new ResourceLocation(WorkersMain.MOD_ID,"textures/entity/human/human_builder_cloth.png"),
            new ResourceLocation(WorkersMain.MOD_ID,"textures/entity/human/human_cook_cloth.png")
    };

    public WorkersHumanProfessionLayer(LivingEntityRenderer<AbstractRecruitEntity, HumanoidModel<AbstractRecruitEntity>> renderer) {
        super(renderer);
    }

    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int p_117722_, AbstractRecruitEntity worker, float p_117724_, float p_117725_, float p_117726_, float p_117727_, float p_117728_, float p_117729_) {
        if(!worker.isInvisible()){
            VertexConsumer vertexconsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(getResourceType(worker)));
            this.getParentModel().renderToBuffer(poseStack, vertexconsumer, p_117722_, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private ResourceLocation getResourceType(AbstractRecruitEntity worker){
        if(worker instanceof MinerEntity){
            return TEXTURE[0];
        }
        else if(worker instanceof FarmerEntity){
            return TEXTURE[1];
        }
        else if(worker instanceof FishermanEntity){
            return TEXTURE[2];
        }
        else if(worker instanceof AnimalFarmerEntity){
            return TEXTURE[3];
        }
        else if(worker instanceof LumberjackEntity){
            return TEXTURE[4];
        }
        else if(worker instanceof MerchantEntity){
            return TEXTURE[5];
        }
        else if(worker instanceof CourierEntity){
            return TEXTURE[6];
        }
        else if(worker instanceof BuilderEntity){
            return TEXTURE[7];
        }
        return TEXTURE[0];
    }

}
