package com.talhanation.workers.world;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VillagerInviteRegistry {

    private static final Map<UUID, UUID> INVITES = new ConcurrentHashMap<>();

    public static boolean tryInvite(UUID villagerUUID, UUID merchantUUID) {
        return INVITES.putIfAbsent(villagerUUID, merchantUUID) == null;
    }

    @Nullable
    public static UUID getInvitedBy(UUID villagerUUID) {
        return INVITES.get(villagerUUID);
    }

    public static boolean isInvited(UUID villagerUUID) {
        return INVITES.containsKey(villagerUUID);
    }

    public static void release(UUID villagerUUID) {
        INVITES.remove(villagerUUID);
    }

    public static void clear() {
        INVITES.clear();
    }
}
