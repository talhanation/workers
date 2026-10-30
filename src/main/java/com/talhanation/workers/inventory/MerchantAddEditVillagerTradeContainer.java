package com.talhanation.workers.inventory;

import com.talhanation.workers.entities.MerchantEntity;
import com.talhanation.workers.init.ModMenuTypes;
import com.talhanation.workers.world.VillagerPriceTable;
import com.talhanation.workers.world.WorkersMerchantTrade;
import de.maxhenkel.corelib.inventory.ContainerBase;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class MerchantAddEditVillagerTradeContainer extends ContainerBase {

    private final MerchantEntity merchantEntity;
    private final SimpleContainer tradeItemContainer;
    private final WorkersMerchantTrade trade;

    public MerchantAddEditVillagerTradeContainer(int id, MerchantEntity worker, Inventory playerInventory, WorkersMerchantTrade trade) {
        super(ModMenuTypes.MERCHANT_VILLAGER_TRADE_CONTAINER_TYPE.get(), id, playerInventory, worker.getInventory());
        this.merchantEntity = worker;
        this.trade = trade;
        this.tradeItemContainer = new SimpleContainer(1);
        this.tradeItemContainer.setItem(0, trade.tradeItem.copyWithCount(1));

        addPlayerInventorySlots();
        setUpSlots();
    }

    public void setUpSlots() {
        addItemSlot(tradeItemContainer, 80, 28);
    }

    public WorkersMerchantTrade getTrade() {
        return trade;
    }

    public ItemStack getTradeItem() {
        return tradeItemContainer.getItem(0);
    }

    @Override
    public int getInvOffset() {
        return 56;
    }

    public MerchantEntity getMerchantEntity() {
        return merchantEntity;
    }

    public void addItemSlot(SimpleContainer slotContainer, int x, int y) {
        this.addSlot(new Slot(slotContainer, 0, x, y) {
            @Override
            public boolean mayPlace(@NotNull ItemStack itemStack) {
                // villagers pay with the currency, it can not be the trade item
                if (itemStack.is(VillagerPriceTable.getCurrency())) return false;
                // the amount comes from the villager offer, so the ghost item is always 1
                slotContainer.setItem(this.getSlotIndex(), itemStack.copyWithCount(1));
                return false;
            }

            @Override
            public boolean mayPickup(@NotNull Player player) {
                ItemStack slotStack = slotContainer.getItem(this.getSlotIndex());
                if (!ItemStack.isSameItemSameTags(slotStack, this.getCarried())) {
                    slotContainer.setItem(this.getSlotIndex(), ItemStack.EMPTY);
                }
                return false;
            }

            public ItemStack getCarried() {
                return MerchantAddEditVillagerTradeContainer.this.getCarried();
            }
        });
    }
}
