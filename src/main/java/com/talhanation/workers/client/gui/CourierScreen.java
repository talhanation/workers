package com.talhanation.workers.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.talhanation.recruits.client.gui.worldmap.WorldMapScreen;
import com.talhanation.recruits.client.gui.widgets.ScrollDropDownMenu;
import com.talhanation.recruits.client.gui.widgets.RecruitsCheckBox;
import com.talhanation.recruits.client.gui.worldmap.route.RouteNamePopup;
import com.talhanation.recruits.client.gui.worldmap.storage.WorldMapStorageId;
import com.talhanation.recruits.world.RecruitsRoute;
import com.talhanation.workers.WorkersMain;
import com.talhanation.workers.entities.CourierEntity;
import com.talhanation.workers.inventory.CourierContainer;
import com.talhanation.workers.network.MessageCourierSetRoute;
import com.talhanation.workers.network.MessageRequestCourierTargets;
import com.talhanation.workers.network.MessageToClientCourierTargets;
import com.talhanation.workers.world.CourierAction;
import com.talhanation.workers.world.CourierRoute;
import com.talhanation.workers.client.gui.widgets.CourierDropDownMenu;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.widget.ExtendedButton;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

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
    private static final int COL_SRC  = 58;
    private static final int SRC_DROPDOWN_W   = 150;   // open target list is wider than the column
    private static final int SRC_DROPDOWN_VIS = 8;
    private static final int MAX_AMOUNT       = 9999;
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

    private static final int COLOR_TARGET_ANY      = 0xFFFFFFFF;
    private static final int COLOR_TARGET_SPECIFIC = 0xFFFFD27F;
    private static final int COLOR_TARGET_UNKNOWN  = 0xFFAAAAAA;
    private static final int COLOR_TARGET_MISSING  = 0xFFFF5555;
    private static final int COLOR_CURRENT_WP      = 0x55FF55;
    private static final int COLOR_HINT            = 0x707070;

    private static final List<CourierAction.SourceType> SOURCE_TYPES = List.of(
            CourierAction.SourceType.CHEST,
            CourierAction.SourceType.STORAGE,
            CourierAction.SourceType.MARKET,
            CourierAction.SourceType.KITCHEN,
            CourierAction.SourceType.WORKSHOP);

    // ── i18n ──────────────────────────────────────────────────────────────────

    private static final MutableComponent TEXT_NO_ROUTE = Component.translatable("gui.workers.courier.noRoute");
    private static final MutableComponent TEXT_APPLY = Component.translatable("gui.workers.courier.apply");
    private static final MutableComponent TEXT_MAP = Component.translatable("gui.workers.courier.map");
    private static final MutableComponent TEXT_VEHICLE_INV = Component.translatable("gui.workers.checkbox.useVehicleInventory");
    private static final MutableComponent TOOLTIP_VEHICLE_INV = Component.translatable("gui.workers.checkbox.tooltip.useVehicleInventory");
    private static final MutableComponent TEXT_CYCLE = Component.translatable("gui.workers.checkbox.cycle");
    private static final MutableComponent TOOLTIP_CYCLE = Component.translatable("gui.workers.checkbox.tooltip.cycle");

    private static final MutableComponent TOOLTIP_TAKE = Component.translatable("gui.workers.tooltip.courier.take");
    private static final MutableComponent TOOLTIP_PUT = Component.translatable("gui.workers.tooltip.courier.put");
    private static final MutableComponent TOOLTIP_TAKE_ANY = Component.translatable("gui.workers.tooltip.courier.take_any");
    private static final MutableComponent TOOLTIP_PUT_ANY = Component.translatable("gui.workers.tooltip.courier.put_any");
    private static final MutableComponent TOOLTIP_TAKE_ALL = Component.translatable("gui.workers.tooltip.courier.take_all");
    private static final MutableComponent TOOLTIP_PUT_ALL = Component.translatable("gui.workers.tooltip.courier.put_all");
    private static final MutableComponent TOOLTIP_TAKE_FILL = Component.translatable("gui.workers.tooltip.courier.take_fill");
    private static final MutableComponent TOOLTIP_PUT_FILL = Component.translatable("gui.workers.tooltip.courier.put_fill");
    private static final MutableComponent TOOLTIP_WAIT = Component.translatable("gui.workers.tooltip.courier.wait");

    private static final MutableComponent TOOLTIP_CHEST = Component.translatable("gui.workers.tooltip.courier.chest");
    private static final MutableComponent TOOLTIP_STORAGE = Component.translatable("gui.workers.tooltip.courier.storage");
    private static final MutableComponent TOOLTIP_MARKET = Component.translatable("gui.workers.tooltip.courier.market");
    private static final MutableComponent TOOLTIP_KITCHEN = Component.translatable("gui.workers.tooltip.courier.kitchen");
    private static final MutableComponent TOOLTIP_WORKSHOP = Component.translatable("gui.workers.tooltip.courier.workshop");

    private static final MutableComponent TEXT_ADD_ACTION = Component.translatable("gui.workers.courier.addAction");
    private static final MutableComponent TEXT_CHEST = Component.translatable("gui.workers.courier.source.chest");
    private static final MutableComponent TEXT_STORAGE = Component.translatable("gui.workers.courier.source.storage");
    private static final MutableComponent TEXT_MARKET = Component.translatable("gui.workers.courier.source.market");
    private static final MutableComponent TEXT_KITCHEN = Component.translatable("gui.workers.courier.source.kitchen");
    private static final MutableComponent TEXT_WORKSHOP = Component.translatable("gui.workers.courier.source.workshop");

    private static final MutableComponent TEXT_TAKE = Component.translatable("gui.workers.courier.action.take");
    private static final MutableComponent TEXT_PUT = Component.translatable("gui.workers.courier.action.put");
    private static final MutableComponent TEXT_TAKE_ANY = Component.translatable("gui.workers.courier.action.take_any");
    private static final MutableComponent TEXT_PUT_ANY = Component.translatable("gui.workers.courier.action.put_any");
    private static final MutableComponent TEXT_TAKE_ALL = Component.translatable("gui.workers.courier.action.take_all");
    private static final MutableComponent TEXT_PUT_ALL = Component.translatable("gui.workers.courier.action.put_all");
    private static final MutableComponent TEXT_TAKE_FILL = Component.translatable("gui.workers.courier.action.take_fill");
    private static final MutableComponent TEXT_PUT_FILL = Component.translatable("gui.workers.courier.action.put_fill");
    private static final MutableComponent TEXT_WAIT = Component.translatable("gui.workers.courier.action.wait");

    private static final MutableComponent TEXT_TARGETS_LOADING = Component.translatable("gui.workers.courier.targetsLoading");
    private static final MutableComponent TEXT_TARGETS_NOT_LOADED = Component.translatable("gui.workers.courier.targetsNotLoaded");
    private static final MutableComponent TOOLTIP_TARGETS_NOT_LOADED = Component.translatable("gui.workers.tooltip.courier.targetsNotLoaded");
    private static final MutableComponent TOOLTIP_TARGET_MISSING = Component.translatable("gui.workers.tooltip.courier.targetMissing");
    private static final MutableComponent TOOLTIP_TARGET_UNKNOWN = Component.translatable("gui.workers.tooltip.courier.targetUnknown");
    private static final MutableComponent TOOLTIP_AMOUNT = Component.translatable("gui.workers.tooltip.courier.amount");
    private static final MutableComponent TOOLTIP_EMPTY_SLOT = Component.translatable("gui.workers.tooltip.courier.emptySlot");
    private static final MutableComponent TOOLTIP_CURRENT_WP = Component.translatable("gui.workers.tooltip.courier.currentWaypoint");

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
    private final List<CourierDropDownMenu<CourierAction.ActionType>> actionTypeDropDowns = new ArrayList<>();
    private final List<CourierDropDownMenu<SourceOption>>             sourceTypeDropDowns = new ArrayList<>();
    /** [screenX, screenY, actionIndex] for each visible item slot. */
    private final List<int[]> itemSlotPos = new ArrayList<>();

    private ItemStack hoveredTooltipStack = ItemStack.EMPTY;
    private int hoveredTooltipX, hoveredTooltipY;
    @Nullable private List<Component> hoveredWaypointTooltip = null;
    @Nullable private List<Component> hoveredSlotTooltip     = null;
    @Nullable private Component hoveredHintTooltip            = null;

    // Work areas at each waypoint, as reported by the server (see MessageRequestCourierTargets).
    private final Map<BlockPos, List<MessageToClientCourierTargets.Target>> targetCache = new HashMap<>();
    private final Set<BlockPos> unloadedWaypoints  = new HashSet<>();
    private final Set<BlockPos> requestedWaypoints = new HashSet<>();

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
    public static File getRoutesDirectory() {
        return new File(Minecraft.getInstance().gameDirectory, "recruits/routes/" + WorldMapStorageId.detectCurrent());
    }
    private void loadAvailableRoutes() {
        availableRoutes.clear();
        try {
            availableRoutes.addAll(RecruitsRoute.loadAllRoutes(getRoutesDirectory()));
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
                TEXT_MAP,
                b -> {
                    WorldMapScreen mapScreen = new WorldMapScreen();
                    if(selectedRoute == null){
                        //mapScreen.routeNamePopup = new RouteNamePopup(mapScreen);
                        //TODO: ADD Shortcut to create new route
                    }
                    else{
                        mapScreen.selectedRoute = this.selectedRoute;
                    }
                    Minecraft.getInstance().setScreen(mapScreen);
                }


        );
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

        CourierDropDownMenu<CourierAction.ActionType> typeDD = new CourierDropDownMenu<>(
                action.getActionType(),
                x, rowY, COL_TYPE, h,
                List.of(CourierAction.ActionType.TAKE,
                        CourierAction.ActionType.TAKE_ANY,
                        CourierAction.ActionType.TAKE_ALL,
                        CourierAction.ActionType.TAKE_FILL,
                        CourierAction.ActionType.PUT,
                        CourierAction.ActionType.PUT_ANY,
                        CourierAction.ActionType.PUT_ALL,
                        CourierAction.ActionType.PUT_FILL,
                        CourierAction.ActionType.WAIT),
                CourierScreen::actionLabel,
                t -> {
                    action.setActionType(t);
                    if (t == CourierAction.ActionType.WAIT) {
                        action.setSourceType(null);
                        action.clearTarget();
                        action.setItemStack(null);
                        if (action.getWaitSeconds() <= 0) action.setWaitSeconds(5);
                    } else {
                        if (action.getSourceType() == null)
                            action.setSourceType(CourierAction.SourceType.CHEST);
                        if (!t.hasItemSlot()) action.setItemStack(null);
                        else if (action.getItemStack() == null) action.setItemStack(ItemStack.EMPTY);
                    }
                    buildWidgets();
                },
                CourierScreen::actionTooltip);
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

            // ── Source / target dropdown ──────────────────────────────────────
            // One entry per source type ("any in range") followed by the concrete work areas
            // the server found at this waypoint.

            List<SourceOption> sourceOptions = buildSourceOptions(action, wp);
            SourceOption selectedSource = findSelectedSource(sourceOptions, action);

            CourierDropDownMenu<SourceOption> srcDD = new CourierDropDownMenu<>(
                    selectedSource,
                    x, rowY, COL_SRC, h,
                    sourceOptions,
                    this::sourceOptionText,
                    o -> {
                        action.setSourceType(o.type());
                        if (o.id() == null) action.clearTarget();
                        else {
                            action.setTargetId(o.id());
                            action.setTargetName(o.label());
                        }
                        buildWidgets();
                    },
                    this::sourceOptionTooltip);
            srcDD.setDropdownWidth(SRC_DROPDOWN_W);
            srcDD.setMaxVisibleOptions(SRC_DROPDOWN_VIS);
            srcDD.setDisplayTextGetter(o -> font.plainSubstrByWidth(o.id() == null ? sourceLabel(o.type()) : o.label(), COL_SRC - 16));
            srcDD.setDisplayColor(sourceOptionColor(selectedSource));
            srcDD.setOptionColorGetter(this::sourceOptionColor);
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
        if (workingRoute != null && index >= 0 && index < workingRoute.size())
            requestTargets(workingRoute.getWaypoints().get(index).getPosition());
        buildActionArea();
    }

    // ── Work area targets ──────────────────────────────────────────────────────

    private void requestTargets(BlockPos waypointPos) {
        if (!requestedWaypoints.add(waypointPos)) return;
        WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageRequestCourierTargets(courierEntity.getUUID(), waypointPos));
    }

    /** Called by {@link MessageToClientCourierTargets} when the server answered. */
    public void onTargetsReceived(BlockPos waypointPos, boolean loaded, List<MessageToClientCourierTargets.Target> targets) {
        if (loaded) {
            targetCache.put(waypointPos, new ArrayList<>(targets));
            unloadedWaypoints.remove(waypointPos);
        } else {
            // Not loaded on the server → allow asking again the next time this waypoint is selected.
            targetCache.remove(waypointPos);
            unloadedWaypoints.add(waypointPos);
            requestedWaypoints.remove(waypointPos);
        }
        var wp = getSelectedWaypoint();
        if (wp != null && wp.getPosition().equals(waypointPos)) buildActionArea();
    }

    @Nullable
    private CourierRoute.CourierWaypoint getSelectedWaypoint() {
        if (workingRoute == null || selectedWaypointIndex < 0 || selectedWaypointIndex >= workingRoute.size()) return null;
        return workingRoute.getWaypoints().get(selectedWaypointIndex);
    }

    private List<SourceOption> buildSourceOptions(CourierAction action, CourierRoute.CourierWaypoint wp) {
        List<MessageToClientCourierTargets.Target> targets = targetCache.getOrDefault(wp.getPosition(), List.of());
        boolean known = targetCache.containsKey(wp.getPosition());
        List<SourceOption> options = new ArrayList<>();

        for (CourierAction.SourceType type : SOURCE_TYPES) {
            options.add(SourceOption.any(type));
            if (type == CourierAction.SourceType.CHEST) continue;

            boolean savedListed = false;
            for (MessageToClientCourierTargets.Target t : targets) {
                if (t.type() != type) continue;
                int distance = (int) Math.round(Math.sqrt(t.pos().distSqr(wp.getPosition())));
                options.add(new SourceOption(type, t.id(), targetLabel(t, distance), t.pos(), distance, t.slots(), false, false));
                if (t.id().equals(action.getTargetId())) savedListed = true;
            }
            // Keep the saved target visible even if the server did not list it: unknown while the
            // waypoint is not loaded, missing when the area is gone.
            if (action.hasTarget() && action.getSourceType() == type && !savedListed) {
                String label = action.getTargetName().isEmpty() ? sourceLabel(type) : action.getTargetName();
                options.add(new SourceOption(type, action.getTargetId(), label, null, -1, -1, known, !known));
            }
        }
        return options;
    }

    private static SourceOption findSelectedSource(List<SourceOption> options, CourierAction action) {
        CourierAction.SourceType type = action.getSourceType() != null ? action.getSourceType() : CourierAction.SourceType.CHEST;
        UUID id = type == CourierAction.SourceType.CHEST ? null : action.getTargetId();
        for (SourceOption o : options) {
            if (o.type() == type && java.util.Objects.equals(o.id(), id)) return o;
        }
        return options.get(0);
    }

    /** Area name, or for unnamed areas the type with its distance to the waypoint. */
    private String targetLabel(MessageToClientCourierTargets.Target t, int distance) {
        if (!t.name().isEmpty()) return t.name();
        return Component.translatable("gui.workers.courier.unnamedTarget", sourceLabel(t.type()), distance).getString();
    }

    private String sourceOptionText(SourceOption o) {
        if (o.id() == null) return Component.translatable("gui.workers.courier.source.any", sourceLabel(o.type())).getString();
        return font.plainSubstrByWidth("  " + o.label(), SRC_DROPDOWN_W - 12);
    }

    private int sourceOptionColor(SourceOption o) {
        if (o.id() == null) return COLOR_TARGET_ANY;
        if (o.missing()) return COLOR_TARGET_MISSING;
        if (o.unknown()) return COLOR_TARGET_UNKNOWN;
        return COLOR_TARGET_SPECIFIC;
    }

    private List<Component> sourceOptionTooltip(SourceOption o) {
        if (o.id() == null) return sourceTooltip(o.type());
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(sourceLabel(o.type()) + ": " + o.label()).withStyle(ChatFormatting.GOLD));
        if (o.pos() != null)
            lines.add(Component.translatable("gui.workers.tooltip.courier.targetInfo", o.distance(), o.slots()));
        if (o.missing()) lines.add(TOOLTIP_TARGET_MISSING.copy().withStyle(ChatFormatting.RED));
        if (o.unknown()) lines.add(TOOLTIP_TARGET_UNKNOWN.copy().withStyle(ChatFormatting.GRAY));
        return lines;
    }

    /** One entry of the source dropdown: a source type ("any", {@code id == null}) or a concrete work area. */
    private record SourceOption(CourierAction.SourceType type, @Nullable UUID id, String label, @Nullable BlockPos pos,
                                int distance, int slots, boolean missing, boolean unknown) {
        static SourceOption any(CourierAction.SourceType type) {
            return new SourceOption(type, null, "", null, -1, -1, false, false);
        }
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

    private static String actionLabel(CourierAction.ActionType type) {
        return switch (type) {
            case TAKE      -> TEXT_TAKE.getString();
            case PUT       -> TEXT_PUT.getString();
            case TAKE_ANY  -> TEXT_TAKE_ANY.getString();
            case PUT_ANY   -> TEXT_PUT_ANY.getString();
            case TAKE_ALL  -> TEXT_TAKE_ALL.getString();
            case PUT_ALL   -> TEXT_PUT_ALL.getString();
            case PUT_FILL  -> TEXT_PUT_FILL.getString();
            case TAKE_FILL -> TEXT_TAKE_FILL.getString();
            case WAIT      -> TEXT_WAIT.getString();
        };
    }

    private static String sourceLabel(CourierAction.SourceType type) {
        return switch (type) {
            case CHEST    -> TEXT_CHEST.getString();
            case STORAGE  -> TEXT_STORAGE.getString();
            case MARKET   -> TEXT_MARKET.getString();
            case KITCHEN  -> TEXT_KITCHEN.getString();
            case WORKSHOP -> TEXT_WORKSHOP.getString();
        };
    }

    private static List<Component> actionTooltip(CourierAction.ActionType type) {
        String s = switch (type) {
            case TAKE      -> TOOLTIP_TAKE.getString();
            case PUT       -> TOOLTIP_PUT.getString();
            case TAKE_ANY  -> TOOLTIP_TAKE_ANY.getString();
            case PUT_ANY   -> TOOLTIP_PUT_ANY.getString();
            case TAKE_ALL  -> TOOLTIP_TAKE_ALL.getString();
            case PUT_ALL   -> TOOLTIP_PUT_ALL.getString();
            case PUT_FILL  -> TOOLTIP_PUT_FILL.getString();
            case TAKE_FILL -> TOOLTIP_TAKE_FILL.getString();
            case WAIT      -> TOOLTIP_WAIT.getString();
        };
        return List.of(Component.literal(s));
    }

    /** Tooltip text shown for each target type (closed display or open option). */
    private static List<Component> sourceTooltip(CourierAction.SourceType type) {
        String s = switch (type) {
            case CHEST   -> TOOLTIP_CHEST.getString();
            case STORAGE -> TOOLTIP_STORAGE.getString();
            case MARKET  -> TOOLTIP_MARKET.getString();
            case KITCHEN -> TOOLTIP_KITCHEN.getString();
            case WORKSHOP -> TOOLTIP_WORKSHOP.getString();
        };
        return List.of(Component.literal(s));
    }

    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        hoveredTooltipStack    = ItemStack.EMPTY;
        hoveredWaypointTooltip = null;
        hoveredSlotTooltip     = null;
        hoveredHintTooltip     = null;

        super.render(g, mouseX, mouseY, partialTicks);
        renderActionRows(g, mouseX, mouseY);
        renderActionScrollbar(g, mouseX, mouseY);

        if (!hoveredTooltipStack.isEmpty()) {
            // Item tooltip + the configured amount (may exceed the stack size) and how to change it.
            List<Component> lines = new ArrayList<>(Screen.getTooltipFromItem(minecraft, hoveredTooltipStack));
            lines.add(Component.translatable("gui.workers.tooltip.courier.amountValue", hoveredTooltipStack.getCount()).withStyle(ChatFormatting.GOLD));
            lines.add(TOOLTIP_AMOUNT.copy().withStyle(ChatFormatting.GRAY));
            g.renderComponentTooltip(font, lines, hoveredTooltipX, hoveredTooltipY);
        }
        else if (hoveredSlotTooltip != null)
            g.renderComponentTooltip(font, hoveredSlotTooltip, mouseX, mouseY);
        if (hoveredWaypointTooltip != null)
            g.renderComponentTooltip(font, hoveredWaypointTooltip, mouseX, mouseY);
        if (hoveredHintTooltip != null)
            g.renderTooltip(font, font.split(hoveredHintTooltip, 200), mouseX, mouseY);

        // Dropdown tooltips — rendered last and at a high z so they sit above an open dropdown
        // (whose options are drawn at z+500). Works for both the closed display and open options.
        List<Component> ddTooltip = null;
        for (CourierDropDownMenu<CourierAction.ActionType> dd : actionTypeDropDowns) {
            ddTooltip = dd.getHoverTooltip(mouseX, mouseY);
            if (ddTooltip != null) break;
        }
        if (ddTooltip == null) {
            for (CourierDropDownMenu<SourceOption> dd : sourceTypeDropDowns) {
                ddTooltip = dd.getHoverTooltip(mouseX, mouseY);
                if (ddTooltip != null) break;
            }
        }
        if (ddTooltip != null) {
            g.pose().pushPose();
            g.pose().translate(0, 0, 600);
            g.renderComponentTooltip(font, ddTooltip, mouseX, mouseY);
            g.pose().popPose();
        }
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
                String lbl = TEXT_ADD_ACTION.getString();
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

                    if (mouseX >= sx && mouseX < sx + COL_ITEM
                            && mouseY >= sy && mouseY < sy + COL_ITEM
                            && !isOverActionDropDown(mouseX, mouseY)) {
                        if (!item.isEmpty()) {
                            hoveredTooltipStack = item;
                            hoveredTooltipX     = mouseX;
                            hoveredTooltipY     = mouseY;
                        } else {
                            hoveredSlotTooltip = List.of(TOOLTIP_EMPTY_SLOT);
                        }
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

        // Target list state of the selected waypoint, right-aligned above the inventory.
        var wp = getSelectedWaypoint();
        if (wp != null) {
            Component hint = null;
            if (unloadedWaypoints.contains(wp.getPosition())) hint = TEXT_TARGETS_NOT_LOADED;
            else if (!targetCache.containsKey(wp.getPosition())) hint = TEXT_TARGETS_LOADING;

            if (hint != null) {
                int hintX = ACT_X + ACT_W - font.width(hint);
                guiGraphics.drawString(font, hint, hintX, INV_LABEL_Y, COLOR_HINT, false);
                if (hint == TEXT_TARGETS_NOT_LOADED
                        && mouseX - leftPos >= hintX && mouseX - leftPos < ACT_X + ACT_W
                        && mouseY - topPos >= INV_LABEL_Y && mouseY - topPos < INV_LABEL_Y + 9)
                    hoveredHintTooltip = TOOLTIP_TARGETS_NOT_LOADED;
            }
        }
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
                                current.setCount(Math.min(MAX_AMOUNT, current.getCount() + cursor.getCount()));
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
                    wp.actions.add(CourierAction.take(CourierAction.SourceType.CHEST, ItemStack.EMPTY));
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
        // Scrolling over an item slot changes the amount: 1, Shift = 10, Ctrl = 64.
        CourierAction slotAction = getActionAtItemSlot(mx, my);
        if (slotAction != null && slotAction.getItemStack() != null && !slotAction.getItemStack().isEmpty()) {
            int step = hasControlDown() ? 64 : hasShiftDown() ? 10 : 1;
            ItemStack stack = slotAction.getItemStack();
            stack.setCount(Math.max(1, Math.min(MAX_AMOUNT, stack.getCount() + (delta > 0 ? step : -step))));
            return true;
        }

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

    private boolean isOverActionDropDown(double mx, double my) {
        for (var dd : actionTypeDropDowns) if (dd.isMouseOver(mx, my)) return true;
        for (var dd : sourceTypeDropDowns) if (dd.isMouseOver(mx, my)) return true;
        return false;
    }

    @Nullable
    private CourierAction getActionAtItemSlot(double mx, double my) {
        var wp = getSelectedWaypoint();
        if (wp == null) return null;
        // An open dropdown lies above the slots.
        if (isOverActionDropDown(mx, my)) return null;

        for (int[] slot : itemSlotPos) {
            int sx = slot[0], sy = slot[1], idx = slot[2];
            if (idx >= wp.actions.size()) continue;
            if (mx >= sx && mx < sx + COL_ITEM && my >= sy && my < sy + COL_ITEM) {
                CourierAction action = wp.actions.get(idx);
                return action.getActionType().hasItemSlot() ? action : null;
            }
        }
        return null;
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
    /** True if the courier is currently working this route and is at / heading to the given waypoint. */
    private boolean isCourierCurrentWaypoint(int index) {
        if (workingRoute == null) return false;
        CompoundTag data = courierEntity.getRouteData();
        if (!data.getBoolean("hasRoute") || !data.contains("route")) return false;
        CompoundTag route = data.getCompound("route");
        if (workingRoute.getRouteId() == null || !route.hasUUID("RouteId") || !workingRoute.getRouteId().equals(route.getUUID("RouteId"))) return false;
        return data.getInt("currentIndex") == index;
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

                int actionCount = 0;
                String name     = "";
                boolean hasName = false;
                if (workingRoute != null && waypointIndex < workingRoute.size()) {
                    var wp = workingRoute.getWaypoints().get(waypointIndex);
                    actionCount = wp.actions.size();
                    hasName     = !wp.displayName.isEmpty();
                    name        = wp.displayName.isEmpty()
                            ? Component.translatable("gui.workers.courier.waypoint", waypointIndex + 1).getString()
                            : wp.displayName;
                }
                boolean current = isCourierCurrentWaypoint(waypointIndex);

                // Waypoint number (green = where the courier currently is / is heading to)
                String indexStr = String.valueOf(waypointIndex + 1);
                g.drawString(font, indexStr, left + 4, top + 5, current ? COLOR_CURRENT_WP : FONT_COLOR, false);

                // Action count in white, right-aligned
                String countStr = "(" + actionCount + ")";
                int countX = left + WaypointList.this.getRowWidth() - 4 - font.width(countStr);
                g.drawString(font, countStr, countX, top + 5, 0xFFFFFF, false);

                // Name, cut to the space between number and count
                int nameX = left + 4 + font.width(indexStr) + 4;
                if (hasName) {
                    String shortName = font.plainSubstrByWidth(name, Math.max(0, countX - 3 - nameX));
                    g.drawString(font, shortName, nameX, top + 5, 0xFFFFFF, false);
                }

                // Full name as hover tooltip (rendered after all entries)
                if (hovered && !name.isEmpty()) {
                    List<Component> lines = new ArrayList<>();
                    lines.add(Component.literal(name));
                    if (current) lines.add(TOOLTIP_CURRENT_WP.copy().withStyle(ChatFormatting.GREEN));
                    CourierScreen.this.hoveredWaypointTooltip = lines;
                }
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