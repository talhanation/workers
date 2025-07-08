package com.talhanation.workers.client.gui;

import com.talhanation.recruits.client.gui.component.ActivateableButton;
import com.talhanation.recruits.client.gui.widgets.BlackShowingTextField;
import com.talhanation.workers.Main;
import com.talhanation.workers.client.gui.structureRenderer.ScannedBlock;
import com.talhanation.workers.client.gui.structureRenderer.StructurePreviewWidget;
import com.talhanation.workers.client.gui.widgets.ItemScrollDropDownMenu;
import com.talhanation.workers.entities.workarea.BuildArea;
import com.talhanation.workers.network.MessageUpdateBuildArea;
import com.talhanation.workers.world.StructureScanner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.gui.widget.ExtendedButton;

import java.util.ArrayList;
import java.util.List;

public class BuildAreaScreen extends WorkAreaScreen {

    public final BuildArea buildArea;
    public Button scanButton;
    public Button buildButton;
    public StructurePreviewWidget structurePreview;
    public ItemScrollDropDownMenu structureOptions;
    public CompoundTag structureNBT;
    public List<ScannedBlock> structure;
    public Mode mode;
    public EditBox scanNameEditBox;
    public Button modeScanButton;
    public Button modeLoadButton;
    public Button saveButton;
    public Button sizePlusButton;
    public Button sizeMinusButton;
    public Button heightPlusButton;
    public Button heightMinusButton;
    public int areaSize;
    public int areaHeight;
    public BuildAreaScreen(BuildArea buildArea, Player player) {
        super(buildArea.getCustomName(), buildArea, player);
        this.buildArea = buildArea;
    }

