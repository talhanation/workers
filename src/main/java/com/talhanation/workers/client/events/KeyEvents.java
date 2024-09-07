package com.talhanation.workers.client.events;

import com.talhanation.workers.CommandEvents;

import com.talhanation.workers.Main;
import com.talhanation.workers.entities.MerchantEntity;
import com.talhanation.workers.network.MessageWriteSpawnEgg;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@OnlyIn(Dist.CLIENT)
public class KeyEvents {

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer clientPlayerEntity = minecraft.player;
        if (clientPlayerEntity == null) return;

        if (Main.OPEN_COMMAND_SCREEN.isDown()) {
            CommandEvents.openCommandScreen(clientPlayerEntity);
        }
    }

    @SubscribeEvent
    public void onPlayerPick(InputEvent.ClickInputEvent event){
        if(event.isPickBlock()){
            Minecraft minecraft = Minecraft.getInstance();
            LocalPlayer clientPlayerEntity = minecraft.player;
            if (clientPlayerEntity == null || !clientPlayerEntity.isCreative())
                return;


            Entity target = ClientEvent.getEntityByLooking();
            if(target instanceof MerchantEntity recruitEntity){
                Main.SIMPLE_CHANNEL.sendToServer(new MessageWriteSpawnEgg(recruitEntity.getUUID()));
                event.setCanceled(true);
            }
        }
    }
}
