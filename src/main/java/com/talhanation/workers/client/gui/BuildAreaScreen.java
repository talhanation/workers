package com.talhanation.workers.client.gui;

import com.talhanation.recruits.client.gui.component.ActivateableButton;
import com.talhanation.recruits.client.gui.widgets.BlackShowingTextField;
import com.talhanation.recruits.client.gui.widgets.ScrollDropDownMenu;
import com.talhanation.workers.WorkersMain;
import com.talhanation.workers.client.gui.widgets.DisplayTextItemScrollDropDownMenu;
import com.talhanation.workers.world.ScannedBlock;
import com.talhanation.workers.client.gui.structureRenderer.StructurePreviewWidget;
import com.talhanation.workers.entities.workarea.BuildArea;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BuildAreaScreen extends WorkAreaScreen {

    public final BuildArea buildArea;
    public Button scanButton;
    public Button buildButton;
    public Button placeButton;
    public StructurePreviewWidget structurePreview;
    public ScrollDropDownMenu<String> structureOptions;
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
    public String savedName;
    public int areaXSize;
    public int areaYSize;
    public int areaZSize;
    public List< ItemStack> requiredItems = new ArrayList<>();
    public DisplayTextItemScrollDropDownMenu requiredItemsDropDownMenu;
    public BuildAreaScreen(BuildArea buildArea, Player player) {
        super(buildArea.getCustomName(), buildArea, player);
        this.buildArea = buildArea;
    }

    @Override
    protected void init() {
        structureNBT = buildArea.getStructureNBT();
        if(structureNBT != null && !structureNBT.isEmpty()){
            mode = Mode.LOAD;
            structure = StructureManager.parseStructureFromNBT(structureNBT);
            this.requiredItems = buildArea.getRequiredMaterials(structureNBT);
        }
        else mode = Mode.SCAN;

        this.areaXSize = buildArea.getWidthSize();
        this.areaYSize = buildArea.getHeightSize();
        this.areaZSize = buildArea.getDepthSize();
        setButtons();
    }

    @Override
    public void tick() {
        super.tick();
        if(scanNameEditBox != null && scanNameEditBox.isFocused()) scanNameEditBox.tick();
    }

    @Override
    public void setButtons() {
        this.clearWidgets();
        super.setButtons();
        structureOptions = null;
        int buttonWidth = 80;
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

        modeLoadButton = addRenderableWidget(new ActivateableButton(x - buttonWidth - 101 , y - previewHeight / 2 + 130 + buttonHeight, buttonWidth, buttonHeight, Component.literal("Load"),
                btn -> {
                    this.mode = Mode.LOAD;

                    this.resetScan();
                    this.setButtons();
                }
        ));
        modeLoadButton.active = this.mode == Mode.LOAD;

        switch (mode){
            case SCAN -> {
                xSizePlusButton = addRenderableWidget(new ExtendedButton(x - buttonWidth - 61, y + 121, 20, 20, Component.literal("+"),
                        btn -> {
                            if(hasShiftDown()) areaXSize += 5;
                            else areaXSize++;
                            areaXSize = Mth.clamp(areaXSize, 3, 32);

                            this.workArea.setWidthSize(areaXSize);
                            WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateBuildArea(this.workArea.getUUID(), areaXSize, areaYSize, areaZSize, structureNBT, false, false));

                            this.resetScan();
                            this.setButtons();
                        }
                ));

                xSizeMinusButton = addRenderableWidget(new ExtendedButton(x - buttonWidth - 41, y + 121, 20, 20, Component.literal("-"),
                        btn -> {
                            if(hasShiftDown()) areaXSize -= 5;
                            else areaXSize--;
                            areaXSize = Mth.clamp(areaXSize, 3, 32);

                            this.workArea.setWidthSize(areaXSize);
                            WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateBuildArea(this.workArea.getUUID(), areaXSize, areaYSize, areaZSize,  structureNBT, false, false));

                            this.resetScan();
                            this.setButtons();
                        }
                ));

                ySizePlusButton = addRenderableWidget(new ExtendedButton(x - buttonWidth - 61, y + 141, 20, 20, Component.literal("+"),
                        btn -> {
                            if(hasShiftDown()) areaYSize += 5;
                            else areaYSize++;
                            areaYSize = Mth.clamp(areaYSize, 3, 32);

                            this.workArea.setHeightSize(areaYSize);
                            WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateBuildArea(this.workArea.getUUID(), areaXSize, areaYSize, areaZSize, structureNBT, false, false));

                            this.resetScan();
                            this.setButtons();
                        }
                ));

                ySizeMinusButton = addRenderableWidget(new ExtendedButton(x - buttonWidth - 41, y + 141, 20, 20, Component.literal("-"),
                        btn -> {
                            if(hasShiftDown()) areaYSize -= 5;
                            else areaYSize--;
                            areaYSize = Mth.clamp(areaYSize, 3, 32);

                            this.workArea.setHeightSize(areaYSize);
                            UUID uuid = this.buildArea.getUUID();
                            WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateBuildArea(uuid, areaXSize, areaYSize, areaZSize, structureNBT, false, false));
                            this.resetScan();
                            this.setButtons();
                        }
                ));

                zSizePlusButton = addRenderableWidget(new ExtendedButton(x - buttonWidth - 61, y + 161, 20, 20, Component.literal("+"),
                        btn -> {
                            if(hasShiftDown()) areaZSize += 5;
                            else areaZSize++;
                            areaZSize = Mth.clamp(areaZSize, 3, 32);

                            this.workArea.setDepthSize(areaZSize);
                            WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateBuildArea(this.workArea.getUUID(), areaXSize, areaYSize, areaZSize, structureNBT, false, false));
                            this.resetScan();
                            this.setButtons();
                        }
                ));

                zSizeMinusButton = addRenderableWidget(new ExtendedButton(x - buttonWidth - 41, y + 161, 20, 20, Component.literal("-"),
                        btn -> {
                            if(hasShiftDown()) areaZSize -= 5;
                            else areaZSize--;
                            areaZSize = Mth.clamp(areaZSize, 3, 32);

                            this.workArea.setDepthSize(areaZSize);
                            UUID uuid = this.buildArea.getUUID();
                            WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateBuildArea(uuid, areaXSize, areaYSize, areaZSize, structureNBT, false , false));
                            this.resetScan();
                            this.setButtons();
                        }
                ));

                scanNameEditBox = new EditBox(font, x - previewWidth/2 , y - previewHeight / 2 + 130 - boxHeight - 2, previewWidth, boxHeight, Component.literal(""));
                scanNameEditBox.setValue(savedName);
                scanNameEditBox.setTextColor(-1);
                scanNameEditBox.setTextColorUneditable(-1);
                scanNameEditBox.setBordered(true);
                scanNameEditBox.setMaxLength(32);
                scanNameEditBox.setEditable(mode == Mode.SCAN);
                scanNameEditBox.setResponder(this::checkScanNameUpdate);
                this.addRenderableWidget(scanNameEditBox);

                scanButton = addRenderableWidget(new ExtendedButton(x - buttonWidth / 2, y - buttonHeight / 2 + 130, buttonWidth, buttonHeight, Component.literal("Scan Area"),
                        btn ->{
                            this.performClientScan();
                            this.checkScanButtonActive();

                            checkSaveButtonActive(this.scanNameEditBox.getValue());
                        }
                ));

                saveButton = addRenderableWidget(new ExtendedButton(x - buttonWidth / 2, y + 182, buttonWidth, buttonHeight, Component.literal("Save"),
                        btn -> StructureManager.saveStructureToFile(this.scanNameEditBox.getValue(), structure, this.buildArea.getWidthSize(), this.buildArea.getHeightSize(), this.buildArea.getDepthSize(), this.buildArea.getFacing())
                ));

                structurePreview = new StructurePreviewWidget(x - previewWidth / 2, y - previewHeight / 2 + 130, previewWidth, previewHeight, buildArea.getWidthSize(), buildArea.getHeightSize());
                addRenderableWidget(structurePreview);
                checkScanButtonActive();
            }

            case LOAD -> {
                List<String> scans = StructureManager.loadAvailableScans();
                String title = scans.isEmpty() ? "Empty" : "Select NBT-File";
                structureOptions = new ScrollDropDownMenu<>(title, x - previewWidth/2 -1 , y - previewHeight / 2 + 131 - boxHeight - 2, previewWidth +2, boxHeight +2,
                        scans,
                        string -> string,
                        selectedName -> {
                            CompoundTag tag = StructureManager.loadScanNbt(selectedName);
                            if (tag != null) {
                                int width = tag.getInt("width");
                                int height = tag.getInt("height");;
                                int depth = tag.getInt("depth");;
                                this.savedName = tag.getString("name");
                                this.areaXSize = width;
                                this.areaYSize = height;
                                this.areaZSize = depth;
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
                        }
                );
                addRenderableWidget(structureOptions);

                buildButton = addRenderableWidget(new ExtendedButton(x - buttonWidth / 2, y + 182, buttonWidth, buttonHeight, Component.literal("Build"),
                        btn -> {
                            WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateBuildArea(this.buildArea.getUUID(), this.buildArea.getWidthSize(), this.buildArea.getHeightSize(), areaZSize, this.structureNBT, true, false));
                        }
                ));

                if(player.isCreative()){
                    placeButton = addRenderableWidget(new ExtendedButton(x - buttonWidth/2 + buttonWidth, y + 182, buttonWidth, buttonHeight, Component.literal("Place"),
                            btn -> {
                                WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateBuildArea(this.buildArea.getUUID(), this.buildArea.getWidthSize(), this.buildArea.getHeightSize(), areaZSize, this.structureNBT, true, true));
                            }
                    ));
                }

                requiredItemsDropDownMenu = new DisplayTextItemScrollDropDownMenu(ItemStack.EMPTY, "Blocks", x + 101 , y + 60, 110, boxHeight, requiredItems, null);
                requiredItemsDropDownMenu.setBgFillSelected(FastColor.ARGB32.color(255, 139, 139, 139));
                requiredItemsDropDownMenu.setCanSelectItem(false);
                requiredItemsDropDownMenu.setResetCount(false);

                addRenderableWidget(requiredItemsDropDownMenu);

                structurePreview = new StructurePreviewWidget(x - previewWidth / 2, y - previewHeight / 2 + 130, previewWidth, previewHeight, buildArea.getWidthSize(), buildArea.getHeightSize());
                addRenderableWidget(structurePreview);
                if(structure != null) structurePreview.setStructure(this.structure, this.structureNBT);
                checkBuildButtonActive();

            }
        }

        int blackboxWidth = mode == Mode.SCAN ? 40 : 80;
        int blackboxHeight = 20;
        int blackBoxPosX = x - 181;
        int blackBoxPosY = y + 100;
        addRenderableWidget(new BlackShowingTextField(blackBoxPosX, blackBoxPosY + 21, blackboxWidth, blackboxHeight, Component.literal("x: " + areaXSize)));
        addRenderableWidget(new BlackShowingTextField(blackBoxPosX, blackBoxPosY + 41, blackboxWidth, blackboxHeight, Component.literal("y: " + areaYSize)));
        addRenderableWidget(new BlackShowingTextField(blackBoxPosX, blackBoxPosY + 61, blackboxWidth, blackboxHeight, Component.literal("z: " + areaZSize)));
    }

    private void setStructure(List<ScannedBlock> structure, CompoundTag structureNBT) {
        this.structurePreview.setStructure(structure, structureNBT);
        this.requiredItems = buildArea.getRequiredMaterials(structureNBT);

        this.requiredItemsDropDownMenu.setOptions(requiredItems);
    }

    public void resetScan(){
        structure = null;
        structureNBT = new CompoundTag();
        if(this.structurePreview != null) structurePreview.setStructure(null, null);
        this.requiredItems = new ArrayList<>();
    }


    private void checkScanNameUpdate(String s) {
        checkSaveButtonActive(s);
    }

    private void checkScanButtonActive(){
        if(this.scanButton == null) return;
        boolean active = this.structure == null;
        scanButton.active = active;
        scanButton.visible = active;
    }
    private void checkBuildButtonActive() {
        if(this.buildButton == null) return;

        this.buildButton.active = this.structure != null;
        if(placeButton != null) this.placeButton.active = this.buildButton.active;
    }

    private void checkSaveButtonActive(String s) {
        if(this.saveButton == null) return;

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
        if(structureOptions != null){
            structureOptions.onMouseMove(x,y);
        }
        if(requiredItemsDropDownMenu != null){
            requiredItemsDropDownMenu.onMouseMove(x,y);
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
        if(structureOptions != null  && structureOptions.isMouseOver(x, y)) structureOptions.mouseScrolled(x,y,d);
        if(requiredItemsDropDownMenu != null && requiredItemsDropDownMenu.isMouseOver(x, y)) requiredItemsDropDownMenu.mouseScrolled(x,y,d);
        return super.mouseScrolled(x, y, d);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public void onAreaMoved() {
        this.resetScan();
        checkScanButtonActive();
    }

    public enum Mode{
        SCAN,
        LOAD
    }


}
