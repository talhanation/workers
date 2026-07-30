package com.talhanation.workers.network.compat;

import de.maxhenkel.corelib.net.Message;

public final class WorkersChannel {
    public void sendToServer(Message<?> message) {
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(message);
    }

    public void send(WorkersPacketDistributor.Target target, Message<?> message) {
        target.send(message);
    }
}
