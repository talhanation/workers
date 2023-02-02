package com.talhanation.workers.client.events;

import com.talhanation.workers.Main;
import com.talhanation.workers.init.ModShortcuts;
import com.talhanation.workers.network.MessageBed;
import com.talhanation.workers.network.MessageChest;
import com.talhanation.workers.network.MessageStartPos;

import de.maxhenkel.corelib.net.Message;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
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

        if (
            ModShortcuts.ASSIGN_WORKSPACE_KEY.isDown() ||
            ModShortcuts.ASSIGN_CHEST_KEY.isDown() ||
            ModShortcuts.ASSIGN_BED_KEY.isDown()
        ) {
            HitResult rayTraceResult = minecraft.hitResult;
            if (rayTraceResult != null) {
                if (rayTraceResult.getType() == HitResult.Type.BLOCK) {
                    BlockHitResult blockraytraceresult = (BlockHitResult) rayTraceResult;
                    BlockPos blockpos = blockraytraceresult.getBlockPos();
                    
                    Message<?> message;
                    if (ModShortcuts.ASSIGN_WORKSPACE_KEY.isDown()) {
                        message = new MessageStartPos(clientPlayerEntity.getUUID(), blockpos);
                    } else if (ModShortcuts.ASSIGN_CHEST_KEY.isDown()) {
                        message = new MessageChest(clientPlayerEntity.getUUID(), blockpos);
                    } else {
                        message = new MessageBed(clientPlayerEntity.getUUID(), blockpos);
                    }

                    Main.SIMPLE_CHANNEL.sendToServer(message);
                }
            }
        }
    }
}
