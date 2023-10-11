package com.talhanation.workers;


import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.VersionChecker;

public class UpdateChecker {

    @SubscribeEvent
    public void onPlayerJoinWorld(PlayerEvent.PlayerLoggedInEvent event){
        VersionChecker.Status status = VersionChecker.getResult((ModList.get().getModContainerById("workers").get()).getModInfo()).status();

        switch (status){
            case OUTDATED -> {
                Player player = event.getEntity();
                if(player != null){
                    player.sendSystemMessage(Component.literal("A new version of Villager Workers is available!").withStyle(ChatFormatting.GOLD));

                    MutableComponent link = Component.literal("Download the update " + ChatFormatting.BLUE + "here").withStyle(ChatFormatting.GREEN);
                    link.withStyle(link.getStyle().withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://www.curseforge.com/minecraft/mc-mods/workers/files")));
                    player.sendSystemMessage(link);
                }
                else{
                    Main.LOGGER.warn("Villager workers is outdated!");
                }
            }

            case FAILED -> {
                Main.LOGGER.error("Villager workers could not check for updates!");
            }
        }
    }
}
