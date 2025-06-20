package com.talhanation.workers.client.gui;

import com.talhanation.recruits.client.gui.RecruitsScreenBase;
import com.talhanation.workers.Main;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class WorkAreaScreen extends RecruitsScreenBase {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Main.MOD_ID, "textures/gui/workareascreen.png");

    private EditBox textFieldName;
    private Button moveForward;
    private Button moveBackward;
    private Button moveLeft;
    private Button moveRight;
    private Button destroy;
    protected WorkAreaScreen(Component title, int xSize, int ySize) {
        super(title, xSize, ySize);
    }


}
