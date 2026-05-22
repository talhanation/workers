package com.talhanation.workers.client.gui;

import com.talhanation.recruits.client.gui.component.ActivateableButton;
import com.talhanation.recruits.client.gui.widgets.BlackShowingTextField;
import com.talhanation.recruits.client.gui.widgets.RecruitsCheckBox;
import com.talhanation.workers.WorkersMain;
import com.talhanation.workers.client.gui.widgets.DisplayTextItemScrollDropDownMenu;
import com.talhanation.workers.client.gui.widgets.ScrollDropDownMenuWithFolders;
import com.talhanation.workers.world.ScannedBlock;
import com.talhanation.workers.client.gui.structureRenderer.StructurePreviewWidget;
import com.talhanation.workers.entities.workarea.BuildArea;
import com.talhanation.workers.client.WorkersClientManager;
import com.talhanation.workers.config.BuildMode;
import com.talhanation.workers.network.MessageRequestPresetContent;
import com.talhanation.workers.network.MessageRequestPresetList;
import com.talhanation.workers.network.MessageToClientPresetContent;
import com.talhanation.workers.network.MessageUpdateBuildArea;
import com.talhanation.workers.world.StructureManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor;
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

    private static final net.minecraft.network.chat.MutableComponent TEXT_FREE_AREA = Component.translatable("gui.workers.checkbox.freeArea");

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
    public RecruitsCheckBox freeAreaCheckBox;
    public String savedName;
    public int areaWidthSize;
    public int areaHeightSize;
    public int areaDepthSize;
    public boolean freeArea;
    public List<ItemStack> requiredItems = new ArrayList<>();
    private boolean presetLoading = false;
    public DisplayTextItemScrollDropDownMenu requiredItemsDropDownMenu;

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
        this.freeArea = buildArea.getFreeArea();

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
    public void setButtons() {
        this.clearWidgets();
        super.setButtons();
        structureOptions = null;
        int buttonWidth = 100;
        int buttonHeight = 20;
        int previewWidth = 200;
        int previewHeight = 100;
        int boxWidth = 80;
        int boxHeight = 20;
        y = y - 20;
        //MODE
        modeScanButton = addRenderableWidget(new ActivateableButton(x - buttonWidth - 101, y - previewHeight / 2 + 130, buttonWidth, buttonHeight, Component.literal("Scan"),
                btn -> {
                    this.mode = Mode.SCAN;

                    this.resetScan();
                    this.setButtons();
                }
        ));
        modeScanButton.active = this.mode == Mode.SCAN;

        modeLoadButton = addRenderableWidget(new ActivateableButton(x - buttonWidth - 101, y - previewHeight / 2 + 130 + buttonHeight, buttonWidth, buttonHeight, Component.literal("Load"),
                btn -> {
                    this.mode = Mode.LOAD;

                    this.resetScan();
                    this.setButtons();
                }
        ));
        modeLoadButton.active = this.mode == Mode.LOAD;

        switch (mode) {
            case SCAN -> {
                xSizePlusButton = addRenderableWidget(new ExtendedButton(x - buttonWidth - 41, y + 121, 20, 20, Component.literal("+"),
                        btn -> {
                            if (hasShiftDown()) areaWidthSize += 5;
                            else areaWidthSize++;
                            areaWidthSize = Mth.clamp(areaWidthSize, 3, 32);

                            this.workArea.setWidthSize(areaWidthSize);
                            WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateBuildArea(this.workArea.getUUID(), areaWidthSize, areaHeightSize, areaDepthSize, structureNBT, false, false, freeArea));

                            this.resetScan();
                            this.setButtons();
                        }
                ));

                xSizeMinusButton = addRenderableWidget(new ExtendedButton(x - buttonWidth - 21, y + 121, 20, 20, Component.literal("-"),
                        btn -> {
                            if (hasShiftDown()) areaWidthSize -= 5;
                            else areaWidthSize--;
                            areaWidthSize = Mth.clamp(areaWidthSize, 3, 32);

                            this.workArea.setWidthSize(areaWidthSize);
                            WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateBuildArea(this.workArea.getUUID(), areaWidthSize, areaHeightSize, areaDepthSize, structureNBT, false, false, freeArea));

                            this.resetScan();
                            this.setButtons();
                        }
                ));

                ySizePlusButton = addRenderableWidget(new ExtendedButton(x - buttonWidth - 41, y + 141, 20, 20, Component.literal("+"),
                        btn -> {
                            if (hasShiftDown()) areaHeightSize += 5;
                            else areaHeightSize++;
                            areaHeightSize = Mth.clamp(areaHeightSize, 3, 32);

                            this.workArea.setHeightSize(areaHeightSize);
                            WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateBuildArea(this.workArea.getUUID(), areaWidthSize, areaHeightSize, areaDepthSize, structureNBT, false, false, freeArea));

                            this.resetScan();
                            this.setButtons();
                        }
                ));

                ySizeMinusButton = addRenderableWidget(new ExtendedButton(x - buttonWidth - 21, y + 141, 20, 20, Component.literal("-"),
                        btn -> {
                            if (hasShiftDown()) areaHeightSize -= 5;
                            else areaHeightSize--;
                            areaHeightSize = Mth.clamp(areaHeightSize, 3, 32);

                            this.workArea.setHeightSize(areaHeightSize);
                            UUID uuid = this.buildArea.getUUID();
                            WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateBuildArea(uuid, areaWidthSize, areaHeightSize, areaDepthSize, structureNBT, false, false, freeArea));
                            this.resetScan();
                            this.setButtons();
                        }
                ));

                zSizePlusButton = addRenderableWidget(new ExtendedButton(x - buttonWidth - 41, y + 161, 20, 20, Component.literal("+"),
                        btn -> {
                            if (hasShiftDown()) areaDepthSize += 5;
                            else areaDepthSize++;
                            areaDepthSize = Mth.clamp(areaDepthSize, 3, 32);

                            this.workArea.setDepthSize(areaDepthSize);
                            WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateBuildArea(this.workArea.getUUID(), areaWidthSize, areaHeightSize, areaDepthSize, structureNBT, false, false, freeArea));
                            this.resetScan();
                            this.setButtons();
                        }
                ));

                zSizeMinusButton = addRenderableWidget(new ExtendedButton(x - buttonWidth - 21, y + 161, 20, 20, Component.literal("-"),
                        btn -> {
                            if (hasShiftDown()) areaDepthSize -= 5;
                            else areaDepthSize--;
                            areaDepthSize = Mth.clamp(areaDepthSize, 3, 32);

                            this.workArea.setDepthSize(areaDepthSize);
                            UUID uuid = this.buildArea.getUUID();
                            WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateBuildArea(uuid, areaWidthSize, areaHeightSize, areaDepthSize, structureNBT, false, false, freeArea));
                            this.resetScan();
                            this.setButtons();
                        }
                ));

                scanNameEditBox = new EditBox(font, x - previewWidth / 2, y - previewHeight / 2 + 130 - boxHeight - 2, previewWidth, boxHeight, Component.literal(""));
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

                scanButton = addRenderableWidget(new ExtendedButton(x - buttonWidth / 2, y - buttonHeight / 2 + 130, buttonWidth, buttonHeight, Component.literal("Scan Area"),
                        btn -> {
                            this.performClientScan();
                            this.checkScanButtonActive();

                            checkSaveButtonActive(this.scanNameEditBox.getValue());
                        }
                ));

                saveButton = addRenderableWidget(new ExtendedButton(x - buttonWidth / 2, y + 182, buttonWidth, buttonHeight, Component.literal("Save"),
                        btn -> StructureManager.saveStructureToFile(this.scanNameEditBox.getValue(), this.structureNBT)
                ));
                saveButton.active = false;

                structurePreview = new StructurePreviewWidget(x - previewWidth / 2, y - previewHeight / 2 + 130, previewWidth, previewHeight, buildArea.getWidthSize(), buildArea.getDepthSize());
                addRenderableWidget(structurePreview);
                checkScanButtonActive();
            }

            case LOAD -> {
                BuildMode bm = WorkersClientManager.buildMode;

                if (bm == BuildMode.FREE) {
                    Path scanRoot = Path.of(Minecraft.getInstance().gameDirectory.getAbsolutePath(), "workers", "scan");

                    structureOptions = new ScrollDropDownMenuWithFolders(
                            x - previewWidth / 2 - 1,
                            y - previewHeight / 2 + 131 - boxHeight - 2,
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
                    addRenderableWidget(structureOptions);

                }
                else {
                    int refreshW = 22;
                    addRenderableWidget(new ExtendedButton(
                            x - previewWidth / 2 - 24,
                            y - previewHeight / 2 + 131 - boxHeight - 2,
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
                            y - previewHeight / 2 + 131 - boxHeight - 2,
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
                    addRenderableWidget(structureOptions);
                }

                buildButton = addRenderableWidget(new ExtendedButton(x - buttonWidth / 2, y + 182, buttonWidth, buttonHeight, Component.literal("Build"),
                        btn -> {
                            WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateBuildArea(this.buildArea.getUUID(), areaWidthSize, areaHeightSize, areaDepthSize, this.structureNBT, true, false, freeArea));
                        }
                ));

                if (player.isCreative()) {
                    placeButton = addRenderableWidget(new ExtendedButton(x - buttonWidth / 2 + buttonWidth, y + 182, buttonWidth, buttonHeight, Component.literal("Place"),
                            btn -> {
                                WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateBuildArea(this.buildArea.getUUID(), areaWidthSize, areaHeightSize, areaDepthSize, this.structureNBT, true, true, freeArea));
                            }
                    ));
                }

                requiredItemsDropDownMenu = new DisplayTextItemScrollDropDownMenu(ItemStack.EMPTY, "Blocks", x + 101, y + 60, 110, boxHeight, requiredItems, null);
                requiredItemsDropDownMenu.setBgFillSelected(FastColor.ARGB32.color(255, 139, 139, 139));
                requiredItemsDropDownMenu.setCanSelectItem(false);
                requiredItemsDropDownMenu.setResetCount(false);

                addRenderableWidget(requiredItemsDropDownMenu);

                structurePreview = new StructurePreviewWidget(x - previewWidth / 2, y - previewHeight / 2 + 130, previewWidth, previewHeight, buildArea.getWidthSize(), buildArea.getDepthSize());
                addRenderableWidget(structurePreview);
                if (structure != null) structurePreview.setStructure(this.structure, this.structureNBT);
                checkBuildButtonActive();
            }
        }

        int blackboxWidth = mode == Mode.SCAN ? 40 : 70;
        int blackboxWidth2 = mode == Mode.SCAN ? 20 : 30;
        int blackboxHeight = 20;
        int blackBoxPosX = x - 201;
        int blackBoxPosY = y + 100;
        addRenderableWidget(new BlackShowingTextField(blackBoxPosX, blackBoxPosY + 21, blackboxWidth, blackboxHeight, Component.literal("Width:")));
        addRenderableWidget(new BlackShowingTextField(blackBoxPosX, blackBoxPosY + 41, blackboxWidth, blackboxHeight, Component.literal("Height:")));
        addRenderableWidget(new BlackShowingTextField(blackBoxPosX, blackBoxPosY + 61, blackboxWidth, blackboxHeight, Component.literal("Depth:")));
        addRenderableWidget(new BlackShowingTextField(blackBoxPosX + blackboxWidth, blackBoxPosY + 21, blackboxWidth2, blackboxHeight, Component.literal("" + areaWidthSize)));
        addRenderableWidget(new BlackShowingTextField(blackBoxPosX + blackboxWidth, blackBoxPosY + 41, blackboxWidth2, blackboxHeight, Component.literal("" + areaHeightSize)));
        addRenderableWidget(new BlackShowingTextField(blackBoxPosX + blackboxWidth, blackBoxPosY + 61, blackboxWidth2, blackboxHeight, Component.literal("" + areaDepthSize)));

        // Free Area checkbox — directly under the xyz showing boxes
        this.freeAreaCheckBox = new RecruitsCheckBox(blackBoxPosX, blackBoxPosY + 81, 100, 20, TEXT_FREE_AREA,
                this.freeArea,
                (bool) -> {
                    this.freeArea = bool;
                    WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateBuildArea(this.buildArea.getUUID(), areaWidthSize, areaHeightSize, areaDepthSize, structureNBT, false, false, freeArea));
                }
        );
        addRenderableWidget(freeAreaCheckBox);
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

        WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateBuildArea(this.buildArea.getUUID(), width, height, depth, tag, false, false, freeArea));

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
            this.resetScan();
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
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        if (presetLoading) {
            int previewWidth = 200;
            int previewHeight = 100;
            // Grey overlay on preview area
            guiGraphics.fill(x - previewWidth / 2, y - previewHeight / 2 + 130,
                    x + previewWidth / 2, y + previewHeight / 2 + 130,
                    0xAA222222);
            guiGraphics.drawCenteredString(font,
                    Component.literal("Loading..."),
                    x, y + 130, 0xFFFFFF);
        }
    }

    @Override
    public void onAreaMoved() {
        this.resetScan();
        checkScanButtonActive();
    }

    public enum Mode {
        SCAN,
        LOAD
    }
}