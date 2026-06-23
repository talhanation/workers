package com.talhanation.workers.client.gui;

import com.talhanation.recruits.client.gui.widgets.BlackShowingTextField;
import com.talhanation.recruits.client.gui.widgets.DropDownMenu;
import com.talhanation.recruits.client.gui.widgets.RecruitsCheckBox;
import com.talhanation.recruits.client.gui.widgets.ScrollDropDownMenu;
import com.talhanation.workers.WorkersMain;
import com.talhanation.workers.client.gui.widgets.ItemScrollDropDownMenu;
import com.talhanation.workers.entities.workarea.MiningArea;
import com.talhanation.workers.entities.workarea.MiningArea.MiningMode;
import com.talhanation.workers.network.MessageUpdateMiningArea;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.client.gui.widget.ExtendedButton;

import java.util.List;
import java.util.UUID;

public class MiningAreaScreen extends WorkAreaScreen {
    private static final MutableComponent TEXT_CLOSE_FLOOR = Component.translatable("gui.workers.checkbox.closeFloor");
    private static final MutableComponent TEXT_CLOSE_FLUIDS = Component.translatable("gui.workers.checkbox.closeFluids");
    private static final MutableComponent TEXT_MINE_WALL_ORES = Component.translatable("gui.workers.checkbox.mineWallOres");
    public final MiningArea miningArea;
    public Button xSizePlusButton;
    public Button xSizeMinusButton;
    public Button ySizePlusButton;
    public Button ySizeMinusButton;
    public Button zSizePlusButton;
    public Button zSizeMinusButton;
    public int areaXSize;
    public int areaYSize;
    public int areaZSize;
    public int areaYOffset;
    private RecruitsCheckBox closeFloorCheckBox;
    private boolean closeFloor;
    private RecruitsCheckBox closeFluidsCheckBox;
    private boolean closeFluids;
    private RecruitsCheckBox mineWallOresCheckBox;
    private boolean mineWallOres;
    private ItemScrollDropDownMenu fillItemDropDown;
    private ItemStack fillItem = new ItemStack(Blocks.COBBLESTONE);
    private ScrollDropDownMenu<MiningMode> modeDropDown;
    private MiningMode mode;
    public MiningAreaScreen(MiningArea miningArea, Player player) {
        super(miningArea.getCustomName(), miningArea, player);
        this.miningArea = miningArea;
    }

    @Override
    protected void init() {
        super.init();
        this.areaXSize = miningArea.getWidthSize();
        this.areaYSize = miningArea.getHeightSize();
        this.areaZSize = miningArea.getDepthSize();
        this.areaYOffset = miningArea.getHeightOffset();
        this.closeFloor = miningArea.getCloseFloor();
        this.closeFluids = miningArea.getCloseFluids();
        this.mineWallOres = miningArea.getMineWallOres();
        this.fillItem = miningArea.getFillItem();
        this.mode = miningArea.getMode();

        this.setButtons();
    }

    @Override
    public void tick() {
        super.tick();

    }

