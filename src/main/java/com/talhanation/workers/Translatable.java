package com.talhanation.workers;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class Translatable {
    public static final MutableComponent TOOLTIP_TRAVEL_INFO =  new TranslatableComponent("gui.workers.tooltip.travel_info");
    public static final MutableComponent TOOLTIP_AUTO_START_TRAVEL =  new TranslatableComponent("gui.workers.tooltip.autoStartTravel");

    public static final MutableComponent TOOLTIP_TRAVEL_SPEED =  new TranslatableComponent("gui.workers.tooltip.travelSpeed");
    public static final MutableComponent TEXT_TRAVEL_INFO =  new TranslatableComponent("chat.workers.text.travel_info");
    public static final MutableComponent TOOLTIP_TRAVEL_START =  new TranslatableComponent("gui.workers.tooltip.travel_start");
    public static final MutableComponent TOOLTIP_TRAVEL_STOP =  new TranslatableComponent("gui.workers.tooltip.travel_stop");
    public static final MutableComponent TOOLTIP_TRAVEL_RETURN =  new TranslatableComponent("gui.workers.tooltip.travel_return");
    public static final MutableComponent TOOLTIP_FARMER_USE_EGGS = new TranslatableComponent("gui.workers.tooltip.farmerUseEggs");;
    public static final MutableComponent TEXT_BUTTON_ADD_WAYPOINT = new TranslatableComponent("gui.workers.button.travel_add_waypoint");
    public static final MutableComponent TEXT_BUTTON_TRAVEL_RETURN = new TranslatableComponent("gui.workers.button.travel_return");
    public static final MutableComponent TEXT_BUTTON_TRAVEL_START = new TranslatableComponent("gui.workers.button.travel_start");
    public static final MutableComponent TEXT_BUTTON_TRAVEL_STOP = new TranslatableComponent("gui.workers.button.travel_stop");
    public static final MutableComponent TEXT_BUTTON_FOLLOW = new TranslatableComponent("gui.workers.button.follow");
	public static final MutableComponent TOOLTIP_SEND_INFO =  new TranslatableComponent("gui.workers.tooltip.sendInfo");


    public static MutableComponent TEXT_HELLO(String job) {
        return new TranslatableComponent("chat.workers.text.hello", job);
    }

    public static MutableComponent TEXT_HELLO_OWNED(String job, String owner) {
        return new TranslatableComponent("chat.workers.text.hello_owned", job, owner);
    }

    // TODO: Use these only when work starts
    public static final MutableComponent TEXT_RECRUITED1 = new TranslatableComponent("chat.workers.text.recruited1");
    public static final MutableComponent TEXT_RECRUITED2 = new TranslatableComponent("chat.workers.text.recruited2");
    public static final MutableComponent TEXT_RECRUITED3 = new TranslatableComponent("chat.workers.text.recruited3");

    public static MutableComponent TEXT_ATTACKED(String job, String attacker) {
        return new TranslatableComponent("chat.workers.text.attacked", job, attacker);
    }

    public static final MutableComponent TEXT_WORKING = new TranslatableComponent("chat.workers.text.working");
    public static final MutableComponent TEXT_DONE = new TranslatableComponent("chat.workers.text.done");
    public static final MutableComponent TEXT_STARVING = new TranslatableComponent("chat.workers.text.starving");

    public static final MutableComponent TEXT_FOLLOW = new TranslatableComponent("chat.workers.text.follow");
    public static final MutableComponent TEXT_CONTINUE = new TranslatableComponent("chat.workers.text.continue");
    public static final MutableComponent TEXT_WANDER = new TranslatableComponent("chat.workers.text.wander");

    /*
    public static MutableComponent TEXT_BED_KEY() {
        MutableComponent currentMapping = ModShortcuts.ASSIGN_BED_KEY.getTranslatedKeyMessage();
        return new TranslatableComponent()("chat.workers.controls.assign_bed_key", currentMapping);
    }
    public static MutableComponent TEXT_CHEST_KEY() {
        MutableComponent currentMapping = ModShortcuts.ASSIGN_CHEST_KEY.getTranslatedKeyMessage();
        return new TranslatableComponent()("chat.workers.controls.assign_chest_key", currentMapping);
    }
    public static MutableComponent TEXT_WORKSPACE_KEY() {
        MutableComponent currentMapping = ModShortcuts.ASSIGN_WORKSPACE_KEY.getTranslatedKeyMessage();
        return new TranslatableComponent()("chat.workers.controls.assign_workspace_key", currentMapping);
    }

     */
    public static MutableComponent TEXT_OUT_OF_TOOLS(ItemStack lastItem) {
        return new TranslatableComponent("chat.workers.text.outOfTools", lastItem.getDisplayName().getString());
    }

    public static MutableComponent TEXT_HIRE_COSTS(int cost, String item) {
        return new TranslatableComponent("chat.workers.text.hire_costs", cost, item);
    }

    public static MutableComponent TEXT_NEED(int sollPrice, Item emerald) {
        return new TranslatableComponent("chat.workers.text.need", sollPrice, emerald);
    }

    public static MutableComponent TEXT_NO_NEED(Item emerald) {
        return new TranslatableComponent("chat.workers.text.noNeed");
    }
    public static final MutableComponent TEXT_FISHER_NO_WATER = new TranslatableComponent("chat.workers.text.fisherNoWater");
    public static final MutableComponent TEXT_OUT_OF_STOCK = new TranslatableComponent("chat.workers.text.outOfStock");
    public static final MutableComponent TEXT_OUT_OF_STOCK_OWNER = new TranslatableComponent("chat.workers.text.outOfStockOwner");
    public static final MutableComponent TEXT_INV_FULL = new TranslatableComponent("chat.workers.text.invFull");
    public static final MutableComponent TEXT_INV_FULL_OWNER = new TranslatableComponent("chat.workers.text.invFullOwner");
    public static final MutableComponent TEXT_HOME = new TranslatableComponent("chat.workers.text.home");

    public static final MutableComponent NEED_CHEST = new TranslatableComponent("chat.workers.needChest");
    public static final MutableComponent TEXT_CHEST = new TranslatableComponent("chat.workers.text.chest");
    public static final MutableComponent TEXT_CHEST_ERROR = new TranslatableComponent("chat.workers.text.chestError");
    public static final MutableComponent NEED_BED = new TranslatableComponent("chat.workers.needBed");
    public static final MutableComponent TEXT_BED = new TranslatableComponent("chat.workers.text.bed");
    public static final MutableComponent TEXT_BED_ERROR = new TranslatableComponent("chat.workers.text.bedError");

    public static final MutableComponent TEXT_NO_FOOD = new TranslatableComponent("chat.workers.text.noFoodInUpkeep");
    public static final MutableComponent TEXT_NO_SPACE_INV = new TranslatableComponent("chat.workers.text.noSpaceInvForUpkeep");

    public static final MutableComponent TEXT_NO_PICKAXE = new TranslatableComponent("chat.workers.text.noPickaxe");
    public static final MutableComponent TEXT_NO_FISHING_ROD = new TranslatableComponent("chat.workers.text.noFishingRod");
    public static final MutableComponent TEXT_BUTTON_WORK_POS = new TranslatableComponent("gui.workers.button.workPos");
    public static final MutableComponent TEXT_BUTTON_CHEST_POS = new TranslatableComponent("gui.workers.button.chestPos");
    public static final MutableComponent TEXT_BUTTON_SLEEP_POS = new TranslatableComponent("gui.workers.button.sleepPos");

    public static final MutableComponent TEXT_CANT_FIND_CHEST = new TranslatableComponent("chat.workers.cantFindChest");
    public static final MutableComponent TEXT_CHEST_FULL = new TranslatableComponent("chat.workers.chestFull");
    public static final MutableComponent TEXT_COULD_NOT_DEPOSIT = new TranslatableComponent("chat.workers.couldNotDeposit");
    public static final MutableComponent TEXT_WAYPOINT_NOT_NEAR_TO_PREV = new TranslatableComponent("chat.workers.text.waypointNotNearToPrev");

}
