package com.talhanation.workers.client.gui;

import com.talhanation.recruits.client.gui.RecruitsScreenBase;
import com.talhanation.recruits.client.gui.player.PlayersList;
import com.talhanation.recruits.client.gui.player.SelectPlayerScreen;
import com.talhanation.recruits.client.gui.widgets.RecruitsCheckBox;
import com.talhanation.recruits.client.gui.widgets.SelectedPlayerWidget;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import com.talhanation.workers.WorkersMain;
import com.talhanation.workers.entities.workarea.AbstractWorkAreaEntity;
import com.talhanation.workers.network.MessageRotateWorkArea;
import com.talhanation.workers.network.MessageUpdateOwner;
import com.talhanation.workers.network.MessageUpdateWorkArea;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
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
    private static final MutableComponent TEXT_TEAM_ACCESS = Component.translatable("gui.workers.checkbox.access");
    private static final MutableComponent TOOLTIP_TEAM_ACCESS = Component.translatable("gui.workers.checkbox.tooltip.access");
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

    public SelectedPlayerWidget selectedPlayerWidget;
    public RecruitsPlayerInfo playerInfo;
    public RecruitsCheckBox teamAccessCheckBox;
    public Button selectedPlayerButton;
    public boolean teamAccess;

    protected WorkAreaScreen(Component title, AbstractWorkAreaEntity workArea, Player player) {
        super(title, 200, 222);
        this.workArea = workArea;
        this.player = player;
    }

    @Override
    protected void init() {
        super.init();
        workArea.showBox = true;
        playerInfo = new RecruitsPlayerInfo(workArea.getPlayerUUID(), workArea.getPlayerName());
        teamAccess = workArea.getTeamAccess();
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
                WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateWorkArea(this.workArea.getUUID(), this.workArea.getCustomName().getString(), newPos, false, teamAccess));
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
                        WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateWorkArea(this.workArea.getUUID(), this.workArea.getCustomName().getString(), newPos, false, teamAccess));
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
                WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateWorkArea(this.workArea.getUUID(), this.workArea.getCustomName().getString(), newPos, false, teamAccess));
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
                WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateWorkArea(this.workArea.getUUID(), this.workArea.getCustomName().getString(), newPos, false, teamAccess));
                this.onAreaMoved();
            }
        ));

        // Destroy
        destroy = addRenderableWidget(new ExtendedButton(x - buttonWidth / 2, y - buttonHeight / 2, buttonWidth, buttonHeight, TEXT_DESTROY,
            btn -> {
                WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateWorkArea(this.workArea.getUUID(), this.workArea.getCustomName().getString(), workArea.position(), true, teamAccess));
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


        this.selectedPlayerButton = new ExtendedButton(x + 80, y - 50 , 120, 20, SelectPlayerScreen.TITLE,
            button -> {
                minecraft.setScreen(new SelectPlayerScreen(this, player, SelectPlayerScreen.TITLE, SelectPlayerScreen.BUTTON_SELECT, SelectPlayerScreen.BUTTON_SELECT_TOOLTIP, false, PlayersList.FilterType.NONE,
                    (playerInfo) -> {
                        this.playerInfo = playerInfo;
                        WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateOwner(this.workArea.getUUID(), playerInfo));
                        this.workArea.setPlayerName(playerInfo.getName());
                        this.workArea.setPlayerUUID(playerInfo.getUUID());
                        minecraft.setScreen(this);
                        this.onClose();
                    }
                ));
            }
        );
        addRenderableWidget(this.selectedPlayerButton);

        this.selectedPlayerWidget = new SelectedPlayerWidget(font, x + 80, y - 50, 120, 20, Component.literal("x"), // Button label
                () -> {
                    playerInfo = null;
                    this.selectedPlayerWidget.setPlayer(null, null);
                    this.selectedPlayerWidget.visible = false;
                    this.selectedPlayerButton.visible = true;
                }
        );

        this.selectedPlayerWidget.setPlayer(playerInfo.getUUID(), playerInfo.getName());
        addRenderableWidget(this.selectedPlayerWidget);

        //OWNER STUFF
        if(playerInfo != null){
            this.selectedPlayerWidget.visible = true;
            this.selectedPlayerButton.visible = false;

            this.teamAccessCheckBox = new RecruitsCheckBox(x + 80, y - 30, 120, 20, TEXT_TEAM_ACCESS, teamAccess,
                b ->{
                    teamAccess = b;
                    WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateWorkArea(this.workArea.getUUID(), this.workArea.getCustomName().getString(), workArea.position(), false, teamAccess));
                }
            );
            this.teamAccessCheckBox.setTooltip(Tooltip.create(TOOLTIP_TEAM_ACCESS));
            addRenderableWidget(this.teamAccessCheckBox);
        }
        else {
            this.selectedPlayerWidget.visible = false;
            this.selectedPlayerButton.visible = true;
        }
    }

    public void onAreaMoved() {}

    public void onClose() {
        super.onClose();

        this.workArea.showBox = false;
    }
}
