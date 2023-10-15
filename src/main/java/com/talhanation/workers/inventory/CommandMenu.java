package com.talhanation.workers.inventory;

import com.talhanation.workers.Main;
import de.maxhenkel.corelib.inventory.ContainerBase;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;

public class CommandMenu extends ContainerBase {
    private final Player playerEntity;

    public CommandMenu(int id, Player playerEntity) {
        super(Main.COMMAND_CONTAINER_TYPE, id, null, new SimpleContainer(0));
        this.playerEntity = playerEntity;
    }

    public Player getPlayerEntity() {
        return playerEntity;
    }
}