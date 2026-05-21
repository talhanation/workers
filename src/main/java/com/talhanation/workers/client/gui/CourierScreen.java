package com.talhanation.workers.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.talhanation.recruits.client.gui.worldmap.WorldMapScreen;
import com.talhanation.recruits.client.gui.widgets.ScrollDropDownMenu;
import com.talhanation.recruits.client.gui.widgets.RecruitsCheckBox;
import com.talhanation.recruits.world.RecruitsRoute;
import com.talhanation.workers.WorkersMain;
import com.talhanation.workers.entities.CourierEntity;
import com.talhanation.workers.inventory.CourierContainer;
import com.talhanation.workers.network.MessageCourierSetRoute;
import com.talhanation.workers.world.CourierAction;
import com.talhanation.workers.world.CourierRoute;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.widget.ExtendedButton;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static net.minecraft.client.gui.components.AbstractWidget.WIDGETS_LOCATION;

public class CourierScreen extends ScreenBase<CourierContainer> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(WorkersMain.MOD_ID, "textures/gui/courier.png");

    private static final int IMG_W = 256;
    private static final int IMG_H = 197;
    private static final int ROUTE_X = 3;
    private static final int ROUTE_Y = 4;
    private static final int ROUTE_W = 200;
    private static final int ROUTE_H = 16;

    private static final int MAP_X = 208;
    private static final int MAP_Y = 4;
    private static final int MAP_W = 44;
    private static final int MAP_H = 16;
    private static final int APPLY_W = 60;
    private static final int APPLY_H = 20;

    private static final int WP_X = 4;
    private static final int WP_Y = 22;
    private static final int WP_W = 78;
    private static final int WP_H = 166;
    private static final int WP_ITEM_H = 20;
    private static final int ACT_X = 87;
    private static final int ACT_Y = 22;
    private static final int ACT_W = 159;
    private static final int ACT_H = 87;
    private static final int ACT_ROW_H = 20;
    private static final int ACT_MAX_VIS = 4;
    private static final int ACT_PAD_X = 2;
    private static final int ACT_SB_W = 5;
    private static final int ACT_SB_X = ACT_X + ACT_W - ACT_SB_W;  // = 241
    private static final int ACT_SB_H = ACT_MAX_VIS * ACT_ROW_H;   // = 72
    private static final int COL_TYPE = 50;
    private static final int COL_SRC  = 50;
    private static final int COL_ITEM = 16;
    private static final int COL_ADJ  = 12;
    private static final int COL_GAP  = 3;

    private static final int INV_LABEL_Y = ACT_Y + ACT_H - 5;

    private static final int FONT_COLOR = 4210752;
    private static final int ROW_BG_COLOR = FastColor.ARGB32.color(200, 40, 40, 40);
    private static final int ADD_ENTRY_COLOR = FastColor.ARGB32.color(160, 30, 30, 30);
    private static final int ADD_TEXT_COLOR = FastColor.ARGB32.color(255, 180, 180, 180);

    private static final int SLOT_BORDER = 0xFF000000;
    private static final int SLOT_FILL = 0xFF8B8B8B;

    // ── i18n ──────────────────────────────────────────────────────────────────

    private static final MutableComponent TEXT_NO_ROUTE = Component.translatable("gui.workers.courier.noRoute");
    private static final MutableComponent TEXT_APPLY = Component.translatable("gui.workers.courier.apply");
    private static final MutableComponent TEXT_MAP = Component.translatable("gui.workers.courier.map");
    private static final MutableComponent TEXT_VEHICLE_INV = Component.translatable("gui.workers.checkbox.useVehicleInventory");
    private static final MutableComponent TOOLTIP_VEHICLE_INV = Component.translatable("gui.workers.checkbox.tooltip.useVehicleInventory");
    private static final MutableComponent TEXT_CYCLE = Component.translatable("gui.workers.checkbox.cycle");
    private static final MutableComponent TOOLTIP_CYCLE = Component.translatable("gui.workers.checkbox.tooltip.cycle");
    // ── Screen state ──────────────────────────────────────────────────────────

    private final CourierEntity courierEntity;
    private final Player player;

    private final List<RecruitsRoute> availableRoutes = new ArrayList<>();
    @Nullable private RecruitsRoute selectedRoute   = null;
    @Nullable private CourierRoute workingRoute    = null;

    private int selectedWaypointIndex = -1;
    private int actionScrollOffset = 0;
    private boolean isDraggingScrollbar = false;
    private int     scrollbarDragStartY  = 0;
    /** Whether the courier should use the vehicle's inventory instead of its own. */
    private boolean useVehicleInventory  = false;
    private boolean shouldCycle = false;
    // ── Tracked widgets ───────────────────────────────────────────────────────

    @Nullable private ScrollDropDownMenu<RecruitsRoute>                  routeDropDown;
    @Nullable private WaypointList                                        waypointList;
    private final List<AbstractWidget>                                    actionWidgets = new ArrayList<>();
    private final List<ScrollDropDownMenu<CourierAction.ActionType>> actionTypeDropDowns = new ArrayList<>();
    private final List<ScrollDropDownMenu<CourierAction.SourceType>> sourceTypeDropDowns = new ArrayList<>();
    /** [screenX, screenY, actionIndex] for each visible item slot. */
    private final List<int[]> itemSlotPos = new ArrayList<>();

    private ItemStack hoveredTooltipStack = ItemStack.EMPTY;
    private int hoveredTooltipX, hoveredTooltipY;
    @Nullable private Component hoveredWaypointTooltip   = null;

    // ── Constructor ────────────────────────────────────────────────────────────

    public CourierScreen(CourierContainer container, Inventory playerInventory, Component title) {
        super(TEXTURE, container, playerInventory, Component.literal("Courier"));
        this.courierEntity = container.getCourierEntity();
        this.player        = playerInventory.player;
        this.imageWidth    = IMG_W;
        this.imageHeight   = IMG_H;
    }

    // ── init ──────────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        super.init();
        loadAvailableRoutes();
        loadWorkingDataFromEntity();
        buildWidgets();
    }

    private void loadAvailableRoutes() {
        availableRoutes.clear();
        try {
            availableRoutes.addAll(RecruitsRoute.loadAllRoutes(RecruitsRoute.getRoutesDirectory()));
            availableRoutes.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        } catch (IOException e) {
            WorkersMain.LOGGER.warn("Could not load courier routes: {}", e.getMessage());
        }
    }

    private void loadWorkingDataFromEntity() {
        var routeData = courierEntity.getRouteData();
        if (routeData.getBoolean("hasRoute") && routeData.contains("route"))
            workingRoute = CourierRoute.fromNBT(routeData.getCompound("route"));

        if (workingRoute != null && workingRoute.getRouteId() != null) {
            var id = workingRoute.getRouteId();
            for (RecruitsRoute r : availableRoutes) {
                if (r.getId().equals(id)) { selectedRoute = r; break; }
            }
        }
        useVehicleInventory = routeData.getBoolean("useVehicleInventory");
        shouldCycle = routeData.getBoolean("shouldCycle");
    }

    // ── Widget construction ────────────────────────────────────────────────────

    private void buildWidgets() {
        clearWidgets();
        actionWidgets.clear();
        actionTypeDropDowns.clear();
        sourceTypeDropDowns.clear();
        itemSlotPos.clear();

        int x = leftPos, y = topPos;

        // ── Route dropdown (wide) ──────────────────────────────────────────────

        List<RecruitsRoute> opts = new ArrayList<>();
        opts.add(null);
        opts.addAll(availableRoutes);

        routeDropDown = new ScrollDropDownMenu<>(
                selectedRoute,
                x + ROUTE_X, y + ROUTE_Y, ROUTE_W, ROUTE_H,
                opts,
                r -> r == null ? TEXT_NO_ROUTE.getString() : r.getName(),
                r -> {
                    selectedRoute         = r;
                    selectedWaypointIndex = -1;
                    actionScrollOffset    = 0;
                    workingRoute = (r == null) ? null : CourierRoute.fromRecruitsRoute(r);
                    buildWidgets();
                });
        addRenderableWidget(routeDropDown);

        // ── Map shortcut ───────────────────────────────────────────────────────

        ExtendedButton mapBtn = new ExtendedButton(
                x + MAP_X, y + MAP_Y, MAP_W, MAP_H,
                TEXT_MAP, b -> {
                    WorldMapScreen mapScreen = new WorldMapScreen();
                    if(selectedRoute != null){
                        mapScreen.selectedRoute = this.selectedRoute;
                    }
                    Minecraft.getInstance().setScreen(mapScreen);
        });
        mapBtn.active = selectedRoute != null;
        addRenderableWidget(mapBtn);

        // ── Apply button — outside image, bottom right ─────────────────────────
        // Positioned to the right of the image panel.

        ExtendedButton applyBtn = new ExtendedButton(
                x + IMG_W + 2, y + IMG_H - APPLY_H - 1,
                APPLY_W, APPLY_H,
                TEXT_APPLY, b -> applyChanges(true));
        applyBtn.active = workingRoute != null;
        addRenderableWidget(applyBtn);

        RecruitsCheckBox shouldCycleCheckbox = new RecruitsCheckBox(
            x + IMG_W + 2,
            applyBtn.getY() - 1 - 41,
            20, 20,
                TEXT_CYCLE, shouldCycle, false,
            val -> {
                shouldCycle = val;
                applyChanges(false);
            }
        );
        shouldCycleCheckbox.setTooltip(Tooltip.create(TOOLTIP_CYCLE));
        addRenderableWidget(shouldCycleCheckbox);

        RecruitsCheckBox vehicleCheckbox = new RecruitsCheckBox(
            x + IMG_W + 2,
            applyBtn.getY() - 1 - 20,
            20, 20,
            TEXT_VEHICLE_INV, useVehicleInventory, false,
            val -> {
                useVehicleInventory = val;
                applyChanges(false);
            }
        );
        vehicleCheckbox.setTooltip(Tooltip.create(TOOLTIP_VEHICLE_INV));
        addRenderableWidget(vehicleCheckbox);

        // ── Waypoint list (MerchantTradeScreen pattern) ────────────────────────

        int listLeft   = x + WP_X;
        int listTop    = y + WP_Y;
        int listBottom = listTop + WP_H;

        waypointList = new WaypointList(
                minecraft, WP_W, WP_H, listTop, listBottom, WP_ITEM_H, WP_W);
        waypointList.setLeftPos(listLeft);
        waypointList.setRenderBackground(false);
        waypointList.setRenderTopAndBottom(false);
        waypointList.setRenderSelection(false);
        addRenderableWidget(waypointList);

        if (workingRoute != null) {
            for (int i = 0; i < workingRoute.size(); i++)
                waypointList.addEntry(waypointList.new WaypointEntry(i));

            if (selectedWaypointIndex >= 0 && selectedWaypointIndex < workingRoute.size()) {
                for (var e : waypointList.children()) {
                    if (e.waypointIndex == selectedWaypointIndex) {
                        waypointList.setSelected(e);
                        break;
                    }
                }
            }
        }

        // ── Action area ───────────────────────────────────────────────────────

        buildActionArea();
    }

    private void buildActionArea() {
        for (AbstractWidget w : actionWidgets) removeWidget(w);
        actionWidgets.clear();
        actionTypeDropDowns.clear();
        sourceTypeDropDowns.clear();
        itemSlotPos.clear();

        if (selectedWaypointIndex < 0 || workingRoute == null
                || selectedWaypointIndex >= workingRoute.size()) return;

        var wp = workingRoute.getWaypoints().get(selectedWaypointIndex);

        int maxScroll = Math.max(0, totalListItems(wp) - ACT_MAX_VIS);
        actionScrollOffset = Math.max(0, Math.min(actionScrollOffset, maxScroll));

        int visible = Math.min(totalListItems(wp) - actionScrollOffset, ACT_MAX_VIS);
        for (int i = 0; i < visible; i++) {
            int idx = actionScrollOffset + i;
            int rowY = topPos + ACT_Y + i * ACT_ROW_H;
            if (idx < wp.actions.size())
                buildActionRow(leftPos + ACT_X + ACT_PAD_X, rowY, idx, wp.actions.get(idx), wp);
            // add-entry row has no widgets — rendered manually
        }
    }

    /** Virtual list size: real actions + 1 add-entry when < 8. */
    private int totalListItems(CourierRoute.CourierWaypoint wp) {
        return wp.actions.size() + (wp.actions.size() < 8 ? 1 : 0);
    }

    private void buildActionRow(int rowX, int rowY, int actionIdx, CourierAction action, CourierRoute.CourierWaypoint wp) {
        int x = rowX;
        int h = ACT_ROW_H - 2;   // widget height (fits inside row with 1px gap)
        int finalIdx = actionIdx;

        // ── ActionType dropdown ───────────────────────────────────────────────

        ScrollDropDownMenu<CourierAction.ActionType> typeDD = new ScrollDropDownMenu<>(
                action.getActionType(),
                x, rowY, COL_TYPE, h,
                List.of(CourierAction.ActionType.TAKE,
                        CourierAction.ActionType.TAKE_ANY,
                        CourierAction.ActionType.TAKE_ALL,
                        CourierAction.ActionType.PUT,
                        CourierAction.ActionType.PUT_ANY,
                        CourierAction.ActionType.PUT_ALL,
                        CourierAction.ActionType.WAIT),
                CourierAction.ActionType::displayLabel,
                t -> {
                    action.setActionType(t);
                    if (t == CourierAction.ActionType.WAIT) {
                        action.setSourceType(null);
                        action.setItemStack(null);
                        if (action.getWaitSeconds() <= 0) action.setWaitSeconds(5);
                    } else {
                        if (action.getSourceType() == null)
                            action.setSourceType(CourierAction.SourceType.CHEST);
                        if (!t.hasItemSlot()) action.setItemStack(null);
                        else if (action.getItemStack() == null) action.setItemStack(ItemStack.EMPTY);
                    }
                    buildWidgets();
                });
        addRenderableWidget(typeDD);
        actionWidgets.add(typeDD);
        actionTypeDropDowns.add(typeDD);
        x += COL_TYPE + COL_GAP;

        // ── Remove button — 16×16, flush right before scrollbar ───────────────
        // removeX: ACT_SB_X - 2px gap - COL_REM = 241 - 2 - 18 = 221

        int removeX = leftPos + ACT_SB_X - 2 - 16;
        ExtendedButton removeBtn = new ExtendedButton(
                removeX, rowY, h, h,
                Component.literal("\u2715"),   // ✕
                b -> { wp.actions.remove(finalIdx);
                       actionScrollOffset = Math.max(0, actionScrollOffset - 1);
                       buildWidgets(); });
        addRenderableWidget(removeBtn);
        actionWidgets.add(removeBtn);

        if (action.getActionType() != CourierAction.ActionType.WAIT) {

            // ── SourceType dropdown ───────────────────────────────────────────

            ScrollDropDownMenu<CourierAction.SourceType> srcDD = new ScrollDropDownMenu<>(
                    action.getSourceType() != null
                            ? action.getSourceType()
                            : CourierAction.SourceType.CHEST,
                    x, rowY, COL_SRC, h,
                    List.of(CourierAction.SourceType.CHEST,
                            CourierAction.SourceType.STORAGE,
                            CourierAction.SourceType.MARKET,
                            CourierAction.SourceType.KITCHEN
                            ),
                            //CourierAction.SourceType.WORKER),
                    s -> switch (s) {
                        case CHEST   -> "Chest";
                        case STORAGE -> "Storage";
                        case MARKET  -> "Market";
                        case KITCHEN  -> "KITCHEN";
                        //case WORKER  -> "Worker";
                    },
                    s -> { action.setSourceType(s); buildWidgets(); });
            addRenderableWidget(srcDD);
            actionWidgets.add(srcDD);
            sourceTypeDropDowns.add(srcDD);
            x += COL_SRC + COL_GAP;

            // Item slot — rendered manually; only for types that need a filter
            if (action.getActionType().hasItemSlot())
                itemSlotPos.add(new int[]{ x, rowY, actionIdx });

        } else {

            // ── Wait seconds +/− ──────────────────────────────────────────────

            ExtendedButton minus = new ExtendedButton(x, rowY, h, h, Component.literal("-"),
                b -> {
                    int amt = hasShiftDown() ? 5 : 1;
                    action.setWaitSeconds(Math.max(1, action.getWaitSeconds() - amt));
                    buildWidgets();
                }
            );
            addRenderableWidget(minus);
            actionWidgets.add(minus);
            x += 52;

            ExtendedButton plus = new ExtendedButton(x, rowY, h, h, Component.literal("+"),
                b -> {
                    int amt = hasShiftDown() ? 5 : 1;
                    action.setWaitSeconds(Math.max(1, action.getWaitSeconds() + amt)); buildWidgets();
                }
            );
            addRenderableWidget(plus);
            actionWidgets.add(plus);
        }
    }

    // ── Waypoint selection ─────────────────────────────────────────────────────

    private void onWaypointSelected(int index) {
        if (index != selectedWaypointIndex) {
            actionScrollOffset = 0;
        }
        selectedWaypointIndex = index;
        buildActionArea();
    }

    // ── Apply / save ──────────────────────────────────────────────────────────

    private void applyChanges(boolean start) {
        if (workingRoute == null) return;
        WorkersMain.SIMPLE_CHANNEL.sendToServer(
                new MessageCourierSetRoute(courierEntity.getUUID(), workingRoute, useVehicleInventory, shouldCycle, start));
    }

    /** Auto-save on close (ESC, map button, etc.). */
    @Override
    public void onClose() {
        applyChanges(false);
        super.onClose();
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        hoveredTooltipStack    = ItemStack.EMPTY;
        hoveredWaypointTooltip = null;

        super.render(g, mouseX, mouseY, partialTicks);
        renderActionRows(g, mouseX, mouseY);
        renderActionScrollbar(g, mouseX, mouseY);

        if (!hoveredTooltipStack.isEmpty())
            g.renderTooltip(font, hoveredTooltipStack, hoveredTooltipX, hoveredTooltipY);
        if (hoveredWaypointTooltip != null)
            g.renderTooltip(font, hoveredWaypointTooltip, mouseX, mouseY);
    }

    /** Draws all visible rows of the action list including the add-entry. */
    private void renderActionRows(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (workingRoute == null || selectedWaypointIndex < 0 || selectedWaypointIndex >= workingRoute.size()) return;

        var wp = workingRoute.getWaypoints().get(selectedWaypointIndex);
        int visible = Math.min(totalListItems(wp) - actionScrollOffset, ACT_MAX_VIS);

        for (int i = 0; i < visible; i++) {
            int idx  = actionScrollOffset + i;
            int rowY = topPos  + ACT_Y + i * ACT_ROW_H;
            int rowX = leftPos + ACT_X;

            if (idx == wp.actions.size()) {
                // ── Add-entry ─────────────────────────────────────────────────
                boolean hovered = mouseX >= rowX && mouseX < rowX + ACT_W
                        && mouseY >= rowY && mouseY < rowY + ACT_ROW_H;
                RenderSystem.enableBlend();
                guiGraphics.fill(rowX, rowY, rowX + ACT_W, rowY + ACT_ROW_H - 1,
                        hovered ? FastColor.ARGB32.color(200, 60, 60, 60) : ADD_ENTRY_COLOR);
                String lbl = "+ Add Action";
                guiGraphics.drawString(font, lbl,
                        rowX + (ACT_W - font.width(lbl)) / 2,
                        rowY + (ACT_ROW_H - 8) / 2,
                        ADD_TEXT_COLOR, false);
                continue;
            }

            CourierAction action = wp.actions.get(idx);

            // ── Row background ─────────────────────────────────────────────────
            RenderSystem.enableBlend();
            guiGraphics.fill(rowX, rowY, rowX + ACT_W, rowY + ACT_ROW_H - 1, ROW_BG_COLOR);

            if (action.getActionType().hasItemSlot()) {

                // ── Item slot (MC style: black border, gray fill) ──────────────
                for (int[] slot : itemSlotPos) {
                    if (slot[2] != idx) continue;
                    int sx = slot[0], sy = slot[1] + 1;
                    ItemStack item = action.getItemStack() != null
                            ? action.getItemStack() : ItemStack.EMPTY;

                    guiGraphics.fill(sx - 1, sy - 1, sx + COL_ITEM + 1, sy + COL_ITEM + 1, SLOT_BORDER);
                    guiGraphics.fill(sx, sy, sx + COL_ITEM, sy + COL_ITEM, SLOT_FILL);
                    guiGraphics.renderFakeItem(item, sx, sy);
                    guiGraphics.renderItemDecorations(font, item, sx, sy);

                    if (!item.isEmpty()
                            && mouseX >= sx && mouseX < sx + COL_ITEM
                            && mouseY >= sy && mouseY < sy + COL_ITEM) {
                        hoveredTooltipStack = item;
                        hoveredTooltipX     = mouseX;
                        hoveredTooltipY     = mouseY;
                    }
                }

            }
            else if(action.getActionType().hasTime()) {
                int numX = leftPos + ACT_X + ACT_PAD_X + COL_TYPE + COL_GAP + COL_ADJ;
                String sec = action.getWaitSeconds() + "s";
                guiGraphics.drawString(font, sec, numX + (font.width(sec)) / 2 + 3, rowY + 5, 0xFFFFFF, false);
            }
        }
    }

    /** Action scrollbar — MC ObjectSelectionList style. */
    private void renderActionScrollbar(GuiGraphics g, int mouseX, int mouseY) {
        if (workingRoute == null || selectedWaypointIndex < 0
                || selectedWaypointIndex >= workingRoute.size()) return;

        var wp = workingRoute.getWaypoints().get(selectedWaypointIndex);
        int total = totalListItems(wp);
        if (total <= ACT_MAX_VIS) return;

        int trackX = leftPos + ACT_SB_X;
        int trackY = topPos  + ACT_Y;
        int trackH = ACT_SB_H;
        int max    = total - ACT_MAX_VIS;
        int handleH = Math.max(8, (ACT_MAX_VIS * trackH) / total);
        int handleY = trackY + (max == 0 ? 0 : (actionScrollOffset * (trackH - handleH)) / max);

        g.fill(trackX, trackY, trackX + ACT_SB_W, trackY + trackH, 0xFF000000);
        g.fill(trackX, handleY, trackX + ACT_SB_W, handleY + handleH, 0xFF808080);
        g.fill(trackX, handleY, trackX + ACT_SB_W - 1, handleY + handleH - 1, 0xFFC0C0C0);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, player.getInventory().getDisplayName().getVisualOrderText(), menu.getInvXOffset() + 8, INV_LABEL_Y, FONT_COLOR, false);
    }

    // ── Scrollbar helpers ──────────────────────────────────────────────────────

    @Nullable
    private int[] actionScrollbarHandle() {
        if (workingRoute == null || selectedWaypointIndex < 0 || selectedWaypointIndex >= workingRoute.size()) return null;

        var wp = workingRoute.getWaypoints().get(selectedWaypointIndex);
        int total = totalListItems(wp);

        if (total <= ACT_MAX_VIS) return null;

        int max     = total - ACT_MAX_VIS;
        int trackH  = ACT_SB_H;
        int handleH = Math.max(8, (ACT_MAX_VIS * trackH) / total);
        int handleY = topPos + ACT_Y + (max == 0 ? 0 : (actionScrollOffset * (trackH - handleH)) / max);

        return new int[]{ leftPos + ACT_SB_X, handleY, ACT_SB_W, handleH };
    }

    private int actionScrollbarMaxScroll() {
        if (workingRoute == null || selectedWaypointIndex < 0 || selectedWaypointIndex >= workingRoute.size()) return 0;
        
        return Math.max(0, totalListItems(workingRoute.getWaypoints().get(selectedWaypointIndex)) - ACT_MAX_VIS);
    }

    // ── Mouse ──────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int button) {

        // ── Action scrollbar handle drag start ────────────────────────────────
        int[] handle = actionScrollbarHandle();
        if (handle != null && button == 0
                && mx >= handle[0] && mx < handle[0] + handle[2]
                && my >= handle[1] && my < handle[1] + handle[3]) {
            isDraggingScrollbar = true;
            scrollbarDragStartY = (int) my - handle[1];
            return true;
        }

        // ── Action scrollbar track click ──────────────────────────────────────
        if (handle != null && button == 0) {
            int tx = leftPos + ACT_SB_X, ty = topPos + ACT_Y;
            if (mx >= tx && mx < tx + ACT_SB_W && my >= ty && my < ty + ACT_SB_H) {
                int max   = actionScrollbarMaxScroll();
                actionScrollOffset = Math.max(0, Math.min(max,
                        max * ((int) my - ty) / ACT_SB_H));
                buildActionArea();
                return true;
            }
        }

        // ── Route dropdown ────────────────────────────────────────────────────
        if (routeDropDown != null) routeDropDown.onMouseClick(mx, my);

        // ── Action dropdown isolation ─────────────────────────────────────────
        // If the click lands inside any action dropdown's visual area (including
        // its expanded list — which isMouseOver() covers), deliver it ONLY to the
        // dropdowns and swallow the event so no other widget is accidentally activated.
        boolean clickedOnActionDD =
                actionTypeDropDowns.stream().anyMatch(dd -> dd.isMouseOver(mx, my)) ||
                sourceTypeDropDowns.stream().anyMatch(dd -> dd.isMouseOver(mx, my));

        // Always forward to dropdowns (they close themselves if clicked outside).
        new ArrayList<>(actionTypeDropDowns).forEach(dd -> dd.onMouseClick(mx, my));
        new ArrayList<>(sourceTypeDropDowns).forEach(dd -> dd.onMouseClick(mx, my));

        if (clickedOnActionDD) return true; // swallow — don't bleed through to slots or add-entry

        // ── Item slot hit-test ────────────────────────────────────────────────
        if (workingRoute != null && selectedWaypointIndex >= 0
                && selectedWaypointIndex < workingRoute.size()) {
            var wp = workingRoute.getWaypoints().get(selectedWaypointIndex);

            for (int[] slot : itemSlotPos) {
                int sx = slot[0], sy = slot[1], idx = slot[2];
                if (idx >= wp.actions.size()) continue;
                if (mx >= sx && mx < sx + COL_ITEM && my >= sy && my < sy + COL_ITEM) {
                    CourierAction action = wp.actions.get(idx);
                    if (!action.getActionType().hasItemSlot()) continue;

                    if (button == 1) {
                        // Right-click: clear slot
                        action.setItemStack(ItemStack.EMPTY);
                    } else {
                        // Left-click: set or stack from cursor
                        ItemStack cursor = (minecraft.player != null)
                                ? minecraft.player.containerMenu.getCarried()
                                : ItemStack.EMPTY;
                        if (!cursor.isEmpty()) {
                            ItemStack current = action.getItemStack();
                            if (current != null && !current.isEmpty()
                                    && ItemStack.isSameItem(current, cursor)) {
                                // Same item → accumulate count
                                current.setCount(current.getCount() + cursor.getCount());
                            } else {
                                action.setItemStack(cursor.copy());
                            }
                        }
                    }
                    buildWidgets();
                    return true;
                }
            }

            // ── Add-entry hit-test ────────────────────────────────────────────
            int visible = Math.min(totalListItems(wp) - actionScrollOffset, ACT_MAX_VIS);
            for (int i = 0; i < visible; i++) {
                if (actionScrollOffset + i != wp.actions.size()) continue;
                int rowY = topPos + ACT_Y + i * ACT_ROW_H;
                if (mx >= leftPos + ACT_X && mx < leftPos + ACT_X + ACT_W
                        && my >= rowY && my < rowY + ACT_ROW_H) {
                    wp.actions.add(CourierAction.wait(5));
                    buildWidgets();
                    return true;
                }
            }
        }

        return super.mouseClicked(mx, my, button);
    }

    @Override
    public void mouseMoved(double mx, double my) {
        if (routeDropDown != null) routeDropDown.onMouseMove(mx, my);
        // Snapshot copies — onMouseMove can trigger buildWidgets() via dropdown close
        new ArrayList<>(actionTypeDropDowns).forEach(dd -> dd.onMouseMove(mx, my));
        new ArrayList<>(sourceTypeDropDowns).forEach(dd -> dd.onMouseMove(mx, my));
        super.mouseMoved(mx, my);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        // Action dropdowns get priority — isMouseOver() returns true for the expanded list
        // only when the dropdown is open, so this naturally lets open dropdowns consume scroll.
        for (var dropDownMenu : actionTypeDropDowns)
            if (dropDownMenu.isMouseOver(mx, my)) return dropDownMenu.mouseScrolled(mx, my, delta);
        for (var dropDownMenu : sourceTypeDropDowns)
            if (dropDownMenu.isMouseOver(mx, my)) return dropDownMenu.mouseScrolled(mx, my, delta);

        // Action area row scroll (only reached when no action dropdown is open over it)
        if (workingRoute != null && selectedWaypointIndex >= 0
                && selectedWaypointIndex < workingRoute.size()) {
            int ax = leftPos + ACT_X, ay = topPos + ACT_Y;
            if (mx >= ax && mx < ax + ACT_W && my >= ay && my < ay + ACT_H) {
                var wp  = workingRoute.getWaypoints().get(selectedWaypointIndex);
                int max = Math.max(0, totalListItems(wp) - ACT_MAX_VIS);
                int neo = (int) Math.max(0, Math.min(max, actionScrollOffset - delta));
                if (neo != actionScrollOffset) { actionScrollOffset = neo; buildActionArea(); }
                return true;
            }
        }
        if (routeDropDown != null && routeDropDown.isMouseOver(mx, my))
            return routeDropDown.mouseScrolled(mx, my, delta);
        if (waypointList != null && waypointList.isMouseOver(mx, my))
            return waypointList.mouseScrolled(mx, my, delta);
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dragX, double dragY) {
        // Action scrollbar drag
        if (isDraggingScrollbar && button == 0) {
            int[] h = actionScrollbarHandle();
            if (h == null) { isDraggingScrollbar = false; return true; }
            int max    = actionScrollbarMaxScroll();
            int trackH = ACT_SB_H;
            int handleH = h[3];
            int range   = trackH - handleH;
            if (range <= 0) { actionScrollOffset = 0; }
            else {
                int newHandleY = (int) my - (topPos + ACT_Y) - scrollbarDragStartY;
                actionScrollOffset = Math.max(0, Math.min(max, (newHandleY * max) / range));
            }
            buildActionArea();
            return true;
        }

        // Waypoint list scrollbar drag — explicitly forward so the list handles it
        if (waypointList != null)
            waypointList.mouseDragged(mx, my, button, dragX, dragY);

        return super.mouseDragged(mx, my, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (isDraggingScrollbar && button == 0) { isDraggingScrollbar = false; return true; }
        return super.mouseReleased(mx, my, button);
    }
    private class WaypointList extends ObjectSelectionList<WaypointList.WaypointEntry> {

        WaypointList(Minecraft mc, int width, int height, int top, int bottom, int itemHeight, int itemWidth) {
            super(mc, width, height, top, bottom, itemHeight);
        }

        @Override protected int  addEntry(WaypointEntry e) { return super.addEntry(e); }
        @Override protected void clearEntries()             { super.clearEntries(); }

        @Override protected int getScrollbarPosition() { return getRowLeft() + getRowWidth() + 5; }
        @Override public    int getRowLeft()            { return super.getRowLeft() - 7; }
        @Override public    int getRowWidth()           { return WP_W - 12; }

        @Override
        public Optional<GuiEventListener> getChildAt(double x, double y) {
            return super.getChildAt(x, y);
        }

        @Override
        public void setSelected(@Nullable WaypointEntry entry) {
            super.setSelected(entry);
            if (entry != null) CourierScreen.this.onWaypointSelected(entry.waypointIndex);
        }

        class WaypointEntry extends ObjectSelectionList.Entry<WaypointEntry> {

            final int waypointIndex;

            WaypointEntry(int index) { this.waypointIndex = index; }

            @Override
            public void render(GuiGraphics g, int index, int top, int left, int entryW, int entryH, int mouseX, int mouseY, boolean hovered, float partial) {
                boolean selected = (WaypointList.this.getSelected() == this);
                int textureY = (selected || hovered) ? 86 : 66;

                RenderSystem.enableBlend();
                g.blitNineSliced(WIDGETS_LOCATION, left, top,
                        WaypointList.this.getRowWidth(), entryH,
                        20, 4, 200, 20, 0, textureY);

                // Show only the action count; show waypoint name as hover tooltip
                int actionCount = 0;
                String name     = "";
                if (workingRoute != null && waypointIndex < workingRoute.size()) {
                    var wp = workingRoute.getWaypoints().get(waypointIndex);
                    actionCount = wp.actions.size();
                    name        = wp.displayName.isEmpty()
                            ? "Waypoint " + (waypointIndex + 1)
                            : wp.displayName;
                }

                // Waypoint number + action count, centred vertically
                String indexStr = String.valueOf(waypointIndex + 1);
                g.drawString(font, indexStr, left + 4, top + 5, FONT_COLOR, false);

                // Action count in white, right-aligned
                String countStr = "(" + actionCount + ")";
                g.drawString(font, countStr, left + WaypointList.this.getRowWidth() - 4 - font.width(countStr), top + 5, 0xFFFFFF, false);

                // Accumulate tooltip for the hover name (rendered after all entries)
                if (hovered && !name.isEmpty())
                    CourierScreen.this.hoveredWaypointTooltip = Component.literal(name);
            }

            @Override
            public boolean mouseClicked(double x, double y, int button) {
                WaypointList.this.setSelected(this);
                return true;
            }

            @Override public Component getNarration() { return Component.empty(); }
        }
    }
}
