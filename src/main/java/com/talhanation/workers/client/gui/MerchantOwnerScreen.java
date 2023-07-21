package com.talhanation.workers.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.workers.Main;
import com.talhanation.workers.entities.MerchantEntity;
import com.talhanation.workers.inventory.MerchantInventoryContainer;
import com.talhanation.workers.network.*;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public class MerchantOwnerScreen extends ScreenBase<MerchantInventoryContainer> {

    private static final ResourceLocation GUI_TEXTURE_3 = new ResourceLocation(Main.MOD_ID,
            "textures/gui/merchant_owner_gui.png");
    public static int FONT_COLOR_WHITE= 14342874;
    private final MerchantEntity merchant;
    private final Inventory playerInventory;
    private final Player player;
    public static List<BlockPos> waypoints;

    private int returnTime;

    public MerchantOwnerScreen(MerchantInventoryContainer container, Inventory playerInventory, Component title) {
        super(GUI_TEXTURE_3, container, playerInventory, Component.literal(""));
        this.merchant = (MerchantEntity) container.getWorker();
        this.playerInventory = playerInventory;
        this.player = playerInventory.player;
        this.imageWidth = 176;
        this.imageHeight = 222;
    }

    @Override
    protected void init() {
        super.init();
        this.returnTime = merchant.getReturningTime();
        this.setUpdatableButtons();
        int zeroLeftPos = leftPos + 180;
        int zeroTopPos = topPos - 30;
        int mirror = 240 - 60;

        addRenderableWidget(new Button(zeroLeftPos - mirror + 180, zeroTopPos + 60, 41, 20, Component.literal("Return"), button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageMerchantTravel(merchant.getUUID(), true, true));
            this.setUpdatableButtons();
        }));
        addRenderableWidget(new Button(zeroLeftPos - mirror + 180, zeroTopPos + 85, 41, 20, Component.literal("Travel"), button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageMerchantTravel(merchant.getUUID(),  !merchant.getTraveling(), false));
            this.setUpdatableButtons();
        }));

        if(this.player.isCreative() && this.player.createCommandSourceStack().hasPermission(4)){
            createCreativeButton(zeroLeftPos - mirror + 180, zeroTopPos + 148);
        }
    }

    public void setUpdatableButtons(){
        int zeroLeftPos = leftPos + 180;
        int zeroTopPos = topPos - 30;
        int mirror = 240 - 60;

        //Add Waypoint
        Button addWaypointButton = createAddWaypointButton(zeroLeftPos - mirror + 180, zeroTopPos + 106);
        addWaypointButton.active =  !merchant.getTraveling() || !merchant.getReturning();

        Button removeWaypointButton = createRemoveWaypointButton(zeroLeftPos - mirror + 201, zeroTopPos + 106);
        removeWaypointButton.active =  !merchant.getTraveling() || !merchant.getReturning();

        // ReturnTime
        addRenderableWidget(new Button(zeroLeftPos - mirror + 180, zeroTopPos + 36, 20, 20, Component.literal("+"), button -> {
            this.returnTime = merchant.getReturningTime();
            this.returnTime++;
            Main.SIMPLE_CHANNEL.sendToServer(new MessageMerchantReturnTime(this.returnTime, merchant.getUUID()));
        }));

        addRenderableWidget(new Button(zeroLeftPos - mirror + 201, zeroTopPos + 36, 20, 20, Component.literal("-"), button -> {
            this.returnTime = merchant.getReturningTime();
            if (this.returnTime > 0) {
                this.returnTime--;
                Main.SIMPLE_CHANNEL.sendToServer(new MessageMerchantReturnTime(this.returnTime, merchant.getUUID()));
            }
        }));

        createHorseButton(zeroLeftPos - mirror + 180, zeroTopPos + 127);
    }

    private void createHorseButton(int x, int y) {
        String dis_mount;
        if(merchant.getVehicle() != null) dis_mount = "Dismount";
        else dis_mount = "Mount Horse";

        addRenderableWidget(new Button(x, y, 41, 20, Component.literal(dis_mount),
                button -> {
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageMerchantHorse(merchant.getUUID()));
                }));

        this.setUpdatableButtons();
    }
    private void createCreativeButton(int x, int y) {
        addRenderableWidget(new Button(x, y, 41, 20, Component.literal("Creative"),
            button -> {
                Main.SIMPLE_CHANNEL.sendToServer(new MessageMerchantSetCreative(merchant.getUUID(), !merchant.isCreative()));
        }));
    }

    private Button createAddWaypointButton(int x, int y){
        return addRenderableWidget(new Button(x, y, 20, 20, Component.literal("+"), button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageMerchantAddWayPoint(merchant.getUUID()));
        }));
    }

    private Button createRemoveWaypointButton(int x, int y){
        return addRenderableWidget(new Button(x, y, 20, 20, Component.literal("-"), button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageMerchantRemoveWayPoint(merchant.getUUID()));
        }));
    }

    @Override
    protected void renderLabels(PoseStack matrixStack, int mouseX, int mouseY) {
        super.renderLabels(matrixStack, mouseX, mouseY);
        returnTime = merchant.getReturningTime();
        font.draw(matrixStack, merchant.getDisplayName().getVisualOrderText(), 8, 6, FONT_COLOR);
        font.draw(matrixStack, playerInventory.getDisplayName().getVisualOrderText(), 8, imageHeight - 152 + 59, FONT_COLOR);
        int currentWayPoint =  merchant.getCurrentWayPointIndex();
        int currentReturningTime = merchant.getCurrentReturningTime();

        if(waypoints != null) {
            font.draw(matrixStack, "Waypoints: " + currentWayPoint + "/" + waypoints.size(), 70, 6, FONT_COLOR);
            for (int i = 0; i < waypoints.size(); i++) {
                BlockPos pos = waypoints.get(i);
                font.draw(matrixStack, i + ": " + " x: " + pos.getX() + " y: " + pos.getY() + " z: " + pos.getZ(), -160, i * 13, FONT_COLOR_WHITE);
            }
        }
        font.draw(matrixStack, "Days: " + currentReturningTime +"/" + returnTime, 120, 6, FONT_COLOR);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    protected void renderBg(PoseStack matrixStack, float partialTicks, int mouseX, int mouseY) {
        super.renderBg(matrixStack, partialTicks, mouseX, mouseY);
    }
}
