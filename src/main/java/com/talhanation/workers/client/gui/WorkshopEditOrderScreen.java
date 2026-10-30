package com.talhanation.workers.client.gui;

import com.talhanation.recruits.client.gui.widgets.BlackShowingTextField;
import com.talhanation.recruits.client.gui.widgets.RecruitsCheckBox;
import com.talhanation.workers.WorkersMain;
import com.talhanation.workers.client.gui.widgets.ItemSearchWidget;
import com.talhanation.workers.entities.workarea.WorkshopArea;
import com.talhanation.workers.inventory.WorkshopEditOrderContainer;
import com.talhanation.workers.network.MessageSetWorkshopRecipe;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.gui.widget.ExtendedButton;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private static final MutableComponent TEXT_VARIANTS     = Component.translatable("gui.workers.checkbox.allowVariants");
    private static final MutableComponent TEXT_ENABLED      = Component.translatable("gui.workers.checkbox.enabled");
    private static final MutableComponent TOOLTIP_TYPE      = Component.translatable("gui.workers.workshop.tooltip.type");
    private static final MutableComponent TOOLTIP_MODE      = Component.translatable("gui.workers.workshop.tooltip.mode");
    private static final MutableComponent TOOLTIP_VARIANTS  = Component.translatable("gui.workers.checkbox.tooltip.allowVariants");
    private static final MutableComponent TOOLTIP_AUTO_FUEL = Component.translatable("gui.workers.workshop.tooltip.autoFuel");
    private static final MutableComponent TOOLTIP_SEARCH_PICK = Component.translatable("gui.workers.search.tooltip.pickRecipe");
    private static final int fontColor = 4210752;
    private static final int PANEL_GAP = 6;
    private static final int PANEL_WIDTH = 110;

    private final WorkshopArea workshopArea;
    private final WorkshopEditOrderContainer orderContainer;
    private final Player player;
    private ExtendedButton saveButton;
    private ItemStack previewResult = ItemStack.EMPTY;
    @Nullable private ItemSearchWidget search;
    /** Recipes by result, one map per mode, built once when the screen opens. */
    private final Map<Item, List<Recipe<?>>> craftingRecipes = new HashMap<>();
    private final Map<Item, List<Recipe<?>>> smeltingRecipes = new HashMap<>();
    @Nullable private Item lastPicked;
    private int lastRecipeIndex;

    private boolean smeltMode;
    private int amount;
    private CraftOrder.Mode mode;
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
        // item search left, the settings panel right of the vanilla texture, center all together
        int searchWidth = ItemSearchWidget.WIDTH + PANEL_GAP;
        this.leftPos = (this.width - (searchWidth + imageWidth + PANEL_GAP + PANEL_WIDTH)) / 2 + searchWidth;

        if (craftingRecipes.isEmpty() && smeltingRecipes.isEmpty()) collectRecipes();
        int searchX = leftPos - searchWidth;
        if (search == null) search = new ItemSearchWidget(font, searchX, topPos + 4, item -> getRecipes().containsKey(item));
        else search.setPosition(searchX, topPos + 4);

        CraftOrder order = orderContainer.getOrder();
        this.smeltMode = orderContainer.smeltMode;
        this.amount = order.amount;
        this.mode = order.mode;
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
                    if (search != null) search.refresh(); // crafting and smelting have different results
                    this.lastPicked = null;
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

        RecruitsCheckBox variantsCheckBox = new RecruitsCheckBox(x, y + 72, w, h, TEXT_VARIANTS, allowVariants,
                bool -> this.allowVariants = bool);
        variantsCheckBox.setTooltip(Tooltip.create(TOOLTIP_VARIANTS));
        addRenderableWidget(variantsCheckBox);

        addRenderableWidget(new RecruitsCheckBox(x, y + 96, w, h, TEXT_ENABLED, enabled,
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

        if (search != null) addRenderableWidget(search.getBox());

        updatePreview();
    }

    private void collectRecipes() {
        Level level = player.getCommandSenderWorld();
        if (level == null) return;
        for (Recipe<?> recipe : level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) addRecipe(craftingRecipes, recipe, level);
        for (Recipe<?> recipe : level.getRecipeManager().getAllRecipesFor(RecipeType.SMELTING)) addRecipe(smeltingRecipes, recipe, level);
    }

    private static void addRecipe(Map<Item, List<Recipe<?>>> map, Recipe<?> recipe, Level level) {
        if (recipe.isSpecial()) return; // map cloning, fireworks, ... have no fixed ingredients
        ItemStack result = recipe.getResultItem(level.registryAccess());
        if (result.isEmpty()) return;
        map.computeIfAbsent(result.getItem(), item -> new ArrayList<>()).add(recipe);
    }

    private Map<Item, List<Recipe<?>>> getRecipes() {
        return smeltMode ? smeltingRecipes : craftingRecipes;
    }

    /** Click on a result fills the grid with its recipe, clicking the same item again shows the next recipe. */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        ItemStack picked = search != null ? search.getItemAt(mouseX, mouseY) : null;
        if (picked != null) {
            List<Recipe<?>> recipes = getRecipes().get(picked.getItem());
            if (recipes != null && !recipes.isEmpty()) {
                lastRecipeIndex = picked.getItem() == lastPicked ? (lastRecipeIndex + 1) % recipes.size() : 0;
                lastPicked = picked.getItem();
                WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageSetWorkshopRecipe(recipes.get(lastRecipeIndex).getId()));
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (search != null && search.mouseScrolled(mouseX, mouseY, delta)) return true;
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (search != null && search.keyPressed(keyCode, scanCode, modifiers)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void save() {
        CraftOrder order = orderContainer.getOrder().copy();
        order.type = smeltMode ? CraftOrder.OrderType.SMELTING : CraftOrder.OrderType.CRAFTING;
        ItemStack[] grid = orderContainer.getGrid();
        for (int i = 0; i < CraftOrder.GRID_SIZE; i++) order.grid[i] = grid[i];
        order.result = previewResult.copy();
        order.amount = amount;
        order.mode = mode;
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

        if (search != null) {
            search.render(guiGraphics, mouseX, mouseY);
            ItemStack hovered = search.getItemAt(mouseX, mouseY);
            List<Recipe<?>> recipes = hovered != null ? getRecipes().get(hovered.getItem()) : null;
            int count = recipes != null ? recipes.size() : 0;
            search.renderTooltip(guiGraphics, mouseX, mouseY, count > 1
                    ? Component.translatable("gui.workers.search.tooltip.pickRecipes", count)
                    : TOOLTIP_SEARCH_PICK);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, 8, 6, fontColor, false);
        guiGraphics.drawString(font, player.getInventory().getDisplayName().getVisualOrderText(), 8, this.imageHeight - 94, fontColor, false);
    }
}
