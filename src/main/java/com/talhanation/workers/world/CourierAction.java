package com.talhanation.workers.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public class CourierAction {

    public enum ActionType {
        PICKUP, DEPOSIT, WAIT;

        public static ActionType fromString(String s){
            try{
                return valueOf(s);
            }
            catch(Exception e){
                return WAIT;
            }
        }
    }

    public enum SourceType {
        CHEST, STORAGE, WORKER;

        public static SourceType fromString(String s){
            try{
                return valueOf(s);
            }
            catch (Exception e){
                return CHEST;
            }
        }
    }

    private ActionType actionType;
    @Nullable private SourceType sourceType;
    @Nullable private ItemStack itemStack;
    private int waitSeconds;

    // ── Factories ─────────────────────────────────────────────────────────────

    public static CourierAction pickup(SourceType source, ItemStack item){
        CourierAction action = new CourierAction();
        action.actionType  = ActionType.PICKUP;
        action.sourceType  = source;
        action.itemStack = item.copy();
        action.waitSeconds = 0;
        return action;
    }

    public static CourierAction deposit(SourceType source, ItemStack item){
        CourierAction action = new CourierAction();
        action.actionType  = ActionType.DEPOSIT;
        action.sourceType  = source;
        action.itemStack = item.copy();
        action.waitSeconds = 0;
        return action;
    }

    public static CourierAction wait(int seconds){
        CourierAction action = new CourierAction();
        action.actionType  = ActionType.WAIT;
        action.sourceType  = null;
        action.itemStack = null;
        action.waitSeconds = seconds;
        return action;
    }

    private CourierAction(){

    }


    public ActionType getActionType(){
        return actionType;
    }
    public void setActionType(ActionType t){
        this.actionType = t;
    }

    @Nullable public SourceType getSourceType(){
        return sourceType;
    }
    public void setSourceType(@Nullable SourceType s){
        this.sourceType = s;
    }

    @Nullable public ItemStack getItemStack(){
        return itemStack;
    }
    public void setItemStack(@Nullable ItemStack stack){
        this.itemStack = stack != null ? stack.copy() : null;
    }

    public int getWaitSeconds(){
        return waitSeconds;
    }
    public void setWaitSeconds(int seconds){
        this.waitSeconds = seconds;
    }

    public CompoundTag toNBT(){
        CompoundTag nbt = new CompoundTag();
        nbt.putString("ActionType", actionType.name());
        if (sourceType != null) nbt.putString("SourceType", sourceType.name());
        if (itemStack != null && !itemStack.isEmpty()) nbt.put("Item", itemStack.serializeNBT());

        nbt.putInt("WaitSeconds", waitSeconds);

        return nbt;
    }

    @Nullable
    public static CourierAction fromNBT(CompoundTag nbt){
        if (nbt == null || nbt.isEmpty()) return null;

        ActionType type = ActionType.fromString(nbt.getString("ActionType"));

        if (type == ActionType.WAIT){
            return wait(nbt.getInt("WaitSeconds"));
        }

        SourceType source = SourceType.fromString(nbt.getString("SourceType"));
        ItemStack item = nbt.contains("Item") ? ItemStack.of(nbt.getCompound("Item")) : ItemStack.EMPTY;

        if (type == ActionType.PICKUP) return pickup(source, item);
        if (type == ActionType.DEPOSIT) return deposit(source, item);

        return null;
    }
}
