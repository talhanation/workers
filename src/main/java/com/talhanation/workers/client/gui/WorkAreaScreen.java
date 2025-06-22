package com.talhanation.workers.client.gui;

import com.talhanation.recruits.client.gui.RecruitsScreenBase;
import com.talhanation.workers.Main;
import com.talhanation.workers.entities.workarea.AbstractWorkAreaEntity;
import com.talhanation.workers.network.MessageUpdateWorkArea;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.gui.widget.ExtendedButton;

public abstract class WorkAreaScreen extends RecruitsScreenBase {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Main.MOD_ID, "textures/gui/workareascreen.png");

    private static final MutableComponent TEXT_FORWARD = Component.translatable("gui.workers.command.text.forward");
    private static final MutableComponent TEXT_BACKWARD = Component.translatable("gui.workers.command.text.back");
    private static final MutableComponent TEXT_LEFT = Component.translatable("gui.workers.command.text.left");
    private static final MutableComponent TEXT_RIGHT = Component.translatable("gui.workers.command.text.right");
    private EditBox textFieldName;
    private Button moveForward;
    private Button moveBackward;
    private Button moveLeft;
    private Button moveRight;
    private Button destroy;

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

        setButtons();
    }

    private void setButtons(){
        clearWidgets();

        moveForward = addRenderableWidget(new ExtendedButton(guiLeft + 32, guiTop + ySize - 120 - 7, 70, 20, TEXT_FORWARD,
                btn -> {
                    int amount = Minecraft.getInstance().options.keyShift.isDown() ? 5 : 1;

                    Vec3 newPos = workArea.position().relative(player.getDirection(), amount);
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageUpdateWorkArea(this.workArea.getUUID(), this.workArea.getCustomName().getString(), newPos, false));
                }
        ));
        //moveForward.setTooltip(Tooltip.create(TOOLTIP_TAKE_OWNERSHIP));

        moveBackward = addRenderableWidget(new ExtendedButton(guiLeft + 32, guiTop + ySize - 160 - 7, 70, 20, TEXT_BACKWARD,
                btn -> {
                    int amount = Minecraft.getInstance().options.keyShift.isDown() ? 5 : 1;

                    Vec3 newPos = workArea.position().relative(player.getDirection().getOpposite(), amount);
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageUpdateWorkArea(this.workArea.getUUID(), this.workArea.getCustomName().getString(), newPos, false));
                }
        ));
        //moveBackward.setTooltip(Tooltip.create(TOOLTIP_TAKE_OWNERSHIP));

        moveLeft = addRenderableWidget(new ExtendedButton(guiLeft + 32, guiTop + ySize - 200 - 7, 70, 20, TEXT_LEFT,
                btn -> {
                    int amount = Minecraft.getInstance().options.keyShift.isDown() ? 5 : 1;

                    Vec3 newPos = workArea.position().relative(player.getDirection().getCounterClockWise(), amount);
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageUpdateWorkArea(this.workArea.getUUID(), this.workArea.getCustomName().getString(), newPos, false));
                }
        ));
        //moveLeft.setTooltip(Tooltip.create(TOOLTIP_TAKE_OWNERSHIP));

        moveRight = addRenderableWidget(new ExtendedButton(guiLeft + 32 + 5 + 70, guiTop + ySize - 200 - 7, 70, 20, TEXT_RIGHT,
                btn -> {
                    int amount = Minecraft.getInstance().options.keyShift.isDown() ? 5 : 1;

                    Vec3 newPos = workArea.position().relative(player.getDirection().getClockWise(), amount);
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageUpdateWorkArea(this.workArea.getUUID(), this.workArea.getCustomName().getString(), newPos, false));
                }
        ));
        //moveRight.setTooltip(Tooltip.create(TOOLTIP_TAKE_OWNERSHIP));
    }
}
