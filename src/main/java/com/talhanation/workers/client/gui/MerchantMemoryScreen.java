package com.talhanation.workers.client.gui;

import com.talhanation.workers.WorkersMain;
import com.talhanation.workers.entities.MerchantEntity;
import com.talhanation.workers.entities.ai.MerchantTripGoal;
import com.talhanation.workers.network.MessageUpdateMerchantMemory;
import com.talhanation.workers.world.MerchantMemory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.widget.ExtendedButton;

import java.util.*;

/**
 * Markets the travelling merchant knows. A click on a market opens it and shows what it buys and sells as text.
 * Markets he can not reach before the night are grayed out. Star = favorite (visited first), cross = avoid.
 */
@OnlyIn(Dist.CLIENT)
public class MerchantMemoryScreen extends Screen {

    private static final MutableComponent BUTTON_BACK     = Component.translatable("gui.workers.button.back");
    private static final MutableComponent TEXT_EMPTY      = Component.translatable("gui.workers.memory.empty");
    private static final MutableComponent TOOLTIP_FAVORITE = Component.translatable("gui.workers.memory.tooltip.favorite");
    private static final MutableComponent TOOLTIP_AVOID   = Component.translatable("gui.workers.memory.tooltip.avoid");
    private static final MutableComponent TEXT_UNREACHABLE = Component.translatable("gui.workers.memory.unreachable");
    private static final int PANEL_WIDTH = 280;
    private static final int HEADER_HEIGHT = 20;
    private static final int LINE_HEIGHT = 10;
    private static final int PADDING = 6;
    private static final int COLOR_TEXT = 0xFFFFFF;
    private static final int COLOR_GRAY = 0x808080;
    private static final int COLOR_INFO = 0xC0C0C0;
    private static final int COLOR_FAVORITE = 0xFFD700;
    private static final int COLOR_AVOID = 0xFF5555;

    private final MerchantEntity merchant;
    private final Player player;
    private final List<MerchantMemory.Entry> entries;
    private final Set<UUID> expanded = new HashSet<>();
    private int left;
    private int top;
    private int listTop;
    private int listBottom;
    private double scroll;
    private int contentHeight;

    public MerchantMemoryScreen(MerchantEntity merchant, Player player, List<MerchantMemory.Entry> entries) {
        super(Component.translatable("gui.workers.memory.title", entries.size()));
        this.merchant = merchant;
        this.player = player;
        this.entries = new ArrayList<>(entries);
        // favorites first, avoided last, then nearest
        this.entries.sort(Comparator.comparingInt((MerchantMemory.Entry e) -> e.favorite ? 0 : e.avoid ? 2 : 1)
                .thenComparingDouble(this::getDistance));
    }

    @Override
    protected void init() {
        super.init();
        left = (width - PANEL_WIDTH) / 2;
        top = (int) (height * 0.1);
        listTop = top + 20;
        listBottom = height - (int) (height * 0.1) - 28;

        addRenderableWidget(new ExtendedButton(left + PANEL_WIDTH / 2 - 40, listBottom + 6, 80, 20, BUTTON_BACK,
                button -> onClose()));
    }

    @Override
    public void onClose() {
        super.onClose();
        merchant.openTradeGUI(player);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        renderBackground(guiGraphics);
        guiGraphics.fill(left, top, left + PANEL_WIDTH, listBottom + 32, 0xC0101010);
        guiGraphics.renderOutline(left, top, PANEL_WIDTH, listBottom + 32 - top, 0xFF555555);
        guiGraphics.drawCenteredString(font, title, left + PANEL_WIDTH / 2, top + 6, COLOR_TEXT);

        if (entries.isEmpty()) {
            guiGraphics.drawCenteredString(font, TEXT_EMPTY, left + PANEL_WIDTH / 2, (listTop + listBottom) / 2, COLOR_GRAY);
        }

        Component tooltip = null;
        guiGraphics.enableScissor(left, listTop, left + PANEL_WIDTH, listBottom);
        int y = listTop - (int) scroll;
        long dayTime = merchant.getCommandSenderWorld().getDayTime();
        for (MerchantMemory.Entry entry : entries) {
            boolean reachable = isReachable(entry, dayTime);
            boolean open = expanded.contains(entry.market);
            int nameColor = entry.avoid || !reachable ? COLOR_GRAY : COLOR_TEXT;

            if (y + HEADER_HEIGHT > listTop && y < listBottom) {
                boolean hovered = mouseX >= left && mouseX < left + PANEL_WIDTH && mouseY >= y && mouseY < y + HEADER_HEIGHT
                        && mouseY >= listTop && mouseY < listBottom;
                if (hovered) guiGraphics.fill(left + 2, y, left + PANEL_WIDTH - 2, y + HEADER_HEIGHT, 0x30FFFFFF);

                String arrow = open ? "\u25BE " : "\u25B8 ";
                guiGraphics.drawString(font, arrow + entry.name, left + PADDING, y + 6, nameColor, false);

                // right side: distance, favorite, avoid
                int starX = left + PANEL_WIDTH - PADDING - 22;
                int crossX = left + PANEL_WIDTH - PADDING - 8;
                String distance = (int) getDistance(entry) + " m";
                guiGraphics.drawString(font, distance, starX - 8 - font.width(distance), y + 6, nameColor, false);
                guiGraphics.drawString(font, "\u2605", starX, y + 6, entry.favorite ? COLOR_FAVORITE : COLOR_GRAY, false);
                guiGraphics.drawString(font, "\u2716", crossX, y + 6, entry.avoid ? COLOR_AVOID : COLOR_GRAY, false);

                if (isOver(mouseX, mouseY, starX, y + 6)) tooltip = TOOLTIP_FAVORITE;
                else if (isOver(mouseX, mouseY, crossX, y + 6)) tooltip = TOOLTIP_AVOID;
                else if (hovered && !reachable) tooltip = TEXT_UNREACHABLE;
            }
            y += HEADER_HEIGHT;

            if (open) {
                for (FormattedCharSequence line : getDetailLines(entry)) {
                    if (y + LINE_HEIGHT > listTop && y < listBottom) {
                        guiGraphics.drawString(font, line, left + PADDING + 10, y, COLOR_INFO, false);
                    }
                    y += LINE_HEIGHT;
                }
                y += 4;
            }
        }
        contentHeight = y + (int) scroll - listTop;
        guiGraphics.disableScissor();

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        if (tooltip != null) guiGraphics.renderTooltip(font, tooltip, mouseX, mouseY);
    }

