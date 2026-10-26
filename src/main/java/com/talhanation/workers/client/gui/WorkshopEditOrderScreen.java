package com.talhanation.workers.client.gui;

import com.talhanation.recruits.client.gui.widgets.BlackShowingTextField;
import com.talhanation.recruits.client.gui.widgets.RecruitsCheckBox;
import com.talhanation.workers.WorkersMain;
import com.talhanation.workers.entities.workarea.WorkshopArea;
import com.talhanation.workers.inventory.WorkshopEditOrderContainer;
import com.talhanation.workers.network.MessageUpdateWorkshopOrder;
import com.talhanation.workers.world.CraftOrder;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.client.gui.widget.ExtendedButton;

/**
 * Edit screen of a workshop order. Crafting shows the vanilla crafting table with a 3x3 ghost grid,
 * smelting shows the vanilla furnace with a single input slot. The result appears on the right.
 */
public class WorkshopEditOrderScreen extends ScreenBase<WorkshopEditOrderContainer> {

    private static final ResourceLocation CRAFTING_TEXTURE = new ResourceLocation("textures/gui/container/crafting_table.png");
    private static final ResourceLocation FURNACE_TEXTURE  = new ResourceLocation("textures/gui/container/furnace.png");
    private static final MutableComponent TITLE             = Component.translatable("gui.workers.workshop.editOrder");
    private static final MutableComponent BUTTON_SAVE       = Component.translatable("gui.workers.button.save");
    private static final MutableComponent BUTTON_CANCEL     = Component.translatable("gui.workers.button.cancel");
    private static final MutableComponent TEXT_CRAFTING     = Component.translatable("gui.workers.workshop.crafting");
    private static final MutableComponent TEXT_SMELTING     = Component.translatable("gui.workers.workshop.smelting");
    private static final MutableComponent TEXT_ONCE         = Component.translatable("gui.workers.workshop.once");
    private static final MutableComponent TEXT_STOCK        = Component.translatable("gui.workers.workshop.stock");
    private static final MutableComponent TEXT_AMOUNT       = Component.translatable("gui.workers.workshop.amount");
    private static final MutableComponent TEXT_PRIORITY     = Component.translatable("gui.workers.workshop.priority");
    private static final MutableComponent TEXT_VARIANTS     = Component.translatable("gui.workers.checkbox.allowVariants");
    private static final MutableComponent TEXT_ENABLED      = Component.translatable("gui.workers.checkbox.enabled");
    private static final MutableComponent TOOLTIP_TYPE      = Component.translatable("gui.workers.workshop.tooltip.type");
    private static final MutableComponent TOOLTIP_MODE      = Component.translatable("gui.workers.workshop.tooltip.mode");
    private static final MutableComponent TOOLTIP_PRIORITY  = Component.translatable("gui.workers.workshop.tooltip.priority");
    private static final MutableComponent TOOLTIP_VARIANTS  = Component.translatable("gui.workers.checkbox.tooltip.allowVariants");
    private static final MutableComponent TOOLTIP_AUTO_FUEL = Component.translatable("gui.workers.workshop.tooltip.autoFuel");
    private static final int fontColor = 4210752;
    private static final int PANEL_GAP = 6;
    private static final int PANEL_WIDTH = 110;

    private final WorkshopArea workshopArea;
    private final WorkshopEditOrderContainer orderContainer;
    private final Player player;
    private ExtendedButton saveButton;
    private ItemStack previewResult = ItemStack.EMPTY;

    private boolean smeltMode;
    private int amount;
    private CraftOrder.Mode mode;
    private int priority;
    private boolean allowVariants;
    private boolean enabled;

    public WorkshopEditOrderScreen(WorkshopEditOrderContainer orderContainer, Inventory playerInventory, Component title) {
        super(CRAFTING_TEXTURE, orderContainer, playerInventory, TITLE);
        this.orderContainer = orderContainer;
        this.workshopArea = orderContainer.getWorkshopArea();
        this.player = playerInventory.player;
        imageWidth = 176;
        imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        // the settings panel sits right of the vanilla texture, center both together
        this.leftPos = (this.width - (imageWidth + PANEL_GAP + PANEL_WIDTH)) / 2;

        CraftOrder order = orderContainer.getOrder();
        this.smeltMode = orderContainer.smeltMode;
        this.amount = order.amount;
        this.mode = order.mode;
        this.priority = order.priority;
        this.allowVariants = order.allowVariants;
        this.enabled = order.enabled;

        this.setWidgets();
    }