    @Override
    public void setButtons() {
        super.setButtons();
        int previewHeight = 100;
        int boxWidth = 120;
        int boxHeight = 20;

        int sizeButtonX = 120;
        int sizeButtonY = 130;
        addRenderableWidget(new BlackShowingTextField(x - boxWidth/2, y - previewHeight / 2 + 130, boxWidth, boxHeight, Component.literal("x: " + areaXSize)));
        addRenderableWidget(new BlackShowingTextField(x - boxWidth/2, y - previewHeight / 2 + 130 + boxHeight, boxWidth, boxHeight, Component.literal( "y: " + areaYSize)));
        addRenderableWidget(new BlackShowingTextField(x - boxWidth/2, y - previewHeight / 2 + 130 + boxHeight*2, boxWidth, boxHeight, Component.literal( "z: " + areaZSize)));


        xSizePlusButton = addRenderableWidget(new ExtendedButton(x - boxWidth/2 + sizeButtonX, y - previewHeight / 2 + sizeButtonY, 20, 20, Component.literal("+"),
                btn -> {
                    int step = hasShiftDown() ? 5 : 1;
                    areaXSize += step;
                    areaXSize = Mth.clamp(areaXSize, 1, 16);

                    // In STAIRS mode width (z) is fixed; height (y) grows together with x.
                    // x is bound to the same 2..8 range so the stepped profile stays square.
                    if(isStairsMode(mode)){
                        areaXSize = Mth.clamp(areaXSize, 2, 16);
                        areaYSize = areaXSize;
                    }

                    this.sendMessage();
                    this.setButtons();
                }
        ));

        xSizeMinusButton = addRenderableWidget(new ExtendedButton(x - boxWidth/2 + sizeButtonX + 20, y - previewHeight / 2 + sizeButtonY, 20, 20, Component.literal("-"),
                btn -> {
                    int step = hasShiftDown() ? 5 : 1;
                    areaXSize -= step;
                    areaXSize = Mth.clamp(areaXSize, 1, 16);

                    // In STAIRS mode width (z) is fixed; height (y) shrinks together with x.
                    // x is bound to the same 2..8 range so the stepped profile stays square.
                    if(isStairsMode(mode)){
                        areaXSize = Mth.clamp(areaXSize, 2, 16);
                        areaYSize = areaXSize;
                    }

                    this.miningArea.setWidthSize(areaXSize);
                    this.sendMessage();
                    this.setButtons();
                }
        ));

        ySizePlusButton = addRenderableWidget(new ExtendedButton(x - boxWidth/2 + sizeButtonX, y - previewHeight / 2 + sizeButtonY + 20, 20, 20, Component.literal("+"),
                btn -> {
                    if(hasShiftDown()) areaYSize += 5;
                    else areaYSize++;
                    areaYSize = Mth.clamp(areaYSize, 2, 8);

                    this.miningArea.setHeightSize(areaYSize);
                    this.sendMessage();
                    this.setButtons();
                }
        ));

        ySizeMinusButton = addRenderableWidget(new ExtendedButton(x - boxWidth/2 + sizeButtonX + 20, y - previewHeight / 2 + sizeButtonY + 20, 20, 20, Component.literal("-"),
                btn -> {
                    if(hasShiftDown()) areaYSize -= 5;
                    else areaYSize--;
                    areaYSize = Mth.clamp(areaYSize, 2, 8);

                    this.miningArea.setHeightSize(areaYSize);
                    this.sendMessage();
                    this.setButtons();
                }
        ));

        zSizePlusButton = addRenderableWidget(new ExtendedButton(x - boxWidth/2 + sizeButtonX, y - previewHeight / 2 + sizeButtonY + 40, 20, 20, Component.literal("+"),
                btn -> {
                    if(hasShiftDown()) areaZSize += 5;
                    else areaZSize++;
                    areaZSize = Mth.clamp(areaZSize, 1, 16);

                    this.miningArea.setDepthSize(areaZSize);
                    this.sendMessage();
                    this.setButtons();
                }
        ));

        zSizeMinusButton = addRenderableWidget(new ExtendedButton(x - boxWidth/2 + sizeButtonX + 20, y - previewHeight / 2 + sizeButtonY + 40, 20, 20, Component.literal("-"),
                btn -> {
                    if(hasShiftDown()) areaZSize -= 5;
                    else areaZSize--;
                    areaZSize = Mth.clamp(areaZSize, 1, 16);

                    this.miningArea.setDepthSize(areaZSize);
                    this.sendMessage();
                    this.setButtons();
                }
        ));

        int checkBoxY = y - previewHeight / 2 + 155 + boxHeight*2;
        this.closeFloorCheckBox = new RecruitsCheckBox(x - boxWidth/2, checkBoxY, boxWidth, boxHeight, TEXT_CLOSE_FLOOR,
                this.closeFloor,
                (bool) -> {
                    this.closeFloor = bool;
                    // Show the fill-item picker only while close floor is active.
                    if(this.fillItemDropDown != null) this.fillItemDropDown.visible = bool;
                    this.sendMessage();
                }
        );
        addRenderableWidget(closeFloorCheckBox);

        // Fill-item picker shown next to the close-floor checkbox. Default cobblestone,
        // plus other common, cheap blocks. Only visible while close floor is on.
        List<ItemStack> fillOptions = List.of(
                new ItemStack(Blocks.COBBLESTONE),
                new ItemStack(Blocks.DIRT),
                new ItemStack(Blocks.DEEPSLATE),
                new ItemStack(Blocks.COBBLED_DEEPSLATE),
                new ItemStack(Blocks.STONE),
                new ItemStack(Blocks.SANDSTONE),
                new ItemStack(Blocks.NETHERRACK)
        );
        int fillDropWidth = boxWidth / 2;
        this.fillItemDropDown = new ItemScrollDropDownMenu(this.fillItem, x - boxWidth/2 + boxWidth + 4, checkBoxY, fillDropWidth, boxHeight, fillOptions,
                (stack) -> {
                    this.fillItem = stack;
                    this.sendMessage();
                }
        );
        this.fillItemDropDown.visible = this.closeFloor;
        addRenderableWidget(fillItemDropDown);

        this.closeFluidsCheckBox = new RecruitsCheckBox(x - boxWidth/2, checkBoxY + boxHeight, boxWidth, boxHeight, TEXT_CLOSE_FLUIDS,
                this.closeFluids,
                (bool) -> {
                    this.closeFluids = bool;
                    this.sendMessage();
                }
        );
        addRenderableWidget(closeFluidsCheckBox);

        this.mineWallOresCheckBox = new RecruitsCheckBox(x - boxWidth/2, checkBoxY + boxHeight*2, boxWidth, boxHeight, TEXT_MINE_WALL_ORES,
                this.mineWallOres,
                (bool) -> {
                    this.mineWallOres = bool;
                    this.sendMessage();
                }
        );
        addRenderableWidget(mineWallOresCheckBox);

        this.modeDropDown = new ScrollDropDownMenu<>(
                this.mode,
                x - boxWidth/2, y - previewHeight / 2 + 90, boxWidth, boxHeight,
                List.of(MiningMode.values()),
                m -> Component.translatable(m.getTranslationKey()).getString(),
                m -> {
                    this.mode = m;

                    if(isStairsMode(m)){
                        areaXSize = 3;
                        areaYSize = 3;
                        areaZSize = 3;
                        this.closeFloor = false;
                        this.closeFluids = true;
                        this.mineWallOres = false;
                    }

                    this.sendMessage();
                    this.setButtons();
                }
        );
        modeDropDown.setBgFillSelected(FastColor.ARGB32.color(255, 139, 139, 139));
        addRenderableWidget(modeDropDown);

        boolean stairs = isStairsMode(mode);

        ySizePlusButton.active = !stairs;
        ySizeMinusButton.active = !stairs;
        zSizePlusButton.active = !stairs;
        zSizeMinusButton.active = !stairs;
    }
    public void sendMessage(){
        if(miningArea == null) return;

        this.miningArea.setWidthSize(areaXSize);
        WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateMiningArea(this.miningArea.getUUID(), areaXSize, areaYSize, areaZSize, areaYOffset, closeFloor, closeFluids, mineWallOres, fillItem, mode.getIndex()));
    }
    @Override
    public void mouseMoved(double x, double y) {
        if(modeDropDown != null){
            modeDropDown.onMouseMove(x,y);
        }
        if(fillItemDropDown != null){
            fillItemDropDown.onMouseMove(x,y);
        }
        super.mouseMoved(x, y);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (modeDropDown != null && modeDropDown.isMouseOver(mouseX, mouseY)) {
            modeDropDown.onMouseClick(mouseX, mouseY);
            return true;
        }
        if (fillItemDropDown != null && fillItemDropDown.isMouseOver(mouseX, mouseY)) {
            fillItemDropDown.onMouseClick(mouseX, mouseY);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    @Override
    public boolean mouseScrolled(double x, double y, double d) {
        if(modeDropDown != null) modeDropDown.mouseScrolled(x,y,d);
        if(fillItemDropDown != null) fillItemDropDown.mouseScrolled(x,y,d);
        return super.mouseScrolled(x, y, d);
    }
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        if (modeDropDown != null) {
            modeDropDown.renderWidget(guiGraphics, mouseX, mouseY, partialTicks);
        }
        if (fillItemDropDown != null) {
            fillItemDropDown.renderWidget(guiGraphics, mouseX, mouseY, partialTicks);
        }
    }

    @Override
    public void onAreaMoved() {

    }

    private static boolean isStairsMode(MiningMode mode) {
        return mode == MiningMode.STAIRS_DOWN || mode == MiningMode.STAIRS_UP;
    }
}