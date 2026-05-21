package com.talhanation.workers.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public class CourierAction {

    public enum ActionType {
        // Specific item (carries an ItemStack template)
        TAKE,         // take up to N of a specific item from target
        PUT,        // put up to N of a specific item into target
        TAKE_ANY,     // take ALL of a specific item type (no count cap)
        PUT_ANY,    // put ALL of a specific item type from courier into target

        TAKE_ALL,     // take everything from target until courier inventory full
        PUT_ALL,    // empty entire courier inventory into target
        FILL,    // fill entire target to defined amount
        // Timing
        WAIT;

        /** True if this action type needs an ItemStack filter in the GUI. */
        public boolean hasItemSlot() {
            return this == TAKE
                    || this == PUT
                    || this == TAKE_ANY
                    || this == FILL
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
                case FILL     -> "Fill";
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
        KITCHEN;
        //WORKER;

        public static SourceType fromString(String s) {
            try { return valueOf(s); } catch (Exception e) { return CHEST; }
        }
    }

    private ActionType          actionType;
    @Nullable private SourceType sourceType;
    @Nullable private ItemStack  itemStack;
    private int                  waitSeconds;

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
    public static CourierAction fill(SourceType src, ItemStack item) {
        CourierAction a = new CourierAction(); a.actionType = ActionType.FILL;
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

    // NBT
    public CompoundTag toNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("ActionType", actionType.name());
        if (sourceType != null) nbt.putString("SourceType", sourceType.name());
        if (itemStack != null && !itemStack.isEmpty()) nbt.put("Item", itemStack.serializeNBT());
        nbt.putInt("WaitSeconds", waitSeconds);
        return nbt;
    }

    @Nullable
    public static CourierAction fromNBT(CompoundTag nbt) {
        if (nbt == null || nbt.isEmpty()) return null;
        ActionType type   = ActionType.fromString(nbt.getString("ActionType"));
        if (type == ActionType.WAIT) return wait(nbt.getInt("WaitSeconds"));
        SourceType source = SourceType.fromString(nbt.getString("SourceType"));
        ItemStack  item   = nbt.contains("Item") ? ItemStack.of(nbt.getCompound("Item")) : ItemStack.EMPTY;
        return switch (type) {
            case TAKE -> take(source, item);
            case PUT -> put(source, item);
            case TAKE_ANY -> takeAny(source, item);
            case PUT_ANY -> putAny(source, item);
            case TAKE_ALL -> takeAll(source);
            case PUT_ALL -> putAll(source);
            case FILL -> fill(source, item);
            default          -> null;
        };
    }
}
