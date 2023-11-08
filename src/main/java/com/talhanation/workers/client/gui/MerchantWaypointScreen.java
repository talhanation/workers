package com.talhanation.workers.client.gui;

import com.talhanation.workers.Main;
import com.talhanation.workers.Translatable;
import com.talhanation.workers.entities.MerchantEntity;
import com.talhanation.workers.inventory.MerchantWaypointContainer;
import com.talhanation.workers.network.*;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.widget.ExtendedButton;

import java.util.List;

public class MerchantWaypointScreen extends ScreenBase<MerchantWaypointContainer> {

    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(Main.MOD_ID,"textures/gui/waypoint_list_gui.png");
    private int leftPos;
    private int topPos;
    private int page = 1;
    public static List<BlockPos> waypoints;
    public static List<ItemStack> waypointItems;
    private final MerchantEntity merchant;
    private final Player player;
    private int returnTime;
    public static boolean isStarted;
    public static boolean isReturning;
    public static boolean isStopped;
    public boolean autoStart;
    public boolean sendInfo;

    public int travelSpeedState;
    private static final int fontColor = 4210752;
    public MerchantWaypointScreen(MerchantWaypointContainer container, Inventory playerInventory, Component title) {
        super(RESOURCE_LOCATION, container, playerInventory, Component.literal(""));
        imageWidth = 211;
        imageHeight = 250;
        this.merchant = (MerchantEntity) container.getWorkerEntity();
        this.player = playerInventory.player;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        this.returnTime = merchant.getReturningTime();
        this.autoStart = merchant.getAutoStartTravel();
        this.sendInfo = merchant.getSendInfo();
        this.travelSpeedState = merchant.getTravelSpeedState();
        this.setButtons();
    }
    private void setButtons(){
        this.clearWidgets();

        this.setStartButtons();
        this.setPageButtons();
        this.setWaypointButtons();
        this.setDaysButtons();

        String s = "";
        switch (travelSpeedState){
            case 0 -> s = "Slow";
            case 1 -> s = "Medium";
            case 2 -> s = "Fast";
        }
        this.createSetTravelSpeedButton(s);

        String ss = autoStart ? "True" : "False";
        this.createSetAutoStartTravelButton(ss);

        String sss = sendInfo ? "True" : "False";
        this.createSetSendInfoButton(sss);
    }

    private void createSetTravelSpeedButton(String s) {
        ExtendedButton extendedButton = addRenderableWidget(new ExtendedButton(leftPos + 240, topPos + 32, 40, 20, Component.literal(s), button -> {
            travelSpeedState++;
            if(travelSpeedState > 2) travelSpeedState = 0;

            Main.SIMPLE_CHANNEL.sendToServer(new MessageMerchantSetTravelSpeed(merchant.getUUID(), travelSpeedState));
            this.setButtons();
        }));

        extendedButton.setTooltip(Tooltip.create(Translatable.TOOLTIP_TRAVEL_SPEED));
    }

    private void createSetAutoStartTravelButton(String ss) {
        ExtendedButton extendedButton = addRenderableWidget(new ExtendedButton(leftPos + 240, topPos + 57, 40, 20, Component.literal(ss), button -> {
            this.autoStart = !autoStart;

            Main.SIMPLE_CHANNEL.sendToServer(new MessageMerchantSetAutoStartTravel(merchant.getUUID(), autoStart));
            this.setButtons();
        }));

        extendedButton.setTooltip(Tooltip.create(Translatable.TOOLTIP_AUTO_START_TRAVEL));
    }

    private void createSetSendInfoButton(String ss) {
        ExtendedButton sendInfoButton = addRenderableWidget(new ExtendedButton(leftPos + 240, topPos + 82, 40, 20, Component.literal(ss), button -> {
            this.sendInfo = !sendInfo;

            Main.SIMPLE_CHANNEL.sendToServer(new MessageMerchantSetSendInfo(merchant.getUUID(), sendInfo));
            this.setButtons();
        }));

        sendInfoButton.setTooltip(Tooltip.create(Translatable.TOOLTIP_SEND_INFO));
    }

    public void setStartButtons(){
        //START BUTTONS//

        Button start = createTravelStartButton();
        start.active = true;//!isStarted;

        Button stop = createTravelStopButton();
        stop.active = true;//isStarted;

        Button returnButton = createTravelReturnButton();
        returnButton.active = true;//!isStarted;
    }

    public void setPageButtons() {
        Button pageForwardButton = createPageForwardButton();
        pageForwardButton.active = waypoints.size() > 9;

        Button pageBackButton = createPageBackButton();
        pageBackButton.active = page != 1;
    }

    public void setWaypointButtons() {
        Button addButton = createAddWaypointButton(this.leftPos + 171, this.topPos + 32);
        Button removeButton = createRemoveWaypointButton(this.leftPos + 148, this.topPos + 32);
        addButton.active =  true;//!isStarted && !isReturning;
        removeButton.active = true;//!waypoints.isEmpty() && !isStarted && !isReturning;

        Button info = createTravelInfoButton(this.leftPos + 215, this.topPos + 32);
    }

    public void setDaysButtons(){
        // ReturnTime
        ExtendedButton addButton =
                addRenderableWidget(new ExtendedButton(leftPos + 186, topPos + 8, 9, 9, Component.literal("+"), button -> {
                    this.returnTime = merchant.getReturningTime();

                    if(player.isShiftKeyDown()) returnTime = returnTime + 5;
                    else returnTime++;

                    Main.SIMPLE_CHANNEL.sendToServer(new MessageMerchantReturnTime(this.returnTime, merchant.getUUID()));
                    this.setButtons();
                }));
        ExtendedButton removeButton =
                addRenderableWidget(new ExtendedButton(leftPos + 175, topPos + 8, 9, 9, Component.literal("-"), button -> {
                    this.returnTime = merchant.getReturningTime();

                    if(player.isShiftKeyDown()) returnTime = returnTime - 5;
                    else returnTime--;

                    if(returnTime < 0) returnTime = 0;

                    Main.SIMPLE_CHANNEL.sendToServer(new MessageMerchantReturnTime(this.returnTime, merchant.getUUID()));
                    this.setButtons();
                }));

        removeButton.active = returnTime > 0;
    }

