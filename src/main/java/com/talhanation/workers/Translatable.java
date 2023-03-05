package com.talhanation.workers;

import com.talhanation.workers.init.ModShortcuts;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class Translatable {

    public static MutableComponent TEXT_HELLO(String job) {
        return Component.translatable("chat.workers.text.hello", job);
    }

    public static MutableComponent TEXT_HELLO_OWNED(String job, String owner) {
        return Component.translatable("chat.workers.text.hello_owned", job, owner);
    }

    // TODO: Use these only when work starts
    public static final MutableComponent TEXT_RECRUITED1 = Component.translatable("chat.workers.text.recruited1");
    public static final MutableComponent TEXT_RECRUITED2 = Component.translatable("chat.workers.text.recruited2");
    public static final MutableComponent TEXT_RECRUITED3 = Component.translatable("chat.workers.text.recruited3");

    public static MutableComponent TEXT_ATTACKED(String job, String attacker) {
        return Component.translatable("chat.workers.text.attacked", job, attacker);
    }

    public static final MutableComponent TEXT_WORKING = Component.translatable("chat.workers.text.working");
    public static final MutableComponent TEXT_DONE = Component.translatable("chat.workers.text.done");
    public static final MutableComponent TEXT_STARVING = Component.translatable("chat.workers.text.starving");

    public static final MutableComponent TEXT_FOLLOW = Component.translatable("chat.workers.text.follow");
    public static final MutableComponent TEXT_CONTINUE = Component.translatable("chat.workers.text.continue");
    public static final MutableComponent TEXT_WANDER = Component.translatable("chat.workers.text.wander");

    /*
    public static MutableComponent TEXT_BED_KEY() {
        Component currentMapping = ModShortcuts.ASSIGN_BED_KEY.getTranslatedKeyMessage();
        return Component.translatable("chat.workers.controls.assign_bed_key", currentMapping);
    }
    public static MutableComponent TEXT_CHEST_KEY() {
        Component currentMapping = ModShortcuts.ASSIGN_CHEST_KEY.getTranslatedKeyMessage();
        return Component.translatable("chat.workers.controls.assign_chest_key", currentMapping);
    }
    public static MutableComponent TEXT_WORKSPACE_KEY() {
        Component currentMapping = ModShortcuts.ASSIGN_WORKSPACE_KEY.getTranslatedKeyMessage();
        return Component.translatable("chat.workers.controls.assign_workspace_key", currentMapping);
    }

     */
    public static MutableComponent TEXT_OUT_OF_TOOLS(ItemStack lastItem) {
        return Component.translatable("chat.workers.text.outOfTools", lastItem.getDisplayName().getString());
    }

    public static MutableComponent TEXT_HIRE_COSTS(int cost) {
        return Component.translatable("chat.workers.text.hire_costs", cost);
    }
    public static MutableComponent TEXT_NEED(int sollPrice, Item emerald) {
        return Component.translatable("chat.workers.text.need", sollPrice, emerald);
    }
    public static final MutableComponent TEXT_OUT_OF_STOCK = Component.translatable("chat.workers.text.outOfStock");
    public static final MutableComponent TEXT_OUT_OF_STOCK_OWNER = Component.translatable("chat.workers.text.outOfStockOwner");
    public static final MutableComponent TEXT_INV_FULL = Component.translatable("chat.workers.text.invFull");
    public static final MutableComponent TEXT_INV_FULL_OWNER = Component.translatable("chat.workers.text.invFullOwner");
    public static final MutableComponent TEXT_HOME = Component.translatable("chat.workers.text.home");

    public static final MutableComponent NEED_CHEST = Component.translatable("chat.workers.needChest");
    public static final MutableComponent TEXT_CHEST = Component.translatable("chat.workers.text.chest");
    public static final MutableComponent TEXT_CHEST_ERROR = Component.translatable("chat.workers.text.chestError");
    public static final MutableComponent NEED_BED = Component.translatable("chat.workers.needBed");
    public static final MutableComponent TEXT_BED = Component.translatable("chat.workers.text.bed");
    public static final MutableComponent TEXT_BED_ERROR = Component.translatable("chat.workers.text.bedError");

    public static final MutableComponent TEXT_NO_FOOD = Component.translatable("chat.workers.text.noFoodInUpkeep");
    public static final MutableComponent TEXT_BUTTON_WORK_POS = Component.translatable("gui.workers.button.workPos");
    public static final MutableComponent TEXT_BUTTON_CHEST_POS = Component.translatable("gui.workers.button.chestPos");
    public static final MutableComponent TEXT_BUTTON_SLEEP_POS = Component.translatable("gui.workers.button.sleepPos");


}
