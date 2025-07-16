package com.talhanation.workers.client.gui.structureRenderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.talhanation.workers.entities.workarea.BuildArea;
import com.talhanation.workers.world.ScannedBlock;
import com.talhanation.workers.world.StructureManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.client.model.data.ModelData;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class StructurePreviewWidget extends AbstractWidget {

    private List<ScannedBlock> structure = new ArrayList<>();
    private CompoundTag structureNBT;
    private float rotationX = 25;
    private float rotationY = 180;
    private float zoom = 3.5f;
    private final float areaX;
    private final float areaY;
    private boolean isDragging = false;
    private double lastMouseX = 0;
    private double lastMouseY = 0;
    private float offsetX = 0;
    private float offsetY = 0;
    private boolean isRightDragging = false;
    private long lastClickTime = 0;
    public StructurePreviewWidget(int x, int y, int width, int height, int areaX, int areaY) {
        super(x, y, width, height, Component.empty());
        this.areaX = areaX;
        this.areaY = areaY;
    }

    public void setStructure(List<ScannedBlock> structure, CompoundTag structureNBT) {
        this.structureNBT = structureNBT;
        this.structure = structure;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        guiGraphics.fill(getX() - 1, getY() - 1, getX() + getWidth() + 1, getY() + getHeight() + 1, 0xFF555555);
        guiGraphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0xFF222222);

        int screenHeight = Minecraft.getInstance().getWindow().getHeight();
        int scale = (int) Minecraft.getInstance().getWindow().getGuiScale();

        int scissorX = getX() * scale;
        int scissorY = screenHeight - (getY() + getHeight()) * scale;
        int scissorWidth = getWidth() * scale;
        int scissorHeight = getHeight() * scale;

        RenderSystem.enableScissor(scissorX, scissorY, scissorWidth, scissorHeight);

        renderStructurePreview(guiGraphics.pose(), getX() + getWidth() / 2, getY() + getHeight() / 2);

        RenderSystem.disableScissor();
    }

    private void renderStructurePreview(PoseStack poseStack, int previewX, int previewY) {
        if (structure == null || structure.isEmpty()) return;
        if (structureNBT == null) return;
        poseStack.pushPose();

        poseStack.translate(previewX, previewY, 1000);
        poseStack.scale(zoom, -zoom, zoom);
        poseStack.translate(offsetX, offsetY, 0);

        float pivotX = areaX / 2f;
        float pivotZ = areaY / 2f;
        poseStack.translate(pivotX, 0, pivotZ);
        poseStack.mulPose(Axis.XP.rotationDegrees(rotationX));
        poseStack.mulPose(Axis.YP.rotationDegrees(rotationY));
        poseStack.translate(-pivotX, 0, -pivotZ);

        Minecraft mc = Minecraft.getInstance();
        BlockRenderDispatcher dispatcher = mc.getBlockRenderer();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

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

            String dir = structureNBT.getString("facing");
            Direction areaFacing = Direction.byName(dir);;

            BlockState rotatedState = BuildArea.rotateBlockState(state, 4 - areaFacing.get2DDataValue());

            dispatcher.renderSingleBlock(
                    rotatedState,
                    poseStack,
                    bufferSource,
                    15728880,
                    OverlayTexture.NO_OVERLAY,
                    modelData,
                    renderType
            );

            poseStack.popPose();
        }

        poseStack.pushPose();
        poseStack.translate(areaX - 1, 0, 0);

        AABB originBox = new AABB(
                0, 0, 0,
                1, 2, 1
        ).inflate(0.01);

        LevelRenderer.renderLineBox(
                poseStack,
                bufferSource.getBuffer(RenderType.lines()),
                originBox,
                1.0f, 0.0f, 0.0f, 1.0f
        );
        poseStack.popPose();

        bufferSource.endBatch();
        poseStack.popPose();
    }

    public static @Nullable Direction getFacing(BlockState state) {
        for (Property<?> prop : state.getProperties()) {
            if (prop instanceof DirectionProperty dirProp) {
                return state.getValue(dirProp);
            }
        }
        return null;
    }

    public void onGlobalMouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 1 && isRightDragging) {
            offsetX += dragX / zoom;
            offsetY -= dragY / zoom;
        } else if (button == 0 && isDragging) {
            rotationY += dragX * 0.5f;
            rotationX += dragY * 0.5f;
            rotationX = Mth.clamp(rotationX, -90f, 90f);
        }
    }
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseWithinRenderArea(mouseX, mouseY)) {
            if (button == 0) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastClickTime < 250) {
                    resetView();
                    lastClickTime = 0;
                    return true;
                }
                lastClickTime = currentTime;

                isDragging = true;
            }
            if (button == 1) isRightDragging = true;
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) isDragging = false;
        if (button == 1) isRightDragging = false;
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        float dx = (float) (mouseX - lastMouseX);
        float dy = (float) (mouseY - lastMouseY);

        if (isDragging) {
            rotationY += dx * 0.5f;
            rotationX += dy * 0.5f;
            rotationX = Mth.clamp(rotationX, -90f, 90f);
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            return true;
        } else if (isRightDragging) {
            offsetX += dx / zoom;
            offsetY -= dy / zoom;
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            return true;
        }
        return false;
    }

    public boolean isMouseWithinRenderArea(double mouseX, double mouseY) {
        return mouseX >= getX() && mouseX < getX() + getWidth()
                && mouseY >= getY() && mouseY < getY() + getHeight();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput p_259858_) {

    }

    private void resetView() {
        rotationX = 30;
        rotationY = 45;
        offsetX = 0;
        offsetY = 0;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double x) {
        if (isHoveredOrFocused()) {
            zoom += x;
            zoom = Mth.clamp(zoom, 3f, 20f);
            return true;
        }
        return false;
    }
}
