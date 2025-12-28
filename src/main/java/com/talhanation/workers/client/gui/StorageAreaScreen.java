package com.talhanation.workers.client.gui;

import com.talhanation.recruits.client.gui.widgets.RecruitsCheckBox;
import com.talhanation.workers.WorkersMain;
import com.talhanation.workers.entities.workarea.StorageArea;
import com.talhanation.workers.network.MessageUpdateStorageArea;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

public class StorageAreaScreen extends WorkAreaScreen {

    private static final MutableComponent TEXT_ALL = Component.translatable("gui.workers.checkbox.all");
    private static final MutableComponent TEXT_MINERS = Component.translatable("gui.workers.checkbox.miners");
    private static final MutableComponent TEXT_LUMBERS = Component.translatable("gui.workers.checkbox.lumbers");
    private static final MutableComponent TEXT_BUILDERS = Component.translatable("gui.workers.checkbox.builders");
    private static final MutableComponent TEXT_ANIMAL_FARMERS = Component.translatable("gui.workers.checkbox.animalFarmers");
    private static final MutableComponent TEXT_FARMERS = Component.translatable("gui.workers.checkbox.farmers");
    private static final MutableComponent TEXT_MERCHANTS = Component.translatable("gui.workers.checkbox.merchants");
    private static final Component TEXT_STORAGE_NAME = Component.translatable("entity.workers.storage");;
    public final StorageArea storageArea;
    private boolean replant;
    private boolean stripLogs;
    private boolean shearLeaves;
    private RecruitsCheckBox minersCheckBox;
    private RecruitsCheckBox lumbersCheckBox;
    private RecruitsCheckBox buildersCheckBox;
    private RecruitsCheckBox farmersCheckBox;
    private RecruitsCheckBox merchantsCheckBox;
    private boolean miners;
    private boolean lumbers;
    private boolean builders;
    private boolean farmers;
    private boolean merchants;
    private EnumSet<StorageArea.StorageType> types;
    public EditBox nameEditBox;
    public Component savedName;
    public StorageAreaScreen(StorageArea storageArea, Player player) {
        super(storageArea.getCustomName(), storageArea, player);
        this.storageArea = storageArea;
        this.types = storageArea.getStorageTypes();
        this.miners = types.contains(StorageArea.StorageType.MINER);
        this.lumbers = types.contains(StorageArea.StorageType.LUMBER);
        this.builders = types.contains(StorageArea.StorageType.BUILDER);
        this.farmers = types.contains(StorageArea.StorageType.FARMER);
        this.merchants = types.contains(StorageArea.StorageType.MERCHANT);
    }

    @Override
    protected void init() {
        this.savedName = workArea.getCustomName();
        if(this.savedName == null || this.savedName.getString().isEmpty()){
            this.savedName = TEXT_STORAGE_NAME;
        }
        setButtons();
    }

    @Override
    public void setButtons() {
        super.setButtons();

        int checkBoxWidth = 100;
        int checkBoxHeight = 20;

        int checkBoxX = x - checkBoxWidth / 2;
        int checkBoxY = y + checkBoxHeight / 2 + 40;

        nameEditBox = new EditBox(font, checkBoxX , checkBoxY - 20, checkBoxWidth, checkBoxHeight, Component.literal(""));
        nameEditBox.setValue(savedName.getString());
        nameEditBox.setTextColor(-1);
        nameEditBox.setTextColorUneditable(-1);
        nameEditBox.setBordered(true);
        nameEditBox.setMaxLength(32);
        nameEditBox.setResponder(this::setName);
        this.addRenderableWidget(nameEditBox);

        this.minersCheckBox = new RecruitsCheckBox(checkBoxX, 10 + checkBoxY, checkBoxWidth, checkBoxHeight, TEXT_MINERS,
                this.miners,
                (bool) -> {
                    this.miners = bool;
                    if(miners){
                        types.add(StorageArea.StorageType.MINER);
                    }
                    else{
                        types.remove(StorageArea.StorageType.MINER);
                    }

                    sendMessage();
                }
        );
        addRenderableWidget(minersCheckBox);

        this.lumbersCheckBox = new RecruitsCheckBox(checkBoxX, 30 + checkBoxY, checkBoxWidth, checkBoxHeight, TEXT_LUMBERS,
                this.lumbers,
                (bool) -> {
                    this.lumbers = bool;
                    if(lumbers){
                        types.add(StorageArea.StorageType.LUMBER);
                    }
                    else{
                        types.remove(StorageArea.StorageType.LUMBER);
                    }
                    sendMessage();
                }
        );
        addRenderableWidget(lumbersCheckBox);

        this.buildersCheckBox = new RecruitsCheckBox(checkBoxX, 50 + checkBoxY, checkBoxWidth, checkBoxHeight, TEXT_BUILDERS,
                this.builders,
                (bool) -> {
                    this.builders = bool;
                    if(builders){
                        types.add(StorageArea.StorageType.BUILDER);
                    }
                    else{
                        types.remove(StorageArea.StorageType.LUMBER);
                    }
                    sendMessage();
                }
        );
        addRenderableWidget(buildersCheckBox);

        this.farmersCheckBox = new RecruitsCheckBox(checkBoxX, 70 + checkBoxY, checkBoxWidth, checkBoxHeight, TEXT_FARMERS,
                this.farmers,
                (bool) -> {
                    this.farmers = bool;
                    if(farmers){
                        types.add(StorageArea.StorageType.FARMER);
                    }
                    else{
                        types.remove(StorageArea.StorageType.FARMER);
                    }
                    sendMessage();
                }
        );
        addRenderableWidget(farmersCheckBox);

        this.merchantsCheckBox = new RecruitsCheckBox(checkBoxX, 90 + checkBoxY, checkBoxWidth, checkBoxHeight, TEXT_MERCHANTS,
                this.merchants,
                (bool) -> {
                    this.merchants = bool;
                    if(merchants){
                        types.add(StorageArea.StorageType.MERCHANT);
                    }
                    else{
                        types.remove(StorageArea.StorageType.MERCHANT);
                    }
                    sendMessage();
                }
        );
        addRenderableWidget(merchantsCheckBox);

    }

    private void setName(String s) {
        savedName = Component.literal(s);
    }

    public void sendMessage(){
        WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateStorageArea(storageArea.getUUID(), storageArea.getStorageMask(types), savedName.getString()));
    }

    @Override
    public void onClose() {
        super.onClose();
        sendMessage();
    }
}