    private List<FormattedCharSequence> getDetailLines(MerchantMemory.Entry entry) {
        int width = PANEL_WIDTH - 2 * PADDING - 10;
        List<FormattedCharSequence> lines = new ArrayList<>();
        lines.addAll(font.split(Component.translatable("gui.workers.memory.owner", entry.ownerName), width));
        long days = Math.max(0, (merchant.getCommandSenderWorld().getGameTime() - entry.lastVisit) / 24000L);
        lines.addAll(font.split(days == 0 ? Component.translatable("gui.workers.memory.lastVisitToday")
                : Component.translatable("gui.workers.memory.lastVisit", days), width));
        lines.addAll(font.split(Component.translatable("gui.workers.memory.buys", listItems(entry.buys)), width));
        lines.addAll(font.split(Component.translatable("gui.workers.memory.sells", listItems(entry.sells)), width));
        return lines;
    }

    private static String listItems(List<String> ids) {
        if (ids.isEmpty()) return "-";
        StringBuilder out = new StringBuilder();
        for (String id : ids) {
            if (out.length() > 0) out.append(", ");
            ResourceLocation location = ResourceLocation.tryParse(id);
            Item item = location != null && BuiltInRegistries.ITEM.containsKey(location) ? BuiltInRegistries.ITEM.get(location) : null;
            out.append(item != null ? item.getDescription().getString() : id);
        }
        return out.toString();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseY >= listTop && mouseY < listBottom && mouseX >= left && mouseX < left + PANEL_WIDTH) {
            int y = listTop - (int) scroll;
            for (MerchantMemory.Entry entry : entries) {
                if (mouseY >= y && mouseY < y + HEADER_HEIGHT) {
                    int starX = left + PANEL_WIDTH - PADDING - 22;
                    int crossX = left + PANEL_WIDTH - PADDING - 8;
                    if (isOver(mouseX, mouseY, starX, y + 6)) {
                        entry.favorite = !entry.favorite;
                        if (entry.favorite) entry.avoid = false;
                        sendUpdate(entry);
                    }
                    else if (isOver(mouseX, mouseY, crossX, y + 6)) {
                        entry.avoid = !entry.avoid;
                        if (entry.avoid) entry.favorite = false;
                        sendUpdate(entry);
                    }
                    else if (!expanded.remove(entry.market)) {
                        expanded.add(entry.market);
                    }
                    return true;
                }
                y += HEADER_HEIGHT;
                if (expanded.contains(entry.market)) y += getDetailLines(entry).size() * LINE_HEIGHT + 4;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int max = Math.max(0, contentHeight - (listBottom - listTop));
        scroll = Mth.clamp(scroll - delta * 12, 0, max);
        return true;
    }

    private void sendUpdate(MerchantMemory.Entry entry) {
        WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateMerchantMemory(merchant.getUUID(), entry.market, entry.favorite, entry.avoid));
    }

    private double getDistance(MerchantMemory.Entry entry) {
        double dx = merchant.getX() - (entry.pos.getX() + 0.5D);
        double dz = merchant.getZ() - (entry.pos.getZ() + 0.5D);
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** Same dimension and arrival plus stay before the night. */
    private boolean isReachable(MerchantMemory.Entry entry, long dayTime) {
        if (!entry.dimension.equals(merchant.getCommandSenderWorld().dimension().location().toString())) return false;
        return MerchantTripGoal.canArriveToday(dayTime, getDistance(entry));
    }

    private boolean isOver(double mouseX, double mouseY, int x, int y) {
        return mouseX >= x - 2 && mouseX < x + 10 && mouseY >= y - 2 && mouseY < y + 10;
    }
}