    @Override
    protected void init() {
        this.mode = Mode.SCAN;
        this.areaHeight = buildArea.getHeight();
        this.areaSize = buildArea.getSize();
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
        int buttonWidth = 80;
        int buttonHeight = 20;
        int previewWidth = 200;
        int previewHeight = 100;
        int boxWidth = 80;
        int boxHeight = 20;

        this.structure = null;
        this.structureNBT = null;

        addRenderableWidget(new BlackShowingTextField(x + previewWidth/2, y - previewHeight / 2 + 130, boxWidth, boxHeight, Component.literal("Size:  " + areaSize )));
        addRenderableWidget(new BlackShowingTextField(x + previewWidth/2, y - previewHeight / 2 + 130 + boxHeight, boxWidth, boxHeight, Component.literal( "Height: " + areaHeight)));

        //MODE
        modeScanButton = addRenderableWidget(new ActivateableButton(x - buttonWidth - 100, y - previewHeight / 2 + 130, buttonWidth, buttonHeight, Component.literal("Scan"),
                btn -> {
                    this.mode = Mode.SCAN;
                    this.setButtons();
                }
        ));
        modeScanButton.active = this.mode == Mode.SCAN;

        modeLoadButton = addRenderableWidget(new ActivateableButton(x - buttonWidth - 100 , y - previewHeight / 2 + 130 + buttonHeight, buttonWidth, buttonHeight, Component.literal("Load"),
                btn -> {
                    this.mode = Mode.LOAD;
                    this.setButtons();
                }
        ));
        modeLoadButton.active = this.mode == Mode.LOAD;

        switch (mode){
            case SCAN -> {
                sizePlusButton = addRenderableWidget(new ExtendedButton(x + previewWidth/2 + 80, y - previewHeight / 2 + 130, 20, 20, Component.literal("+"),
                        btn -> {
                            if(hasShiftDown()) areaSize += 5;
                            else areaSize++;
                            areaSize = Mth.clamp(areaSize, 3, 16);

                            this.workArea.setSize(areaSize);
                            Main.SIMPLE_CHANNEL.sendToServer(new MessageUpdateBuildArea(this.workArea.getUUID(), areaSize, areaHeight, structureNBT));
                            this.setButtons();
                        }
                ));

                sizeMinusButton = addRenderableWidget(new ExtendedButton(x + previewWidth/2 + 80 + 20, y - previewHeight / 2 + 130, 20, 20, Component.literal("-"),
                        btn -> {
                            if(hasShiftDown()) areaSize -= 5;
                            else areaSize--;
                            areaSize = Mth.clamp(areaSize, 3, 16);

                            this.workArea.setSize(areaSize);
                            Main.SIMPLE_CHANNEL.sendToServer(new MessageUpdateBuildArea(this.workArea.getUUID(), areaSize, areaHeight, structureNBT));
                            this.setButtons();
                        }
                ));

                heightPlusButton = addRenderableWidget(new ExtendedButton(x + previewWidth/2 + 80, y - previewHeight / 2 + 130 + 20, 20, 20, Component.literal("+"),
                        btn -> {
                            if(hasShiftDown()) areaHeight += 5;
                            else areaHeight++;
                            areaHeight = Mth.clamp(areaHeight, 3, 16);

                            this.workArea.setHeight(areaHeight);
                            Main.SIMPLE_CHANNEL.sendToServer(new MessageUpdateBuildArea(this.workArea.getUUID(), areaSize, areaHeight, structureNBT));
                            this.setButtons();
                        }
                ));

                heightMinusButton = addRenderableWidget(new ExtendedButton(x + previewWidth/2 + 80 + 20, y - previewHeight / 2 + 130 + 20, 20, 20, Component.literal("-"),
                        btn -> {
                            if(hasShiftDown()) areaHeight -= 5;
                            else areaHeight--;
                            areaHeight = Mth.clamp(areaHeight, 3, 16);

                            this.workArea.setHeight(areaHeight);
                            Main.SIMPLE_CHANNEL.sendToServer(new MessageUpdateBuildArea(this.workArea.getUUID(), areaSize, areaHeight, structureNBT));
                            this.setButtons();
                        }
                ));

                scanNameEditBox = new EditBox(font, x - previewWidth/2 , y - previewHeight / 2 + 130 - boxHeight - 2, previewWidth, boxHeight, Component.literal(""));
                scanNameEditBox.setValue("");
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

                saveButton = addRenderableWidget(new ExtendedButton(x + previewWidth/2, y + previewHeight + previewHeight/2 + 10, buttonWidth, buttonHeight, Component.literal("Save"),
                        btn -> {

                        }
                ));

                checkScanButtonActive();
            }

            case LOAD -> {
                buildButton = addRenderableWidget(new ExtendedButton(x + previewWidth/2, y + previewHeight + previewHeight/2, buttonWidth, buttonHeight, Component.literal("Build"),
                        btn -> {

                        }
                ));

                checkBuildButtonActive();
            }
        }

        structurePreview = new StructurePreviewWidget(x - previewWidth / 2, y - previewHeight / 2 + 130, previewWidth, previewHeight, buildArea.getSize(), buildArea.getHeight());
        addRenderableWidget(structurePreview);
        checkBuildButtonActive();
        checkSaveButtonActive(this.scanNameEditBox.getValue());
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
        BlockPos center = buildArea.getOnPos();
        int size = buildArea.getSize();
        int height = buildArea.getHeight();

        Level level = Minecraft.getInstance().level;
        if (level == null) return;

        this.structureNBT = StructureScanner.scanStructure(level, center, size, height);
        this.structure = parseStructureFromNBT(structureNBT);
        this.structurePreview.setStructure(structure);

        //this.setButtons();
        //Main.SIMPLE_CHANNEL.sendToServer(new MessageUploadStructure(scanResult));
    }

    public static List<ScannedBlock> parseStructureFromNBT(CompoundTag root) {
        List<ScannedBlock> result = new ArrayList<>();

        ListTag blockList = root.getList("blocks", Tag.TAG_COMPOUND);
        for (Tag tag : blockList) {
            CompoundTag blockTag = (CompoundTag) tag;
            BlockState state = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), blockTag.getCompound("state"));
            BlockPos relPos = new BlockPos(blockTag.getInt("x"), blockTag.getInt("y"), blockTag.getInt("z"));

            CompoundTag be = blockTag.contains("blockEntity") ? blockTag.getCompound("blockEntity") : null;
            result.add(new ScannedBlock(state, be, relPos));
        }

        return result;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);

    }

    @Override
    public void onAreaMoved() {
        this.structure = null;
        this.structureNBT = null;
    }

    public enum Mode{
        SCAN,
        LOAD
    }

}
