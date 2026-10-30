package com.talhanation.workers.client.gui;

import com.talhanation.recruits.client.gui.widgets.BlackShowingTextField;
import com.talhanation.recruits.client.gui.widgets.RecruitsCheckBox;
import com.talhanation.workers.WorkersMain;
import com.talhanation.workers.entities.workarea.WorkshopArea;
import com.talhanation.workers.network.MessageUpdateWorkAreaName;
import com.talhanation.workers.network.MessageUpdateWorkshopArea;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.widget.ExtendedButton;

public class WorkshopAreaScreen extends WorkAreaScreen {

    private static final MutableComponent TEXT_CRAFTSMAN   = Component.translatable("entity.workers.craftsman");
    private static final MutableComponent TEXT_TABLE       = Component.translatable("gui.workers.text.craftingTable");
    private static final MutableComponent TEXT_FURNACES    = Component.translatable("gui.workers.text.furnaces");
    private static final MutableComponent TEXT_CONTAINERS  = Component.translatable("gui.workers.text.containers");
    private static final MutableComponent TEXT_SPACE       = Component.translatable("gui.workers.text.space");
    private static final MutableComponent TEXT_YES         = Component.translatable("gui.workers.text.yes");
    private static final MutableComponent TEXT_NO          = Component.translatable("gui.workers.text.no");
    private static final MutableComponent BUTTON_ORDERS    = Component.translatable("gui.workers.button.orders");
    private static final MutableComponent TEXT_NAME_HINT   = Component.translatable("entity.workers.workshop");
    private static final MutableComponent TEXT_USE_STORAGE    = Component.translatable("gui.workers.checkbox.useStorage");
    private static final MutableComponent TOOLTIP_USE_STORAGE = Component.translatable("gui.workers.checkbox.tooltip.useStorage");

    public final WorkshopArea workshopArea;
    private String craftsmanName;
    private int freeSlots;
    private int totalSlots;
    private EditBox nameEditBox;
    private String areaName;
    private boolean useStorage;

    public WorkshopAreaScreen(WorkshopArea workshopArea, Player player) {
        super(Component.translatable("entity.workers.workshop"), workshopArea, player);
        this.workshopArea = workshopArea;
        this.areaName     = workshopArea.getCustomName() != null ? workshopArea.getCustomName().getString() : "";
        this.useStorage   = workshopArea.getUseStorage();
    }

    @Override
    protected void init() {
        super.init();
        this.craftsmanName = workshopArea.getCraftsmanName();
        this.freeSlots     = workshopArea.getFreeSlots();
        this.totalSlots    = workshopArea.getTotalSlots();
        setButtons();
    }

    @Override
    public void setButtons() {
        super.setButtons();

        int w  = 120;
        int h  = 20;
        int bx = x - w / 2;
        int by = y + h / 2 + 42;

        nameEditBox = new EditBox(font, bx, by - h, w, h, Component.literal(""));
        nameEditBox.setValue(areaName);
        nameEditBox.setHint(TEXT_NAME_HINT);
        nameEditBox.setTextColor(-1);
        nameEditBox.setTextColorUneditable(-1);
        nameEditBox.setBordered(true);
        nameEditBox.setMaxLength(32);
        nameEditBox.setResponder(s -> this.areaName = s);
        addRenderableWidget(nameEditBox);

        addRenderableWidget(new BlackShowingTextField(bx, by + 10, w / 2, h,
                Component.literal(TEXT_CRAFTSMAN.getString() + ":")));
        addRenderableWidget(new BlackShowingTextField(bx + w / 2, by + 10, w / 2, h,
                Component.literal("" + this.craftsmanName)));

        addRenderableWidget(new BlackShowingTextField(bx, by + 30, w / 2, h,
                Component.literal(TEXT_TABLE.getString())));
        addRenderableWidget(new BlackShowingTextField(bx + w / 2, by + 30, w / 2, h,
                workshopArea.hasTable() ? TEXT_YES : TEXT_NO));

        addRenderableWidget(new BlackShowingTextField(bx, by + 50, w / 2, h,
                Component.literal(TEXT_FURNACES.getString())));
        addRenderableWidget(new BlackShowingTextField(bx + w / 2, by + 50, w / 2, h,
                Component.literal("" + workshopArea.getFurnaceCount())));

        addRenderableWidget(new BlackShowingTextField(bx, by + 70, w / 2, h,
                Component.literal(TEXT_CONTAINERS.getString())));
        addRenderableWidget(new BlackShowingTextField(bx + w / 2, by + 70, w / 2, h,
                Component.literal(workshopArea.getContainerCount() + " / " + WorkshopArea.MIN_CONTAINERS)));

        addRenderableWidget(new BlackShowingTextField(bx, by + 90, w / 2, h,
                Component.literal(TEXT_SPACE.getString() + ":")));
        addRenderableWidget(new BlackShowingTextField(bx + w / 2, by + 90, w / 2, h,
                Component.literal("" + this.freeSlots + " / " + this.totalSlots)));

        RecruitsCheckBox useStorageCheckBox = new RecruitsCheckBox(bx, by + 112, w, h, TEXT_USE_STORAGE, useStorage,
                bool -> {
                    this.useStorage = bool;
                    WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateWorkshopArea(workshopArea.getUUID(), useStorage));
                });
        useStorageCheckBox.setTooltip(Tooltip.create(TOOLTIP_USE_STORAGE));
        addRenderableWidget(useStorageCheckBox);

        addRenderableWidget(new ExtendedButton(bx, by + 135, w, h, BUTTON_ORDERS,
                button -> {
                    sendName();
                    minecraft.setScreen(new WorkshopOrderScreen(workshopArea, player));
                }));
    }

    @Override
    public void onClose() {
        super.onClose();
        sendName();
    }

    private void sendName() {
        String current = workArea.getCustomName() != null ? workArea.getCustomName().getString() : "";
        if (areaName.equals(current)) return;
        WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateWorkAreaName(workArea.getUUID(), areaName));
    }
}
