package com.talhanation.workers;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import com.talhanation.workers.entities.MerchantEntity;
import com.talhanation.workers.entities.MinerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class CommandEvents {

    public static void setStartPosWorker(UUID player_uuid, AbstractWorkerEntity worker, BlockPos blockpos) {
        LivingEntity owner = worker.getOwner();

        if (worker.isTame() && Objects.equals(worker.getOwnerUUID(), player_uuid)) {
            if (owner != null) {
                if (worker instanceof MinerEntity) {
                    Direction playerDirection = owner.getDirection();
                    MinerEntity miner = (MinerEntity) worker;
                    miner.setMineDirection(playerDirection);
                }
            }
            worker.setStartPos(Optional.of(blockpos));
            worker.setFollow(false);
            worker.setIsWorking(true);
        }
    }

    public static void handleMerchantTrade2(PlayerEntity player, MerchantEntity merchant, int tradeID){
        int[] PRICE_SLOT = new int[]{0,2,4,6};
        int[] TRADE_SLOT = new int[]{1,3,5,7};

        IInventory playerInv = player.inventory;
        IInventory merchantInv = merchant.getInventory();//supply and money
        IInventory merchantTradeInv = merchant.getTradeInventory();//trade interface

        ItemStack moneyItemStack = merchantTradeInv.getItem(PRICE_SLOT[tradeID]);
        Item money = moneyItemStack.getItem();
        int price = moneyItemStack.getCount();
        int playerMoney = 0;

        ItemStack tradeItemStack = merchantTradeInv.getItem(TRADE_SLOT[tradeID]);
        Item tradeItem = tradeItemStack.getItem();
        int tradeCount = tradeItemStack.getCount();

        boolean removeMerchantItems = false;
        boolean givePlayerItems = false;

        for (int i = 0; i < playerInv.getContainerSize(); i++){
            ItemStack itemStack = playerInv.getItem(i);
            Item item = itemStack.getItem();
            int count = itemStack.getCount();

            //check player has the right payment and count
            if(item == money && count >= price){
                itemStack.shrink(price);//decrease money
                //result
                givePlayerItems = true;
                removeMerchantItems = true;
                break;
            }

        }

        if (givePlayerItems){
            ItemStack result = tradeItemStack.copy();
            result.setCount(tradeCount);
            player.inventory.add(result);
        }

        //decrease items from merchant and add money
        if (removeMerchantItems)
            for (int i = 0; i < merchantInv.getContainerSize(); i++) {
                if (i != TRADE_SLOT[tradeID]) {//slot of trade item
                    ItemStack itemStack = merchantInv.getItem(i);
                    Item item = itemStack.getItem();

                    if (item == tradeItem) {
                        itemStack.shrink(tradeCount);

                        for (int j = 0; j < merchantInv.getContainerSize(); j++) {
                            if(j != PRICE_SLOT[tradeID]){ //slot for money item
                                ItemStack result = moneyItemStack.copy();
                                result.setCount(price);
                                merchant.getInventory().addItem(result);
                                break;
                            }
                        }
                        break;
                    }
                }
            }
    }

    public static void handleMerchantTrade(PlayerEntity player, MerchantEntity merchant, int tradeID){
        int[] PRICE_SLOT = new int[]{0,2,4,6};
        int[] TRADE_SLOT = new int[]{1,3,5,7};
        IInventory playerInv = player.inventory;
        IInventory merchantInv = merchant.getInventory();//supply and money
        IInventory merchantTradeInv = merchant.getTradeInventory();//trade interface

        ItemStack moneyItemStack = merchantTradeInv.getItem(PRICE_SLOT[tradeID]);
        Item emerald = moneyItemStack.getItem();//
        int emeraldCount = moneyItemStack.getCount();
        int playerEmeralds = 0;
        int merchantEmeralds = 0;

        ItemStack tradeItemStack = merchantTradeInv.getItem(TRADE_SLOT[tradeID]);
        Item tradeItem = tradeItemStack.getItem();
        int tradeCount = tradeItemStack.getCount();

        //checkPlayerMoney
        for (int i = 0; i < playerInv.getContainerSize(); i++){
            ItemStack itemStackInInv = playerInv.getItem(i);
            if (itemStackInInv == moneyItemStack){
                playerEmeralds = playerEmeralds + itemStackInInv.getCount();

            }
        }
        player.sendMessage(new StringTextComponent("PlayerEmeralds: " + playerEmeralds), player.getUUID());

        //checkMerchantMoney
        for (int i = 0; i < merchantInv.getContainerSize(); i++){
            ItemStack itemStackInInv = merchantInv.getItem(i);
            if (itemStackInInv == moneyItemStack){
                merchantEmeralds = merchantEmeralds + itemStackInInv.getCount();

            }
        }
        player.sendMessage(new StringTextComponent("MerchantEmeralds: " + merchantEmeralds), player.getUUID());
    }
}
