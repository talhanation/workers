package com.talhanation.workers.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.talhanation.recruits.client.events.ClientEvent;
import com.talhanation.workers.entities.workarea.AbstractWorkAreaEntity;
import com.talhanation.workers.entities.workarea.BuildArea;
import net.minecraft.client.Minecraft;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

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

        if(abstractWorkAreaEntity instanceof BuildArea buildArea){
            renderStructurePreview(poseStack, bufferSource, buildArea.getStructureNBT(), buildArea.getOnPos(), player.getCommandSenderWorld());
        }

        double x = Mth.lerp(partialTicks, abstractWorkAreaEntity.xOld, abstractWorkAreaEntity.getX());
        double y = Mth.lerp(partialTicks, abstractWorkAreaEntity.yOld, abstractWorkAreaEntity.getY());
        double z = Mth.lerp(partialTicks, abstractWorkAreaEntity.zOld, abstractWorkAreaEntity.getZ());

        AABB worldBox = new AABB(
                abstractWorkAreaEntity.getOnPos().getX() - abstractWorkAreaEntity.getXSize(),
                abstractWorkAreaEntity.getOnPos().getY(),
                abstractWorkAreaEntity.getOnPos().getZ() - abstractWorkAreaEntity.getZSize(),
                abstractWorkAreaEntity.getOnPos().getX() + abstractWorkAreaEntity.getXSize() + 1,
                abstractWorkAreaEntity.getOnPos().getY() + abstractWorkAreaEntity.getYSize(),
                abstractWorkAreaEntity.getOnPos().getZ() + abstractWorkAreaEntity.getZSize() + 1
        );

        AABB relativeBox = worldBox.move(-x, -y, -z);


        for (AbstractWorkAreaEntity neighbor : AbstractWorkAreaEntity.getNearbyAreas(abstractWorkAreaEntity.level(), abstractWorkAreaEntity.getOnPos(), 20)) {
            if (neighbor.getTeamStringID().equals(abstractWorkAreaEntity.getTeamStringID()) &&
                    neighbor.getPlayerUUID().equals(abstractWorkAreaEntity.getPlayerUUID())) {
                AABB neighborBox = new AABB(
                        neighbor.getOnPos().getX() - neighbor.getXSize(),
                        neighbor.getOnPos().getY(),
                        neighbor.getOnPos().getZ() - neighbor.getZSize(),
                        neighbor.getOnPos().getX() + neighbor.getXSize() + 1,
                        neighbor.getOnPos().getY() + neighbor.getYSize(),
                        neighbor.getOnPos().getZ() + neighbor.getZSize() + 1
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

    public static void renderStructurePreview(PoseStack poseStack, MultiBufferSource buffer, CompoundTag tag, BlockPos origin, Level level) {
        if (tag == null || !tag.contains("blocks")) return;

        ListTag blocks = tag.getList("blocks", Tag.TAG_COMPOUND);
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();

        for (Tag t : blocks) {
            CompoundTag blockTag = (CompoundTag) t;

            BlockState state = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), blockTag.getCompound("state"));
            if (state == null) continue;

            int x = blockTag.getInt("x");
            int y = blockTag.getInt("y");
            int z = blockTag.getInt("z");

            BlockPos worldPos = origin.offset(x, y, z);

            // Transparent rendern
            poseStack.pushPose();
            poseStack.translate(worldPos.getX(), worldPos.getY(), worldPos.getZ());

            dispatcher.renderBatched(
                    state,
                    worldPos,
                    level,
                    poseStack,
                    buffer.getBuffer(RenderType.translucent()),
                    false,
                    level.getRandom()
            );

            poseStack.popPose();
        }
    }
}
