package com.talhanation.workers;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import com.talhanation.workers.entities.MerchantEntity;
import com.talhanation.workers.entities.MinerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
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

    public static void handleMerchantTrade(PlayerEntity player, MerchantEntity merchant, int tradeID){
        int[] PRICE_SLOT = new int[]{0,2,4,6};
        int[] TRADE_SLOT = new int[]{1,3,5,7};

        PlayerInventory playerInv = player.inventory;
        Inventory merchantInv = merchant.getInventory();//supply and money
        Inventory merchantTradeInv = merchant.getTradeInventory();//trade interface

        int playerEmeralds = 0;
        int merchantEmeralds = 0;
        int playerTradeItem = 0;
        int merchantTradeItem = 0;

        ItemStack emeraldItemStack = merchantTradeInv.getItem(PRICE_SLOT[tradeID]);
        Item emerald = emeraldItemStack.getItem();//
        int sollPrice = emeraldItemStack.getCount();

        ItemStack tradeItemStack = merchantTradeInv.getItem(TRADE_SLOT[tradeID]);
        Item tradeItem = tradeItemStack.getItem();
        int tradeCount = tradeItemStack.getCount();

        //checkPlayerMoney
        for (int i = 0; i < playerInv.getContainerSize(); i++){
            ItemStack itemStackInSlot = playerInv.getItem(i);
            Item itemInSlot = itemStackInSlot.getItem();
            if (itemInSlot == emerald){
                playerEmeralds = playerEmeralds + itemStackInSlot.getCount();
            }
        }
        //player.sendMessage(new StringTextComponent("PlayerEmeralds: " + playerEmeralds), player.getUUID());

        //checkMerchantMoney
        for (int i = 0; i < merchantInv.getContainerSize(); i++){
            ItemStack itemStackInSlot = merchantInv.getItem(i);
            Item itemInSlot = itemStackInSlot.getItem();
            if (itemInSlot == emerald){
                merchantEmeralds = merchantEmeralds + itemStackInSlot.getCount();
            }
        }
        //player.sendMessage(new StringTextComponent("MerchantEmeralds: " + merchantEmeralds), player.getUUID());


        //checkPlayerTradeGood
        for (int i = 0; i < playerInv.getContainerSize(); i++){
            ItemStack itemStackInSlot = playerInv.getItem(i);
            Item itemInSlot = itemStackInSlot.getItem();
            if (itemInSlot == tradeItem){
                playerTradeItem = playerTradeItem + itemStackInSlot.getCount();
            }
        }
        //player.sendMessage(new StringTextComponent("PlayerTradeItem: " + playerTradeItem), player.getUUID());

        //checkMerchantTradeGood
        for (int i = 0; i < merchantInv.getContainerSize(); i++){
            ItemStack itemStackInSlot = merchantInv.getItem(i);
            Item itemInSlot = itemStackInSlot.getItem();
            if (itemInSlot == tradeItem){
                merchantTradeItem = merchantTradeItem + itemStackInSlot.getCount();
            }
        }
        //player.sendMessage(new StringTextComponent("MerchantTradeItem: " + merchantTradeItem), player.getUUID());

        boolean merchantHasItems = merchantTradeItem >= tradeCount;
        boolean playerCanPay = playerEmeralds >= sollPrice;
        boolean canAddItemToInv = merchantInv.canAddItem(emeraldItemStack);

        if (canAddItemToInv && merchantHasItems && playerCanPay){
            //give player
            //remove merchant ->add left
            //

            merchantTradeItem = merchantTradeItem - tradeCount;
            //playerTradeItem = playerTradeItem + tradeCount;

            //remove merchant tradeItem
            for (int i = 0; i < merchantInv.getContainerSize(); i++){
                ItemStack itemStackInSlot = merchantInv.getItem(i);
                Item itemInSlot = itemStackInSlot.getItem();
                if (itemInSlot == tradeItem){
                    merchantInv.removeItemNoUpdate(i);
                }
            }

            //add tradeGoodLeft to merchantInv
            ItemStack tradeGoodLeft = tradeItemStack.copy();
            for(int i = 0; i < 18 ;i++) {
                if (merchantTradeItem > 64) {
                    tradeGoodLeft.setCount(merchantTradeItem);
                    merchantInv.addItem(tradeGoodLeft);

                    merchantTradeItem = merchantTradeItem - 64;

                    //player.sendMessage(new StringTextComponent("count: " + merchantTradeItem), player.getUUID());

                } else {
                    tradeGoodLeft.setCount(merchantTradeItem);
                    merchantInv.addItem(tradeGoodLeft);
                    break;
                }
            }
            //add tradeItem to playerInventory
            ItemStack tradeGood = tradeItemStack.copy();
            tradeGood.setCount(tradeCount);
            playerInv.add(tradeGood);



            //give player tradeGood
            //remove playerEmeralds ->add left
            //
            playerEmeralds = playerEmeralds - sollPrice;

            //merchantEmeralds = merchantEmeralds + sollPrice;

            //remove playerEmeralds
            for (int i = 0; i < playerInv.getContainerSize(); i++){
                ItemStack itemStackInSlot = playerInv.getItem(i);
                Item itemInSlot = itemStackInSlot.getItem();
                if (itemInSlot == emerald){
                    playerInv.removeItemNoUpdate(i);
                }
            }

            //add emeralds to merchantInventory
            ItemStack emeraldsKar = emeraldItemStack.copy();
            emeraldsKar.setCount(sollPrice);//später merchantEmeralds wenn ich alle s löschen tu
            merchantInv.addItem(emeraldsKar);

            //add leftEmeralds to playerInventory
            ItemStack emeraldsLeft = emeraldItemStack.copy();
            emeraldsLeft.setCount(playerEmeralds);//später merchantEmeralds wenn ich alle s löschen tu
            playerInv.add(emeraldsLeft);


            //debug
            //player.sendMessage(new StringTextComponent("###########################"), player.getUUID());
            //player.sendMessage(new StringTextComponent("Soll Price: " + sollPrice), player.getUUID());
            //player.sendMessage(new StringTextComponent("###########################"), player.getUUID());
            //player.sendMessage(new StringTextComponent("MerchantEmeralds: " + merchantEmeralds), player.getUUID());
            //player.sendMessage(new StringTextComponent("PlayerEmeralds: " + playerEmeralds), player.getUUID());
        }
        else if (!merchantHasItems){
            player.sendMessage(new StringTextComponent("" + merchant.getName().getString() + ": Sorry, im out of Stock."), player.getUUID());

            if (merchant.getOwner() != null)
                merchant.getOwner().sendMessage(new StringTextComponent("" + merchant.getName().getString() + ": Im out of Stock."), player.getUUID());
        }
        else if (!playerCanPay){
            player.sendMessage(new StringTextComponent("" + merchant.getName().getString() + ": Sorry, you need " + sollPrice + "x " + emerald +  "."), player.getUUID());
        }
        else if (!canAddItemToInv){
            player.sendMessage(new StringTextComponent("" + merchant.getName().getString() + ": Sorry, i cant take your Items currently."), player.getUUID());

            if (merchant.getOwner() != null)
                merchant.getOwner().sendMessage(new StringTextComponent("" + merchant.getName().getString() + ": My inventory is full, i cant accept new items!"), player.getUUID());
        }
    }
}
