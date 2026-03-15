package com.talhanation.workers.client.gui;

import com.talhanation.recruits.client.gui.CommandScreen;
import com.talhanation.recruits.client.gui.commandscreen.ICommandCategory;
import com.talhanation.recruits.client.gui.group.RecruitsCommandButton;
import com.talhanation.recruits.world.RecruitsGroup;
import com.talhanation.workers.WorkersMain;
import com.talhanation.workers.client.WorkersClientManager;
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

import java.util.List;

public class WorkerCommandScreen implements ICommandCategory {

    private static final MutableComponent TEXT_ADD_FIELD = Component.translatable("gui.workers.command.text.add_field");
    private static final MutableComponent TEXT_ADD_LUMBER = Component.translatable("gui.workers.command.text.add_lumber");
    private static final MutableComponent TEXT_ADD_MINE = Component.translatable("gui.workers.command.text.add_mine");
    private static final MutableComponent TEXT_ADD_FISHING = Component.translatable("gui.workers.command.text.add_fishing");
    private static final MutableComponent TOOLTIP_ADD_FIELD = Component.translatable("gui.workers.command.tooltip.add_field");
    private static final MutableComponent TOOLTIP_ADD_LUMBER = Component.translatable("gui.workers.command.tooltip.add_lumber");
    private static final MutableComponent TOOLTIP_ADD_FISHING = Component.translatable("gui.workers.command.tooltip.add_fishing");
    private static final MutableComponent TOOLTIP_ADD_MINE = Component.translatable("gui.workers.command.tooltip.add_mine");
    private static final MutableComponent TOOLTIP_ADD_STORAGE = Component.translatable("gui.workers.command.tooltip.add_storage");
    private static final MutableComponent TEXT_ADD_BUILDING = Component.translatable("gui.workers.command.text.add_building");
    private static final MutableComponent TEXT_ADD_STORAGE = Component.translatable("gui.workers.command.text.add_storage");
    private static final MutableComponent TEXT_ADD_ANIMAL_PEN = Component.translatable("gui.workers.command.text.add_animal_pen");
    private static final MutableComponent TOOLTIP_ADD_ANIMAL_PEN = Component.translatable("gui.workers.command.tooltip.add_animal_pen");
    private static final MutableComponent TEXT_ADD_MARKET_AREA = Component.translatable("gui.workers.command.text.add_market");
    private static final MutableComponent TOOLTIP_ADD_MARKET_AREA = Component.translatable("gui.workers.command.tooltip.add_market");
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
        boolean canPlace = screen.rayBlockPos != null && WorkersClientManager.isInFactionClaim(screen.rayBlockPos);

        RecruitsCommandButton addCropFieldButton = new RecruitsCommandButton(x, y - 50, TEXT_ADD_FIELD,
                button -> {
                    if(screen.rayBlockPos == null) return;
                    BlockPos pos = screen.rayBlockPos;
                    WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageAddWorkArea(pos, 0));
                });

        addCropFieldButton.setTooltip(Tooltip.create(TOOLTIP_ADD_FIELD));
        addCropFieldButton.active = canPlace;
        screen.addRenderableWidget(addCropFieldButton);

        RecruitsCommandButton addFishingAreaButton = new RecruitsCommandButton(x - 100, y - 50, TEXT_ADD_FISHING,
                button -> {
                    if(screen.rayBlockPos == null) return;
                    BlockPos pos = screen.rayBlockPos;
                    WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageAddWorkArea(pos, 5));
                });

        addFishingAreaButton.setTooltip(Tooltip.create(TOOLTIP_ADD_FISHING));
        addFishingAreaButton.active = canPlace;
        screen.addRenderableWidget(addFishingAreaButton);

        RecruitsCommandButton addLumberArea = new RecruitsCommandButton(x, y + 0, TEXT_ADD_LUMBER,
                button -> {
                    if(screen.rayBlockPos == null) return;
                    BlockPos pos = screen.rayBlockPos;
                    WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageAddWorkArea(pos, 1));
                });

        addLumberArea.setTooltip(Tooltip.create(TOOLTIP_ADD_LUMBER));
        addLumberArea.active = canPlace;
        screen.addRenderableWidget(addLumberArea);

        RecruitsCommandButton addMine = new RecruitsCommandButton(x + 100, y + 0, TEXT_ADD_MINE,
                button -> {
                    if(screen.rayBlockPos == null) return;
                    BlockPos pos = screen.rayBlockPos;
                    WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageAddWorkArea(pos, 3));
                });

        addMine.setTooltip(Tooltip.create(TOOLTIP_ADD_MINE));
        addMine.active = canPlace;
        screen.addRenderableWidget(addMine);

        RecruitsCommandButton addStorageArea = new RecruitsCommandButton(x - 100, y - 0, TEXT_ADD_STORAGE,
            button -> {
                if(screen.rayBlockPos == null) return;
                BlockPos pos = screen.rayBlockPos;
                WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageAddWorkArea(pos, 4));
            });

        addStorageArea.setTooltip(Tooltip.create(TOOLTIP_ADD_STORAGE));
        addStorageArea.active = canPlace;
        screen.addRenderableWidget(addStorageArea);

        RecruitsCommandButton addBuilding = new RecruitsCommandButton(x, y + 30, TEXT_ADD_BUILDING,
                button -> {
                    if(screen.rayBlockPos == null) return;
                    BlockPos pos = screen.rayBlockPos;
                    WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageAddWorkArea(pos, 2));
                });

        addBuilding.active = canPlace;
        screen.addRenderableWidget(addBuilding);

        RecruitsCommandButton addAnimalPen = new RecruitsCommandButton(x + 100, y - 50, TEXT_ADD_ANIMAL_PEN,
                button -> {
                    if(screen.rayBlockPos == null) return;
                    BlockPos pos = screen.rayBlockPos;
                    WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageAddWorkArea(pos, 6));
                });

        addAnimalPen.setTooltip(Tooltip.create(TOOLTIP_ADD_ANIMAL_PEN));
        addAnimalPen.active = canPlace;
        screen.addRenderableWidget(addAnimalPen);

        RecruitsCommandButton addMarket = new RecruitsCommandButton(x + 130, y - 50, TEXT_ADD_MARKET_AREA,
                button -> {
                    if(screen.rayBlockPos == null) return;
                    BlockPos pos = screen.rayBlockPos;
                    WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageAddWorkArea(pos, 7));
                });

        addMarket.setTooltip(Tooltip.create(TOOLTIP_ADD_MARKET_AREA));
        addMarket.active = canPlace;
        screen.addRenderableWidget(addMarket);
    }

    private boolean isDepositPosition(BlockPos rayBlockPos, Player player) {
        if(rayBlockPos == null) return false;

        BlockEntity entity = player.getCommandSenderWorld().getBlockEntity(rayBlockPos);
        BlockState blockState = player.getCommandSenderWorld().getBlockState(rayBlockPos);

        return entity instanceof Container || blockState.getBlock() instanceof ChestBlock;
    }
}
