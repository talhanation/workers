package com.talhanation.workers.entities.ai;

import com.talhanation.workers.Main;
import com.talhanation.workers.entities.AbstractWorkerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public class TransferItemsInChestGoal extends Goal {

    private final AbstractWorkerEntity worker;
    private BlockPos chestPos;
    private BlockPos homePos;
    private Container container;
    private final MutableComponent NEED_ITEMS_HOME = Component.translatable("chat.workers.needHome");
    private final MutableComponent NEED_CHEST = Component.translatable("chat.workers.needChest");

    public TransferItemsInChestGoal(AbstractWorkerEntity worker) {
        this.worker = worker;
    }

    @Override
    public boolean canUse() {
        return true;
    }

    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        if (worker.getHomePos() != null)
            this.homePos = worker.getHomePos();
        if (this.chestPos != null) {
            Main.LOGGER.debug("found chestPos");
        }

        LivingEntity owner = worker.getOwner();
        if (owner == null) {
            return;
        }
        if (homePos == null) {
            owner.sendSystemMessage(
                    Component.literal(worker.getName().getString() + ": " + NEED_ITEMS_HOME.getString()));
        } else {
            this.chestPos = this.findChestPos();

            if (chestPos == null) {
                owner.sendSystemMessage(
                        Component.literal(worker.getName().getString() + ": " + NEED_CHEST.getString()));
            }
        }
    }

    @Override
    public void stop() {
        super.stop();
        this.chestPos = null;
        this.container = null;
    }

    @Override
    public void tick() {
        if (!worker.isSleeping()) {
            if (homePos != null)
                chestPos = this.findChestPos();
        }

        if (chestPos != null && !worker.getInventory().isEmpty()) {
            Main.LOGGER.debug("Moving to chest");
            this.worker.getNavigation().moveTo(chestPos.getX(), chestPos.getY(), chestPos.getZ(), 1.1D);

            if (worker.level.getBlockState(chestPos).getBlock() instanceof BaseEntityBlock entityBlock) {

                BlockEntity entity = worker.level.getBlockEntity(chestPos);
                if (entity instanceof Container containerEntity) {
                    this.container = containerEntity;
                }
            }

            if (container != null && chestPos.closerThan(worker.getOnPos(), 3)) {

                this.worker.getNavigation().stop();
                this.worker.getLookControl().setLookAt(chestPos.getX(), chestPos.getY() + 1, chestPos.getZ(), 10.0F,
                        (float) this.worker.getMaxHeadXRot());

                Main.LOGGER.debug("Depositing to chest");
                this.depositItems();
                // this.done = true;
            }
        }
    }

    private void depositItems() {
        SimpleContainer inventory = worker.getInventory();

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            Item itemInSlot = stack.getItem();

            int itemcount = countItemsInInventory(itemInSlot);
            int depositCount = (int) Math.ceil((double) itemcount / 2);

            Main.LOGGER.debug("itemcount: " + itemcount);
            Main.LOGGER.debug("depositCount: " + depositCount);

            for (int k = 0; k < inventory.getContainerSize(); k++) {
                if (depositCount > 64) {
                    stack.setCount(depositCount);
                    this.addItem(stack, container);

                    depositCount = depositCount - 64;

                } else {
                    stack.setCount(depositCount);
                    this.addItem(stack, container);
                    break;
                }
            }
            Main.LOGGER.debug("added " + stack.getDisplayName().getString() + " " + depositCount + "x");
            Main.LOGGER.debug("deposit done");

            inventory.removeItemNoUpdate(i);

            // add rest to villager inv
            stack.setCount(depositCount);
            this.addItem(stack, worker.getInventory());

        }

        // pro item -> transfer
        // zähl die items nur 1/2 wird deposit
        // check ob platz gibt
        // ja -> remove 1 add 1
        // nein -> abbruch

    }

    private int countItemsInInventory(Item item) {
        int count = 0;
        SimpleContainer workerInv = this.worker.getInventory();

        for (int i = 0; i < workerInv.getContainerSize(); i++) {
            ItemStack itemStackInSlot = workerInv.getItem(i);
            Item itemInSlot = itemStackInSlot.getItem();
            if (itemInSlot == item) {
                count = count + itemStackInSlot.getCount();
            }
        }

        return count;
    }

    @Nullable
    private BlockPos findChestPos() {
        if (this.worker.getHomePos() != null) {
            BlockPos chestPos;
            int range = 8;

            for (int x = -range; x < range; x++) {
                for (int y = -range; y < range; y++) {
                    for (int z = -range; z < range; z++) {
                        chestPos = homePos.offset(x, y, z);
                        BlockEntity block = worker.level.getBlockEntity(chestPos);
                        if (block instanceof Container)
                            return chestPos;
                    }
                }
            }
        }
        return null;
    }

    public ItemStack addItem(ItemStack stack, Container container) {
        ItemStack itemstack = stack.copy();
        this.moveItemToOccupiedSlotsWithSameType(itemstack, container);
        if (itemstack.isEmpty()) {
            return ItemStack.EMPTY;
        } else {
            this.moveItemToEmptySlots(itemstack, container);
            return itemstack.isEmpty() ? ItemStack.EMPTY : itemstack;
        }
    }

    private void moveItemToEmptySlots(ItemStack stack, Container container) {
        for (int i = 0; i < container.getContainerSize(); ++i) {
            ItemStack itemstack = container.getItem(i);
            if (itemstack.isEmpty()) {
                container.setItem(i, stack.copy());
                stack.setCount(0);
                return;
            }
        }

    }

    private void moveItemToOccupiedSlotsWithSameType(ItemStack stack, Container container) {
        for (int i = 0; i < container.getContainerSize(); ++i) {
            ItemStack itemstack = container.getItem(i);
            if (ItemStack.isSameItemSameTags(itemstack, stack)) {
                this.moveItemsBetweenStacks(stack, itemstack, container);
                if (stack.isEmpty()) {
                    return;
                }
            }
        }

    }

    private void moveItemsBetweenStacks(ItemStack stack, ItemStack stack1, Container container) {
        int i = Math.min(container.getMaxStackSize(), stack1.getMaxStackSize());
        int j = Math.min(stack.getCount(), i - stack1.getCount());
        if (j > 0) {
            stack1.grow(j);
            stack.shrink(j);
            container.setChanged();
        }

    }
}
