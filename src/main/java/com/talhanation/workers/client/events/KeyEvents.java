package com.talhanation.workers.client.events;

import com.talhanation.workers.CommandEvents;
import com.talhanation.workers.Main;
import com.talhanation.workers.entities.AbstractWorkerEntity;
import com.talhanation.workers.init.ModShortcuts;
import com.talhanation.workers.network.MessageBedPos;
import com.talhanation.workers.network.MessageChestPos;
import com.talhanation.workers.network.MessageStartPos;

import com.talhanation.workers.network.MessageToClientUpdateCommandScreen;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

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
