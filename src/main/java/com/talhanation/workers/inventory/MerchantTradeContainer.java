package com.talhanation.workers.inventory;

import com.talhanation.workers.entities.MerchantEntity;
import com.talhanation.workers.init.ModMenuTypes;
import de.maxhenkel.corelib.inventory.ContainerBase;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;

public class MerchantTradeContainer extends ContainerBase {


    private final MerchantEntity merchant;

    public MerchantTradeContainer(int id, MerchantEntity merchant, Container playerInventory) {
        super(ModMenuTypes.MERCHANT_TRADE_CONTAINER_TYPE.get(), id, playerInventory, merchant.getInventory());
        this.merchant = merchant;
        this.addPlayerInventorySlots();
    }

    public int getInvYOffset() {
        return 30;
    }

    public int getInvXOffset() {
        return 79;
    }

    public MerchantEntity getMerchantEntity() {
        return merchant;
    }

    protected void addPlayerInventorySlots() {
        if (this.playerInventory != null) {
            int k;
            for(k = 0; k < 3; ++k) {
                for(int j = 0; j < 9; ++j) {
                    this.addSlot(new Slot(this.playerInventory, j + k * 9 + 9,this.getInvXOffset() + 8 + j * 18, 84 + k * 18 + this.getInvYOffset()));
                }
            }

            for(k = 0; k < 9; ++k) {
                this.addSlot(new Slot(this.playerInventory, k, this.getInvXOffset() + 8 + k * 18, 142 + this.getInvYOffset()));
            }
        }
    }


    @Override
    public void removed(Player player) {
        merchant.setTrading(false);
        super.removed(player);
    }
}
