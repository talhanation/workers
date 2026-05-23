package com.talhanation.workers.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class CourierDropDownMenu<T> extends AbstractWidget {
    private int bgFill = FastColor.ARGB32.color(255, 60, 60, 60);
    private int bgFillHovered = FastColor.ARGB32.color(255, 100, 100, 100);
    private int bgFillSelected = FastColor.ARGB32.color(255, 10, 10, 10);
    private int displayColor = FastColor.ARGB32.color(255, 255, 255, 255);
    private int optionTextColor = FastColor.ARGB32.color(255, 255, 255, 255);
    private int scrollbarColor = FastColor.ARGB32.color(255, 100, 100, 100);
    private int scrollbarHandleColor = FastColor.ARGB32.color(255, 150, 150, 150);

    private final List<T> options;
    private final Consumer<T> onSelect;
    private final Function<T, String> optionTextGetter;
    @Nullable private final Function<T, List<Component>> tooltipGetter;
    private T selectedOption;
    private boolean isOpen;
    private final int optionHeight;

    private int scrollOffset = 0;
    private final int maxVisibleOptions;
    private boolean isScrolling = false;
    private final int scrollbarWidth = 6;
    public boolean canSelect = true;

    public CourierDropDownMenu(T selectedOption, int x, int y, int width, int height, List<T> options,
                               Function<T, String> optionTextGetter, Consumer<T> onSelect,
                               @Nullable Function<T, List<Component>> tooltipGetter) {
        super(x, y, width, height, Component.literal(""));
        this.selectedOption = selectedOption;
        this.options = options;
        this.onSelect = onSelect;
        this.optionTextGetter = optionTextGetter;
        this.tooltipGetter = tooltipGetter;
        this.optionHeight = height;
        this.maxVisibleOptions = Math.min(5, options.size());
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        if (!visible) return;

        if (isMouseOverDisplay(mouseX, mouseY)) {
            guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, bgFillHovered);
        } else {
            guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, bgFillSelected);
        }

        guiGraphics.drawCenteredString(Minecraft.getInstance().font, getSelectedText(), this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2, displayColor);

        if (isOpen) {
            int dropdownHeight = maxVisibleOptions * optionHeight;
            guiGraphics.fill(this.getX(), this.getY() + this.height, this.getX() + this.width, this.getY() + this.height + dropdownHeight, bgFill);

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 500);
            RenderSystem.enableScissor((int) (this.getX() * Minecraft.getInstance().getWindow().getGuiScale()),
                    (int) (Minecraft.getInstance().getWindow().getHeight() - (this.getY() + this.height + dropdownHeight) * Minecraft.getInstance().getWindow().getGuiScale()),
                    (int) (this.width * Minecraft.getInstance().getWindow().getGuiScale()),
                    (int) (dropdownHeight * Minecraft.getInstance().getWindow().getGuiScale()));

            for (int i = 0; i < options.size(); i++) {
                int optionY = this.getY() + this.height + (i - scrollOffset) * optionHeight;

                if (isMouseOverOption(mouseX, mouseY, optionY)) {
                    guiGraphics.fill(this.getX(), optionY, this.getX() + this.width, optionY + optionHeight, bgFillHovered);
                } else {
                    guiGraphics.fill(this.getX(), optionY, this.getX() + this.width, optionY + optionHeight, bgFill);
                }

                String text = optionTextGetter.apply(options.get(i));
                guiGraphics.drawCenteredString(Minecraft.getInstance().font, text, this.getX() + this.width / 2, optionY + (optionHeight - 8) / 2, optionTextColor);
            }

            RenderSystem.disableScissor();

            if (options.size() > maxVisibleOptions) {
                int scrollbarX = this.getX() + this.width - scrollbarWidth;
                int scrollbarY = this.getY() + this.height + (int) ((float) scrollOffset / options.size() * dropdownHeight);
                int scrollbarHeight = (int) ((float) maxVisibleOptions / options.size() * dropdownHeight);

                guiGraphics.fill(scrollbarX, this.getY() + this.height, scrollbarX + scrollbarWidth, this.getY() + this.height + dropdownHeight, scrollbarColor);
                guiGraphics.fill(scrollbarX, scrollbarY, scrollbarX + scrollbarWidth, scrollbarY + scrollbarHeight, scrollbarHandleColor);
            }

            guiGraphics.pose().popPose();
        }
    }

    /**
     * Returns the tooltip lines for the option currently under the cursor (when open) or for the
     * selected value (when hovering the closed display), or {@code null} if nothing relevant is
     * hovered. The screen should render this last so it draws above the open dropdown.
     */
    @Nullable
    public List<Component> getHoverTooltip(int mouseX, int mouseY) {
        if (!visible || tooltipGetter == null) return null;

        if (isOpen) {
            for (int i = 0; i < options.size(); i++) {
                int visibleIndex = i - scrollOffset;
                if (visibleIndex < 0 || visibleIndex >= maxVisibleOptions) continue;
                int optionY = this.getY() + this.height + visibleIndex * optionHeight;
                if (isMouseOverOption(mouseX, mouseY, optionY)) {
                    return tooltipGetter.apply(options.get(i));
                }
            }
            return null;
        }

        if (isMouseOverDisplay(mouseX, mouseY)) {
            return selectedOption != null ? tooltipGetter.apply(selectedOption) : null;
        }
        return null;
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        // Do not use
    }

    public void onMouseClick(double mouseX, double mouseY) {
        if (!visible) return;
        if (!canSelect) return;
        if (isOpen) {
            if (isMouseOverScrollbar((int) mouseX, (int) mouseY)) {
                isScrolling = true;
                return;
            }
            for (int i = 0; i < options.size(); i++) {
                int optionY = this.getY() + this.height + (i - scrollOffset) * optionHeight;
                if (isMouseOverOption((int) mouseX, (int) mouseY, optionY)) {
                    selectOption(options.get(i));
                    return;
                }
            }
        }

        if (isMouseOverDisplay((int) mouseX, (int) mouseY)) {
            isOpen = !isOpen;
        } else {
            isOpen = false;
        }
    }

    public void onMouseMove(double mouseX, double mouseY) {
        if (!visible) return;

        if (isOpen) {
            boolean isOverDropdown = isMouseOverDropdown((int) mouseX, (int) mouseY);
            boolean isOverDisplay = isMouseOverDisplay((int) mouseX, (int) mouseY);
            if (!isOverDropdown && !isOverDisplay) {
                isOpen = false;
            }
        }

        if (isScrolling) {
            int dropdownHeight = maxVisibleOptions * optionHeight;
            int scrollbarY = (int) mouseY - (this.getY() + this.height);
            scrollOffset = (int) ((float) scrollbarY / dropdownHeight * options.size());
            scrollOffset = Math.max(0, Math.min(scrollOffset, options.size() - maxVisibleOptions));
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!visible) return false;
        if (isOpen) {
            scrollOffset -= (int) delta;
            scrollOffset = Math.max(0, Math.min(scrollOffset, options.size() - maxVisibleOptions));
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!visible) return false;
        if (isScrolling) {
            isScrolling = false;
            return true;
        }
        return false;
    }

    private boolean isMouseOverScrollbar(int mouseX, int mouseY) {
        if (!isOpen || options.size() <= maxVisibleOptions) return false;
        int scrollbarX = this.getX() + this.width - scrollbarWidth;
        int scrollbarY = this.getY() + this.height;
        int scrollbarHeight = maxVisibleOptions * optionHeight;
        return mouseX >= scrollbarX && mouseX <= scrollbarX + scrollbarWidth &&
                mouseY >= scrollbarY && mouseY <= scrollbarY + scrollbarHeight;
    }

    private boolean isMouseOverDisplay(int mouseX, int mouseY) {
        return mouseX >= this.getX() && mouseX <= this.getX() + this.width && mouseY >= this.getY() && mouseY <= this.getY() + this.height;
    }

    private boolean isMouseOverDropdown(int mouseX, int mouseY) {
        if (!isOpen) return false;
        int dropdownStartX = this.getX();
        int dropdownStartY = this.getY() + this.height;
        int dropdownEndX = dropdownStartX + this.width;
        int dropdownEndY = dropdownStartY + maxVisibleOptions * optionHeight;
        return mouseX >= dropdownStartX && mouseX <= dropdownEndX && mouseY >= dropdownStartY && mouseY <= dropdownEndY;
    }

    private boolean isMouseOverOption(int mouseX, int mouseY, int optionY) {
        return mouseX >= this.getX() && mouseX <= this.getX() + this.width && mouseY >= optionY && mouseY <= optionY + optionHeight;
    }

    @Override
    public boolean isMouseOver(double x, double y) {
        return isMouseOverDisplay((int) x, (int) y) || isMouseOverDropdown((int) x, (int) y) || isMouseOverScrollbar((int) x, (int) y) || super.isMouseOver(x, y);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {
    }

    private void selectOption(T option) {
        selectedOption = option;
        onSelect.accept(option);
        isOpen = false;
    }

    private String getSelectedText() {
        return selectedOption != null ? optionTextGetter.apply(selectedOption) : "";
    }

    public void setBgFill(int bgFill) { this.bgFill = bgFill; }
    public void setBgFillHovered(int bgFillHovered) { this.bgFillHovered = bgFillHovered; }
    public void setBgFillSelected(int bgFillSelected) { this.bgFillSelected = bgFillSelected; }
    public void setDisplayColor(int displayColor) { this.displayColor = displayColor; }
    public void setOptionTextColor(int optionTextColor) { this.optionTextColor = optionTextColor; }
}
