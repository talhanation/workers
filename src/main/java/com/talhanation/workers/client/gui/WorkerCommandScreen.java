package com.talhanation.workers.client.gui;

import com.talhanation.recruits.client.gui.CommandScreen;
import com.talhanation.recruits.client.gui.commandscreen.ICommandCategory;
import com.talhanation.recruits.client.gui.group.RecruitsCommandButton;
import com.talhanation.recruits.world.RecruitsGroup;
import com.talhanation.workers.WorkAreaTypes;
import com.talhanation.workers.WorkersMain;
import com.talhanation.workers.client.WorkersClientManager;
import com.talhanation.workers.entities.workarea.BuildArea;
import com.talhanation.workers.network.MessageAddWorkArea;
import net.minecraft.client.Minecraft;
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
    private static final MutableComponent TOOLTIP_DISABLED_BY_SERVER = Component.translatable("gui.workers.command.tooltip.disabled_by_server");
    private static final MutableComponent TOOLTIP_NOT_IN_CLAIM = Component.translatable("gui.workers.command.tooltip.not_in_claim");
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

        x = x + 45;
        y = y + 10;
        final int colLeft   = x - 95;
        final int colRight  = x + 5;
        final int colCenter = x - 45; // centered across both columns

        final int rH = 22;
        final int r3 = y - 30;
        final int r2 = r3 - rH;
        final int r1 = r2 - rH;
        final int r4 = y + 10;
        final int r5 = r4 + rH;
        final int r6 = r5 + rH; // Building row

        // ── Left column (5 buttons) ───────────────────────────────────────────
        addButton(screen, colLeft, r1, TEXT_PLACE_FIELD,      TOOLTIP_PLACE_FIELD,      WorkAreaTypes.CROPAREA);
        addButton(screen, colLeft, r2, TEXT_PLACE_LUMBER,     TOOLTIP_PLACE_LUMBER,     WorkAreaTypes.LUMBER);
        addButton(screen, colLeft, r3, TEXT_PLACE_MINE,       TOOLTIP_PLACE_MINE,       WorkAreaTypes.MINING);
        addButton(screen, colLeft, r4, TEXT_PLACE_FISHING,    TOOLTIP_PLACE_FISHING,    WorkAreaTypes.FISHING);
        addButton(screen, colLeft, r5, TEXT_PLACE_ANIMAL_PEN, TOOLTIP_PLACE_ANIMAL_PEN, WorkAreaTypes.ANIMAL_PEN);

        // ── Right column (4 buttons) ──────────────────────────────────────────
        addButton(screen, colRight, r1, TEXT_PLACE_STORAGE,      TOOLTIP_PLACE_STORAGE,      WorkAreaTypes.STORAGE);
        addButton(screen, colRight, r2, TEXT_PLACE_MARKET_AREA,  TOOLTIP_PLACE_MARKET_AREA,  WorkAreaTypes.MARKET);
        addButton(screen, colRight, r3, TEXT_PLACE_KITCHEN_AREA, TOOLTIP_PLACE_KITCHEN_AREA, WorkAreaTypes.KITCHEN);
        addButton(screen, colRight, r4, TEXT_PLACE_HOME_AREA,    TOOLTIP_PLACE_HOME_AREA,    WorkAreaTypes.HOME);

        // ── Building centered below both columns ──────────────────────────────
        addButton(screen, colCenter, r6, TEXT_PLACE_BUILDING, null, WorkAreaTypes.BUILDING);
    }

    private void addButton(CommandScreen screen, int bx, int by,
                           MutableComponent label, MutableComponent tooltip,
                           WorkAreaTypes type) {
        RecruitsCommandButton btn = new RecruitsCommandButton(bx, by, label, button -> {
            if (screen.rayBlockPos == null) return;
            WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageAddWorkArea(screen.rayBlockPos, type));
        });


        btn.active = canPlace(screen, type);

        if(btn.active){
            if (tooltip != null) btn.setTooltip(Tooltip.create(tooltip));
        }
        else if(type != WorkAreaTypes.BUILDING && WorkersClientManager.configValueOnlyBuildings){
            btn.setTooltip(Tooltip.create(TOOLTIP_DISABLED_BY_SERVER));
        }
        else if(WorkersClientManager.configValueWorkAreaOnlyInFactionClaim){
            btn.setTooltip(Tooltip.create(TOOLTIP_NOT_IN_CLAIM));
        }

        screen.addRenderableWidget(btn);
    }

    private boolean canPlace(CommandScreen screen, WorkAreaTypes type) {
        if (screen.rayBlockPos == null) return false;
        if (!hasAirAbove(screen.rayBlockPos)) return false;
        return WorkersClientManager.isInFactionClaim(screen.rayBlockPos, type);
    }

    private boolean hasAirAbove(BlockPos pos) {
        var level = Minecraft.getInstance().level;
        return level != null && level.isEmptyBlock(pos.above());
    }

    private boolean isDepositPosition(BlockPos rayBlockPos, Player player) {
        if(rayBlockPos == null) return false;

        BlockEntity entity = player.getCommandSenderWorld().getBlockEntity(rayBlockPos);
        BlockState blockState = player.getCommandSenderWorld().getBlockState(rayBlockPos);

        return entity instanceof Container || blockState.getBlock() instanceof ChestBlock;
    }
}