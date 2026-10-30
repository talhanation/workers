package com.talhanation.workers.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.talhanation.recruits.client.gui.RecruitsScreenBase;
import com.talhanation.workers.WorkersMain;
import com.talhanation.workers.client.gui.widgets.WorkersPanelList;
import com.talhanation.workers.entities.workarea.WorkshopArea;
import com.talhanation.workers.network.MessageUpdateWorkshopOrder;
import com.talhanation.workers.world.CraftOrder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.widget.ExtendedButton;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Order list of a workshop, in the design of the recruits faction list.
 * The craftsman works the orders in turn.
 */
@OnlyIn(Dist.CLIENT)
public class WorkshopOrderScreen extends RecruitsScreenBase {

    private static final ResourceLocation TEXTURE = new ResourceLocation("recruits", "textures/gui/select_player.png");
    private static final MutableComponent TITLE          = Component.translatable("gui.workers.workshop.orders");
    private static final MutableComponent BUTTON_ADD     = Component.translatable("gui.workers.button.add");
    private static final MutableComponent BUTTON_EDIT    = Component.translatable("gui.workers.button.edit");
    private static final MutableComponent BUTTON_REMOVE  = Component.translatable("gui.workers.button.remove");
    private static final MutableComponent BUTTON_TOGGLE  = Component.translatable("gui.workers.button.onOff");
    private static final MutableComponent BUTTON_BACK    = Component.translatable("gui.workers.button.back");
    private static final MutableComponent TOOLTIP_TOGGLE = Component.translatable("gui.workers.workshop.tooltip.onOff");
    private static final MutableComponent TOOLTIP_LOCKED = Component.translatable("gui.workers.workshop.tooltip.governorLocked");
    private static final MutableComponent TEXT_EMPTY     = Component.translatable("gui.workers.workshop.noOrders");
    private static final MutableComponent TOOLTIP_FULL   = Component.translatable("gui.workers.workshop.tooltip.maxOrders", WorkshopArea.MAX_ORDERS);

    protected static final int HEADER_SIZE = 16;
    protected static final int FOOTER_SIZE = 32;
    protected static final int UNIT_SIZE   = 18;
    protected static final int CELL_HEIGHT = 30;

    private final WorkshopArea workshopArea;
    private final Player player;
    private WorkersPanelList<OrderEntry> orderList;
    private CompoundTag lastOrdersTag;
    @Nullable private UUID selected;
    private int units;

    private ExtendedButton addButton;
    private ExtendedButton editButton;
    private ExtendedButton removeButton;
    private ExtendedButton toggleButton;

    public WorkshopOrderScreen(WorkshopArea workshopArea, Player player) {
        super(TITLE, 236, 0);
        this.workshopArea = workshopArea;
        this.player = player;
    }

    @Override
    protected void init() {
        super.init();
        int gapTop = (int) (this.height * 0.1);
        int gapBottom = (int) (this.height * 0.1);
        guiTop = gapTop;

        int minUnits = Mth.ceil((float) (CELL_HEIGHT * 2) / (float) UNIT_SIZE);
        units = Math.max(minUnits, (height - HEADER_SIZE - FOOTER_SIZE - gapTop - gapBottom) / UNIT_SIZE);

        int listTop = guiTop + HEADER_SIZE;
        int listHeight = units * UNIT_SIZE;
        this.orderList = new WorkersPanelList<>(guiLeft + 8, listTop, 210, listHeight, CELL_HEIGHT);
        addWidget(orderList);
        loadOrders();

        int buttonY = guiTop + HEADER_SIZE + 5 + units * UNIT_SIZE;
        int bw = 42;
        int bx = guiLeft + 8;

        addButton = new ExtendedButton(bx, buttonY, bw, 20, BUTTON_ADD,
                button -> workshopArea.openEditOrderGUI(player, new CraftOrder()));
        addRenderableWidget(addButton);

        editButton = new ExtendedButton(bx + (bw + 2), buttonY, bw, 20, BUTTON_EDIT,
                button -> {
                    CraftOrder order = getSelectedOrder();
                    if (order != null) workshopArea.openEditOrderGUI(player, order);
                });
        addRenderableWidget(editButton);

        removeButton = new ExtendedButton(bx + (bw + 2) * 2, buttonY, bw, 20, BUTTON_REMOVE,
                button -> {
                    CraftOrder order = getSelectedOrder();
                    if (order == null) return;
                    WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateWorkshopOrder(workshopArea.getUUID(), order, MessageUpdateWorkshopOrder.Action.REMOVE));
                    selected = null;
                    updateButtons();
                });
        addRenderableWidget(removeButton);

