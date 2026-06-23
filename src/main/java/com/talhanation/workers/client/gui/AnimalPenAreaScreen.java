package com.talhanation.workers.client.gui;

import com.talhanation.recruits.client.gui.widgets.BlackShowingTextField;
import com.talhanation.recruits.client.gui.widgets.RecruitsCheckBox;
import com.talhanation.recruits.client.gui.widgets.ScrollDropDownMenu;
import com.talhanation.workers.WorkersMain;
import com.talhanation.workers.entities.workarea.AnimalPenArea;
import com.talhanation.workers.network.MessageUpdateAnimalPenArea;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.widget.ExtendedButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AnimalPenAreaScreen extends WorkAreaScreen {

    private static final MutableComponent TEXT_BREED = Component.translatable("gui.workers.checkbox.breed");
    private static final MutableComponent TEXT_MAX_ANIMALS = Component.translatable("gui.workers.checkbox.maxAnimals");
    private static final MutableComponent TEXT_SLAUGHTER = Component.translatable("gui.workers.checkbox.slaughter");
    private static final MutableComponent TEXT_SHEAR_SHEEP = Component.translatable("gui.workers.checkbox.shearSheep");
    private static final MutableComponent TEXT_MILK_COW = Component.translatable("gui.workers.checkbox.milkCow");
    private static final MutableComponent TEXT_THROW_EGGS = Component.translatable("gui.workers.checkbox.throwEggs");
    private MutableComponent TEXT_SPECIAL = Component.empty();
    public final AnimalPenArea animalPenArea;
    private ScrollDropDownMenu<AnimalPenArea.AnimalTypes> animalTypesScrollDropDownMenu;
    private AnimalPenArea.AnimalTypes animalType;
    private boolean breed;
    private boolean slaughter;
    private boolean special;
    private int maxAnimals;
    private Button maxAnimalsPlusButton;
    private Button maxAnimalsMinusButton;
    private List<ItemStack> possibleAnimalTypes;
    private RecruitsCheckBox breedCheckBox;
    private RecruitsCheckBox slaughterCheckBox;
    private RecruitsCheckBox specialCheckBox;
    private BlackShowingTextField maxAnimalShowField;
    public AnimalPenAreaScreen(AnimalPenArea animalPenArea, Player player) {
        super(animalPenArea.getCustomName(), animalPenArea, player);
        this.animalPenArea = animalPenArea;
        this.animalType = animalPenArea.getAnimalType();
        this.special = animalPenArea.getSpecial();
        this.slaughter = animalPenArea.getSlaughter();
        this.breed = animalPenArea.getBreed();
        this.maxAnimals = animalPenArea.getMaxAnimals();
    }

    @Override
    protected void init() {
        super.init();
        this.possibleAnimalTypes = getPossibleAnimalTypes();

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
        int checkBoxY = y + checkBoxHeight / 2 - checkBoxHeight + 50;

        animalTypesScrollDropDownMenu = new ScrollDropDownMenu<>(
                this.animalType,
                x - dropDownWidth / 2, 60 + y + dropDownHeight / 2 - dropDownHeight, dropDownWidth, dropDownHeight,
                List.of(AnimalPenArea.AnimalTypes.values()),
                type -> Component.translatable(type.getTranslationKey()).getString(),
                this::setAnimalType
        );

        animalTypesScrollDropDownMenu.setBgFillSelected(FastColor.ARGB32.color(255, 139, 139, 139));
        addRenderableWidget(animalTypesScrollDropDownMenu);

        maxAnimalShowField = new BlackShowingTextField(checkBoxX, 50 + checkBoxY, checkBoxWidth, checkBoxHeight, Component.literal(TEXT_MAX_ANIMALS.getString() + "   " + maxAnimals));
        addRenderableWidget(maxAnimalShowField);

        maxAnimalsPlusButton = new ExtendedButton(x + 50, 50 + checkBoxY, 20, 20, Component.literal("+"),
                btn -> {
                    if(hasShiftDown()) maxAnimals += 5;
                    else maxAnimals++;
                    maxAnimals = Mth.clamp(maxAnimals, 0, 16);

                    sendUpdate();
                    setButtons();
                }
        );
        addRenderableWidget(maxAnimalsPlusButton);

        maxAnimalsMinusButton = new ExtendedButton(x + 70, 50 + checkBoxY, 20, 20, Component.literal("-"),
                btn -> {
                    if(hasShiftDown()) maxAnimals -= 5;
                    else maxAnimals--;
                    maxAnimals = Mth.clamp(maxAnimals, 0, 16);

                    sendUpdate();
                    setButtons();
                }
        );
        addRenderableWidget(maxAnimalsMinusButton);


        this.breedCheckBox = new RecruitsCheckBox(checkBoxX, 70 + checkBoxY, checkBoxWidth, checkBoxHeight, TEXT_BREED,
                this.breed,
                (bool) -> {
                    this.breed = bool;
                    sendUpdate();
                }
        );
        addRenderableWidget(breedCheckBox);

        this.slaughterCheckBox = new RecruitsCheckBox(checkBoxX, 90 + checkBoxY, checkBoxWidth, checkBoxHeight, TEXT_SLAUGHTER,
                this.slaughter,
                (bool) -> {
                    this.slaughter = bool;
                    sendUpdate();
                }
        );
        addRenderableWidget(slaughterCheckBox);

        this.specialCheckBox = new RecruitsCheckBox(checkBoxX, 110 + checkBoxY, checkBoxWidth, checkBoxHeight, TEXT_SPECIAL,
                this.special,
                (bool) -> {
                    this.special = bool;
                    sendUpdate();
                }
        );
        addRenderableWidget(specialCheckBox);
        checkSpecialCheckBox();
    }

    public String setAnimalType(AnimalPenArea.AnimalTypes animalType) {
        this.animalType = animalType;

        checkSpecialCheckBox();

        sendUpdate();
        return animalType.name();
    }

    public void sendUpdate(){
        WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateAnimalPenArea(animalPenArea.getUUID(), animalType, breed, slaughter, special, maxAnimals));
    }


    public void checkSpecialCheckBox(){
        boolean active = false;

        if(animalType == AnimalPenArea.AnimalTypes.CHICKEN){
            TEXT_SPECIAL = TEXT_THROW_EGGS;
            active = true;
        }
        else if(animalType == AnimalPenArea.AnimalTypes.COW){
            TEXT_SPECIAL = TEXT_MILK_COW;
            active = true;
        }
        else if(animalType == AnimalPenArea.AnimalTypes.SHEEP){
            TEXT_SPECIAL = TEXT_SHEAR_SHEEP;
            active = true;
        }

        specialCheckBox.active = active;
        specialCheckBox.visible = active;
        specialCheckBox.setMessage(TEXT_SPECIAL);
    }

    private List<ItemStack> getPossibleAnimalTypes() {
        List<ItemStack> stacks = new ArrayList<>();



        return stacks;
    }

    @Override
    public void mouseMoved(double x, double y) {
        if(animalTypesScrollDropDownMenu != null){
            animalTypesScrollDropDownMenu.onMouseMove(x,y);
        }
        super.mouseMoved(x, y);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (animalTypesScrollDropDownMenu != null && animalTypesScrollDropDownMenu.isMouseOver(mouseX, mouseY)) {
            animalTypesScrollDropDownMenu.onMouseClick(mouseX, mouseY);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    @Override
    public boolean mouseScrolled(double x, double y, double d) {
        if(animalTypesScrollDropDownMenu != null) animalTypesScrollDropDownMenu.mouseScrolled(x,y,d);
        return super.mouseScrolled(x, y, d);
    }
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        if (animalTypesScrollDropDownMenu != null) {
            animalTypesScrollDropDownMenu.renderWidget(guiGraphics, mouseX, mouseY, partialTicks);
        }
    }
}
