package com.talhanation.workers.network.compat;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

public final class WorkersPacketDistributor {
    public static final PlayerTarget PLAYER = new PlayerTarget();

    private WorkersPacketDistributor() {
    }

    public interface Target {
        void send(CustomPacketPayload payload);
    }

    public record PlayerPacketTarget(ServerPlayer player) implements Target {
        @Override
        public void send(CustomPacketPayload payload) {
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, payload);
        }
    }

    public static final class PlayerTarget {
        public Target with(Supplier<ServerPlayer> player) {
            return new PlayerPacketTarget(player.get());
        }
    }
}
