package com.talhanation.workers.init;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

public class ModShortcuts {


    public static KeyMapping COMMAND_SCREEN_KEY;
    /*
    public static KeyMapping ASSIGN_WORKSPACE_KEY = new KeyMapping(
        Component.translatable("controls.assign_workspace").getString(),
        InputConstants.Type.KEYSYM, 
        InputConstants.KEY_C, 
        "Workers"
    );
    public static KeyMapping ASSIGN_CHEST_KEY = new KeyMapping(
        Component.translatable("controls.assign_chest").getString(),
        InputConstants.Type.KEYSYM, 
        InputConstants.KEY_X, 
        "Workers"
        );
        public static KeyMapping ASSIGN_BED_KEY = new KeyMapping(
        Component.translatable("controls.assign_bed").getString(),
        InputConstants.Type.KEYSYM, 
        InputConstants.KEY_Z, 
        "Workers"
    );
        */

    public static KeyMapping OPEN_COMMAND_SCREEN = new KeyMapping(
            Component.translatable("controls.open_command_screen").getString(),
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_X,
            "Workers"
    );

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void registerBindings(RegisterKeyMappingsEvent event) {
        COMMAND_SCREEN_KEY = new KeyMapping("key.workers.open_command_screen", GLFW.GLFW_KEY_X, "Workers");


        //event.register(ASSIGN_WORKSPACE_KEY);
        //event.register(ASSIGN_CHEST_KEY);
        //event.register(ASSIGN_BED_KEY);
        event.register(OPEN_COMMAND_SCREEN);
    }
}
