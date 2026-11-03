package com.talhanation.workers.world.template;

import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Server lifecycle of the Governor helpers: template index on start, both indices cleared on stop. */
public class TemplateEvents {

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        TemplateIndex.onServerStarting(event.getServer());
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        TemplateIndex.onServerStopped();
        com.talhanation.workers.world.WorkAreaIndex.clearAll();
    }
}
