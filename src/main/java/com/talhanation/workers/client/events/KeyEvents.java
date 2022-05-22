package com.talhanation.workers.client.events;

import com.talhanation.workers.Main;
import com.talhanation.workers.network.MessageStartPos;
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
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer clientPlayerEntity = minecraft.player;
        if (clientPlayerEntity == null)
            return;

        if (Main.C_KEY.isDown()) {
            HitResult rayTraceResult = minecraft.hitResult;
            if (rayTraceResult != null) {
                if (rayTraceResult.getType() == HitResult.Type.BLOCK) {
                    BlockHitResult blockraytraceresult = (BlockHitResult) rayTraceResult;
                    BlockPos blockpos = blockraytraceresult.getBlockPos();
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageStartPos(clientPlayerEntity.getUUID(), blockpos));
                }
            }
        }
    }
}
