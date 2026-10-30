package com.talhanation.workers.client.gui.widgets;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * Small item search: a text field and a grid with all items whose name or id contains the text.
 * Lets the player set up ghost slots without having the item. The screen adds {@link #getBox()} as widget
 * and forwards render, clicks and scrolling.
 */
@OnlyIn(Dist.CLIENT)
public class ItemSearchWidget {
    public static final int COLUMNS = 6;
    public static final int ROWS = 7;
    public static final int CELL = 18;
    public static final int WIDTH = COLUMNS * CELL;
    private static final int MAX_RESULTS = 600;
    private static final Component HINT = Component.translatable("gui.workers.search.hint");
    private static final Component NO_RESULTS = Component.translatable("gui.workers.search.noResults");

    private final Font font;
    private final EditBox box;
    private final Predicate<Item> filter;
    private final List<ItemStack> results = new ArrayList<>();
    private String lastQuery = "";
    private int scrollRow;
    private int x;
    private int y;

    public ItemSearchWidget(Font font, int x, int y, Predicate<Item> filter) {
        this.font = font;
        this.filter = filter;
        this.box = new EditBox(font, x, y, WIDTH, 16, Component.empty());
        this.box.setMaxLength(50);
        this.box.setHint(HINT);
        setPosition(x, y);
    }

    public EditBox getBox() {
        return box;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
        box.setX(x);
        box.setY(y);
    }

    public boolean isFocused() {
        return box.isFocused();
    }

    /** Searches again with the same text, e.g. after the filter changed. */
    public void refresh() {
        lastQuery = "\0";
    }

    private void updateResults() {
        String query = box.getValue().trim().toLowerCase(Locale.ROOT);
        if (query.equals(lastQuery)) return;
        lastQuery = query;
        results.clear();
        scrollRow = 0;
        if (query.isEmpty()) return;

        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR || !filter.test(item)) continue;
            ItemStack stack = new ItemStack(item);
            String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
            String id = BuiltInRegistries.ITEM.getKey(item).toString();
            if (name.contains(query) || id.contains(query)) results.add(stack);
            if (results.size() >= MAX_RESULTS) break;
        }
    }

    private int gridTop() {
        return y + 18;
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        updateResults();
        if (lastQuery.isEmpty()) return;

        int top = gridTop();
        guiGraphics.fill(x - 1, top - 1, x + WIDTH + 1, top + ROWS * CELL + 1, 0xC0000000);
        if (results.isEmpty()) {
            guiGraphics.drawString(font, NO_RESULTS, x + 3, top + 4, 0xA0A0A0, false);
            return;
        }

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                int index = (scrollRow + row) * COLUMNS + col;
                if (index >= results.size()) return;
                int cx = x + col * CELL;
                int cy = top + row * CELL;
                if (mouseX >= cx && mouseX < cx + CELL && mouseY >= cy && mouseY < cy + CELL) {
                    guiGraphics.fill(cx, cy, cx + CELL, cy + CELL, 0x60FFFFFF);
                }
                guiGraphics.renderFakeItem(results.get(index), cx + 1, cy + 1);
            }
        }
    }

    /** Tooltip of the hovered result, call after everything else is rendered. */
    public void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY, @Nullable Component hint) {
        ItemStack stack = getItemAt(mouseX, mouseY);
        if (stack == null) return;
        List<Component> lines = new ArrayList<>();
        lines.add(stack.getHoverName());
        if (hint != null) lines.add(hint);
        guiGraphics.renderComponentTooltip(font, lines, mouseX, mouseY);
    }

    @Nullable
    public ItemStack getItemAt(double mouseX, double mouseY) {
        if (lastQuery.isEmpty() || results.isEmpty()) return null;
        int top = gridTop();
        if (mouseX < x || mouseX >= x + WIDTH || mouseY < top || mouseY >= top + ROWS * CELL) return null;
        int col = (int) (mouseX - x) / CELL;
        int row = (int) (mouseY - top) / CELL;
        int index = (scrollRow + row) * COLUMNS + col;
        return index < results.size() ? results.get(index) : null;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int top = gridTop();
        if (lastQuery.isEmpty() || mouseX < x || mouseX >= x + WIDTH || mouseY < top || mouseY >= top + ROWS * CELL) return false;
        int rows = (results.size() + COLUMNS - 1) / COLUMNS;
        scrollRow = Mth.clamp(scrollRow - (int) Math.signum(delta), 0, Math.max(0, rows - ROWS));
        return true;
    }

    /** While the field is focused it takes all keys, so e.g. the inventory key does not close the screen. Escape still closes. */
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!box.isFocused() || keyCode == 256) return false;
        box.keyPressed(keyCode, scanCode, modifiers);
        return true;
    }
}
