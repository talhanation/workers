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
    private static final MutableComponent TEXT_FISHERMAN = Component.translatable("gui.workers.checkbox.fisherman");
    private static final MutableComponent TEXT_COURIERS = Component.translatable("gui.workers.checkbox.couriers");
    private static final MutableComponent TEXT_COOKS = Component.translatable("gui.workers.checkbox.cooks");
    private static final Component TEXT_STORAGE_NAME = Component.translatable("entity.workers.storage");;
    public final StorageArea storageArea;
    private RecruitsCheckBox minersCheckBox;
    private RecruitsCheckBox lumbersCheckBox;
    private RecruitsCheckBox buildersCheckBox;
    private RecruitsCheckBox farmersCheckBox;
    private RecruitsCheckBox merchantsCheckBox;
    private RecruitsCheckBox fishermanCheckBox;
    private RecruitsCheckBox animalFarmerCheckBox;
    private RecruitsCheckBox courierCheckBox;
    private RecruitsCheckBox cookCheckBox;
    private boolean miners;
    private boolean lumbers;
    private boolean builders;
    private boolean farmers;
    private boolean merchants;
    private boolean fisherman;
    private boolean animalFarmer;
    private boolean courier;
    private boolean cook;
    private EnumSet<StorageArea.StorageType> types;
    public EditBox nameEditBox;
    public Component savedName;
    public StorageAreaScreen(StorageArea storageArea, Player player) {
        super(storageArea.getCustomName(), storageArea, player);
        this.storageArea = storageArea;
        this.types = storageArea.getStorageTypes();
        this.miners = types.contains(StorageArea.StorageType.MINERS);
        this.lumbers = types.contains(StorageArea.StorageType.LUMBERS);
        this.builders = types.contains(StorageArea.StorageType.BUILDERS);
        this.farmers = types.contains(StorageArea.StorageType.FARMERS);
        this.merchants = types.contains(StorageArea.StorageType.MERCHANTS);
        this.fisherman = types.contains(StorageArea.StorageType.FISHERMAN);
        this.animalFarmer = types.contains(StorageArea.StorageType.ANIMAL_FARMERS);
        this.courier = types.contains(StorageArea.StorageType.COURIER);
        this.cook = types.contains(StorageArea.StorageType.COOK);
    }

    @Override
    protected void init() {
        super.init();
        this.savedName = workArea.getCustomName();
        if(this.savedName == null || this.savedName.getString().isEmpty()){
            this.savedName = TEXT_STORAGE_NAME;
        }
        setButtons();
    }

    @Override
    public void setButtons() {
        super.setButtons();

        int cbW = 100;
        int cbH = 20;
        int gap = 1;

        // Two columns: left starts at x - cbW - 5, right at x + 5
        int leftX  = x - cbW - 5;
        int rightX = x + 5;
        int startY = y + cbH / 2 + 45;

        nameEditBox = new EditBox(font, leftX, startY - cbH - 4, cbW * 2 + 10, cbH, Component.literal(""));
        nameEditBox.setValue(savedName.getString());
        nameEditBox.setTextColor(-1);
        nameEditBox.setTextColorUneditable(-1);
        nameEditBox.setBordered(true);
        nameEditBox.setMaxLength(32);
        nameEditBox.setResponder(this::setName);
        this.addRenderableWidget(nameEditBox);

        // Left column: Miners, Lumbers, Builders, Farmers
        this.minersCheckBox = new RecruitsCheckBox(leftX, startY, cbW, cbH, TEXT_MINERS,
                this.miners, bool -> { this.miners = bool; toggleType(StorageArea.StorageType.MINERS, bool); });
        addRenderableWidget(minersCheckBox);

        this.lumbersCheckBox = new RecruitsCheckBox(leftX, startY + gap + cbH, cbW, cbH, TEXT_LUMBERS,
                this.lumbers, bool -> { this.lumbers = bool; toggleType(StorageArea.StorageType.LUMBERS, bool); });
        addRenderableWidget(lumbersCheckBox);

        this.buildersCheckBox = new RecruitsCheckBox(leftX, startY + (gap + cbH) * 2, cbW, cbH, TEXT_BUILDERS,
                this.builders, bool -> { this.builders = bool; toggleType(StorageArea.StorageType.BUILDERS, bool); });
        addRenderableWidget(buildersCheckBox);

        this.farmersCheckBox = new RecruitsCheckBox(leftX, startY + (gap + cbH) * 3, cbW, cbH, TEXT_FARMERS,
                this.farmers, bool -> { this.farmers = bool; toggleType(StorageArea.StorageType.FARMERS, bool); });
        addRenderableWidget(farmersCheckBox);

        // Right column: Merchants, Fisherman, AnimalFarmer, Courier
        this.merchantsCheckBox = new RecruitsCheckBox(rightX, startY, cbW, cbH, TEXT_MERCHANTS,
                this.merchants, bool -> { this.merchants = bool; toggleType(StorageArea.StorageType.MERCHANTS, bool); });
        addRenderableWidget(merchantsCheckBox);

        this.fishermanCheckBox = new RecruitsCheckBox(rightX, startY + gap + cbH, cbW, cbH, TEXT_FISHERMAN,
                this.fisherman, bool -> { this.fisherman = bool; toggleType(StorageArea.StorageType.FISHERMAN, bool); });
        addRenderableWidget(fishermanCheckBox);

        this.animalFarmerCheckBox = new RecruitsCheckBox(rightX, startY + (gap + cbH) * 2, cbW, cbH, TEXT_ANIMAL_FARMERS,
                this.animalFarmer, bool -> { this.animalFarmer = bool; toggleType(StorageArea.StorageType.ANIMAL_FARMERS, bool); });
        addRenderableWidget(animalFarmerCheckBox);

        this.courierCheckBox = new RecruitsCheckBox(rightX, startY + (gap + cbH) * 3, cbW, cbH, TEXT_COURIERS,
                this.courier, bool -> { this.courier = bool; toggleType(StorageArea.StorageType.COURIER, bool); });
        addRenderableWidget(courierCheckBox);

        this.cookCheckBox = new RecruitsCheckBox(rightX, startY + (gap + cbH) * 4, cbW, cbH, TEXT_COOKS,
                this.cook, bool -> { this.cook = bool; toggleType(StorageArea.StorageType.COOK, bool); });
        addRenderableWidget(cookCheckBox);
    }

    private void toggleType(StorageArea.StorageType type, boolean add) {
        if (add) types.add(type);
        else types.remove(type);
        sendMessage();
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