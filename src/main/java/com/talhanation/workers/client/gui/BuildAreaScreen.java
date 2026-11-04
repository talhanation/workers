package com.talhanation.workers.client.gui;

import com.talhanation.recruits.client.gui.component.ActivateableButton;
import com.talhanation.recruits.client.gui.widgets.BlackShowingTextField;
import com.talhanation.recruits.client.gui.widgets.RecruitsCheckBox;
import com.talhanation.workers.Translatable;
import com.talhanation.workers.WorkersMain;
import com.talhanation.workers.client.gui.widgets.DisplayTextItemScrollDropDownMenu;
import com.talhanation.workers.client.gui.widgets.ScrollDropDownMenuWithFolders;
import com.talhanation.workers.world.ScannedBlock;
import com.talhanation.workers.client.gui.structureRenderer.StructurePreviewWidget;
import com.talhanation.workers.entities.workarea.BuildArea;
import com.talhanation.workers.client.WorkersClientManager;
import com.talhanation.workers.config.BuildMode;
import com.talhanation.workers.config.BuildPaymentMode;
import com.talhanation.workers.network.MessageRequestPresetContent;
import com.talhanation.workers.network.MessageRequestPresetList;
import com.talhanation.workers.network.MessageToClientPresetContent;
import com.talhanation.workers.network.MessageUpdateBuildArea;
import com.talhanation.workers.network.MessageUpdateBuildAreaFoundation;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.world.item.BlockItem;
import com.talhanation.workers.world.StructureManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.util.FastColor;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.gui.widget.ExtendedButton;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BuildAreaScreen extends WorkAreaScreen {

    private static final net.minecraft.network.chat.MutableComponent TEXT_ALWAYS_SHOW_PROJECTION = Component.translatable("gui.workers.checkbox.alwaysShowProjection");
    private static final net.minecraft.network.chat.MutableComponent TEXT_FOUNDATION = Component.translatable("gui.workers.checkbox.foundation");

    public final BuildArea buildArea;
    public Button scanButton;
    public Button buildButton;
    public Button placeButton;
    public StructurePreviewWidget structurePreview;
    public ScrollDropDownMenuWithFolders structureOptions;
    public CompoundTag structureNBT;
    public List<ScannedBlock> structure;
    public Mode mode;
    public EditBox scanNameEditBox;
    public Button modeScanButton;
    public Button modeLoadButton;
    public Button saveButton;
    public Button xSizePlusButton;
    public Button xSizeMinusButton;
    public Button ySizePlusButton;
    public Button ySizeMinusButton;
    public Button zSizePlusButton;
    public Button zSizeMinusButton;
    public RecruitsCheckBox alwaysShowProjectionCheckBox;
    public RecruitsCheckBox foundationCheckBox;
    public Button fillItemButton;
    public BuildStatusWidget statusWidget;
    public boolean foundation;
    public ItemStack fillItem = ItemStack.EMPTY;
    public String savedName;
    public int areaWidthSize;
    public int areaHeightSize;
    public int areaDepthSize;
    public boolean alwaysShowProjection;
    public List<ItemStack> requiredItems = new ArrayList<>();
    private boolean presetLoading = false;
    public DisplayTextItemScrollDropDownMenu requiredItemsDropDownMenu;

    // Layout: move buttons + owner (50 above y), below them the name row, the preview, the build row and the price.
    // The preview shrinks in small windows so everything stays on the screen.
    private static final int LAYOUT_ABOVE_Y = 50;
    private static final int LAYOUT_BELOW_Y = 30 + 6 + 22 + 2 + 20 + 20; // move buttons, gap, name row, gap, build row, price
    private static final int PREVIEW_WIDTH = 200;
    private static final int PREVIEW_MIN_HEIGHT = 60;
    private static final int PREVIEW_MAX_HEIGHT = 100;
    private int previewHeight = PREVIEW_MAX_HEIGHT;
    private int previewTop;

    public BuildAreaScreen(BuildArea buildArea, Player player) {
        super(buildArea.getCustomName(), buildArea, player);
        this.buildArea = buildArea;
    }

    @Override
    protected void init() {
        super.init();
        structureNBT = buildArea.getStructureNBT();
        if (structureNBT != null && !structureNBT.isEmpty()) {
            mode = Mode.LOAD;
            structure = StructureManager.parseStructureFromNBT(structureNBT);
            this.requiredItems = buildArea.getRequiredMaterials(structureNBT);
        }
        else mode = Mode.SCAN;

        this.areaWidthSize = buildArea.getWidthSize();
        this.areaHeightSize = buildArea.getHeightSize();
        this.areaDepthSize = buildArea.getDepthSize();
        this.alwaysShowProjection = buildArea.getAlwaysShowProjection();
        this.foundation = buildArea.getFoundation();
        this.fillItem = buildArea.getFillItem();

        if(WorkersClientManager.buildMode != BuildMode.FREE){
            WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageRequestPresetList());
        }

        setButtons();
    }

    @Override
    public void tick() {
        super.tick();
        if (scanNameEditBox != null && scanNameEditBox.isFocused()) scanNameEditBox.tick();
    }

    @Override
    protected int getLayoutY() {
        int available = this.height - 8;
        this.previewHeight = Mth.clamp(available - LAYOUT_ABOVE_Y - LAYOUT_BELOW_Y, PREVIEW_MIN_HEIGHT, PREVIEW_MAX_HEIGHT);
        int total = LAYOUT_ABOVE_Y + LAYOUT_BELOW_Y + previewHeight;
        // centered, but never above the top of the screen
        return Math.max(4, (this.height - total) / 2) + LAYOUT_ABOVE_Y;
    }

    @Override
    public void setButtons() {
        this.clearWidgets();
        super.setButtons();
        structureOptions = null;
        requiredItemsDropDownMenu = null;
        int buttonWidth = 100;
        int buttonHeight = 20;
        int previewWidth = PREVIEW_WIDTH;
        int boxHeight = 20;

        // left column: mode + size, middle: name/file row + preview + build row, right column: options
        int top = y + 36;
        int leftX = x - 201;
        int rightX = x + 101;
        int sizeRowY = top + 40;
        this.previewTop = top + 22;
        int buttonRowY = previewTop + previewHeight + 2;

        //MODE
        modeScanButton = addRenderableWidget(new ActivateableButton(leftX, top, buttonWidth, buttonHeight, Component.literal("Scan"),
                btn -> {
                    this.mode = Mode.SCAN;

                    this.resetScan();
                    this.setButtons();
                }
        ));
        modeScanButton.active = this.mode == Mode.SCAN;

        modeLoadButton = addRenderableWidget(new ActivateableButton(leftX, top + buttonHeight, buttonWidth, buttonHeight, Component.literal("Load"),
                btn -> {
                    this.mode = Mode.LOAD;

                    this.resetScan();
                    this.setButtons();
                }
        ));
        modeLoadButton.active = this.mode == Mode.LOAD;

        switch (mode) {
            case SCAN -> {
                xSizePlusButton = addRenderableWidget(new ExtendedButton(x - buttonWidth - 41, sizeRowY, 20, 20, Component.literal("+"),
                        btn -> {
                            if (hasShiftDown()) areaWidthSize += 5;
                            else areaWidthSize++;
                            areaWidthSize = Mth.clamp(areaWidthSize, 3, 32);

                            this.workArea.setWidthSize(areaWidthSize);
                            WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateBuildArea(this.workArea.getUUID(), areaWidthSize, areaHeightSize, areaDepthSize, structureNBT, false, false));

                            this.resetScan();
                            this.setButtons();
                        }
                ));

                xSizeMinusButton = addRenderableWidget(new ExtendedButton(x - buttonWidth - 21, sizeRowY, 20, 20, Component.literal("-"),
                        btn -> {
                            if (hasShiftDown()) areaWidthSize -= 5;
                            else areaWidthSize--;
                            areaWidthSize = Mth.clamp(areaWidthSize, 3, 32);

                            this.workArea.setWidthSize(areaWidthSize);
                            WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateBuildArea(this.workArea.getUUID(), areaWidthSize, areaHeightSize, areaDepthSize, structureNBT, false, false));

                            this.resetScan();
                            this.setButtons();
                        }
                ));

                ySizePlusButton = addRenderableWidget(new ExtendedButton(x - buttonWidth - 41, sizeRowY + 20, 20, 20, Component.literal("+"),
                        btn -> {
                            if (hasShiftDown()) areaHeightSize += 5;
                            else areaHeightSize++;
                            areaHeightSize = Mth.clamp(areaHeightSize, 3, 32);

                            this.workArea.setHeightSize(areaHeightSize);
                            WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateBuildArea(this.workArea.getUUID(), areaWidthSize, areaHeightSize, areaDepthSize, structureNBT, false, false));

                            this.resetScan();
                            this.setButtons();
                        }
                ));

                ySizeMinusButton = addRenderableWidget(new ExtendedButton(x - buttonWidth - 21, sizeRowY + 20, 20, 20, Component.literal("-"),
                        btn -> {
                            if (hasShiftDown()) areaHeightSize -= 5;
                            else areaHeightSize--;
                            areaHeightSize = Mth.clamp(areaHeightSize, 3, 32);

                            this.workArea.setHeightSize(areaHeightSize);
                            UUID uuid = this.buildArea.getUUID();
                            WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateBuildArea(uuid, areaWidthSize, areaHeightSize, areaDepthSize, structureNBT, false, false));
                            this.resetScan();
                            this.setButtons();
                        }
                ));

                zSizePlusButton = addRenderableWidget(new ExtendedButton(x - buttonWidth - 41, sizeRowY + 40, 20, 20, Component.literal("+"),
                        btn -> {
                            if (hasShiftDown()) areaDepthSize += 5;
                            else areaDepthSize++;
                            areaDepthSize = Mth.clamp(areaDepthSize, 3, 32);

                            this.workArea.setDepthSize(areaDepthSize);
                            WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateBuildArea(this.workArea.getUUID(), areaWidthSize, areaHeightSize, areaDepthSize, structureNBT, false, false));
                            this.resetScan();
                            this.setButtons();
                        }
                ));

                zSizeMinusButton = addRenderableWidget(new ExtendedButton(x - buttonWidth - 21, sizeRowY + 40, 20, 20, Component.literal("-"),
                        btn -> {
                            if (hasShiftDown()) areaDepthSize -= 5;
                            else areaDepthSize--;
                            areaDepthSize = Mth.clamp(areaDepthSize, 3, 32);

                            this.workArea.setDepthSize(areaDepthSize);
                            UUID uuid = this.buildArea.getUUID();
                            WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateBuildArea(uuid, areaWidthSize, areaHeightSize, areaDepthSize, structureNBT, false, false));
                            this.resetScan();
                            this.setButtons();
                        }
                ));

                scanNameEditBox = new EditBox(font, x - previewWidth / 2, top, previewWidth, boxHeight, Component.literal(""));
                scanNameEditBox.setValue(savedName != null ? savedName.toLowerCase(java.util.Locale.ROOT) : "");
                scanNameEditBox.setTextColor(-1);
                scanNameEditBox.setTextColorUneditable(-1);
                scanNameEditBox.setBordered(true);
                scanNameEditBox.setMaxLength(32);
                scanNameEditBox.setEditable(mode == Mode.SCAN);
                // Only allow lowercase letters, digits, underscores and slashes — no capitals
                scanNameEditBox.setFilter(s -> s.equals(s.toLowerCase(java.util.Locale.ROOT)));
                scanNameEditBox.setResponder(this::checkScanNameUpdate);
                this.addRenderableWidget(scanNameEditBox);

                scanButton = addRenderableWidget(new ExtendedButton(x - buttonWidth / 2, previewTop + previewHeight / 2 - buttonHeight / 2, buttonWidth, buttonHeight, Component.literal("Scan Area"),
                        btn -> {
                            this.performClientScan();
                            this.checkScanButtonActive();

                            checkSaveButtonActive(this.scanNameEditBox.getValue());
                        }
                ));

                saveButton = addRenderableWidget(new ExtendedButton(x - buttonWidth / 2, buttonRowY, buttonWidth, buttonHeight, Component.literal("Save"),
                        btn -> StructureManager.saveStructureToFile(this.scanNameEditBox.getValue(), this.structureNBT)
                ));
                saveButton.active = false;

                structurePreview = new StructurePreviewWidget(x - previewWidth / 2, previewTop, previewWidth, previewHeight, buildArea.getWidthSize(), buildArea.getDepthSize());
                addRenderableWidget(structurePreview);
                checkScanButtonActive();
            }

            case LOAD -> {
                BuildMode bm = WorkersClientManager.buildMode;

                if (bm == BuildMode.FREE) {
                    Path scanRoot = Path.of(Minecraft.getInstance().gameDirectory.getAbsolutePath(), "workers", "scan");

                    structureOptions = new ScrollDropDownMenuWithFolders(
                            x - previewWidth / 2 - 1,
                            previewTop - 21,
                            previewWidth + 2,
                            boxHeight + 2,
                            scanRoot,
                            selectedRelPath -> {
                                CompoundTag tag = StructureManager.loadScanNbt(selectedRelPath);
                                if (tag != null) {
                                    applyLoadedNbt(tag);
                                }
                            }
                    );

                }
                else {
                    int refreshW = 22;
                    addRenderableWidget(new ExtendedButton(
                            x - previewWidth / 2 - 24,
                            previewTop - 21,
                            refreshW, boxHeight + 2,
                            Component.literal("\u27f3"),
                            btn -> {
                                WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageRequestPresetList());
                                setButtons();
                            }
                    ));

                    // Dropdown — starts directly after refresh button
                    structureOptions = new ScrollDropDownMenuWithFolders(
                            x - previewWidth / 2 - 1,
                            previewTop - 21,
                            previewWidth + 2,
                            boxHeight + 2,
                            WorkersClientManager.serverBuildingPresetNames,
                            selectedName -> {
                                presetLoading = true;
                                MessageToClientPresetContent.pendingCallback = msg -> {
                                    presetLoading = false;
                                    applyLoadedNbt(msg.nbt);
                                };
                                WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageRequestPresetContent(selectedName));
                            }
                    );
                }

                buildButton = addRenderableWidget(new ExtendedButton(player.isCreative() ? x - buttonWidth : x - buttonWidth / 2, buttonRowY, buttonWidth, buttonHeight, Component.literal("Build"),
                        btn -> {
                            // never send an empty structure, it would overwrite the one on the server
                            if (this.structureNBT == null || this.structureNBT.isEmpty()) return;
                            WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateBuildArea(this.buildArea.getUUID(), areaWidthSize, areaHeightSize, areaDepthSize, this.structureNBT, true, false));
                        }
                ));

                if (player.isCreative()) {
                    placeButton = addRenderableWidget(new ExtendedButton(x, buttonRowY, buttonWidth, buttonHeight, Component.literal("Place"),
                            btn -> {
                                if (this.structureNBT == null || this.structureNBT.isEmpty()) return;
                                WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateBuildArea(this.buildArea.getUUID(), areaWidthSize, areaHeightSize, areaDepthSize, this.structureNBT, true, true));
                            }
                    ));
                }

                requiredItemsDropDownMenu = new DisplayTextItemScrollDropDownMenu(ItemStack.EMPTY, "Blocks", rightX, top, 110, boxHeight, requiredItems, null);
                requiredItemsDropDownMenu.setBgFillSelected(FastColor.ARGB32.color(255, 139, 139, 139));
                requiredItemsDropDownMenu.setCanSelectItem(false);
                requiredItemsDropDownMenu.setResetCount(false);

                structurePreview = new StructurePreviewWidget(x - previewWidth / 2, previewTop, previewWidth, previewHeight, buildArea.getWidthSize(), buildArea.getDepthSize());
                addRenderableWidget(structurePreview);
                if (structure != null) structurePreview.setStructure(this.structure, this.structureNBT);
                checkBuildButtonActive();
            }
        }

        int blackboxWidth = mode == Mode.SCAN ? 40 : 70;
        int blackboxWidth2 = mode == Mode.SCAN ? 20 : 30;
        int blackboxHeight = 20;
        int blackBoxPosX = leftX;
        addRenderableWidget(new BlackShowingTextField(blackBoxPosX, sizeRowY, blackboxWidth, blackboxHeight, Component.literal("Width:")));
        addRenderableWidget(new BlackShowingTextField(blackBoxPosX, sizeRowY + 20, blackboxWidth, blackboxHeight, Component.literal("Height:")));
        addRenderableWidget(new BlackShowingTextField(blackBoxPosX, sizeRowY + 40, blackboxWidth, blackboxHeight, Component.literal("Depth:")));
        addRenderableWidget(new BlackShowingTextField(blackBoxPosX + blackboxWidth, sizeRowY, blackboxWidth2, blackboxHeight, Component.literal("" + areaWidthSize)));
        addRenderableWidget(new BlackShowingTextField(blackBoxPosX + blackboxWidth, sizeRowY + 20, blackboxWidth2, blackboxHeight, Component.literal("" + areaHeightSize)));
        addRenderableWidget(new BlackShowingTextField(blackBoxPosX + blackboxWidth, sizeRowY + 40, blackboxWidth2, blackboxHeight, Component.literal("" + areaDepthSize)));

        // Options in the right column, next to the preview
        // Always-show-projection checkbox — keeps the structure preview visible
        // even when the player is not aiming the crosshair at the build area.
        this.alwaysShowProjectionCheckBox = new RecruitsCheckBox(rightX, previewTop, 100, 20, TEXT_ALWAYS_SHOW_PROJECTION,
                this.alwaysShowProjection,
                (bool) -> {
                    this.alwaysShowProjection = bool;
                    WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateBuildArea(this.buildArea.getUUID(), areaWidthSize, areaHeightSize, areaDepthSize, structureNBT, false, false, alwaysShowProjection));
                }
        );
        addRenderableWidget(alwaysShowProjectionCheckBox);

        // Foundation checkbox — the builder levels the ground: cuts hills and fills holes under the building
        this.foundationCheckBox = new RecruitsCheckBox(rightX, previewTop + 20, 100, 20, TEXT_FOUNDATION,
                this.foundation,
                (bool) -> {
                    this.foundation = bool;
                    WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateBuildAreaFoundation(this.buildArea.getUUID(), foundation, fillItem));
                    if (this.fillItemButton != null) this.fillItemButton.active = foundation;
                }
        );
        this.foundationCheckBox.setTooltip(Tooltip.create(Component.translatable("gui.workers.checkbox.tooltip.foundation")));
        addRenderableWidget(foundationCheckBox);

        // Fill item: every click the next block of the fixed palette (auto, dirt, sand, ... ice), shift = back
        this.fillItemButton = addRenderableWidget(new FillItemButton(rightX, previewTop + 42, 100, 20, btn -> {
                    this.fillItem = BuildArea.cycleFillItem(this.fillItem, hasShiftDown());
                    WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateBuildAreaFoundation(this.buildArea.getUUID(), foundation, fillItem));
                }));
        this.fillItemButton.setTooltip(Tooltip.create(Component.translatableWithFallback("gui.workers.button.tooltip.fillItemPalette",
                "Fill block of the foundation. Click: next block, Shift: back. Auto: taken from the ground.")));
        this.fillItemButton.active = foundation;

        // build status under the options: status, builder, progress
        this.statusWidget = addRenderableWidget(new BuildStatusWidget(rightX, previewTop + 66));

        // dropdowns are added last: opened they are drawn over the preview and the options below them
        if (structureOptions != null) addRenderableWidget(structureOptions);
        if (requiredItemsDropDownMenu != null) addRenderableWidget(requiredItemsDropDownMenu);
    }

    /**
     * The open "Blocks" list lies over the options in the right column. Those are hidden meanwhile,
     * so their tooltips do not show through the list and they can not be clicked through it.
     */
    private void updateOptionsUnderBlockList() {
        int listBottom = requiredItemsDropDownMenu != null && requiredItemsDropDownMenu.isOpen() ? requiredItemsDropDownMenu.getBottom() : Integer.MIN_VALUE;
        for (AbstractWidget widget : new AbstractWidget[]{alwaysShowProjectionCheckBox, foundationCheckBox, fillItemButton, statusWidget}) {
            if (widget != null) widget.visible = widget.getY() >= listBottom;
        }
    }

    private Component getFillItemText() {
        return fillItem.isEmpty()
                ? Component.translatable("gui.workers.button.fillItem.auto")
                : Component.translatable("gui.workers.button.fillItem", fillItem.getHoverName());
    }

    private void setStructure(List<ScannedBlock> structure, CompoundTag structureNBT) {
        this.requiredItems = buildArea.getRequiredMaterials(structureNBT);
        if (this.structurePreview != null) {
            this.structurePreview.setStructure(structure, structureNBT);
        }
        if (this.requiredItemsDropDownMenu != null) {
            this.requiredItemsDropDownMenu.setOptions(requiredItems);
        }
    }

    private void applyLoadedNbt(CompoundTag tag) {
        int width = tag.getInt("width");
        int height = tag.getInt("height");
        int depth = tag.getInt("depth");
        this.savedName = tag.getString("name");
        this.areaWidthSize = width;
        this.areaHeightSize = height;
        this.areaDepthSize = depth;

        this.buildArea.setWidthSize(width);
        this.buildArea.setHeightSize(height);
        this.buildArea.setDepthSize(depth);

        WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateBuildArea(this.buildArea.getUUID(), width, height, depth, tag, false, false));

        this.structureNBT = tag;
        this.structure = StructureManager.parseStructureFromNBT(tag);

        this.setStructure(this.structure, this.structureNBT);
        checkBuildButtonActive();
        this.setButtons();
    }

    public void resetScan() {
        structure = null;
        structureNBT = new CompoundTag();
        if (this.structurePreview != null) structurePreview.setStructure(null, null);
        this.requiredItems = new ArrayList<>();
    }


    private void checkScanNameUpdate(String s) {
        checkSaveButtonActive(s);
    }

    private void checkScanButtonActive() {
        if (this.scanButton == null) return;
        boolean active = this.structure == null;
        scanButton.active = active;
        scanButton.visible = active;
    }

    private void checkBuildButtonActive() {
        if (this.buildButton == null) return;

        this.buildButton.active = this.structure != null;
        if (placeButton != null) this.placeButton.active = this.buildButton.active;
    }

    private void checkSaveButtonActive(String s) {
        if (this.saveButton == null) return;

        this.saveButton.active = this.structure != null && s != null && s.length() >= 3;
    }

    private void performClientScan() {
        Level level = Minecraft.getInstance().level;
        if (level == null) return;

        this.structureNBT = StructureManager.scanStructure(level, this.buildArea, this.scanNameEditBox.getValue());
        this.structure = StructureManager.parseStructureFromNBT(structureNBT);
        this.structurePreview.setStructure(structure, structureNBT);
    }

    @Override
    public void mouseMoved(double x, double y) {
        if (structureOptions != null) {
            structureOptions.onMouseMove(x, y);
        }
        if (requiredItemsDropDownMenu != null) {
            requiredItemsDropDownMenu.onMouseMove(x, y);
        }
        super.mouseMoved(x, y);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (requiredItemsDropDownMenu != null && requiredItemsDropDownMenu.isMouseOver(mouseX, mouseY)) {
            this.requiredItemsDropDownMenu.onMouseClick(mouseX, mouseY);
            return true;
        }

        if (structureOptions != null && structureOptions.isMouseOver(mouseX, mouseY)) {
            // no resetScan here: only opening the dropdown would lose the selected file,
            // applyLoadedNbt replaces it once a new file is picked
            this.structureOptions.onMouseClick(mouseX, mouseY);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double d) {
        if (structureOptions != null && structureOptions.isMouseOver(x, y)) structureOptions.mouseScrolled(x, y, d);
        if (requiredItemsDropDownMenu != null && requiredItemsDropDownMenu.isMouseOver(x, y))
            requiredItemsDropDownMenu.mouseScrolled(x, y, d);
        return super.mouseScrolled(x, y, d);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        updateOptionsUnderBlockList();
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        if (presetLoading) {
            // Grey overlay on preview area
            guiGraphics.fill(x - PREVIEW_WIDTH / 2, previewTop,
                    x + PREVIEW_WIDTH / 2, previewTop + previewHeight,
                    0xAA222222);
            guiGraphics.drawCenteredString(font,
                    Component.literal("Loading..."),
                    x, previewTop + previewHeight / 2 - 4, 0xFFFFFF);
        }

        //CURRENCY payment mode: show the building price (full blueprint, synced from the server) under the build button
        if (mode == Mode.LOAD && structure != null && buildButton != null && WorkersClientManager.buildPaymentMode == BuildPaymentMode.CURRENCY) {
            int price = buildArea.getCurrencyPrice();
            int priceY = buildButton.getY() + buildButton.getHeight() + 4;
            if (price < 0) {
                guiGraphics.drawCenteredString(font, Translatable.TEXT_BUILDING_NOT_PURCHASABLE, x, priceY + 4, 0xFF5555);
            }
            else {
                Component priceText = Translatable.TEXT_BUILDING_PRICE(price);
                int startX = x - (18 + font.width(priceText)) / 2;
                guiGraphics.renderFakeItem(WorkersClientManager.currency, startX, priceY);
                guiGraphics.drawString(font, priceText, startX + 18, priceY + 4, 0xFFFFFF);
            }
        }
    }

    @Override
    public void onAreaMoved() {
        // A loaded file does not depend on the position or facing of the area, keep it.
        // Only a scan has to be redone, because it scanned the blocks at the old position.
        if (mode == Mode.LOAD) return;

        this.resetScan();
        checkScanButtonActive();
    }

    /** Fill item button: name on the left, the block itself on the right. */
    public class FillItemButton extends ExtendedButton {

        public FillItemButton(int x, int y, int width, int height, OnPress onPress) {
            super(x, y, width, height, Component.empty(), onPress);
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
            super.renderWidget(guiGraphics, mouseX, mouseY, partialTicks);

            String text = getFillItemText().getString();
            int color = this.active ? 0xFFFFFF : 0xA0A0A0;
            guiGraphics.drawString(font, font.plainSubstrByWidth(text, this.width - 26), getX() + 4, getY() + (this.height - 8) / 2, color);

            if (!fillItem.isEmpty()) {
                guiGraphics.renderFakeItem(fillItem, getX() + this.width - 19, getY() + (this.height - 16) / 2);
            }
        }
    }

    /** Status of the build (same data the Governor reads): status, builder and progress. Hover while waiting shows the materials. */
    public class BuildStatusWidget extends AbstractWidget {
        // same background as the BlackShowingTextField displays
        private static final int BG_FILL = FastColor.ARGB32.color(255, 10, 10, 10);

        public BuildStatusWidget(int x, int y) {
            super(x, y, 110, 32, Component.empty());
            this.active = false;
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
            BuildArea.BuildStatus status = buildArea.getBuildStatus();
            if (status == BuildArea.BuildStatus.NONE) return;

            guiGraphics.fill(getX(), getY(), getX() + this.width, getY() + this.height, BG_FILL);

            int textX = getX() + 2;
            int textWidth = this.width - 4;
            guiGraphics.drawString(font, font.plainSubstrByWidth(status.getDisplayName().getString(), textWidth), textX, getY() + 2, status.getColor(), false);

            String builder = buildArea.getBuilderName();
            if (!builder.isEmpty()) {
                guiGraphics.drawString(font, font.plainSubstrByWidth(builder, textWidth), textX, getY() + 12, 0xFFFFFF, false);
            }

            int total = buildArea.getTotalBlocks();
            if (total > 0) {
                String progress = buildArea.getPlacedBlocks() + " / " + total + " (" + Math.round(buildArea.getProgress() * 100) + "%)";
                guiGraphics.drawString(font, font.plainSubstrByWidth(progress, textWidth), textX, getY() + 22, 0xAAAAAA, false);
            }

            if (this.isHovered && status == BuildArea.BuildStatus.WAITING_FOR_MATERIALS) {
                List<FormattedCharSequence> lines = new ArrayList<>();
                for (ItemStack stack : buildArea.getMissingMaterials()) {
                    lines.add(stack.getHoverName().getVisualOrderText());
                }
                if (!lines.isEmpty()) BuildAreaScreen.this.setTooltipForNextRenderPass(lines);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
        }
    }

    public enum Mode {
        SCAN,
        LOAD
    }
}