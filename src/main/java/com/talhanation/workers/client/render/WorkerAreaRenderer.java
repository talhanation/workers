package com.talhanation.workers.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.talhanation.recruits.client.events.ClientEvent;
import com.talhanation.workers.entities.workarea.AbstractWorkAreaEntity;
import com.talhanation.workers.entities.workarea.BuildArea;
import com.talhanation.workers.world.ScannedBlock;
import com.talhanation.workers.world.StructureManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;

import java.util.List;

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

        poseStack.translate(0, 0.5, 0);
        poseStack.scale(2.30f, 2.30f, 2.30f);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));

        BakedModel bakedmodel = this.itemRenderer.getModel(itemstack, abstractWorkAreaEntity.level(), null, abstractWorkAreaEntity.getId());
        this.itemRenderer.render(itemstack, ItemDisplayContext.GROUND, false, poseStack, bufferSource, packedLight, OverlayTexture.NO_OVERLAY, bakedmodel);

        poseStack.popPose();

        Entity looking = ClientEvent.getEntityByLooking();

        if(!abstractWorkAreaEntity.showBox && (looking == null || !looking.equals(abstractWorkAreaEntity))) return;

        if(abstractWorkAreaEntity instanceof BuildArea buildArea){
            //renderStructurePreview(poseStack, buildArea);
        }

        double x = Mth.lerp(partialTicks, abstractWorkAreaEntity.xOld, abstractWorkAreaEntity.getX());
        double y = Mth.lerp(partialTicks, abstractWorkAreaEntity.yOld, abstractWorkAreaEntity.getY());
        double z = Mth.lerp(partialTicks, abstractWorkAreaEntity.zOld, abstractWorkAreaEntity.getZ());

        AABB worldBox = abstractWorkAreaEntity.getArea();
        AABB relativeBox = worldBox.move(-x, -y, -z);

        for (AbstractWorkAreaEntity neighbor : AbstractWorkAreaEntity.getNearbyAreas(abstractWorkAreaEntity.level(), abstractWorkAreaEntity.getOnPos(), 20)) {
            if (neighbor.getTeamStringID().equals(abstractWorkAreaEntity.getTeamStringID()) && neighbor.getPlayerUUID().equals(abstractWorkAreaEntity.getPlayerUUID())) {
                AABB neighborBox = neighbor.getArea().move(-x, -y, -z);

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

    private void renderStructurePreview(PoseStack poseStack, BuildArea buildArea) {
        if (buildArea.getStructureNBT() == null) return;
        List<ScannedBlock> structure = StructureManager.parseStructureFromNBT(buildArea.getStructureNBT());
        if (structure.isEmpty()) return;

        poseStack.pushPose();

        Minecraft mc = Minecraft.getInstance();
        BlockRenderDispatcher dispatcher = mc.getBlockRenderer();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        String dir = buildArea.getStructureNBT().getString("facing");
        Direction areaFacing = Direction.byName(dir);;

        int rotationSteps = switch (areaFacing) {
            case NORTH -> 1;
            case EAST -> 2;
            case SOUTH -> 3;
            case WEST -> 1;
            default -> 0;
        };

        poseStack.mulPose(Axis.YP.rotationDegrees(rotationSteps * 90f));
        poseStack.translate(-buildArea.getWidthSize() + 0.5, -1, -0.5);

        for (ScannedBlock block : structure) {
            BlockState state = block.state();
            FluidState fluidState = state.getFluidState();
            BlockPos relPos = block.relativePos();

            poseStack.pushPose();
            poseStack.translate(relPos.getX(), relPos.getY(), relPos.getZ());

            RenderType renderType = null;
            if (!fluidState.isEmpty()) {
                renderType = ItemBlockRenderTypes.getRenderLayer(fluidState);
            }

            ModelData modelData = ModelData.EMPTY;
            if (state.getBlock() instanceof EntityBlock entityBlock) {
                BlockEntity be = entityBlock.newBlockEntity(BlockPos.ZERO, state);
                if (be != null) {
                    modelData = be.getModelData();
                }
            }

            BlockState rotatedState = BuildArea.rotateBlockState(state, 4 - areaFacing.get2DDataValue());

            dispatcher.renderSingleBlock(
                    rotatedState,
                    poseStack,
                    bufferSource,
                    0xF000F0,
                    OverlayTexture.NO_OVERLAY,
                    modelData,
                    renderType
            );
            poseStack.popPose();
        }

        bufferSource.endBatch();
        poseStack.popPose();
    }


}
