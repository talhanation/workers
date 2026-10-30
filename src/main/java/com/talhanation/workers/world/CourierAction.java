package com.talhanation.workers.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.UUID;

public class CourierAction {

    public enum ActionType {
        // Specific item (carries an ItemStack template)
        TAKE,         // take up to N of a specific item from target
        PUT,        // put up to N of a specific item into target
        TAKE_ANY,     // take ALL of a specific item type (no count cap)
        PUT_ANY,    // put ALL of a specific item type from courier into target

        TAKE_ALL,     // take everything from target until courier inventory full
        PUT_ALL,    // empty entire courier inventory into target
        PUT_FILL,   // fill the TARGET up to a defined amount (checked against the target)
        TAKE_FILL,  // fill the courier's OWN inventory up to a defined amount (checked against own inv)
        // Timing
        WAIT;

        /** True if this action type needs an ItemStack filter in the GUI. */
        public boolean hasItemSlot() {
            return this == TAKE
                    || this == PUT
                    || this == TAKE_ANY
                    || this == PUT_FILL
                    || this == TAKE_FILL
                    || this == PUT_ANY;
        }

        public boolean hasTime() {
            return this == WAIT;
        }

        /** Label shown in the GUI dropdown. */
        public String displayLabel() {
            return switch (this) {
                case TAKE        -> "Take";
                case PUT         -> "Put";
                case PUT_FILL    -> "Put Fill";
                case TAKE_FILL   -> "Take Fill";
                case TAKE_ANY -> "Take Any";
                case PUT_ANY  -> "Put Any";
                case TAKE_ALL -> "Take All";
                case PUT_ALL  -> "Put All";
                case WAIT     -> "Wait";
            };
        }

        public static ActionType fromString(String s) {
            try { return valueOf(s); } catch (Exception e) { return WAIT; }
        }
    }

    public enum SourceType {
        CHEST,
        STORAGE,
        MARKET,
        KITCHEN,
        WORKSHOP;
        //WORKER;

        public static SourceType fromString(String s) {
            try { return valueOf(s); } catch (Exception e) { return CHEST; }
        }
    }

    private ActionType          actionType;
    @Nullable private SourceType sourceType;
    @Nullable private ItemStack  itemStack;
    private int                  waitSeconds;
    // Specific work area to use at the waypoint. null = every accessible area of the source type in range.
    @Nullable private UUID       targetId;
    // Display name of the target, cached so the GUI can show it while the area is not loaded.
    private String               targetName = "";

    // Factories

    public static CourierAction take(SourceType src, ItemStack item) {
        CourierAction a = new CourierAction(); a.actionType = ActionType.TAKE;
        a.sourceType = src; a.itemStack = item.copy(); return a;
    }
    public static CourierAction put(SourceType src, ItemStack item) {
        CourierAction a = new CourierAction(); a.actionType = ActionType.PUT;
        a.sourceType = src; a.itemStack = item.copy(); return a;
    }
    public static CourierAction takeAny(SourceType src, ItemStack item) {
        CourierAction a = new CourierAction(); a.actionType = ActionType.TAKE_ANY;
        a.sourceType = src; a.itemStack = item.copy(); return a;
    }
    public static CourierAction putAny(SourceType src, ItemStack item) {
        CourierAction a = new CourierAction(); a.actionType = ActionType.PUT_ANY;
        a.sourceType = src; a.itemStack = item.copy(); return a;
    }
    public static CourierAction takeAll(SourceType src) {
        CourierAction a = new CourierAction(); a.actionType = ActionType.TAKE_ALL;
        a.sourceType = src; return a;
    }

    public static CourierAction putAll(SourceType src) {
        CourierAction a = new CourierAction(); a.actionType = ActionType.PUT_ALL;
        a.sourceType = src; return a;
    }
    public static CourierAction putFill(SourceType src, ItemStack item) {
        CourierAction a = new CourierAction(); a.actionType = ActionType.PUT_FILL;
        a.sourceType = src; a.itemStack = item.copy(); return a;
    }
    public static CourierAction takeFill(SourceType src, ItemStack item) {
        CourierAction a = new CourierAction(); a.actionType = ActionType.TAKE_FILL;
        a.sourceType = src; a.itemStack = item.copy(); return a;
    }
    public static CourierAction wait(int seconds) {
        CourierAction a = new CourierAction(); a.actionType = ActionType.WAIT;
        a.waitSeconds = seconds; return a;
    }

    private CourierAction() {}

    // Getters / setters
    public ActionType getActionType()                  { return actionType; }
    public void setActionType(ActionType t)            { this.actionType = t; }
    @Nullable public SourceType getSourceType()        { return sourceType; }
    public void setSourceType(@Nullable SourceType s)  { this.sourceType = s; }
    @Nullable public ItemStack getItemStack()          { return itemStack; }
    public void setItemStack(@Nullable ItemStack s)    { this.itemStack = (s != null) ? s.copy() : null; }
    public int getWaitSeconds()                        { return waitSeconds; }
    public void setWaitSeconds(int s)                  { this.waitSeconds = s; }
    @Nullable public UUID getTargetId()                { return targetId; }
    public void setTargetId(@Nullable UUID id)         { this.targetId = id; }
    public String getTargetName()                      { return targetName; }
    public void setTargetName(@Nullable String name)   { this.targetName = (name != null) ? name : ""; }
    public boolean hasTarget()                         { return targetId != null; }

    public void clearTarget() {
        this.targetId   = null;
        this.targetName = "";
    }

    // NBT
    public CompoundTag toNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("ActionType", actionType.name());
        if (sourceType != null) nbt.putString("SourceType", sourceType.name());
        if (itemStack != null && !itemStack.isEmpty()) {
            // ItemStack stores its count as a byte, so amounts > 127 are saved separately as int.
            nbt.put("Item", itemStack.copyWithCount(1).serializeNBT());
            nbt.putInt("Amount", itemStack.getCount());
        }
        nbt.putInt("WaitSeconds", waitSeconds);
        if (targetId != null) {
            nbt.putUUID("TargetId", targetId);
            nbt.putString("TargetName", targetName);
        }
        return nbt;
    }

    @Nullable
    public static CourierAction fromNBT(CompoundTag nbt) {
        if (nbt == null || nbt.isEmpty()) return null;
        String raw = nbt.getString("ActionType");
        // Backward compatibility: the old single "FILL" action behaved like PUT_FILL.
        ActionType type = "FILL".equals(raw) ? ActionType.PUT_FILL : ActionType.fromString(raw);
        if (type == ActionType.WAIT) return wait(nbt.getInt("WaitSeconds"));
        SourceType source = SourceType.fromString(nbt.getString("SourceType"));
        ItemStack  item   = nbt.contains("Item") ? ItemStack.of(nbt.getCompound("Item")) : ItemStack.EMPTY;
        if (!item.isEmpty() && nbt.contains("Amount")) item.setCount(Math.max(1, nbt.getInt("Amount")));

        CourierAction action = switch (type) {
            case TAKE -> take(source, item);
            case PUT -> put(source, item);
            case TAKE_ANY -> takeAny(source, item);
            case PUT_ANY -> putAny(source, item);
            case TAKE_ALL -> takeAll(source);
            case PUT_ALL -> putAll(source);
            case PUT_FILL -> putFill(source, item);
            case TAKE_FILL -> takeFill(source, item);
            default          -> null;
        };
        if (action != null && nbt.hasUUID("TargetId")) {
            action.setTargetId(nbt.getUUID("TargetId"));
            action.setTargetName(nbt.getString("TargetName"));
        }
        return action;
    }
}