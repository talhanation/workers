package com.talhanation.workers.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.workers.Main;
import com.talhanation.workers.Translatable;
import com.talhanation.workers.entities.MerchantEntity;
import com.talhanation.workers.inventory.MerchantWaypointContainer;
import com.talhanation.workers.network.*;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.VideoSettingsScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
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
    public int travelSpeedState;

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
    }

    private void createSetTravelSpeedButton(String s) {
        addRenderableWidget(new Button(leftPos + 240, topPos + 32, 40, 20, Component.literal(s), button -> {
            travelSpeedState++;
            if(travelSpeedState > 2) travelSpeedState = 0;

            Main.SIMPLE_CHANNEL.sendToServer(new MessageMerchantSetTravelSpeed(merchant.getUUID(), travelSpeedState));
            this.setButtons();
        },
            (button1, poseStack, i, i1) -> {
        this.renderTooltip(poseStack, Translatable.TOOLTIP_TRAVEL_SPEED, i, i1);
        }));
    }

    private void createSetAutoStartTravelButton(String ss) {
        addRenderableWidget(new Button(leftPos + 240, topPos + 57, 40, 20, Component.literal(ss), button -> {
            this.autoStart = !autoStart;

            Main.SIMPLE_CHANNEL.sendToServer(new MessageMerchantSetAutoStartTravel(merchant.getUUID(), autoStart));
            this.setButtons();
        },
                (button1, poseStack, i, i1) -> {
                    this.renderTooltip(poseStack, Translatable.TOOLTIP_AUTO_START_TRAVEL, i, i1);
                }));
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

    private Button createTravelStartButton() {
        return addRenderableWidget(new Button(leftPos + 19, topPos + 32, 40, 20, Translatable.TEXT_BUTTON_TRAVEL_START, button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageMerchantTravel(merchant.getUUID(),  true, false));
            this.setButtons();
        },
        (button1, poseStack, i, i1) -> {
            this.renderTooltip(poseStack, Translatable.TOOLTIP_TRAVEL_START, i, i1);
        }));
    }

    private Button createTravelStopButton() {
        return addRenderableWidget(new Button(leftPos + 62, topPos + 32, 40, 20, Translatable.TEXT_BUTTON_TRAVEL_STOP, button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageMerchantTravel(merchant.getUUID(),  false, false));
            this.setButtons();
        },
        (button1, poseStack, i, i1) -> {
            this.renderTooltip(poseStack, Translatable.TOOLTIP_TRAVEL_STOP, i, i1);
        }));
    }

    private Button createTravelReturnButton() {
        return addRenderableWidget(new Button(leftPos + 105, topPos + 32, 40, 20, Translatable.TEXT_BUTTON_TRAVEL_RETURN, button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageMerchantTravel(merchant.getUUID(), true, true));
            this.setButtons();
        },
        (button1, poseStack, i, i1) -> {
            this.renderTooltip(poseStack, Translatable.TOOLTIP_TRAVEL_RETURN, i, i1);
        }));
    }

    private Button createTravelInfoButton(int x, int y) {
        Button button = addRenderableWidget(new Button(x, y, 20, 20, Component.literal("i"),
                press -> {
                    this.setButtons();
                    this.player.sendSystemMessage(Translatable.TEXT_TRAVEL_INFO);
                    this.onClose();
               },
                (button1, poseStack, i, i1) -> {
                    this.renderTooltip(poseStack, Translatable.TOOLTIP_TRAVEL_INFO, i, i1);
                }
        ));

        return button;
    }


    @Override
    protected void renderLabels(PoseStack matrixStack, int mouseX, int mouseY) {
        super.renderLabels(matrixStack, mouseX, mouseY);
        // Info
        int fontColor = 4210752;
        int waypointsPerPage = 10;
        this.returnTime = merchant.getReturningTime();
        int currentReturningTime = merchant.getCurrentReturningTime();

        font.draw(matrixStack, "Days:", 20, 9, FONT_COLOR);
        font.draw(matrixStack, "" + currentReturningTime + " / " + merchant.getReturningTime(), 110, 9, FONT_COLOR);

        int currentWayPoint = waypoints.size() == 0 ? 0 : merchant.getCurrentWayPointIndex() + 1;

        font.draw(matrixStack, "Waypoints:", 20, 20, FONT_COLOR);
        font.draw(matrixStack, "" + currentWayPoint + " / " + waypoints.size(), 110, 20, FONT_COLOR);

        int startIndex = (page - 1) * waypointsPerPage;
        int endIndex = Math.min(startIndex + waypointsPerPage, waypoints.size());

        if (!waypoints.isEmpty()) {
            for (int i = startIndex; i < endIndex; i++) {
                BlockPos pos = waypoints.get(i);

                int x = pos.getX();
                int y = pos.getY();
                int z = pos.getZ();



                String coordinates = String.format("%d:  (%d,  %d,  %d)", i + 1, x, y, z);

                if(!waypointItems.isEmpty() && waypointItems.get(i) != null) renderItemAt(waypointItems.get(i), 15, 58 + ((i - startIndex) * 17)); // Adjust the Y position here
                else{
                    BlockPos pos1 =  waypoints.get(i);
                    ItemStack itemStack = merchant.getItemStackToRender(pos1);

                    renderItemAt(itemStack, 15, 58 + ((i - startIndex) * 17));
                }
                font.draw(matrixStack, coordinates, 35, 60 + ((i - startIndex) * 17), fontColor);
            }

            if (waypoints.size() > waypointsPerPage)
                font.draw(matrixStack, "Page: " + page, 90, 230, fontColor);
        }

    }

    private void renderItemAt(ItemStack itemStack, int x, int y) {
        if(itemStack != null) itemRenderer.renderAndDecorateItem(itemStack, x, y);
    }

    public Button createPageBackButton() {
        return addRenderableWidget(new Button(leftPos + 15, topPos + 230, 12, 12, Component.literal("<"),
                button -> {
                    if(this.page > 1) page--;
                    this.setButtons();
                }
        ));
    }

    public Button createPageForwardButton() {
        return addRenderableWidget(new Button(leftPos + 184, topPos + 230, 12, 12, Component.literal(">"),
                button -> {
                    page++;
                    this.setButtons();
                }
        ));
    }

    private Button createAddWaypointButton(int x, int y){
        return addRenderableWidget(new Button(x, y, 20, 20, Component.literal("+"),
                button -> {
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageMerchantAddWayPoint(merchant.getUUID()));

                    this.setButtons();
                }
        ));
    }

    private Button createRemoveWaypointButton(int x, int y){
        return addRenderableWidget(new Button(x, y, 20, 20, Component.literal("-"),
                button -> {
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageMerchantRemoveWayPoint(merchant.getUUID()));

                    this.setButtons();
                }
        ));
    }
}
