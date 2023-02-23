package com.talhanation.workers.inventory;

import com.talhanation.workers.init.ModMenuTypes;
import de.maxhenkel.corelib.inventory.ContainerBase;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;

public class CommandMenu extends ContainerBase {
    private Player playerEntity;

    public CommandMenu(int id, Player playerEntity) {
        super(ModMenuTypes.COMMAND_CONTAINER_TYPE.get(), id, null, new SimpleContainer(0));
        this.playerEntity = playerEntity;
    }

    public Player getPlayerEntity() {
        return playerEntity;
    }
}