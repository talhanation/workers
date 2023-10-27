package com.talhanation.workers.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.workers.Main;
import com.talhanation.workers.Translatable;
import com.talhanation.workers.config.WorkersModConfig;
import com.talhanation.workers.inventory.CommandMenu;
import com.talhanation.workers.network.*;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.gui.widget.ExtendedButton;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class CommandScreen extends ScreenBase<CommandMenu> {

    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(Main.MOD_ID, "textures/gui/command_gui.png");
    private static final int fontColor = 16250871;
    public static List<UUID> worker_ids;
    public static List<String> worker_names;
    public final Player player;
    private byte index;
    private String name;
    private BlockPos blockpos;
    private Button rightButton;
    private Button leftButton;
    private Button workPosButton;
    private Button followButton;
    private Button startTravelButton;
    private Button stopTravelButton;
    private Button returnTravelButton;
    private Button sleepPosButton;
    private Button chestPosButton;
    //TODO: add worker type as integer
    public CommandScreen(CommandMenu commandContainer, Inventory playerInventory, Component title) {
        super(RESOURCE_LOCATION, commandContainer, playerInventory, new TextComponent(""));
        imageWidth = 201;
        imageHeight = 170;
        player = playerInventory.player;
    }

    @Override
    public boolean keyReleased(int x, int y, int z) {
        super.keyReleased(x, y, z);
        if(!WorkersModConfig.CommandScreenToggle.get()) this.onClose();
        return true;
    }

    @Override
    protected void init() {
        super.init();
        this.setButtons();
    }

    private void setButtons(){
        this.clearWidgets();

        int zeroLeftPos = leftPos + 150;
        int zeroTopPos = topPos - 40;

        this.rightButton = cycleButtonRight(zeroLeftPos, zeroTopPos);
        this.leftButton = cycleButtonLeft(zeroLeftPos, zeroTopPos);

        this.blockpos = getBlockPos();

        this.followButton = setFollow(zeroLeftPos, zeroTopPos);
        this.workPosButton = setWorkPosition(blockpos, zeroLeftPos, zeroTopPos);
        this.sleepPosButton = setSleepPosition(blockpos, zeroLeftPos, zeroTopPos);
        this.chestPosButton = setChestPosition(blockpos, zeroLeftPos, zeroTopPos);

        this.leftButton.active = canCycleLeft();
        this.rightButton.active = canCycleRight();

        this.workPosButton.active = blockpos != null;
        this.sleepPosButton.active = blockpos != null && getBlockState().isBed(player.level, blockpos, player);
        this.chestPosButton.active = blockpos != null && player.level.getBlockEntity(blockpos) instanceof Container;

        if(!worker_names.isEmpty() && index < worker_names.size()) this.name = worker_names.get(index);
        //Buttons for special workers
        if(name.contains("Merchant")){
            this.startTravelButton = this.setStartTravel(zeroLeftPos, zeroTopPos);
            this.stopTravelButton = this.setStopTravel(zeroLeftPos, zeroTopPos);
            this.returnTravelButton = this.setReturnTravel(zeroLeftPos, zeroTopPos);
        }

    }

    @Override
    protected void containerTick() {
        super.containerTick();
    }

    @Override
    protected void renderLabels(PoseStack matrixStack, int mouseX, int mouseY) {
        super.renderLabels(matrixStack, mouseX, mouseY);

        int size = worker_ids == null ? 0 : worker_ids.size() ;

        int k = 85;//rechst links
        int l = 65;//höhe
        String workers = "Workers: ";
        String worker = this.index + 1 + ": " + this.name;
        if(size > 0){
            font.draw(matrixStack, workers + size, k - workers.length(), l, fontColor);
            font.draw(matrixStack, worker, k - worker.length() - 15, l + 25, fontColor);
        }
    }

    private Button setFollow(int x, int y){
        return addRenderableWidget(new ExtendedButton(x - 90, y + 160, 80, 18, Translatable.TEXT_BUTTON_FOLLOW,
            button -> {
                Main.SIMPLE_CHANNEL.sendToServer(new MessageFollow(this.player.getUUID(), getCurrentWorker()));
            }
        ));
    }

    private Button setReturnTravel(int x, int y){
        return addRenderableWidget(new ExtendedButton(x - 20, y + 180, 40, 18, Translatable.TEXT_BUTTON_TRAVEL_RETURN,
                button -> {
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageMerchantTravel(getCurrentWorker(), true, true));
                }
        ));
    }

    private Button setStopTravel(int x, int y){
        return addRenderableWidget(new ExtendedButton(x - 70, y + 180, 40, 18, Translatable.TEXT_BUTTON_TRAVEL_STOP,
            button -> {
                Main.SIMPLE_CHANNEL.sendToServer(new MessageMerchantTravel(getCurrentWorker(), false, false));
            }
        ));
    }

    private Button setStartTravel(int x, int y){
        return addRenderableWidget(new ExtendedButton(x - 120, y + 180, 40, 18, Translatable.TEXT_BUTTON_TRAVEL_START,
            button -> {
                Main.SIMPLE_CHANNEL.sendToServer(new MessageMerchantTravel(getCurrentWorker(), true, false));
            }
        ));
    }

    private Button setWorkPosition(BlockPos pos, int x, int y){
        //TODO: add worker type
        Component component = name.contains("Merchant") ? Translatable.TEXT_BUTTON_ADD_WAYPOINT : Translatable.TEXT_BUTTON_WORK_POS;

        return addRenderableWidget(new ExtendedButton(x - 90, y + 140, 80, 18, component,
            button -> {
                Main.SIMPLE_CHANNEL.sendToServer(new MessageStartPos(this.player.getUUID(), pos, getCurrentWorker()));
            }
        ));
    }

    private Button setChestPosition(BlockPos pos, int x, int y){
        return addRenderableWidget(new ExtendedButton(x - 180 , y + 140, 80, 18, Translatable.TEXT_BUTTON_CHEST_POS,
            button -> {
                Main.SIMPLE_CHANNEL.sendToServer(new MessageChestPos(this.player.getUUID(), pos, getCurrentWorker()));
            }
        ));
    }

    private Button setSleepPosition(BlockPos pos, int x, int y){
        return addRenderableWidget(new ExtendedButton(x , y + 140, 80, 18, Translatable.TEXT_BUTTON_SLEEP_POS,
            button -> {
                Main.SIMPLE_CHANNEL.sendToServer(new MessageBedPos(this.player.getUUID(), pos, getCurrentWorker()));
            }
        ));
    }

    private Button cycleButtonLeft(int x, int y){
        return addRenderableWidget(new ExtendedButton(x - 150, y + 50, 50, 18,  new TextComponent("<"),
            button -> {
                if(canCycleLeft()){
                    index--;
                    this.setButtons();
                }
            }
        ));
    }

    private Button cycleButtonRight(int x, int y){
        return addRenderableWidget(new ExtendedButton(x, y + 50, 50, 18, new TextComponent(">"),
            button -> {
                if(canCycleRight()){
                    index++;
                    this.setButtons();
                }
            }
        ));
    }

    private boolean canCycleLeft() {
        return !worker_ids.isEmpty() && index > 0;
    }
    private boolean canCycleRight() {
        return !worker_ids.isEmpty() && this.index + 1 != worker_ids.size();
    }
    private UUID getCurrentWorker(){
        return worker_ids.get(this.index);
    }

    private BlockPos getBlockPos() {
        HitResult rayTraceResult = player.pick(50, 1F, true);
        if (rayTraceResult != null) {
            if (rayTraceResult.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockraytraceresult = (BlockHitResult) rayTraceResult;

                return blockraytraceresult.getBlockPos();
            }
        }
        return null;
    }

    private BlockState getBlockState(){
        return player.level.getBlockState(blockpos);
    }
}
