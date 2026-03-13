package com.talhanation.workers.client.gui;

import com.talhanation.recruits.client.gui.RecruitsScreenBase;
import com.talhanation.workers.WorkersMain;
import com.talhanation.workers.entities.workarea.AbstractWorkAreaEntity;
import com.talhanation.workers.network.MessageRotateWorkArea;
import com.talhanation.workers.network.MessageUpdateWorkArea;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.gui.widget.ExtendedButton;

public abstract class WorkAreaScreen extends RecruitsScreenBase {

    private static final ResourceLocation TEXTURE = new ResourceLocation(WorkersMain.MOD_ID, "textures/gui/workareascreen.png");

    private static final MutableComponent TEXT_FORWARD = Component.translatable("gui.workers.command.text.forward");
    private static final MutableComponent TEXT_BACKWARD = Component.translatable("gui.workers.command.text.back");
    private static final MutableComponent TEXT_LEFT = Component.translatable("gui.workers.command.text.left");
    private static final MutableComponent TEXT_RIGHT = Component.translatable("gui.workers.command.text.right");
    private static final MutableComponent TEXT_DESTROY = Component.translatable("gui.workers.command.text.destroy");
    private EditBox textFieldName;
    private Button moveForward;
    private Button moveBackward;
    private Button moveLeft;
    private Button moveRight;
    private Button destroy;
    private Button rotateLeft;
    private Button rotateRight;

    public Player player;
    public AbstractWorkAreaEntity workArea;

    protected WorkAreaScreen(Component title, AbstractWorkAreaEntity workArea, Player player) {
        super(title, 200, 222);
        this.workArea = workArea;
        this.player = player;
    }

    @Override
    protected void init() {
        super.init();
        workArea.showBox = true;
        setButtons();
    }
    public int x;
    public int y;
    public void setButtons(){
        clearWidgets();
        x = this.width / 2;
        y = this.height / 2 - 70;
        int buttonWidth = 80;
        int buttonHeight = 20;

        // Move Forward
        moveForward = addRenderableWidget(new ExtendedButton(x - buttonWidth / 2, y + buttonHeight / 2 - buttonHeight*2, buttonWidth, buttonHeight, TEXT_FORWARD,
                btn -> {
                    this.workArea.showBox = true;
                    int x = 1;
                    if(hasShiftDown()){
                        x = 5;
                    }
                    Vec3 newPos = workArea.position().relative(player.getDirection(), x);
                    WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateWorkArea(this.workArea.getUUID(), this.workArea.getCustomName().getString(), newPos, false));
                    this.onAreaMoved();
                }
        ));

        // Move Backward
        moveBackward = addRenderableWidget(new ExtendedButton(x - buttonWidth / 2, y - buttonHeight / 2 + buttonHeight, buttonWidth, buttonHeight, TEXT_BACKWARD,
                btn -> {
                    this.workArea.showBox = true;
                    int x = 1;
                    if(hasShiftDown()){
                        x = 5;
                    }
                    Vec3 newPos = workArea.position().relative(player.getDirection().getOpposite(), x);
                    WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateWorkArea(this.workArea.getUUID(), this.workArea.getCustomName().getString(), newPos, false));
                    this.onAreaMoved();
                }
        ));

        // Move Left
        moveLeft = addRenderableWidget(new ExtendedButton(x - buttonWidth / 2 - buttonWidth, y - buttonHeight / 2, buttonWidth, buttonHeight, TEXT_LEFT,
                btn -> {
                    this.workArea.showBox = true;
                    int x = 1;
                    if(hasShiftDown()){
                        x = 5;
                    }
                    Vec3 newPos = workArea.position().relative(player.getDirection().getCounterClockWise(), x);
                    WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateWorkArea(this.workArea.getUUID(), this.workArea.getCustomName().getString(), newPos, false));
                    this.onAreaMoved();
                }
        ));

        // Move Right
        moveRight = addRenderableWidget(new ExtendedButton(x - buttonWidth / 2 + buttonWidth, y - buttonHeight / 2, buttonWidth, buttonHeight,  TEXT_RIGHT,
                btn -> {
                    this.workArea.showBox = true;
                    int x = 1;
                    if(hasShiftDown()){
                        x = 5;
                    }
                    Vec3 newPos = workArea.position().relative(player.getDirection().getClockWise(), x);
                    WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateWorkArea(this.workArea.getUUID(), this.workArea.getCustomName().getString(), newPos, false));
                    this.onAreaMoved();
                }
        ));

        // Destroy
        destroy = addRenderableWidget(new ExtendedButton(x - buttonWidth / 2, y - buttonHeight / 2, buttonWidth, buttonHeight, TEXT_DESTROY,
                btn -> {
                    WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateWorkArea(this.workArea.getUUID(), this.workArea.getCustomName().getString(), workArea.position(), true));
                    this.onClose();
                }
        ));

        rotateLeft = addRenderableWidget(new ExtendedButton(x - buttonWidth / 2 - buttonWidth, y - buttonHeight / 2 + buttonHeight, buttonWidth, buttonHeight, Component.literal("\u21BB"),
                btn -> {
                    this.workArea.showBox = true;
                    WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageRotateWorkArea(this.workArea.getUUID(), false));
                    this.workArea.setFacing(this.workArea.getFacing().getCounterClockWise());
                    this.onAreaMoved();
                }
        ));

        rotateRight = addRenderableWidget(new ExtendedButton(x - buttonWidth / 2 + buttonWidth, y - buttonHeight / 2 + buttonHeight, buttonWidth, buttonHeight, Component.literal("\u21BA"),
                btn -> {
                    this.workArea.showBox = true;
                    WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageRotateWorkArea(this.workArea.getUUID(), true));
                    this.workArea.setFacing(this.workArea.getFacing().getClockWise());
                    this.onAreaMoved();
                }
        ));
    }

    public void onAreaMoved() {}

    public void onClose() {
        super.onClose();

        this.workArea.showBox = false;
    }
}
