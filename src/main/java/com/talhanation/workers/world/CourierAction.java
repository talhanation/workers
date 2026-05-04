package com.talhanation.workers.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public class CourierAction {

    public enum ActionType {
        // Specific item (carries an ItemStack template)
        PICKUP,         // take up to N of a specific item from target
        DEPOSIT,        // put up to N of a specific item into target
        PICKUP_ANY,     // take ALL of a specific item type (no count cap)
        DEPOSIT_ANY,    // put ALL of a specific item type from courier into target
        // Bulk / wildcard
        PICKUP_ALL,     // take everything from target until courier inventory full
        DEPOSIT_ALL,    // empty entire courier inventory into target
        // Timing
        WAIT;

        /** True if this action type needs an ItemStack filter in the GUI. */
        public boolean hasItemSlot() {
            return this == PICKUP || this == DEPOSIT
                    || this == PICKUP_ANY || this == DEPOSIT_ANY;
        }

        /** True if the count field (stack size) is meaningful for this type. */
        public boolean hasCount() {
            return this == PICKUP || this == DEPOSIT;
        }

        /** Label shown in the GUI dropdown. */
        public String displayLabel() {
            return switch (this) {
                case PICKUP      -> "Pickup";
                case DEPOSIT     -> "Deposit";
                case PICKUP_ANY  -> "Pickup Any";
                case DEPOSIT_ANY -> "Deposit Any";
                case PICKUP_ALL  -> "Pickup All";
                case DEPOSIT_ALL -> "Deposit All";
                case WAIT        -> "Wait";
            };
        }

        public static ActionType fromString(String s) {
            try { return valueOf(s); } catch (Exception e) { return WAIT; }
        }
    }

    public enum SourceType {
        CHEST,
        STORAGE,
        MARKET;
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

    public static CourierAction pickup(SourceType src, ItemStack item) {
        CourierAction a = new CourierAction(); a.actionType = ActionType.PICKUP;
        a.sourceType = src; a.itemStack = item.copy(); return a;
    }
    public static CourierAction deposit(SourceType src, ItemStack item) {
        CourierAction a = new CourierAction(); a.actionType = ActionType.DEPOSIT;
        a.sourceType = src; a.itemStack = item.copy(); return a;
    }
    public static CourierAction pickupAny(SourceType src, ItemStack item) {
        CourierAction a = new CourierAction(); a.actionType = ActionType.PICKUP_ANY;
        a.sourceType = src; a.itemStack = item.copy(); return a;
    }
    public static CourierAction depositAny(SourceType src, ItemStack item) {
        CourierAction a = new CourierAction(); a.actionType = ActionType.DEPOSIT_ANY;
        a.sourceType = src; a.itemStack = item.copy(); return a;
    }
    public static CourierAction pickupAll(SourceType src) {
        CourierAction a = new CourierAction(); a.actionType = ActionType.PICKUP_ALL;
        a.sourceType = src; return a;
    }
    public static CourierAction depositAll(SourceType src) {
        CourierAction a = new CourierAction(); a.actionType = ActionType.DEPOSIT_ALL;
        a.sourceType = src; return a;
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
            case PICKUP      -> pickup(source, item);
            case DEPOSIT     -> deposit(source, item);
            case PICKUP_ANY  -> pickupAny(source, item);
            case DEPOSIT_ANY -> depositAny(source, item);
            case PICKUP_ALL  -> pickupAll(source);
            case DEPOSIT_ALL -> depositAll(source);
            default          -> null;
        };
    }
}
