package com.talhanation.workers.client.gui.widgets;

import com.talhanation.recruits.client.gui.widgets.ScrollDropDownMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Consumer;

public class ItemScrollDropDownMenu extends ScrollDropDownMenu<ItemStack> {

    public ItemScrollDropDownMenu(ItemStack selected, int x, int y, int width, List<ItemStack> options, Consumer<ItemStack> onSelect) {
        super(selected, x, y, width, 20, options, stack -> stack.getHoverName().getString(), onSelect);
    }
    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        super.renderWidget(guiGraphics, mouseX, mouseY, delta);
        if(this.visible){
            int baseY = this.getY() + this.height;
            int xTextOffset = this.getX() + 20; // Text etwas rechts vom ItemIcon
            int iconX = this.getX() + 2;
            int iconY = this.getY() + 2;

            ItemStack selected = this.getSelected();
            selected.setCount(1);

            guiGraphics.renderFakeItem(selected, iconX, iconY);
            guiGraphics.renderItemDecorations(Minecraft.getInstance().font, selected, iconX, iconY);

            if(getIsOpen()){
                List<ItemStack> options = this.getOptions(); // optional Hilfsmethode für Zugriff auf protected List

                for (int i = 0; i < options.size(); i++) {
                    int optionY = baseY + (i - getScrollOffset()) * 20;
                    if (optionY + 20 < baseY || optionY > baseY + 20 * getMaxVisibleOptions()) continue;

                    ItemStack stack = options.get(i);
                    iconY = optionY + 2; // leicht eingerückt
                    stack.setCount(1);
                    // Render Item
                    guiGraphics.renderFakeItem(stack, iconX, iconY);
                    guiGraphics.renderItemDecorations(Minecraft.getInstance().font, stack, iconX, iconY);

                    // Text neu rendern mit Versatz (übermalt den aus Parent)
                    guiGraphics.drawString(Minecraft.getInstance().font, stack.getHoverName(), xTextOffset, optionY + 6, 0xFFFFFF);
                }
            }
        }


        if (this.getIsOpen() && this.visible) {

        }
    }

    @SuppressWarnings("unchecked")
    private List<ItemStack> getOptions() {
        try {
            Field field = ScrollDropDownMenu.class.getDeclaredField("options");
            field.setAccessible(true);
            return (List<ItemStack>) field.get(this);
        } catch (Exception e) {
            return List.of();
        }
    }

    private int getScrollOffset() {
        try {
            Field field = ScrollDropDownMenu.class.getDeclaredField("scrollOffset");
            field.setAccessible(true);
            return field.getInt(this);
        } catch (Exception e) {
            return 0;
        }
    }

    private int getMaxVisibleOptions() {
        try {
            Field field = ScrollDropDownMenu.class.getDeclaredField("maxVisibleOptions");
            field.setAccessible(true);
            return field.getInt(this);
        } catch (Exception e) {
            return 5;
        }
    }

    private boolean getIsOpen() {
        try {
            Field field = ScrollDropDownMenu.class.getDeclaredField("isOpen");
            field.setAccessible(true);
            return field.getBoolean(this);
        } catch (Exception e) {
            return false;
        }
    }

    private ItemStack getSelected(){
        try {
            Field field = ScrollDropDownMenu.class.getDeclaredField("selectedOption");
            field.setAccessible(true);
            return (ItemStack) field.get(this);
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }
}

