package com.talhanation.workers.client.gui;

import com.talhanation.recruits.client.gui.widgets.RecruitsCheckBox;
import com.talhanation.workers.WorkersMain;
import com.talhanation.workers.client.gui.widgets.ItemScrollDropDownMenu;
import com.talhanation.workers.entities.workarea.LumberArea;
import com.talhanation.workers.network.MessageUpdateLumberArea;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SaplingBlock;

import java.util.ArrayList;
import java.util.List;

public class LumberAreaScreen extends WorkAreaScreen {


    private static final MutableComponent TEXT_SHEAR_LEAVES = Component.translatable("gui.workers.checkbox.shearLeaves");
    private static final MutableComponent TEXT_STRIP_LOGS = Component.translatable("gui.workers.checkbox.stripLogs");
    private static final MutableComponent TEXT_REPLANT = Component.translatable("gui.workers.checkbox.replant");
    private static final MutableComponent TEXT_ANY_SAPLING = Component.translatable("gui.workers.command.text.anySapling");
    public final LumberArea lumberArea;
    private ItemScrollDropDownMenu seedItemSelectionDropDownMenu;
    private ItemStack currentSapling;
    private boolean replant;
    private boolean stripLogs;
    private boolean shearLeaves;
    private List<ItemStack> possibleSeeds;
    private RecruitsCheckBox replantCheckBox;
    private RecruitsCheckBox stripLogsCheckBox;
    private RecruitsCheckBox shearLeavesCheckBox;
    public LumberAreaScreen(LumberArea lumberArea, Player player) {
        super(lumberArea.getCustomName(), lumberArea, player);
        this.lumberArea = lumberArea;
        this.currentSapling = lumberArea.getSaplingStack();
        this.shearLeaves = lumberArea.getShearLeaves();
        this.stripLogs = lumberArea.getStripLogs();
        this.replant = lumberArea.getReplant();
    }

    @Override
    protected void init() {
        this.possibleSeeds = getPossibleSaplingsFromInventory();

        setButtons();
    }

    @Override
    public void setButtons() {
        super.setButtons();

        int dropDownWidth = 200;
        int dropDownHeight = 20;

        int checkBoxWidth = 100;
        int checkBoxHeight = 20;

        int checkBoxX = x - checkBoxWidth / 2;
        int checkBoxY = y + checkBoxHeight / 2 - checkBoxHeight;

        seedItemSelectionDropDownMenu = new ItemScrollDropDownMenu(currentSapling,x - dropDownWidth / 2, 120 + y + dropDownHeight / 2 - dropDownHeight, dropDownWidth, dropDownHeight, possibleSeeds, this::setCurrentSapling);
        seedItemSelectionDropDownMenu.insertOption(0, ItemStack.EMPTY, TEXT_ANY_SAPLING.getString());
        seedItemSelectionDropDownMenu.setBgFillSelected(FastColor.ARGB32.color(255, 139, 139, 139));

        addRenderableWidget(seedItemSelectionDropDownMenu);

        this.shearLeavesCheckBox = new RecruitsCheckBox(checkBoxX, 50 + checkBoxY, checkBoxWidth, checkBoxHeight, TEXT_SHEAR_LEAVES,
            this.shearLeaves,
            (bool) -> {
                this.shearLeaves = bool;
                WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateLumberArea(lumberArea.getUUID(), currentSapling, shearLeaves, stripLogs, replant));
            }
        );
        addRenderableWidget(shearLeavesCheckBox);

        this.stripLogsCheckBox = new RecruitsCheckBox(checkBoxX, 70 + checkBoxY, checkBoxWidth, checkBoxHeight, TEXT_STRIP_LOGS,
                this.stripLogs,
                (bool) -> {
                    this.stripLogs = bool;
                    WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateLumberArea(lumberArea.getUUID(), currentSapling, shearLeaves, stripLogs, replant));
                }
        );
        addRenderableWidget(stripLogsCheckBox);

        this.replantCheckBox = new RecruitsCheckBox(checkBoxX, 90 + checkBoxY, checkBoxWidth, checkBoxHeight, TEXT_REPLANT,
                this.replant,
                (bool) -> {
                    this.replant = bool;
                    WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateLumberArea(lumberArea.getUUID(), currentSapling, shearLeaves, stripLogs, replant));
                }
        );
        addRenderableWidget(replantCheckBox);
    }

    public void setCurrentSapling(ItemStack currentSapling) {
        WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateLumberArea(lumberArea.getUUID(), currentSapling, shearLeaves, stripLogs, replant));
        this.currentSapling = currentSapling;
    }

    private List<ItemStack> getPossibleSaplingsFromInventory() {
        List<Item> items = new ArrayList<>();
        List<ItemStack> stacks = new ArrayList<>();
        for (ItemStack itemStack : player.getInventory().items) {
            if (itemStack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof SaplingBlock) {
                if(!items.contains(itemStack.getItem())){
                    stacks.add(itemStack);
                    items.add(itemStack.getItem());
                }
            }
        }
        return stacks;
    }

    @Override
    public void mouseMoved(double x, double y) {
        if(seedItemSelectionDropDownMenu != null){
            seedItemSelectionDropDownMenu.onMouseMove(x,y);
        }
        super.mouseMoved(x, y);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (seedItemSelectionDropDownMenu != null && seedItemSelectionDropDownMenu.isMouseOver(mouseX, mouseY)) {
            seedItemSelectionDropDownMenu.onMouseClick(mouseX, mouseY);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    @Override
    public boolean mouseScrolled(double x, double y, double d) {
        if(seedItemSelectionDropDownMenu != null) seedItemSelectionDropDownMenu.mouseScrolled(x,y,d);
        return super.mouseScrolled(x, y, d);
    }
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        if (seedItemSelectionDropDownMenu != null) {
            seedItemSelectionDropDownMenu.renderWidget(guiGraphics, mouseX, mouseY, partialTicks);
        }
    }
}