    public void setWidgets() {
        this.clearWidgets();

        int x = leftPos + imageWidth + PANEL_GAP;
        int y = topPos + 4;
        int w = PANEL_WIDTH;
        int h = 20;
        int valueWidth = w - 2 * h;

        ExtendedButton typeButton = new ExtendedButton(x, y, w, h, smeltMode ? TEXT_SMELTING : TEXT_CRAFTING,
                button -> {
                    this.smeltMode = !this.smeltMode;
                    this.orderContainer.smeltMode = this.smeltMode;
                    Minecraft.getInstance().gameMode.handleInventoryButtonClick(this.orderContainer.containerId, this.smeltMode ? 1 : 0);
                    this.updatePreview();
                    this.setWidgets();
                });
        typeButton.setTooltip(Tooltip.create(TOOLTIP_TYPE));
        addRenderableWidget(typeButton);

        // stock / once
        ExtendedButton modeButton = new ExtendedButton(x, y + 24, w, h, mode == CraftOrder.Mode.STOCK ? TEXT_STOCK : TEXT_ONCE,
                button -> {
                    mode = mode == CraftOrder.Mode.STOCK ? CraftOrder.Mode.ONCE : CraftOrder.Mode.STOCK;
                    this.setWidgets();
                });
        modeButton.setTooltip(Tooltip.create(TOOLTIP_MODE));
        addRenderableWidget(modeButton);

        // amount
        addRenderableWidget(new BlackShowingTextField(x, y + 48, valueWidth, h, Component.literal(TEXT_AMOUNT.getString() + " " + amount)));
        addRenderableWidget(new ExtendedButton(x + valueWidth, y + 48, h, h, Component.literal("-"),
                button -> {
                    amount = Mth.clamp(amount - (hasShiftDown() ? 16 : 1), 1, CraftOrder.MAX_AMOUNT);
                    this.setWidgets();
                }));
        addRenderableWidget(new ExtendedButton(x + valueWidth + h, y + 48, h, h, Component.literal("+"),
                button -> {
                    amount = Mth.clamp(amount + (hasShiftDown() ? 16 : 1), 1, CraftOrder.MAX_AMOUNT);
                    this.setWidgets();
                }));

        // priority (1 = first)
        BlackShowingTextField prioField = new BlackShowingTextField(x, y + 72, valueWidth, h,
                Component.literal(TEXT_PRIORITY.getString() + " " + priority));
        prioField.setTooltip(Tooltip.create(TOOLTIP_PRIORITY));
        addRenderableWidget(prioField);
        addRenderableWidget(new ExtendedButton(x + valueWidth, y + 72, h, h, Component.literal("-"),
                button -> {
                    priority = Mth.clamp(priority - 1, CraftOrder.MIN_PRIORITY, CraftOrder.MAX_PRIORITY);
                    this.setWidgets();
                }));
        addRenderableWidget(new ExtendedButton(x + valueWidth + h, y + 72, h, h, Component.literal("+"),
                button -> {
                    priority = Mth.clamp(priority + 1, CraftOrder.MIN_PRIORITY, CraftOrder.MAX_PRIORITY);
                    this.setWidgets();
                }));

        RecruitsCheckBox variantsCheckBox = new RecruitsCheckBox(x, y + 96, w, h, TEXT_VARIANTS, allowVariants,
                bool -> this.allowVariants = bool);
        variantsCheckBox.setTooltip(Tooltip.create(TOOLTIP_VARIANTS));
        addRenderableWidget(variantsCheckBox);

        addRenderableWidget(new RecruitsCheckBox(x, y + 120, w, h, TEXT_ENABLED, enabled,
                bool -> this.enabled = bool));

        // save / cancel below the texture
        int by = topPos + imageHeight + 4;
        saveButton = new ExtendedButton(leftPos, by, 86, h, BUTTON_SAVE,
                button -> {
                    save();
                    back();
                });
        addRenderableWidget(saveButton);

        addRenderableWidget(new ExtendedButton(leftPos + 90, by, 86, h, BUTTON_CANCEL,
                button -> back()));

        updatePreview();
    }

    private void save() {
        CraftOrder order = orderContainer.getOrder().copy();
        order.type = smeltMode ? CraftOrder.OrderType.SMELTING : CraftOrder.OrderType.CRAFTING;
        ItemStack[] grid = orderContainer.getGrid();
        for (int i = 0; i < CraftOrder.GRID_SIZE; i++) order.grid[i] = grid[i];
        order.result = previewResult.copy();
        order.amount = amount;
        order.mode = mode;
        order.priority = priority;
        order.allowVariants = allowVariants;
        order.enabled = enabled;
        WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateWorkshopOrder(workshopArea.getUUID(), order, MessageUpdateWorkshopOrder.Action.ADD_OR_UPDATE));
    }

    private void back() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.closeContainer();
        mc.setScreen(new WorkshopOrderScreen(workshopArea, player));
    }

    @Override
    public void onClose() {
        back();
    }

    private void updatePreview() {
        if (player.getCommandSenderWorld() == null) return;
        CraftOrder.OrderType type = smeltMode ? CraftOrder.OrderType.SMELTING : CraftOrder.OrderType.CRAFTING;
        this.previewResult = CraftOrder.previewResult(player.getCommandSenderWorld(), type, orderContainer.getGrid());
        if (saveButton != null) saveButton.active = !previewResult.isEmpty();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updatePreview();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        guiGraphics.blit(smeltMode ? FURNACE_TEXTURE : CRAFTING_TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        // result preview
        int resultX = leftPos + (smeltMode ? 116 : 124);
        int resultY = topPos + 35;
        if (!previewResult.isEmpty()) {
            guiGraphics.renderItem(previewResult, resultX, resultY);
            guiGraphics.renderItemDecorations(font, previewResult, resultX, resultY);
        }

        // smelting: fuel is taken automatically (coal, planks, logs)
        int fuelX = leftPos + 56;
        int fuelY = topPos + 53;
        if (smeltMode) {
            guiGraphics.renderFakeItem(new ItemStack(Items.COAL), fuelX, fuelY);
            guiGraphics.fill(fuelX, fuelY, fuelX + 16, fuelY + 16, 0x998B8B8B);
        }

        if (!previewResult.isEmpty() && isHovering(resultX - leftPos, resultY - topPos, 16, 16, mouseX, mouseY)) {
            guiGraphics.renderTooltip(font, previewResult, mouseX, mouseY);
        }
        else if (smeltMode && isHovering(fuelX - leftPos, fuelY - topPos, 16, 16, mouseX, mouseY)) {
            guiGraphics.renderTooltip(font, TOOLTIP_AUTO_FUEL, mouseX, mouseY);
        }
        else {
            this.renderTooltip(guiGraphics, mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, 8, 6, fontColor, false);
        guiGraphics.drawString(font, player.getInventory().getDisplayName().getVisualOrderText(), 8, this.imageHeight - 94, fontColor, false);
    }
}
