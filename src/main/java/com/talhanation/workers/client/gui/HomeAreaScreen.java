package com.talhanation.workers.client.gui;

import com.talhanation.recruits.client.gui.widgets.BlackShowingTextField;
import com.talhanation.workers.WorkersMain;
import com.talhanation.workers.entities.workarea.HomeArea;
import com.talhanation.workers.network.MessageUpdateHomeArea;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.widget.ExtendedButton;

public class HomeAreaScreen extends WorkAreaScreen {
    private static final MutableComponent TEXT_RESIDENT   = Component.translatable("gui.workers.text.resident");
    private static final MutableComponent TEXT_FREE       = Component.translatable("gui.workers.text.free");
    private static final MutableComponent TEXT_EVICT      = Component.translatable("gui.workers.text.evict");

    public final HomeArea homeArea;
    private Button evictButton;
    private BlackShowingTextField residentShowField;
    private boolean occupied;
    public HomeAreaScreen(HomeArea homeArea, Player player) {
        super(homeArea.getCustomName(), homeArea, player);
        this.homeArea = homeArea;
    }

    @Override
    protected void init() {
        super.init();
        this.occupied = ! homeArea.getResidentName().isEmpty();
        setButtons();
    }
    
    @Override
    public void setButtons() {
        super.setButtons();

        int buttonWidth  = 120;
        int buttonHeight = 20;

        int fieldX = x - buttonWidth / 2;
        int fieldY = y + buttonHeight / 2 - buttonHeight + 50;

        MutableComponent residentDisplay = this.occupied
                ? TEXT_RESIDENT.copy().append(Component.literal(": ").append(Component.literal(homeArea.getResidentName()))).withStyle(ChatFormatting.WHITE)
                : TEXT_FREE.copy().withStyle(ChatFormatting.GREEN);

        residentShowField = new BlackShowingTextField(fieldX, fieldY, buttonWidth, buttonHeight, residentDisplay);
        addRenderableWidget(residentShowField);

        evictButton = addRenderableWidget(new ExtendedButton(fieldX, fieldY + 20, buttonWidth, buttonHeight, TEXT_EVICT,
                btn -> {
                    WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateHomeArea(homeArea.getUUID(), homeArea.getTeamAccess(), true));
                    this.onClose();
                }
        ));


    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);


    }
}
