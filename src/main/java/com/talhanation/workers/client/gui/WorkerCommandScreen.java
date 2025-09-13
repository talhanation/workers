package com.talhanation.workers.client.gui;

import com.talhanation.recruits.client.gui.CommandScreen;
import com.talhanation.recruits.client.gui.commandscreen.ICommandCategory;
import com.talhanation.recruits.client.gui.group.RecruitsCommandButton;
import com.talhanation.recruits.client.gui.group.RecruitsGroup;
import com.talhanation.workers.Main;
import com.talhanation.workers.network.MessageAddDepositPos;
import com.talhanation.workers.network.MessageAddWorkArea;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class WorkerCommandScreen implements ICommandCategory {


    private static final MutableComponent TEXT_ADD_FIELD = Component.translatable("gui.workers.command.text.add_field");
    private static final MutableComponent TEXT_ADD_LUMBER = Component.translatable("gui.workers.command.text.add_lumber");
    private static final MutableComponent TEXT_ADD_MINE = Component.translatable("gui.workers.command.text.add_mine");
    private static final MutableComponent TOOLTIP_ADD_FIELD = Component.translatable("gui.workers.command.tooltip.add_field");
    private static final MutableComponent TOOLTIP_ADD_LUMBER = Component.translatable("gui.workers.command.tooltip.add_lumber");
    private static final MutableComponent TOOLTIP_ADD_MINE = Component.translatable("gui.workers.command.tooltip.add_mine");
    private static final MutableComponent TEXT_ADD_DEPOSIT = Component.translatable("gui.workers.command.text.add_deposit");
    private static final MutableComponent TEXT_ADD_BUILDING = Component.translatable("gui.workers.command.text.add_building");
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
                    Vec3 pos = screen.rayBlockPos.getCenter();
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageAddWorkArea((float) pos.x(), (int) pos.y(), (float) pos.z(), 0));
                });

        addCropFieldButton.setTooltip(Tooltip.create(TOOLTIP_ADD_FIELD));
        addCropFieldButton.active = screen.rayBlockPos != null;
        screen.addRenderableWidget(addCropFieldButton);

        RecruitsCommandButton addLumberArea = new RecruitsCommandButton(x, y + 0, TEXT_ADD_LUMBER,
                button -> {
                    if(screen.rayBlockPos == null) return;
                    Vec3 pos = screen.rayBlockPos.getCenter();
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageAddWorkArea((float) pos.x(), (int) pos.y(), (float) pos.z(), 1));
                });

        addLumberArea.setTooltip(Tooltip.create(TOOLTIP_ADD_LUMBER));
        addLumberArea.active = screen.rayBlockPos != null;
        screen.addRenderableWidget(addLumberArea);

        RecruitsCommandButton addMine = new RecruitsCommandButton(x + 100, y + 0, TEXT_ADD_MINE,
                button -> {
                    if(screen.rayBlockPos == null) return;
                    Vec3 pos = screen.rayBlockPos.getCenter();
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageAddWorkArea((float) pos.x(), (int) pos.y(), (float) pos.z(), 3));
                });

        addMine.setTooltip(Tooltip.create(TOOLTIP_ADD_MINE));
        addMine.active = screen.rayBlockPos != null;
        screen.addRenderableWidget(addMine);

        RecruitsCommandButton addDepositPosition = new RecruitsCommandButton(x, y - 150, TEXT_ADD_DEPOSIT,
            button -> {
                if(screen.rayBlockPos == null) return;
                if (!groups.isEmpty()) {
                    for (RecruitsGroup group : groups) {
                        if (!group.isDisabled() && screen.rayBlockPos != null)
                            Main.SIMPLE_CHANNEL.sendToServer(new MessageAddDepositPos(player.getUUID(), group.getId(), screen.rayBlockPos));
                    }
                }
            });

        addDepositPosition.setTooltip(Tooltip.create(TOOLTIP_ADD_FIELD));
        addDepositPosition.active = isOneGroupActive && isDepositPosition(screen.rayBlockPos, player);
        screen.addRenderableWidget(addDepositPosition);

        RecruitsCommandButton addBuilding = new RecruitsCommandButton(x, y + 30, TEXT_ADD_BUILDING,
                button -> {
                    if(screen.rayBlockPos == null) return;
                    Vec3 pos = screen.rayBlockPos.getCenter();
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageAddWorkArea((float) pos.x(), (int) pos.y(), (float) pos.z(), 2));
                });

        //addBuilding.setTooltip(Tooltip.create(TOOLTIP_ADD_LUMBER));
        addBuilding.active = screen.rayBlockPos != null;
        screen.addRenderableWidget(addBuilding);
    }

    private boolean isDepositPosition(BlockPos rayBlockPos, Player player) {
        if(rayBlockPos == null) return false;

        BlockEntity entity = player.getCommandSenderWorld().getBlockEntity(rayBlockPos);
        BlockState blockState = player.getCommandSenderWorld().getBlockState(rayBlockPos);

        return entity instanceof Container || blockState.getBlock() instanceof ChestBlock;
    }
}
