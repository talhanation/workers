package com.talhanation.workers.client.gui;

import com.talhanation.recruits.client.gui.CommandScreen;
import com.talhanation.recruits.client.gui.commandscreen.ICommandCategory;
import com.talhanation.recruits.client.gui.group.RecruitsCommandButton;
import com.talhanation.recruits.world.RecruitsGroup;
import com.talhanation.workers.WorkAreaTypes;
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
    private static final MutableComponent TEXT_PLACE_FIELD = Component.translatable("gui.workers.command.text.place_field");
    private static final MutableComponent TEXT_PLACE_LUMBER = Component.translatable("gui.workers.command.text.place_lumber");
    private static final MutableComponent TEXT_PLACE_MINE = Component.translatable("gui.workers.command.text.place_mine");
    private static final MutableComponent TEXT_PLACE_FISHING = Component.translatable("gui.workers.command.text.place_fishing");
    private static final MutableComponent TOOLTIP_PLACE_FIELD = Component.translatable("gui.workers.command.tooltip.place_field");
    private static final MutableComponent TOOLTIP_PLACE_LUMBER = Component.translatable("gui.workers.command.tooltip.place_lumber");
    private static final MutableComponent TOOLTIP_PLACE_FISHING = Component.translatable("gui.workers.command.tooltip.place_fishing");
    private static final MutableComponent TOOLTIP_PLACE_MINE = Component.translatable("gui.workers.command.tooltip.place_mine");
    private static final MutableComponent TOOLTIP_PLACE_STORAGE = Component.translatable("gui.workers.command.tooltip.place_storage");
    private static final MutableComponent TEXT_PLACE_BUILDING = Component.translatable("gui.workers.command.text.place_building");
    private static final MutableComponent TEXT_PLACE_STORAGE = Component.translatable("gui.workers.command.text.place_storage");
    private static final MutableComponent TEXT_PLACE_ANIMAL_PEN = Component.translatable("gui.workers.command.text.place_animal_pen");
    private static final MutableComponent TOOLTIP_PLACE_ANIMAL_PEN = Component.translatable("gui.workers.command.tooltip.place_animal_pen");
    private static final MutableComponent TEXT_PLACE_MARKET_AREA = Component.translatable("gui.workers.command.text.place_market");
    private static final MutableComponent TOOLTIP_PLACE_MARKET_AREA = Component.translatable("gui.workers.command.tooltip.place_market");
    private static final MutableComponent TEXT_PLACE_HOME_AREA = Component.translatable("gui.workers.command.text.place_home");
    private static final MutableComponent TOOLTIP_PLACE_HOME_AREA = Component.translatable("gui.workers.command.tooltip.place_home");
    private static final MutableComponent TEXT_PLACE_KITCHEN_AREA = Component.translatable("gui.workers.command.text.place_kitchen");
    private static final MutableComponent TOOLTIP_PLACE_KITCHEN_AREA = Component.translatable("gui.workers.command.tooltip.place_kitchen");
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

        RecruitsCommandButton addLumberArea = new RecruitsCommandButton(x + 100, y, TEXT_PLACE_LUMBER,
                button -> {
                    if(screen.rayBlockPos == null) return;
                    BlockPos pos = screen.rayBlockPos;
                    WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageAddWorkArea(pos, WorkAreaTypes.LUMBER));
                });

        addLumberArea.setTooltip(Tooltip.create(TOOLTIP_PLACE_LUMBER));
        addLumberArea.active = canPlace(screen, WorkAreaTypes.LUMBER);
        screen.addRenderableWidget(addLumberArea);

        RecruitsCommandButton addMine = new RecruitsCommandButton(x - 100, y, TEXT_PLACE_MINE,
                button -> {
                    if(screen.rayBlockPos == null) return;
                    BlockPos pos = screen.rayBlockPos;
                    WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageAddWorkArea(pos, WorkAreaTypes.MINING));
                });

        addMine.setTooltip(Tooltip.create(TOOLTIP_PLACE_MINE));
        addMine.active = canPlace(screen, WorkAreaTypes.MINING);
        screen.addRenderableWidget(addMine);

        y -= 50;
        RecruitsCommandButton addCropFieldButton = new RecruitsCommandButton(x, y, TEXT_PLACE_FIELD,
                button -> {
                    if(screen.rayBlockPos == null) return;
                    BlockPos pos = screen.rayBlockPos;
                    WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageAddWorkArea(pos, WorkAreaTypes.CROPAREA));
                });

        addCropFieldButton.setTooltip(Tooltip.create(TOOLTIP_PLACE_FIELD));
        addCropFieldButton.active = canPlace(screen, WorkAreaTypes.CROPAREA);
        screen.addRenderableWidget(addCropFieldButton);

        RecruitsCommandButton addFishingAreaButton = new RecruitsCommandButton(x - 100, y, TEXT_PLACE_FISHING,
                button -> {
                    if(screen.rayBlockPos == null) return;
                    BlockPos pos = screen.rayBlockPos;
                    WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageAddWorkArea(pos, WorkAreaTypes.FISHING));
                });

        addFishingAreaButton.setTooltip(Tooltip.create(TOOLTIP_PLACE_FISHING));
        addFishingAreaButton.active = canPlace(screen, WorkAreaTypes.FISHING);
        screen.addRenderableWidget(addFishingAreaButton);

        RecruitsCommandButton addAnimalPen = new RecruitsCommandButton(x + 100, y, TEXT_PLACE_ANIMAL_PEN,
                button -> {
                    if(screen.rayBlockPos == null) return;
                    BlockPos pos = screen.rayBlockPos;
                    WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageAddWorkArea(pos, WorkAreaTypes.ANIMAL_PEN));
                });

        addAnimalPen.setTooltip(Tooltip.create(TOOLTIP_PLACE_ANIMAL_PEN));
        addAnimalPen.active = canPlace(screen, WorkAreaTypes.ANIMAL_PEN);
        screen.addRenderableWidget(addAnimalPen);

        y += 100;
        RecruitsCommandButton addStorageArea = new RecruitsCommandButton(x - 100, y, TEXT_PLACE_STORAGE,
            button -> {
                if(screen.rayBlockPos == null) return;
                BlockPos pos = screen.rayBlockPos;
                WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageAddWorkArea(pos, WorkAreaTypes.STORAGE));
            });

        addStorageArea.setTooltip(Tooltip.create(TOOLTIP_PLACE_STORAGE));
        addStorageArea.active = canPlace(screen, WorkAreaTypes.STORAGE);
        screen.addRenderableWidget(addStorageArea);

        RecruitsCommandButton addBuilding = new RecruitsCommandButton(x, y, TEXT_PLACE_BUILDING,
                button -> {
                    if(screen.rayBlockPos == null) return;
                    BlockPos pos = screen.rayBlockPos;
                    WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageAddWorkArea(pos, WorkAreaTypes.BUILDING));
                });

        addBuilding.active = canPlace(screen, WorkAreaTypes.BUILDING);
        screen.addRenderableWidget(addBuilding);

        RecruitsCommandButton addMarket = new RecruitsCommandButton(x + 100, y, TEXT_PLACE_MARKET_AREA,
                button -> {
                    if(screen.rayBlockPos == null) return;
                    BlockPos pos = screen.rayBlockPos;
                    WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageAddWorkArea(pos, WorkAreaTypes.MARKET));
                });

        addMarket.setTooltip(Tooltip.create(TOOLTIP_PLACE_MARKET_AREA));
        addMarket.active = canPlace(screen, WorkAreaTypes.MARKET);
        screen.addRenderableWidget(addMarket);

        RecruitsCommandButton addhome = new RecruitsCommandButton(x + 130, y, TEXT_PLACE_HOME_AREA,
                button -> {
                    if(screen.rayBlockPos == null) return;
                    BlockPos pos = screen.rayBlockPos;
                    WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageAddWorkArea(pos, WorkAreaTypes.HOME));
                });

        addhome.setTooltip(Tooltip.create(TOOLTIP_PLACE_HOME_AREA));
        addhome.active = canPlace(screen, WorkAreaTypes.HOME);
        screen.addRenderableWidget(addhome);

        RecruitsCommandButton addKitchen = new RecruitsCommandButton(x + 170, y, TEXT_PLACE_KITCHEN_AREA,
                button -> {
                    if(screen.rayBlockPos == null) return;
                    BlockPos pos = screen.rayBlockPos;
                    WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageAddWorkArea(pos, WorkAreaTypes.KITCHEN));
                });

        addKitchen.setTooltip(Tooltip.create(TOOLTIP_PLACE_KITCHEN_AREA));
        addKitchen.active = canPlace(screen, WorkAreaTypes.KITCHEN);
        screen.addRenderableWidget(addKitchen);
    }

    private boolean canPlace(CommandScreen screen, WorkAreaTypes type){
        return screen.rayBlockPos != null && WorkersClientManager.isInFactionClaim(screen.rayBlockPos, type);
    }

    private boolean isDepositPosition(BlockPos rayBlockPos, Player player) {
        if(rayBlockPos == null) return false;

        BlockEntity entity = player.getCommandSenderWorld().getBlockEntity(rayBlockPos);
        BlockState blockState = player.getCommandSenderWorld().getBlockState(rayBlockPos);

        return entity instanceof Container || blockState.getBlock() instanceof ChestBlock;
    }
}