        toggleButton = new ExtendedButton(bx + (bw + 2) * 3, buttonY, bw, 20, BUTTON_TOGGLE,
                button -> {
                    CraftOrder order = getSelectedOrder();
                    if (order == null) return;
                    MessageUpdateWorkshopOrder.Action action = hasShiftDown() ? MessageUpdateWorkshopOrder.Action.RESET_PROGRESS : MessageUpdateWorkshopOrder.Action.TOGGLE_ENABLED;
                    WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateWorkshopOrder(workshopArea.getUUID(), order, action));
                });
        toggleButton.setTooltip(Tooltip.create(TOOLTIP_TOGGLE));
        addRenderableWidget(toggleButton);

        addRenderableWidget(new ExtendedButton(bx + (bw + 2) * 4, buttonY, bw, 20, BUTTON_BACK,
                button -> minecraft.setScreen(new WorkshopAreaScreen(workshopArea, player))));

        updateButtons();
    }

    private void loadOrders() {
        this.lastOrdersTag = workshopArea.getSyncedOrdersTag().copy();
        List<CraftOrder> orders = new ArrayList<>(workshopArea.getOrders());

        List<OrderEntry> entries = new ArrayList<>();
        for (CraftOrder order : orders) entries.add(new OrderEntry(this, order));
        orderList.setEntries(entries);

        if (selected != null && orders.stream().noneMatch(order -> order.uuid.equals(selected))) selected = null;
    }

    @Nullable
    private CraftOrder getSelectedOrder() {
        if (selected == null) return null;
        return workshopArea.getOrder(selected);
    }

    private void updateButtons() {
        boolean full = workshopArea.isOrderListFull();
        addButton.active = !full;
        addButton.setTooltip(full ? Tooltip.create(TOOLTIP_FULL) : null);

        CraftOrder order = getSelectedOrder();
        boolean locked = order != null && order.isGovernorOrder();
        editButton.active = order != null && !locked;
        removeButton.active = order != null && !locked;
        toggleButton.active = order != null;
        Tooltip lockedTooltip = locked ? Tooltip.create(TOOLTIP_LOCKED) : null;
        editButton.setTooltip(lockedTooltip);
        removeButton.setTooltip(lockedTooltip);
    }

    @Override
    public void tick() {
        super.tick();
        if (workshopArea.isRemoved()) {
            onClose();
            return;
        }
        if (!workshopArea.getSyncedOrdersTag().equals(lastOrdersTag)) {
            loadOrders();
            updateButtons();
        }
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        RenderSystem.setShaderTexture(0, TEXTURE);
        guiGraphics.blit(TEXTURE, guiLeft, guiTop, 0, 0, xSize, HEADER_SIZE);
        for (int i = 0; i < units; i++) {
            guiGraphics.blit(TEXTURE, guiLeft, guiTop + HEADER_SIZE + UNIT_SIZE * i, 0, HEADER_SIZE, xSize, UNIT_SIZE);
        }
        guiGraphics.blit(TEXTURE, guiLeft, guiTop + HEADER_SIZE + UNIT_SIZE * units, 0, HEADER_SIZE + UNIT_SIZE, xSize, FOOTER_SIZE);
    }

    @Override
    public void renderForeground(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        Component title = Component.literal(TITLE.getString() + " (" + orderList.children().size() + "/" + WorkshopArea.MAX_ORDERS + ")");
        guiGraphics.drawString(font, title, width / 2 - font.width(title) / 2, guiTop + 5, FONT_COLOR, false);

        if (orderList.isEmpty()) {
            guiGraphics.drawCenteredString(font, TEXT_EMPTY, width / 2, guiTop + HEADER_SIZE + (units * UNIT_SIZE) / 2 - font.lineHeight / 2, -1);
            return;
        }
        orderList.render(guiGraphics, mouseX, mouseY, delta);

        OrderEntry hovered = orderList.isMouseOver(mouseX, mouseY) ? orderList.getEntryAt(mouseX, mouseY) : null;
        if (hovered != null) {
            List<FormattedCharSequence> tooltip = hovered.getTooltip();
            if (!tooltip.isEmpty()) guiGraphics.renderTooltip(font, tooltip, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (orderList != null && orderList.isMouseOver(x, y)) {
            OrderEntry entry = orderList.getEntryAt(x, y);
            if (entry != null) {
                selected = entry.order.uuid;
                updateButtons();
            }
        }
        return super.mouseClicked(x, y, button);
    }

    // ── Entry ─────────────────────────────────────────────────────────────────

    @OnlyIn(Dist.CLIENT)
    public static class OrderEntry extends WorkersPanelList.Entry<OrderEntry> {
        private static final int PADDING = 4;
        private final WorkshopOrderScreen screen;
        private final CraftOrder order;

        public OrderEntry(WorkshopOrderScreen screen, CraftOrder order) {
            this.screen = screen;
            this.order = order;
        }

        @Override
        protected WorkersPanelList<OrderEntry> getList() {
            return screen.orderList;
        }

        @Override
        protected boolean isSelected() {
            return order.uuid.equals(screen.selected);
        }

        @Override
        protected void renderContent(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            Font font = Minecraft.getInstance().font;
            int itemX = left + PADDING;
            int itemY = top + (height - 16) / 2;
            guiGraphics.renderItem(order.result, itemX, itemY);
            guiGraphics.renderItemDecorations(font, order.result, itemX, itemY);

            int textX = itemX + 16 + PADDING;
            int textWidth = width - (textX - left) - PADDING;

            // line 1: item, type
            String type = Component.translatable(order.type == CraftOrder.OrderType.SMELTING ? "gui.workers.workshop.smelting" : "gui.workers.workshop.crafting").getString();
            String line1 = order.result.getHoverName().getString() + " (" + type + ")";
            int nameColor = order.enabled ? WorkersPanelList.TEXT_COLOR : WorkersPanelList.TEXT_GRAY;
            guiGraphics.drawString(font, font.plainSubstrByWidth(line1, textWidth), textX, top + 3, nameColor, false);

            // line 2: stock or progress, mode
            boolean stockMode = order.mode == CraftOrder.Mode.STOCK;
            String mode = Component.translatable(stockMode ? "gui.workers.workshop.stock" : "gui.workers.workshop.once").getString();
            String line2 = (stockMode ? order.stock : order.produced) + " / " + order.amount + "  " + mode;
            guiGraphics.drawString(font, font.plainSubstrByWidth(line2, textWidth), textX, top + 3 + font.lineHeight + 1, WorkersPanelList.TEXT_GRAY, false);

            // status right aligned
            Component status = getStatusText(order);
            int statusColor = getStatusColor(order);
            guiGraphics.drawString(font, status, left + width - PADDING - font.width(status), top + 3 + font.lineHeight + 1, statusColor, false);

            if (order.isGovernorOrder()) {
                guiGraphics.drawString(font, "\u2691", left + width - PADDING - font.width("\u2691"), top + 3, 0xFFFFAA00, false);
            }
        }

        public List<FormattedCharSequence> getTooltip() {
            List<FormattedCharSequence> lines = new ArrayList<>();
            if (order.status == CraftOrder.Status.MISSING_ITEMS && !order.missing.isEmpty()) {
                lines.add(Component.translatable("gui.workers.workshop.missing").withStyle(ChatFormatting.RED).getVisualOrderText());
                for (ItemStack stack : order.missing) {
                    lines.add(Component.literal(stack.getCount() + "x " + stack.getHoverName().getString()).getVisualOrderText());
                }
            }
            if (order.isGovernorOrder()) {
                lines.add(Component.translatable("gui.workers.workshop.governorOrder").withStyle(ChatFormatting.GOLD).getVisualOrderText());
            }
            return lines;
        }

        private static Component getStatusText(CraftOrder order) {
            return Component.translatable("gui.workers.workshop.status." + order.status.name().toLowerCase());
        }

        private static int getStatusColor(CraftOrder order) {
            return switch (order.status) {
                case OK -> WorkersPanelList.TEXT_GREEN;
                case DONE -> 0xFF55FFFF;
                case DISABLED -> WorkersPanelList.TEXT_GRAY;
                case STOCK_FULL, FETCHING -> 0xFFFFFF55;
                default -> WorkersPanelList.TEXT_RED;
            };
        }
    }
}
