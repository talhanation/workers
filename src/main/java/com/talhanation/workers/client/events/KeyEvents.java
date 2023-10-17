package com.talhanation.workers.client.events;

import com.talhanation.workers.CommandEvents;
import com.talhanation.workers.init.ModShortcuts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;


@OnlyIn(Dist.CLIENT)
public class KeyEvents {

    @SubscribeEvent
    public void onKeyInput(InputEvent.Key event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer clientPlayerEntity = minecraft.player;
        if (clientPlayerEntity == null) return;

        if (ModShortcuts.OPEN_COMMAND_SCREEN.isDown()) {
            CommandEvents.openCommandScreen(clientPlayerEntity);
        }
    }
}
