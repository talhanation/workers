package com.talhanation.workers.inventory;

import com.talhanation.workers.Main;
import com.talhanation.workers.entities.AbstractWorkerEntity;
import com.talhanation.workers.entities.MerchantEntity;
import de.maxhenkel.corelib.inventory.ContainerBase;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class MerchantOwnerContainer extends ContainerBase {
    private final Container workerTradeInventory;
    private final MerchantEntity merchant;

    public MerchantOwnerContainer(int id, MerchantEntity merchant, Inventory playerInventory) {
        super(Main.MERCHANT_OWNER_CONTAINER_TYPE, id, playerInventory, merchant.getInventory());
        this.merchant = merchant;
        this.workerTradeInventory = merchant.getTradeInventory();
        addWorkerTradeSlots();
        addWorkerPriceSlots();
        addPlayerInventorySlots();
    }

    @Override
    public int getInvOffset() {
        return 56;
    }

    public void addWorkerPriceSlots() {
        for (int k = 0; k < MerchantEntity.PRICE_SLOT.length; ++k) {
            this.addSlot(new Slot(workerTradeInventory, MerchantEntity.PRICE_SLOT[k], 8 + 18, 16 + k * 18) {
                @Override
                public boolean mayPlace(@NotNull ItemStack itemStack) {
                    workerTradeInventory.setItem(this.getSlotIndex(), itemStack.copy());
                    return false;
                }

                @Override
                public boolean mayPickup(@NotNull Player player) {
                    ItemStack slotStack = workerTradeInventory.getItem(this.getSlotIndex());
                    if(slotStack.is(this.getCarried().getItem())){
                        int count = this.getCarried().getCount();
                        int current = slotStack.getCount();
                        int amount = count+current;
                        if(amount > slotStack.getMaxStackSize()){
                            amount = slotStack.getMaxStackSize();
                        }
                        slotStack.setCount(amount);
                    }
                    else{
                        workerTradeInventory.setItem(this.getSlotIndex(), ItemStack.EMPTY);
                    }
                    return false;
                }

                public ItemStack getCarried(){
                    return MerchantOwnerContainer.this.getCarried();
                }

            });
        }
    }

    public void addWorkerTradeSlots() {
        for (int k = 0; k < MerchantEntity.TRADE_SLOT.length; ++k) {
            this.addSlot(new Slot(workerTradeInventory, MerchantEntity.TRADE_SLOT[k], 8 + 18 * 4, 16 + k * 18) {
                @Override
                public boolean mayPlace(@NotNull ItemStack itemStack) {
                    workerTradeInventory.setItem(this.getSlotIndex(), itemStack.copy());
                    return false;
                }

                @Override
                public boolean mayPickup(@NotNull Player player) {
                    ItemStack slotStack = workerTradeInventory.getItem(this.getSlotIndex());
                    if(slotStack.is(this.getCarried().getItem())){
                        int count = this.getCarried().getCount();
                        int current = slotStack.getCount();
                        int amount = count+current;
                        if(amount > slotStack.getMaxStackSize()){
                            amount = slotStack.getMaxStackSize();
                        }
                        slotStack.setCount(amount);
                    }
                    else{
                        workerTradeInventory.setItem(this.getSlotIndex(), ItemStack.EMPTY);
                    }
                    return false;
                }

                public ItemStack getCarried(){
                    return MerchantOwnerContainer.this.getCarried();
                }

            });
        }
    }

    public ItemStack quickMoveStack(Player playerIn, int index) {
        return ItemStack.EMPTY;
    }

    public AbstractWorkerEntity getWorker() {
        return merchant;
    }

    @Override
    public boolean stillValid(Player playerIn) {
        return this.workerTradeInventory.stillValid(playerIn) && this.merchant.isAlive()
                && this.merchant.distanceTo(playerIn) < 8.0F;
    }

    @Override
    public void removed(Player playerIn) {
        super.removed(playerIn);
    }

    public void broadcastChanges() {
        super.broadcastChanges();
        merchant.upgradeTool();
        merchant.upgradeArmor();
    }

}
