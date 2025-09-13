package com.talhanation.workers.client.events;

import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.workers.client.gui.BuildAreaScreen;
import com.talhanation.workers.entities.workarea.AbstractWorkAreaEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@OnlyIn(Dist.CLIENT)
public class ScreenEvents {

    @SubscribeEvent
    public void onMouseDragged(ScreenEvent.MouseDragged.Pre event) {
        Screen screen = event.getScreen();
        if (!(screen instanceof BuildAreaScreen buildAreaScreen)) return;

        double mouseX = event.getMouseX();
        double mouseY = event.getMouseY();
        int button = event.getMouseButton();
        double dragX = event.getDragX();
        double dragY = event.getDragY();

        if (buildAreaScreen.structurePreview != null && buildAreaScreen.structurePreview.isMouseWithinRenderArea(mouseX, mouseY)) {
            buildAreaScreen.structurePreview.onGlobalMouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
    }

    @SubscribeEvent
    public void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !mc.options.renderDebug) return; // nur im F3 Modus

        PoseStack poseStack = event.getPoseStack();
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.getPosition();

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        for (AbstractWorkAreaEntity workArea : mc.level.getEntitiesOfClass(AbstractWorkAreaEntity.class, mc.player.getBoundingBox().inflate(100))) {
            AABB area = workArea.getArea();
            AABB worldBox = new AABB(
                    area.minX, area.minY, area.minZ,
                    area.maxX + 1, area.maxY, area.maxZ + 1
            );

            AABB shifted = worldBox.move(-camPos.x, -camPos.y, -camPos.z);

            LevelRenderer.renderLineBox(
                    poseStack,
                    bufferSource.getBuffer(RenderType.lines()),
                    shifted,
                    1.0F, 1.0F, 1.0F, 1.0F
            );
        }

        bufferSource.endBatch(RenderType.lines());
    }


}
