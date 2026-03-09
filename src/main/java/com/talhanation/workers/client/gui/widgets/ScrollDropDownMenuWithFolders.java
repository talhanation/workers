package com.talhanation.workers.client.gui.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import com.talhanation.workers.WorkersMain;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public class ScrollDropDownMenuWithFolders extends AbstractWidget {

    private static final ResourceLocation FOLDER_ICON =
            new ResourceLocation(WorkersMain.MOD_ID, "textures/gui/folder_image.png");

    private static final ItemStack PICKAXE_STACK = new ItemStack(Items.IRON_PICKAXE);

    private static final int ICON_SIZE   = 16;
    private static final int ICON_MARGIN = 2;

    // ── colours ────────────────────────────────────────────────────────────────
    private int bgFill            = FastColor.ARGB32.color(255,  60,  60,  60);
    private int bgFillHovered     = FastColor.ARGB32.color(255, 100, 100, 100);
    private int bgFillSelected    = FastColor.ARGB32.color(255,  10,  10,  10);
    private int displayColor      = FastColor.ARGB32.color(255, 255, 255, 255);
    private int optionTextColor   = FastColor.ARGB32.color(255, 255, 255, 255);
    private int scrollbarColor    = FastColor.ARGB32.color(255, 100, 100, 100);
    private int scrollbarHandleColor = FastColor.ARGB32.color(255, 150, 150, 150);

    private final Path scanRoot;
    private Path currentRelativePath;

    private List<Entry> entries = new ArrayList<>();

    private final Consumer<String> onSelectNbt;

    private boolean isOpen       = false;
    private boolean isScrolling  = false;
    private int scrollOffset     = 0;
    private int maxVisibleOptions;

    private final int optionHeight;
    private final int scrollbarWidth = 6;

    private record Entry(String displayName, boolean isFolder, boolean isBack, String relativePath) {
        static Entry back() {
            return new Entry("../  (back)", true, true, null);
        }
        static Entry folder(String name, String relPath) {
            return new Entry(name, true, false, relPath);
        }
        static Entry nbt(String name, String relPath) {
            return new Entry(name, false, false, relPath);
        }
    }

    public ScrollDropDownMenuWithFolders(int x, int y, int width, int height,
                                         Path scanRoot,
                                         Consumer<String> onSelectNbt) {
        super(x, y, width, height, Component.empty());
        this.scanRoot            = scanRoot;
        this.currentRelativePath = Path.of("");
        this.onSelectNbt         = onSelectNbt;
        this.optionHeight        = height;
        navigateTo(Path.of(""));
    }

    private void navigateTo(Path relativePath) {
        this.currentRelativePath = relativePath;
        this.scrollOffset        = 0;
        this.entries             = scanDirectory(scanRoot.resolve(relativePath), relativePath);
        this.maxVisibleOptions   = Math.min(5, Math.max(1, entries.size()));
    }

    private List<Entry> scanDirectory(Path dir, Path relativePath) {
        List<Entry> result = new ArrayList<>();

        if (!relativePath.toString().isEmpty()) {
            result.add(Entry.back());
        }

        if (!Files.exists(dir) || !Files.isDirectory(dir)) return result;

        List<Entry> folders = new ArrayList<>();
        List<Entry> files   = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path child : stream) {
                String name = child.getFileName().toString();
                Path childRel = relativePath.toString().isEmpty()
                        ? Path.of(name)
                        : relativePath.resolve(name);
                String relStr = childRel.toString().replace('\\', '/');

                if (Files.isDirectory(child)) {
                    folders.add(Entry.folder(name, relStr));
                } else if (name.endsWith(".nbt")) {
                    String nameNoExt = name.substring(0, name.length() - 4);
                    Path nbtRel = relativePath.toString().isEmpty()
                            ? Path.of(nameNoExt)
                            : relativePath.resolve(nameNoExt);
                    files.add(Entry.nbt(nameNoExt, nbtRel.toString().replace('\\', '/')));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        folders.sort(Comparator.comparing(e -> e.displayName().toLowerCase()));
        files  .sort(Comparator.comparing(e -> e.displayName().toLowerCase()));

        result.addAll(folders);
        result.addAll(files);
        return result;
    }

    @Override
    public void renderWidget(GuiGraphics gui, int mouseX, int mouseY, float delta) {
        if (!this.visible) return;

        int headerBg = isMouseOverDisplay(mouseX, mouseY) ? bgFillHovered : bgFillSelected;
        gui.fill(getX(), getY(), getX() + width, getY() + height, headerBg);

        String headerText = currentRelativePath.toString().isEmpty()
                ? "Select NBT-File"
                : currentRelativePath.toString().replace('\\', '/');
        if (!currentRelativePath.toString().isEmpty()) {
            renderFolderIcon(gui, getX() + ICON_MARGIN, getY() + (height - ICON_SIZE) / 2);
        }
        gui.drawCenteredString(Minecraft.getInstance().font, headerText,
                getX() + width / 2, getY() + (height - 8) / 2, displayColor);

        if (!isOpen) return;

        int dropdownHeight = maxVisibleOptions * optionHeight;
        int dropTop = getY() + height;

        gui.fill(getX(), dropTop, getX() + width, dropTop + dropdownHeight, bgFill);
        gui.pose().pushPose();
        gui.pose().translate(0, 0, 500);

        double scale = Minecraft.getInstance().getWindow().getGuiScale();
        int winH = Minecraft.getInstance().getWindow().getHeight();
        RenderSystem.enableScissor(
                (int)(getX() * scale),
                (int)(winH - (dropTop + dropdownHeight) * scale),
                (int)(width * scale),
                (int)(dropdownHeight * scale)
        );

        for (int i = 0; i < entries.size(); i++) {
            int optionY = dropTop + (i - scrollOffset) * optionHeight;
            Entry entry  = entries.get(i);

            int bg = isMouseOverOption(mouseX, mouseY, optionY) ? bgFillHovered : bgFill;
            gui.fill(getX(), optionY, getX() + width, optionY + optionHeight, bg);

            int textX = getX() + ICON_MARGIN;
            int textCenterX = getX() + width / 2;

            if (entry.isFolder()) {
                renderFolderIcon(gui, textX, optionY + (optionHeight - ICON_SIZE) / 2);
                int labelX = textX + ICON_SIZE + ICON_MARGIN;
                int labelWidth = width - ICON_SIZE - ICON_MARGIN * 3;
                String label = clampText(entry.displayName(), labelWidth);
                gui.drawString(Minecraft.getInstance().font, label,
                        labelX, optionY + (optionHeight - 8) / 2, optionTextColor);
            } else {
                int iconY2 = optionY + (optionHeight - ICON_SIZE) / 2;
                gui.renderFakeItem(PICKAXE_STACK, textX, iconY2);
                int labelX = textX + ICON_SIZE + ICON_MARGIN;
                int labelWidth = width - ICON_SIZE - ICON_MARGIN * 3;
                String label = clampText(entry.displayName(), labelWidth);
                gui.drawString(Minecraft.getInstance().font, label,
                        labelX, optionY + (optionHeight - 8) / 2, optionTextColor);
            }
        }

        RenderSystem.disableScissor();

        if (entries.size() > maxVisibleOptions) {
            int sbX = getX() + width - scrollbarWidth;
            int handleY = dropTop + (int)((float) scrollOffset / entries.size() * dropdownHeight);
            int handleH = Math.max(10, (int)((float) maxVisibleOptions / entries.size() * dropdownHeight));
            gui.fill(sbX, dropTop, sbX + scrollbarWidth, dropTop + dropdownHeight, scrollbarColor);
            gui.fill(sbX, handleY, sbX + scrollbarWidth, handleY + handleH, scrollbarHandleColor);
        }

        gui.pose().popPose();
    }

    private void renderFolderIcon(GuiGraphics gui, int x, int y) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        gui.blit(FOLDER_ICON, x, y, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
    }

    private String clampText(String text, int maxWidth) {
        var font = Minecraft.getInstance().font;
        if (font.width(text) <= maxWidth) return text;
        while (text.length() > 1 && font.width(text + "…") > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }
        return text + "…";
    }

    public void onMouseClick(double mouseX, double mouseY) {
        if (!visible) return;

        if (isOpen) {
            if (isMouseOverScrollbar((int) mouseX, (int) mouseY)) {
                isScrolling = true;
                return;
            }

            for (int i = 0; i < entries.size(); i++) {
                int optionY = getY() + height + (i - scrollOffset) * optionHeight;
                if (isMouseOverOption((int) mouseX, (int) mouseY, optionY)) {
                    Entry entry = entries.get(i);
                    if (entry.isBack()) {
                        Path parent = currentRelativePath.getParent();
                        navigateTo(parent == null ? Path.of("") : parent);
                    } else if (entry.isFolder()) {
                        navigateTo(Path.of(entry.relativePath()));
                    } else {
                        isOpen = false;
                        onSelectNbt.accept(entry.relativePath());
                    }
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
            if (!isMouseOverDropdown((int) mouseX, (int) mouseY)
                    && !isMouseOverDisplay((int) mouseX, (int) mouseY)) {
                isOpen = false;
            }
        }

        if (isScrolling) {
            int dropdownHeight = maxVisibleOptions * optionHeight;
            int relY = (int) mouseY - (getY() + height);
            scrollOffset = (int)((float) relY / dropdownHeight * entries.size());
            scrollOffset = Math.max(0, Math.min(scrollOffset, entries.size() - maxVisibleOptions));
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!visible || !isOpen) return false;
        scrollOffset -= (int) delta;
        scrollOffset = Math.max(0, Math.min(scrollOffset, entries.size() - maxVisibleOptions));
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!visible) return false;
        if (isScrolling) { isScrolling = false; return true; }
        return false;
    }

    @Override
    public boolean isMouseOver(double x, double y) {
        return isMouseOverDisplay((int) x, (int) y)
                || isMouseOverDropdown((int) x, (int) y)
                || isMouseOverScrollbar((int) x, (int) y)
                || super.isMouseOver(x, y);
    }

    private boolean isMouseOverDisplay(int mx, int my) {
        return mx >= getX() && mx <= getX() + width && my >= getY() && my <= getY() + height;
    }

    private boolean isMouseOverDropdown(int mx, int my) {
        if (!isOpen) return false;
        int dropTop = getY() + height;
        int dropBot = dropTop + maxVisibleOptions * optionHeight;
        return mx >= getX() && mx <= getX() + width && my >= dropTop && my <= dropBot;
    }

    private boolean isMouseOverScrollbar(int mx, int my) {
        if (!isOpen || entries.size() <= maxVisibleOptions) return false;
        int sbX = getX() + width - scrollbarWidth;
        int dropTop = getY() + height;
        int dropBot = dropTop + maxVisibleOptions * optionHeight;
        return mx >= sbX && mx <= sbX + scrollbarWidth && my >= dropTop && my <= dropBot;
    }

    private boolean isMouseOverOption(int mx, int my, int optionY) {
        return mx >= getX() && mx <= getX() + width
                && my >= optionY && my <= optionY + optionHeight;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput out) {}
}