    private ExtendedButton createTravelStartButton() {
        ExtendedButton extendedButton = addRenderableWidget(new ExtendedButton(leftPos + 19, topPos + 32, 40, 20, Translatable.TEXT_BUTTON_TRAVEL_START, button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageMerchantTravel(merchant.getUUID(),  true, false));
            this.setButtons();
        }));
        extendedButton.setTooltip(Tooltip.create(Translatable.TOOLTIP_TRAVEL_START));
        return extendedButton;
    }

    private ExtendedButton createTravelStopButton() {
        ExtendedButton extendedButton = addRenderableWidget(new ExtendedButton(leftPos + 62, topPos + 32, 40, 20, Translatable.TEXT_BUTTON_TRAVEL_STOP, button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageMerchantTravel(merchant.getUUID(),  false, false));
            this.setButtons();
        }));
        extendedButton.setTooltip(Tooltip.create(Translatable.TOOLTIP_TRAVEL_STOP));
        return extendedButton;
    }

    private ExtendedButton createTravelReturnButton() {
        ExtendedButton extendedButton = addRenderableWidget(new ExtendedButton(leftPos + 105, topPos + 32, 40, 20, Translatable.TEXT_BUTTON_TRAVEL_RETURN, button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageMerchantTravel(merchant.getUUID(), true, true));
            this.setButtons();
        }));

        extendedButton.setTooltip(Tooltip.create(Translatable.TOOLTIP_TRAVEL_RETURN));
        return extendedButton;
    }

    private ExtendedButton createTravelInfoButton(int x, int y) {
        ExtendedButton extendedButton = addRenderableWidget(new ExtendedButton(x, y, 20, 20, Component.literal("i"), press -> {
            this.setButtons();
            this.player.sendSystemMessage(Translatable.TEXT_TRAVEL_INFO);
            this.onClose();
        }));

        extendedButton.setTooltip(Tooltip.create(Translatable.TOOLTIP_TRAVEL_INFO));
        return extendedButton;
    }


    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        // Info
        int waypointsPerPage = 10;
        this.returnTime = merchant.getReturningTime();
        int currentReturningTime = merchant.getCurrentReturningTime();

        guiGraphics.drawString(font, "Days:", 20, 9, fontColor, false);
        guiGraphics.drawString(font, "" + currentReturningTime + " / " + merchant.getReturningTime(), 110, 9, fontColor, false);

        int currentWayPoint = waypoints.size() == 0 ? 0 : merchant.getCurrentWayPointIndex() + 1;

        guiGraphics.drawString(font, "Waypoints:", 20, 20, fontColor, false);
        guiGraphics.drawString(font, "" + currentWayPoint + " / " + waypoints.size(), 110, 20, fontColor, false);

        int startIndex = (page - 1) * waypointsPerPage;
        int endIndex = Math.min(startIndex + waypointsPerPage, waypoints.size());

        if (!waypoints.isEmpty()) {
            for (int i = startIndex; i < endIndex; i++) {
                BlockPos pos = waypoints.get(i);

                int x = pos.getX();
                int y = pos.getY();
                int z = pos.getZ();



                String coordinates = String.format("%d:  (%d,  %d,  %d)", i + 1, x, y, z);

                if(!waypointItems.isEmpty() && waypointItems.get(i) != null) renderItemAt(guiGraphics, waypointItems.get(i), 15, 58 + ((i - startIndex) * 17)); // Adjust the Y position here
                else{
                    BlockPos pos1 =  waypoints.get(i);
                    ItemStack itemStack = merchant.getItemStackToRender(pos1);

                    renderItemAt(guiGraphics, itemStack, 15, 58 + ((i - startIndex) * 17));
                }
                guiGraphics.drawString(font, coordinates, 35, 60 + ((i - startIndex) * 17), fontColor, false);
            }

            if (waypoints.size() > waypointsPerPage)
                guiGraphics.drawString(font, "Page: " + page, 90, 230, fontColor, false);
        }

    }

    private void renderItemAt(GuiGraphics guiGraphics, ItemStack itemStack, int x, int y) {
        if(itemStack != null) guiGraphics.renderFakeItem(itemStack, x, y);
    }

    public ExtendedButton createPageBackButton() {
        return addRenderableWidget(new ExtendedButton(leftPos + 15, topPos + 230, 12, 12, Component.literal("<"),
                button -> {
                    if(this.page > 1) page--;
                    this.setButtons();
                }
        ));
    }

    public Button createPageForwardButton() {
        return addRenderableWidget(new ExtendedButton(leftPos + 184, topPos + 230, 12, 12, Component.literal(">"),
                button -> {
                    page++;
                    this.setButtons();
                }
        ));
    }

    private Button createAddWaypointButton(int x, int y){
        return addRenderableWidget(new ExtendedButton(x, y, 20, 20, Component.literal("+"),
                button -> {
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageMerchantAddWayPoint(merchant.getUUID()));

                    this.setButtons();
                }
        ));
    }

    private Button createRemoveWaypointButton(int x, int y){
        return addRenderableWidget(new ExtendedButton(x, y, 20, 20, Component.literal("-"),
                button -> {
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageMerchantRemoveWayPoint(merchant.getUUID()));

                    this.setButtons();
                }
        ));
    }
}
