package com.talhanation.workers.client.gui;

import com.talhanation.recruits.client.gui.widgets.BlackShowingTextField;
import com.talhanation.recruits.client.gui.widgets.DropDownMenu;
import com.talhanation.workers.Main;
import com.talhanation.workers.entities.workarea.MiningArea;
import com.talhanation.workers.network.MessageUpdateMiningArea;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.widget.ExtendedButton;

import java.util.List;
import java.util.UUID;

public class MiningAreaScreen extends WorkAreaScreen {

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
    public MiningAreaScreen(MiningArea miningArea, Player player) {
        super(miningArea.getCustomName(), miningArea, player);
        this.miningArea = miningArea;
    }

    @Override
    protected void init() {
        this.areaXSize = miningArea.getWidthSize();
        this.areaYSize = miningArea.getHeightSize();
        this.areaZSize = miningArea.getDepthSize();
        this.areaYOffset = miningArea.getHeightOffset();

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
                    if(hasShiftDown()) areaXSize += 5;
                    else areaXSize++;
                    areaXSize = Mth.clamp(areaXSize, 1, 16);

                    this.miningArea.setWidthSize(areaXSize);
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageUpdateMiningArea(this.miningArea.getUUID(), areaXSize, areaYSize, areaZSize, areaYOffset));
                    this.setButtons();
                }
        ));

        xSizeMinusButton = addRenderableWidget(new ExtendedButton(x - boxWidth/2 + sizeButtonX + 20, y - previewHeight / 2 + sizeButtonY, 20, 20, Component.literal("-"),
                btn -> {
                    if(hasShiftDown()) areaXSize -= 5;
                    else areaXSize--;
                    areaXSize = Mth.clamp(areaXSize, 1, 16);

                    this.miningArea.setWidthSize(areaXSize);
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageUpdateMiningArea(this.miningArea.getUUID(), areaXSize, areaYSize, areaZSize, areaYOffset));
                    this.setButtons();
                }
        ));

        ySizePlusButton = addRenderableWidget(new ExtendedButton(x - boxWidth/2 + sizeButtonX, y - previewHeight / 2 + sizeButtonY + 20, 20, 20, Component.literal("+"),
                btn -> {
                    if(hasShiftDown()) areaYSize += 5;
                    else areaYSize++;
                    areaYSize = Mth.clamp(areaYSize, 2, 8);

                    this.miningArea.setHeightSize(areaYSize);
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageUpdateMiningArea(this.miningArea.getUUID(), areaXSize, areaYSize, areaZSize, areaYOffset));
                    this.setButtons();
                }
        ));

        ySizeMinusButton = addRenderableWidget(new ExtendedButton(x - boxWidth/2 + sizeButtonX + 20, y - previewHeight / 2 + sizeButtonY + 20, 20, 20, Component.literal("-"),
                btn -> {
                    if(hasShiftDown()) areaYSize -= 5;
                    else areaYSize--;
                    areaYSize = Mth.clamp(areaYSize, 2, 8);

                    this.miningArea.setHeightSize(areaYSize);
                    UUID uuid = this.miningArea.getUUID();
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageUpdateMiningArea(uuid, areaXSize, areaYSize, areaZSize, areaYOffset));
                    this.setButtons();
                }
        ));

        zSizePlusButton = addRenderableWidget(new ExtendedButton(x - boxWidth/2 + sizeButtonX, y - previewHeight / 2 + sizeButtonY + 40, 20, 20, Component.literal("+"),
                btn -> {
                    if(hasShiftDown()) areaZSize += 5;
                    else areaZSize++;
                    areaZSize = Mth.clamp(areaZSize, 1, 16);

                    this.miningArea.setDepthSize(areaZSize);
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageUpdateMiningArea(this.miningArea.getUUID(), areaXSize, areaYSize, areaZSize, areaYOffset));
                    this.setButtons();
                }
        ));

        zSizeMinusButton = addRenderableWidget(new ExtendedButton(x - boxWidth/2 + sizeButtonX + 20, y - previewHeight / 2 + sizeButtonY + 40, 20, 20, Component.literal("-"),
                btn -> {
                    if(hasShiftDown()) areaZSize -= 5;
                    else areaZSize--;
                    areaZSize = Mth.clamp(areaZSize, 1, 16);

                    this.miningArea.setDepthSize(areaZSize);
                    UUID uuid = this.miningArea.getUUID();
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageUpdateMiningArea(uuid, areaXSize, areaYSize, areaZSize, areaYOffset));
                    this.setButtons();
                }
        ));
    }

    @Override
    public void mouseMoved(double x, double y) {
        super.mouseMoved(x, y);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseX, mouseY, button);
    }
    @Override
    public boolean mouseScrolled(double x, double y, double d) {
        return super.mouseScrolled(x, y, d);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public void onAreaMoved() {

    }
}
