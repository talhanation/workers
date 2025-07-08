package com.talhanation.workers.client.events;

import com.talhanation.workers.client.gui.BuildAreaScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
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

}
