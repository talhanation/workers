package com.talhanation.workers.client.gui.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Consumer;


public class ItemScrollDropDownMenu extends AbstractWidget {
    private int bgFill = FastColor.ARGB32.color(255, 60, 60, 60);
    private int bgFillHovered = FastColor.ARGB32.color(255, 100, 100, 100);
    private int bgFillSelected = FastColor.ARGB32.color(255, 10, 10, 10);
    private int displayColor = FastColor.ARGB32.color(255, 255, 255, 255);
    private int optionTextColor = FastColor.ARGB32.color(255, 255, 255, 255);
    private int scrollbarColor = FastColor.ARGB32.color(255, 100, 100, 100);
    private int scrollbarHandleColor = FastColor.ARGB32.color(255, 150, 150, 150);
    private final List<ItemStack> options;
    private final Consumer<ItemStack> onSelect;
    private ItemStack selectedOption;
    private boolean isOpen;
    private final int optionHeight;
    private int scrollOffset = 0;
    private int maxVisibleOptions;
    private boolean isScrolling = false;
    private int scrollbarWidth = 6;
    private int scrollbarHandleHeight;

    public ItemScrollDropDownMenu(ItemStack selectedOption, int x, int y, int width, int height, List<ItemStack> options, Consumer<ItemStack> onSelect) {
        super(x, y, width, height, Component.literal(""));
        this.selectedOption = selectedOption;
        this.options = options;
        this.onSelect = onSelect;
        this.optionHeight = height;
        this.maxVisibleOptions = Math.min(5, options.size());
        this.scrollbarHandleHeight = Math.max(10, (int)((float)this.maxVisibleOptions / (float)options.size() * (float)(height * this.maxVisibleOptions)));
    }

    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        if (this.visible) {
            if (this.isMouseOverDisplay(mouseX, mouseY)) {
                guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, this.bgFillHovered);
            } else {
                guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, this.bgFillSelected);
            }

            guiGraphics.drawCenteredString(Minecraft.getInstance().font, selectedOption.getHoverName().getString(), this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2, this.displayColor);

            int iconX = this.getX() + 2;
            int iconY = this.getY() + 2;

            selectedOption.setCount(1);
            guiGraphics.renderFakeItem(selectedOption, iconX, iconY);
            guiGraphics.renderItemDecorations(Minecraft.getInstance().font, selectedOption, iconX, iconY);

            if (this.isOpen) {
                int dropdownHeight = this.maxVisibleOptions * this.optionHeight;
                guiGraphics.fill(this.getX(), this.getY() + this.height, this.getX() + this.width, this.getY() + this.height + dropdownHeight, this.bgFill);
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(0.0F, 0.0F, 500.0F);
                RenderSystem.enableScissor((int)((double)this.getX() * Minecraft.getInstance().getWindow().getGuiScale()), (int)((double)Minecraft.getInstance().getWindow().getHeight() - (double)(this.getY() + this.height + dropdownHeight) * Minecraft.getInstance().getWindow().getGuiScale()), (int)((double)this.width * Minecraft.getInstance().getWindow().getGuiScale()), (int)((double)dropdownHeight * Minecraft.getInstance().getWindow().getGuiScale()));

                int i;
                int optionY;
                for(i = 0; i < this.options.size(); ++i) {
                    optionY = this.getY() + this.height + (i - this.scrollOffset) * this.optionHeight;
                    if (this.isMouseOverOption(mouseX, mouseY, optionY)) {
                        guiGraphics.fill(this.getX(), optionY, this.getX() + this.width, optionY + this.optionHeight, this.bgFillHovered);
                    } else {
                        guiGraphics.fill(this.getX(), optionY, this.getX() + this.width, optionY + this.optionHeight, this.bgFill);
                    }

                    ItemStack stack = this.options.get(i);
                    String text = stack.getHoverName().getString();
                    guiGraphics.drawCenteredString(Minecraft.getInstance().font, text, this.getX() + this.width / 2, optionY + (this.optionHeight - 8) / 2, this.optionTextColor);
                    iconY = optionY + 2;

                    stack.setCount(1);
                    guiGraphics.renderFakeItem(stack, iconX, iconY);
                    guiGraphics.renderItemDecorations(Minecraft.getInstance().font, stack, iconX, iconY);
                }

                RenderSystem.disableScissor();
                if (this.options.size() > this.maxVisibleOptions) {
                    i = this.getX() + this.width - this.scrollbarWidth;
                    optionY = this.getY() + this.height + (int)((float)this.scrollOffset / (float)this.options.size() * (float)dropdownHeight);
                    int scrollbarHeight = (int)((float)this.maxVisibleOptions / (float)this.options.size() * (float)dropdownHeight);
                    guiGraphics.fill(i, this.getY() + this.height, i + this.scrollbarWidth, this.getY() + this.height + dropdownHeight, this.scrollbarColor);
                    guiGraphics.fill(i, optionY, i + this.scrollbarWidth, optionY + scrollbarHeight, this.scrollbarHandleColor);
                }

                guiGraphics.pose().popPose();
            }

        }
    }

    public void onClick(double mouseX, double mouseY) {
    }

    public void onMouseClick(double mouseX, double mouseY) {
        if (this.visible) {
            if (this.isOpen) {
                if (this.isMouseOverScrollbar((int)mouseX, (int)mouseY)) {
                    this.isScrolling = true;
                    return;
                }

                for(int i = 0; i < this.options.size(); ++i) {
                    int optionY = this.getY() + this.height + (i - this.scrollOffset) * this.optionHeight;
                    if (this.isMouseOverOption((int)mouseX, (int)mouseY, optionY)) {
                        this.selectOption(this.options.get(i));
                        return;
                    }
                }
            }

            if (this.isMouseOverDisplay((int)mouseX, (int)mouseY)) {
                this.isOpen = !this.isOpen;
            } else {
                this.isOpen = false;
            }

        }
    }

    public void onMouseMove(double mouseX, double mouseY) {
        if (this.visible) {
            if (this.isOpen) {
                boolean isOverDropdown = this.isMouseOverDropdown((int)mouseX, (int)mouseY);
                boolean isOverDisplay = this.isMouseOverDisplay((int)mouseX, (int)mouseY);
                if (!isOverDropdown && !isOverDisplay) {
                    this.isOpen = false;
                }
            }

            if (this.isScrolling) {
                int dropdownHeight = this.maxVisibleOptions * this.optionHeight;
                int scrollbarY = (int)mouseY - (this.getY() + this.height);
                this.scrollOffset = (int)((float)scrollbarY / (float)dropdownHeight * (float)this.options.size());
                this.scrollOffset = Math.max(0, Math.min(this.scrollOffset, this.options.size() - this.maxVisibleOptions));
            }

        }
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!this.visible) {
            return false;
        } else if (this.isOpen) {
            this.scrollOffset -= (int)delta;
            this.scrollOffset = Math.max(0, Math.min(this.scrollOffset, this.options.size() - this.maxVisibleOptions));
            return true;
        } else {
            return false;
        }
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!this.visible) {
            return false;
        } else if (this.isScrolling) {
            this.isScrolling = false;
            return true;
        } else {
            return false;
        }
    }

    private boolean isMouseOverScrollbar(int mouseX, int mouseY) {
        if (this.isOpen && this.options.size() > this.maxVisibleOptions) {
            int scrollbarX = this.getX() + this.width - this.scrollbarWidth;
            int scrollbarY = this.getY() + this.height;
            int scrollbarHeight = this.maxVisibleOptions * this.optionHeight;
            return mouseX >= scrollbarX && mouseX <= scrollbarX + this.scrollbarWidth && mouseY >= scrollbarY && mouseY <= scrollbarY + scrollbarHeight;
        } else {
            return false;
        }
    }

    private boolean isMouseOverDisplay(int mouseX, int mouseY) {
        return mouseX >= this.getX() && mouseX <= this.getX() + this.width && mouseY >= this.getY() && mouseY <= this.getY() + this.height;
    }

    private boolean isMouseOverDropdown(int mouseX, int mouseY) {
        if (!this.isOpen) {
            return false;
        } else {
            int dropdownStartX = this.getX();
            int dropdownStartY = this.getY() + this.height;
            int dropdownEndX = dropdownStartX + this.width;
            int dropdownEndY = dropdownStartY + this.maxVisibleOptions * this.optionHeight;
            return mouseX >= dropdownStartX && mouseX <= dropdownEndX && mouseY >= dropdownStartY && mouseY <= dropdownEndY;
        }
    }

    private boolean isMouseOverOption(int mouseX, int mouseY, int optionY) {
        return mouseX >= this.getX() && mouseX <= this.getX() + this.width && mouseY >= optionY && mouseY <= optionY + this.optionHeight;
    }

    public boolean isMouseOver(double x, double y) {
        return this.isMouseOverDisplay((int)x, (int)y) || this.isMouseOverDropdown((int)x, (int)y) || this.isMouseOverScrollbar((int)x, (int)y) || super.isMouseOver(x, y);
    }

    protected void updateWidgetNarration(NarrationElementOutput p_259858_) {
    }

    private void selectOption(ItemStack option) {
        this.selectedOption = option;
        this.onSelect.accept(option);
        this.isOpen = false;
    }

    public void setBgFill(int bgFill) {
        this.bgFill = bgFill;
    }

    public void setBgFillHovered(int bgFillHovered) {
        this.bgFillHovered = bgFillHovered;
    }

    public void setBgFillSelected(int bgFillSelected) {
        this.bgFillSelected = bgFillSelected;
    }

    public void setDisplayColor(int displayColor) {
        this.displayColor = displayColor;
    }

    public void setOptionTextColor(int optionTextColor) {
        this.optionTextColor = optionTextColor;
    }
}

