package com.talhanation.workers.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.talhanation.recruits.client.events.ClientEvent;
import com.talhanation.workers.entities.MinerEntity;
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
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
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
        Player player = Minecraft.getInstance().player;
        if(player == null) return;

        super.render(abstractWorkAreaEntity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);

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

        if(!abstractWorkAreaEntity.canPlayerSee(player)) return;

        boolean lookingAtThis = looking != null && looking.equals(abstractWorkAreaEntity);

        if(abstractWorkAreaEntity instanceof BuildArea buildArea){
            // The structure projection stays visible without crosshair focus when the
            // build area has the "always show projection" setting enabled.
            if(buildArea.showBox || lookingAtThis || buildArea.getAlwaysShowProjection()){
                renderStructurePreview(poseStack, buildArea);
            }
        }

        if(!abstractWorkAreaEntity.showBox && !lookingAtThis) return;

        double x = Mth.lerp(partialTicks, abstractWorkAreaEntity.xOld, abstractWorkAreaEntity.getX());
        double y = Mth.lerp(partialTicks, abstractWorkAreaEntity.yOld, abstractWorkAreaEntity.getY());
        double z = Mth.lerp(partialTicks, abstractWorkAreaEntity.zOld, abstractWorkAreaEntity.getZ());

        AABB area  = abstractWorkAreaEntity.getArea();
        AABB worldBox = new AABB(area.minX, area.minY, area.minZ,
                area.maxX + 1, area.maxY, area.maxZ + 1);
        AABB relativeBox = worldBox.move(-x, -y, -z);

        for (AbstractWorkAreaEntity neighbor : AbstractWorkAreaEntity.getNearbyAreas(abstractWorkAreaEntity.level(), abstractWorkAreaEntity.getOnPos(), 20)) {
            if (neighbor.getTeamStringID().equals(abstractWorkAreaEntity.getTeamStringID()) && neighbor.getPlayerUUID().equals(abstractWorkAreaEntity.getPlayerUUID())) {
                AABB area1  = neighbor.getArea();
                AABB worldBox1 = new AABB(area1.minX, area1.minY, area1.minZ,
                        area1.maxX + 1, area1.maxY, area1.maxZ + 1);
                AABB relativeBox1 = worldBox1.move(-x, -y, -z);


                poseStack.pushPose();
                LevelRenderer.renderLineBox(poseStack, bufferSource.getBuffer(RenderType.lines()), relativeBox1, 0.5F, 1.0F, 0.5F, 0.7F);
                poseStack.popPose();
            }
        }

        poseStack.pushPose();
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.lines());
        LevelRenderer.renderLineBox(poseStack, vertexConsumer, relativeBox, 1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
    }

    private void renderStructurePreview(PoseStack poseStack, BuildArea buildArea) {
        CompoundTag nbt = buildArea.getStructureNBT();
        if (nbt == null || nbt.isEmpty()) return;
        List<ScannedBlock> structure = StructureManager.parseStructureFromNBT(nbt);
        if (structure.isEmpty()) return;

        int width = nbt.getInt("width");
        Direction scanFacing = Direction.byName(nbt.getString("facing"));
        Direction facing = buildArea.getFacing();
        Direction right = facing.getClockWise();
        int rotationSteps = (4 + facing.get2DDataValue() - scanFacing.get2DDataValue()) % 4;
        BlockPos origin = buildArea.getOriginPos();

        Minecraft mc = Minecraft.getInstance();
        BlockRenderDispatcher dispatcher = mc.getBlockRenderer();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        poseStack.pushPose();

        for (ScannedBlock scannedBlock : structure) {
            BlockPos relPos = scannedBlock.relativePos();
            int relX = relPos.getX();
            int relY = relPos.getY();
            int relZ = relPos.getZ();

            // Compute world block position using the same formula as setStartBuild
            BlockPos worldPos = origin
                    .relative(facing, relZ)
                    .relative(right, width - 1 - relX)
                    .above(relY);

            // Offset relative to entity render origin (poseStack is already at entity position)
            double dx = worldPos.getX() - buildArea.getX();
            double dy = worldPos.getY() - buildArea.getY();
            double dz = worldPos.getZ() - buildArea.getZ();

            BlockState state = scannedBlock.state();
            BlockState rotatedState = BuildArea.rotateBlockState(state, rotationSteps);

            FluidState fluidState = state.getFluidState();
            RenderType renderType = null;
            if (!fluidState.isEmpty()) {
                renderType = ItemBlockRenderTypes.getRenderLayer(fluidState);
            }

            ModelData modelData = ModelData.EMPTY;
            if (state.getBlock() instanceof EntityBlock entityBlock) {
                BlockEntity be = entityBlock.newBlockEntity(BlockPos.ZERO, state);
                if (be != null) modelData = be.getModelData();
            }

            if (rotatedState.getRenderShape() == RenderShape.MODEL) {
                poseStack.pushPose();
                poseStack.translate(dx, dy, dz);
                dispatcher.renderSingleBlock(rotatedState, poseStack, bufferSource, 0xF000F0, OverlayTexture.NO_OVERLAY, modelData, renderType);
                poseStack.popPose();
            } else if (rotatedState.getBlock() instanceof EntityBlock entityBlock) {
                BlockEntity be = entityBlock.newBlockEntity(worldPos, rotatedState);
                if (be != null) {
                    if (mc.level != null) be.setLevel(mc.level);
                    @SuppressWarnings("unchecked")
                    BlockEntityRenderer<BlockEntity> renderer =
                            (BlockEntityRenderer<BlockEntity>) mc.getBlockEntityRenderDispatcher().getRenderer(be);
                    if (renderer != null) {
                        poseStack.pushPose();
                        poseStack.translate(dx, dy, dz);
                        renderer.render(be, 0f, poseStack, bufferSource, 0xF000F0, OverlayTexture.NO_OVERLAY);
                        poseStack.popPose();
                    }
                }
            }
        }

        bufferSource.endBatch();
        poseStack.popPose();
    }

}
