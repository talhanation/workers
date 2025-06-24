package com.talhanation.workers.client.gui;

import com.talhanation.recruits.client.gui.CommandScreen;
import com.talhanation.recruits.client.gui.commandscreen.ICommandCategory;
import com.talhanation.recruits.client.gui.group.RecruitsCommandButton;
import com.talhanation.recruits.client.gui.group.RecruitsGroup;
import com.talhanation.workers.Main;
import com.talhanation.workers.network.MessageAddWorkArea;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class WorkerCommandScreen implements ICommandCategory {

    private static final MutableComponent TOOLTIP_ADD_FIELD = Component.translatable("gui.workers.command.tooltip.add_field");
    private static final MutableComponent TEXT_ADD_FIELD = Component.translatable("gui.workers.command.text.add_field");
    private static final MutableComponent TEXT_ADD_DEPOSIT = Component.translatable("gui.workers.command.text.add_field");
    @Override
    public Component getToolTipName() {
        return Component.translatable("gui.workers.command.tooltip.workers");
    }

    @Override
    public ItemStack getIcon() {
        return new ItemStack(Items.IRON_PICKAXE);
    }

    @Override
    public void createButtons(CommandScreen screen, int x, int y, List<RecruitsGroup> groups, Player player) {
        boolean isOneGroupActive = groups.stream().anyMatch(g -> !g.isDisabled());

        RecruitsCommandButton addCropFieldButton = new RecruitsCommandButton(x, y - 50, TEXT_ADD_FIELD,
                button -> {
                    if(screen.rayBlockPos == null) return;
                    Vec3 pos = screen.rayBlockPos.above().getCenter();
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageAddWorkArea((float) pos.x(), (int) pos.y(), (float) pos.z(), 0));
                });

        addCropFieldButton.setTooltip(Tooltip.create(TOOLTIP_ADD_FIELD));
        addCropFieldButton.active = screen.rayBlockPos != null;
        screen.addRenderableWidget(addCropFieldButton);

        RecruitsCommandButton addDepositPosition = new RecruitsCommandButton(x, y - 150, TEXT_ADD_DEPOSIT,
            button -> {
                if(screen.rayBlockPos == null) return;
                Vec3 pos = screen.rayBlockPos.above().getCenter();
                Main.SIMPLE_CHANNEL.sendToServer(new MessageAddWorkArea((float) pos.x(), (int) pos.y(), (float) pos.z(), 0));
            });

        addCropFieldButton.setTooltip(Tooltip.create(TOOLTIP_ADD_FIELD));
        addCropFieldButton.active = screen.rayBlockPos != null;
        screen.addRenderableWidget(addCropFieldButton);
    }
}
