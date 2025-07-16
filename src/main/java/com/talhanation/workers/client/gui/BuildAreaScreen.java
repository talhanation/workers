package com.talhanation.workers.client.gui;

import com.talhanation.recruits.client.gui.component.ActivateableButton;
import com.talhanation.recruits.client.gui.widgets.BlackShowingTextField;
import com.talhanation.recruits.client.gui.widgets.ScrollDropDownMenu;
import com.talhanation.workers.Main;
import com.talhanation.workers.world.ScannedBlock;
import com.talhanation.workers.client.gui.structureRenderer.StructurePreviewWidget;
import com.talhanation.workers.entities.workarea.BuildArea;
import com.talhanation.workers.network.MessageUpdateBuildArea;
import com.talhanation.workers.world.StructureManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.gui.widget.ExtendedButton;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BuildAreaScreen extends WorkAreaScreen {

    public final BuildArea buildArea;
    public Button scanButton;
    public Button buildButton;
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
    public BuildAreaScreen(BuildArea buildArea, Player player) {
        super(buildArea.getCustomName(), buildArea, player);
        this.buildArea = buildArea;
    }

    @Override
    protected void init() {
        structureNBT = buildArea.getStructureNBT();
        if(structureNBT != null && !structureNBT.isEmpty()){
            mode = Mode.LOAD;
            structure = parseStructureFromNBT(structureNBT);
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
        super.setButtons();
        this.structureOptions = null;
        int buttonWidth = 80;
        int buttonHeight = 20;
        int previewWidth = 200;
        int previewHeight = 100;
        int boxWidth = 80;
        int boxHeight = 20;

        addRenderableWidget(new BlackShowingTextField(x + previewWidth/2, y - previewHeight / 2 + 130, boxWidth, boxHeight, Component.literal("x: " + areaXSize)));
        addRenderableWidget(new BlackShowingTextField(x + previewWidth/2, y - previewHeight / 2 + 130 + boxHeight, boxWidth, boxHeight, Component.literal( "y: " + areaYSize)));
        addRenderableWidget(new BlackShowingTextField(x + previewWidth/2, y - previewHeight / 2 + 130 + boxHeight*2, boxWidth, boxHeight, Component.literal( "z: " + areaZSize)));

        //MODE
        modeScanButton = addRenderableWidget(new ActivateableButton(x - buttonWidth - 100, y - previewHeight / 2 + 130, buttonWidth, buttonHeight, Component.literal("Scan"),
                btn -> {
                    this.mode = Mode.SCAN;

                    this.resetScan();
                    this.setButtons();
                }
        ));
        modeScanButton.active = this.mode == Mode.SCAN;

        modeLoadButton = addRenderableWidget(new ActivateableButton(x - buttonWidth - 100 , y - previewHeight / 2 + 130 + buttonHeight, buttonWidth, buttonHeight, Component.literal("Load"),
                btn -> {
                    this.mode = Mode.LOAD;

                    this.resetScan();
                    this.setButtons();
                }
        ));
        modeLoadButton.active = this.mode == Mode.LOAD;

        switch (mode){
            case SCAN -> {
                xSizePlusButton = addRenderableWidget(new ExtendedButton(x + previewWidth/2 + 80, y - previewHeight / 2 + 130, 20, 20, Component.literal("+"),
                        btn -> {
                            if(hasShiftDown()) areaXSize += 5;
                            else areaXSize++;
                            areaXSize = Mth.clamp(areaXSize, 3, 16);

                            this.workArea.setWidthSize(areaXSize);
                            Main.SIMPLE_CHANNEL.sendToServer(new MessageUpdateBuildArea(this.workArea.getUUID(), areaXSize, areaYSize, areaZSize, structureNBT, false));

                            this.resetScan();
                            this.setButtons();
                        }
                ));

                xSizeMinusButton = addRenderableWidget(new ExtendedButton(x + previewWidth/2 + 80 + 20, y - previewHeight / 2 + 130, 20, 20, Component.literal("-"),
                        btn -> {
                            if(hasShiftDown()) areaXSize -= 5;
                            else areaXSize--;
                            areaXSize = Mth.clamp(areaXSize, 3, 16);

                            this.workArea.setWidthSize(areaXSize);
                            Main.SIMPLE_CHANNEL.sendToServer(new MessageUpdateBuildArea(this.workArea.getUUID(), areaXSize, areaYSize, areaZSize,  structureNBT, false));

                            this.resetScan();
                            this.setButtons();
                        }
                ));

                ySizePlusButton = addRenderableWidget(new ExtendedButton(x + previewWidth/2 + 80, y - previewHeight / 2 + 130 + 20, 20, 20, Component.literal("+"),
                        btn -> {
                            if(hasShiftDown()) areaYSize += 5;
                            else areaYSize++;
                            areaYSize = Mth.clamp(areaYSize, 3, 16);

                            this.workArea.setHeightSize(areaYSize);
                            Main.SIMPLE_CHANNEL.sendToServer(new MessageUpdateBuildArea(this.workArea.getUUID(), areaXSize, areaYSize, areaZSize, structureNBT, false));

                            this.resetScan();
                            this.setButtons();
                        }
                ));

                ySizeMinusButton = addRenderableWidget(new ExtendedButton(x + previewWidth/2 + 80 + 20, y - previewHeight / 2 + 130 + 20, 20, 20, Component.literal("-"),
                        btn -> {
                            if(hasShiftDown()) areaYSize -= 5;
                            else areaYSize--;
                            areaYSize = Mth.clamp(areaYSize, 3, 16);

                            this.workArea.setHeightSize(areaYSize);
                            UUID uuid = this.buildArea.getUUID();
                            Main.SIMPLE_CHANNEL.sendToServer(new MessageUpdateBuildArea(uuid, areaXSize, areaYSize, areaZSize, structureNBT, false));
                            this.resetScan();
                            this.setButtons();
                        }
                ));

                zSizePlusButton = addRenderableWidget(new ExtendedButton(x + previewWidth/2 + 80, y - previewHeight / 2 + 130 + 40, 20, 20, Component.literal("+"),
                        btn -> {
                            if(hasShiftDown()) areaZSize += 5;
                            else areaZSize++;
                            areaZSize = Mth.clamp(areaZSize, 3, 16);

                            this.workArea.setHeightSize(areaZSize);
                            Main.SIMPLE_CHANNEL.sendToServer(new MessageUpdateBuildArea(this.workArea.getUUID(), areaXSize, areaYSize, areaZSize, structureNBT, false));
                            this.resetScan();
                            this.setButtons();
                        }
                ));

                zSizeMinusButton = addRenderableWidget(new ExtendedButton(x + previewWidth/2 + 80 + 20, y - previewHeight / 2 + 130 + 40, 20, 20, Component.literal("-"),
                        btn -> {
                            if(hasShiftDown()) areaZSize -= 5;
                            else areaZSize--;
                            areaZSize = Mth.clamp(areaZSize, 3, 16);

                            this.workArea.setHeightSize(areaZSize);
                            UUID uuid = this.buildArea.getUUID();
                            Main.SIMPLE_CHANNEL.sendToServer(new MessageUpdateBuildArea(uuid, areaXSize, areaYSize, areaZSize, structureNBT, false));
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

                saveButton = addRenderableWidget(new ExtendedButton(x + previewWidth/2, y + previewHeight + previewHeight/2 + 11, buttonWidth, buttonHeight, Component.literal("Save"),
                        btn -> StructureManager.saveStructureToFile(this.scanNameEditBox.getValue(), structure, this.buildArea.getWidthSize(), this.buildArea.getHeightSize(), this.buildArea.getDepthSize(), this.buildArea.getFacing())
                ));

                structurePreview = new StructurePreviewWidget(x - previewWidth / 2, y - previewHeight / 2 + 130, previewWidth, previewHeight, buildArea.getWidthSize(), buildArea.getHeightSize());
                addRenderableWidget(structurePreview);
                checkScanButtonActive();
            }

            case LOAD -> {
                List<String> scans = StructureManager.loadAvailableScans();
                String title = scans.isEmpty() ? "Empty" : "Select NBT-File";
                structureOptions = new ScrollDropDownMenu<>(title, x - previewWidth/2 -1 , y - previewHeight / 2 + 130 - boxHeight - 2, previewWidth +2, boxHeight +2,
                        scans,
                        string -> string,
                        selectedName -> {
                            CompoundTag tag = StructureManager.loadScanNbt(selectedName);
                            if (tag != null) {
                                this.structureNBT = tag;
                                this.structure = parseStructureFromNBT(tag);
                                structurePreview.setStructure(this.structure, this.structureNBT);
                                checkBuildButtonActive();
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
                                Main.SIMPLE_CHANNEL.sendToServer(new MessageUpdateBuildArea(this.buildArea.getUUID(), width, height, depth, structureNBT, false));
                                this.setButtons();
                            }
                        }
                );
                addRenderableWidget(structureOptions);

                buildButton = addRenderableWidget(new ExtendedButton(x + previewWidth/2, y + previewHeight + previewHeight/2 + 11, buttonWidth, buttonHeight, Component.literal("Build"),
                        btn -> {
                            Main.SIMPLE_CHANNEL.sendToServer(new MessageUpdateBuildArea(this.buildArea.getUUID(), this.buildArea.getWidthSize(), this.buildArea.getHeightSize(), areaZSize, this.structureNBT, true));
                        }
                ));

                structurePreview = new StructurePreviewWidget(x - previewWidth / 2, y - previewHeight / 2 + 130, previewWidth, previewHeight, buildArea.getWidthSize(), buildArea.getHeightSize());
                addRenderableWidget(structurePreview);
                if(structure != null) structurePreview.setStructure(this.structure, this.structureNBT);
                checkBuildButtonActive();
            }
        }

        String string = null;
        if(scanNameEditBox != null) string = this.scanNameEditBox.getValue();
        checkSaveButtonActive(string);
        checkBuildButtonActive();
    }

    public void resetScan(){
        structure = null;
        structureNBT = null;
        if(this.structurePreview != null) structurePreview.setStructure(null, null);
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
    }

    private void checkSaveButtonActive(String s) {
        if(this.saveButton == null) return;

        this.saveButton.active = this.structure != null && s != null && s.length() >= 3;
    }

    private void performClientScan() {
        Level level = Minecraft.getInstance().level;
        if (level == null) return;

        this.structureNBT = StructureManager.scanStructure(level, this.buildArea, this.scanNameEditBox.getValue());
        this.structure = parseStructureFromNBT(structureNBT);
        this.structurePreview.setStructure(structure, structureNBT);
    }

    @Override
    public void mouseMoved(double x, double y) {
        if(structureOptions != null){
            structureOptions.onMouseMove(x,y);
        }
        super.mouseMoved(x, y);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (structureOptions != null && structureOptions.isMouseOver(mouseX, mouseY)) {
            this.structure = null;
            this.structureNBT = null;
            this.structurePreview.setStructure(null, null);
            structureOptions.onMouseClick(mouseX, mouseY);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    @Override
    public boolean mouseScrolled(double x, double y, double d) {
        if(structureOptions != null) structureOptions.mouseScrolled(x,y,d);
        return super.mouseScrolled(x, y, d);
    }
    public static List<ScannedBlock> parseStructureFromNBT(CompoundTag root) {
        List<ScannedBlock> result = new ArrayList<>();
        String dir = root.getString("facing");
        Direction scanFacing = Direction.byName(dir);
        ListTag blockList = root.getList("blocks", Tag.TAG_COMPOUND);
        for (Tag tag : blockList) {
            CompoundTag blockTag = (CompoundTag) tag;
            BlockState state = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), blockTag.getCompound("state"));
            BlockPos relPos = new BlockPos(blockTag.getInt("x"), blockTag.getInt("y"), blockTag.getInt("z"));

            CompoundTag be = blockTag.contains("blockEntity") ? blockTag.getCompound("blockEntity") : null;
            result.add(new ScannedBlock(state, scanFacing, be, relPos));
        }

        return result;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        if (structureOptions != null) {
            structureOptions.renderWidget(guiGraphics, mouseX, mouseY, partialTicks);
        }
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
