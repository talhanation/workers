package com.talhanation.workers.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.workers.Main;
import com.talhanation.workers.entities.MerchantEntity;
import com.talhanation.workers.inventory.MerchantWaypointContainer;
import com.talhanation.workers.network.MessageMerchantAddWayPoint;
import com.talhanation.workers.network.MessageMerchantRemoveWayPoint;
import com.talhanation.workers.network.MessageMerchantReturnTime;
import com.talhanation.workers.network.MessageMerchantTravel;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.client.gui.widget.ExtendedButton;

import java.util.List;

public class MerchantWaypointScreen extends ScreenBase<MerchantWaypointContainer> {

    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(Main.MOD_ID,"textures/gui/waypoint_list_gui.png");
    private int leftPos;
    private int topPos;
    private int page = 1;
    public static List<BlockPos> waypoints;
    private final MerchantEntity merchant;
    private int returnTime;

    public MerchantWaypointScreen(MerchantWaypointContainer container, Inventory playerInventory, Component title) {
        super(RESOURCE_LOCATION, container, playerInventory, Component.literal(""));
        imageWidth = 197;
        imageHeight = 250;
        this.merchant = (MerchantEntity) container.getWorkerEntity();
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        this.returnTime = merchant.getReturningTime();

        this.setPageButtons();

        // ReturnTime
        addRenderableWidget(new Button(leftPos + 180, topPos + 36, 20, 20, Component.literal("+"), button -> {
            this.returnTime = merchant.getReturningTime();
            this.returnTime++;
            Main.SIMPLE_CHANNEL.sendToServer(new MessageMerchantReturnTime(this.returnTime, merchant.getUUID()));
        }));

        addRenderableWidget(new Button(leftPos + 201, topPos + 36, 20, 20, Component.literal("-"), button -> {
            this.returnTime = merchant.getReturningTime();
            if (this.returnTime > 0) {
                this.returnTime--;
                Main.SIMPLE_CHANNEL.sendToServer(new MessageMerchantReturnTime(this.returnTime, merchant.getUUID()));
            }
        }));
    }

    public void setPageButtons() {
        clearWidgets();
        boolean isStarted = merchant.getTraveling();
        boolean isReturning = merchant.getReturning();

        addRenderableWidget(new Button(leftPos + 39, topPos + 29, 41, 20, Component.literal("Travel"), button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageMerchantTravel(merchant.getUUID(),  !merchant.getTraveling(), false));
            this.setPageButtons();
        }));

        addRenderableWidget(new Button(leftPos + 102, topPos + 29, 41, 20, Component.literal("Return"), button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageMerchantTravel(merchant.getUUID(), true, true));
            this.setPageButtons();
        }));


        //ExtendedButton start = createTravelStartButton();
        //start.active = !(isStarted || isReturning);

        //ExtendedButton stop = createTravelStopButton();
        //stop.active = isStarted || isReturning;

        //ExtendedButton returnButton = createTravelReturnButton();
        //returnButton.active = !isReturning;

        if(waypoints.size() > 9) createPageForwardButton();
        if (page > 1) createPageBackButton();

        ExtendedButton addWaypointButton = createAddWaypointButton(this.leftPos + 135, this.topPos + 29);
        ExtendedButton removeWaypointButton = createRemoveWaypointButton(this.leftPos + 150, this.topPos + 29);
        removeWaypointButton.active = !waypoints.isEmpty();
    }

    @Override
    protected void renderLabels(PoseStack matrixStack, int mouseX, int mouseY) {
        super.renderLabels(matrixStack, mouseX, mouseY);
        // Info
        int fontColor = 4210752;
        int waypointsPerPage = 15;
        this.returnTime = merchant.getReturningTime();
        int currentReturningTime = merchant.getCurrentReturningTime();
        int daysleft = currentReturningTime - returnTime;
        font.draw(matrixStack, "Days until Return: " + daysleft, 120, 19, FONT_COLOR);

        if(waypoints.size() > waypointsPerPage)
            font.draw(matrixStack, "Page: " + page, 80, 230, fontColor);


        int startIndex = (page - 1) * waypointsPerPage;
        int endIndex = Math.min(startIndex + waypointsPerPage, waypoints.size());
        int currentWayPoint =  merchant.getCurrentWayPointIndex();

        font.draw(matrixStack, "Current Waypoint: " + currentWayPoint + "of " + waypoints.size(), 38, 19, FONT_COLOR);

        if (!waypoints.isEmpty()) {
            for (int i = startIndex; i < endIndex; i++) {

                BlockPos pos = waypoints.get(i);
                font.draw(matrixStack, i + ": " + "\t x: " + pos.getX() + " y: " + pos.getY() + " z: " + pos.getZ(), 16, 45 + i * 12, fontColor);
            }
        }
    }

    public ExtendedButton createPageBackButton() {
        return addRenderableWidget(new ExtendedButton(leftPos + 15, topPos + 227, 12, 12, Component.literal("<"),
                button -> {
                    if(this.page > 0) page--;
                    this.setPageButtons();
                }
        ));
    }

    public ExtendedButton createPageForwardButton() {
        return addRenderableWidget(new ExtendedButton(leftPos + 170, topPos + 227, 12, 12, Component.literal(">"),
                button -> {
                    page++;
                    this.setPageButtons();
                }
        ));
    }

    private ExtendedButton createAddWaypointButton(int x, int y){
        return addRenderableWidget(new ExtendedButton(x, y, 12, 12, Component.literal("+"),
            button -> {
                Main.SIMPLE_CHANNEL.sendToServer(new MessageMerchantAddWayPoint(merchant.getUUID()));
                this.setPageButtons();
            }
        ));
    }

    private ExtendedButton createRemoveWaypointButton(int x, int y){
        return addRenderableWidget(new ExtendedButton(x, y, 12, 12, Component.literal("-"),
            button -> {
                Main.SIMPLE_CHANNEL.sendToServer(new MessageMerchantRemoveWayPoint(merchant.getUUID()));
                this.setPageButtons();
            }
        ));
    }
}
