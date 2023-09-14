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
    private final MerchantEntity merchant;
    private final Player player;
    private int returnTime;

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
        this.setButtons();
    }
    private void setButtons(){
        this.clearWidgets();

        this.setStartButtons();
        this.setPageButtons();
        this.setWaypointButtons();
        this.setDaysButtons();
    }
    public void setStartButtons(){
        //START BUTTONS//
        boolean isStarted = merchant.getTraveling();
        boolean isReturning = merchant.getReturning();

        Button start = createTravelStartButton();
        start.active = (!isStarted || ! isReturning) && waypoints.size() > 1;

        Button stop = createTravelStopButton();
        stop.active = (isStarted || isReturning) && waypoints.size() > 1;

        Button returnButton = createTravelReturnButton();
        returnButton.active = !isReturning && !isStarted && waypoints.size() > 1;
    }

    public void setPageButtons() {
        ExtendedButton pageForwardButton = createPageForwardButton();
        pageForwardButton.active = waypoints.size() > 10;

        ExtendedButton pageBackButton = createPageBackButton();
        pageBackButton.active = page != 1;
    }

    public void setWaypointButtons() {
        ExtendedButton addButton = createAddWaypointButton(this.leftPos + 171, this.topPos + 32);
        ExtendedButton removeButton = createRemoveWaypointButton(this.leftPos + 148, this.topPos + 32);

        removeButton.active = !waypoints.isEmpty();

        Button info = createTravelInfoButton(this.leftPos + 215, this.topPos + 32);
    }

    public void setDaysButtons(){
        // ReturnTime
        ExtendedButton addButton =
                addRenderableWidget(new ExtendedButton(leftPos + 186, topPos + 8, 9, 9, Component.literal("+"), button -> {
                    this.returnTime = merchant.getReturningTime();
                    this.returnTime++;
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageMerchantReturnTime(this.returnTime, merchant.getUUID()));
                    this.setButtons();
                }));
        ExtendedButton removeButton =
                addRenderableWidget(new ExtendedButton(leftPos + 175, topPos + 8, 9, 9, Component.literal("-"), button -> {
                    this.returnTime = merchant.getReturningTime();
                    if (this.returnTime > 0) {
                        this.returnTime--;
                        Main.SIMPLE_CHANNEL.sendToServer(new MessageMerchantReturnTime(this.returnTime, merchant.getUUID()));
                    }
                    this.setButtons();
                }));

        removeButton.active = returnTime > 0;
    }

    private Button createTravelStartButton() {
        return addRenderableWidget(new Button(leftPos + 19, topPos + 32, 40, 20, Component.literal("Start"), button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageMerchantTravel(merchant.getUUID(),  true, false));
            this.setButtons();
        },
        (button1, poseStack, i, i1) -> {
            this.renderTooltip(poseStack, Translatable.TOOLTIP_TRAVEL_START, i, i1);
        }));
    }

    private Button createTravelStopButton() {
        return addRenderableWidget(new Button(leftPos + 62, topPos + 32, 40, 20, Component.literal("Stop"), button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageMerchantTravel(merchant.getUUID(),  false, false));
            this.setButtons();
        },
        (button1, poseStack, i, i1) -> {
            this.renderTooltip(poseStack, Translatable.TOOLTIP_TRAVEL_STOP, i, i1);
        }));
    }

    private Button createTravelReturnButton() {
        return addRenderableWidget(new Button(leftPos + 105, topPos + 32, 40, 20, Component.literal("Return"), button -> {
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
                    this.player.sendSystemMessage(Translatable.TOOLTIP_TRAVEL_INFO);
                    this.onClose();
               },
                (button1, poseStack, i, i1) -> {
                    this.renderTooltip(poseStack, Translatable.TEXT_TRAVEL_INFO, i, i1);
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

        int startIndex = (page - 1) * waypointsPerPage;
        int endIndex = Math.min(startIndex + waypointsPerPage, waypoints.size());
        int currentWayPoint = waypoints.size() == 0 ? 0 : merchant.getCurrentWayPointIndex() + 1;

        font.draw(matrixStack, "Waypoints:", 20, 20, FONT_COLOR);
        font.draw(matrixStack, "" + currentWayPoint + " / " + waypoints.size(), 110, 20, FONT_COLOR);

        if (!waypoints.isEmpty()) {
            for (int i = startIndex; i < endIndex; i++) {
                BlockPos pos = waypoints.get(i % waypointsPerPage); // Modulo to loop through pages

                int x = pos.getX();
                int y = pos.getY();
                int z = pos.getZ();

                BlockState state = player.level.getBlockState(pos);
                ItemStack itemStack;
                if(state.is(Blocks.WATER)) itemStack = new ItemStack(Items.OAK_BOAT);
                else itemStack = new ItemStack(state.getBlock().asItem());

                String coordinates = String.format("%d:  (%d,  %d,  %d)", i+1, x, y, z);

                renderItemAt(itemStack, 15, 58 + (i % waypointsPerPage * 17));

                font.draw(matrixStack, coordinates, 35, 60 + (i % waypointsPerPage * 17), fontColor);
            }

            if (waypoints.size() > waypointsPerPage)
                font.draw(matrixStack, "Page: " + page, 90, 230, fontColor);

        }
    }

    private void renderItemAt(ItemStack itemStack, int x, int y) {
        if(itemStack != null) itemRenderer.renderAndDecorateItem(itemStack, x, y);
    }

    public ExtendedButton createPageBackButton() {
        return addRenderableWidget(new ExtendedButton(leftPos + 15, topPos + 230, 12, 12, Component.literal("<"),
                button -> {
                    if(this.page > 1) page--;
                    this.setButtons();
                }
        ));
    }

    public ExtendedButton createPageForwardButton() {
        return addRenderableWidget(new ExtendedButton(leftPos + 184, topPos + 230, 12, 12, Component.literal(">"),
                button -> {
                    page++;
                    this.setButtons();
                }
        ));
    }

    private ExtendedButton createAddWaypointButton(int x, int y){
        return addRenderableWidget(new ExtendedButton(x, y, 20, 20, Component.literal("+"),
                button -> {
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageMerchantAddWayPoint(merchant.getUUID()));

                    this.setButtons();
                }
        ));
    }

    private ExtendedButton createRemoveWaypointButton(int x, int y){
        return addRenderableWidget(new ExtendedButton(x, y, 20, 20, Component.literal("-"),
                button -> {
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageMerchantRemoveWayPoint(merchant.getUUID()));

                    this.setButtons();
                }
        ));
    }
}
