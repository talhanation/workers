package com.talhanation.workers;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import com.talhanation.workers.entities.MerchantEntity;
import com.talhanation.workers.entities.MinerEntity;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.item.Items;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class CommandEvents {

    public static final TranslatableComponent TEXT_HIRE_COSTS = new TranslatableComponent("chat.recruits.text.hire_costs");

    public static void setStartPosWorker(UUID player_uuid, AbstractWorkerEntity worker, BlockPos blockpos) {
        LivingEntity owner = worker.getOwner();

        if (worker.isTame() && Objects.equals(worker.getOwnerUUID(), player_uuid)) {
            if (owner != null) {
                if (worker instanceof MinerEntity miner) {
                    Direction playerDirection = owner.getDirection();
                    miner.setMineDirection(playerDirection);
                }
            }
            worker.setStartPos(blockpos);
            worker.setFollow(false);
            worker.setIsWorking(true);
        }
    }

    public static void setHomePosWorker(UUID player_uuid, AbstractWorkerEntity worker, BlockPos blockpos) {
        LivingEntity owner = worker.getOwner();

        if (worker.isTame() && worker.getOwnerUUID().equals(player_uuid)) {
            if (owner != null) {
                Main.LOGGER.debug("Event: " + blockpos.toShortString());
                worker.setHomePos(blockpos);
            }
        }
    }

    public static void handleMerchantTrade(Player player, MerchantEntity merchant, int tradeID){
        int[] PRICE_SLOT = new int[]{0,2,4,6};
        int[] TRADE_SLOT = new int[]{1,3,5,7};

        Inventory playerInv = player.getInventory();
        SimpleContainer merchantInv = merchant.getInventory();//supply and money
        SimpleContainer merchantTradeInv = merchant.getTradeInventory();//trade interface

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
            //int maxSize = tradeGoodLeft.getMaxStackSize();
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
            player.sendMessage(new TextComponent("" + merchant.getName().getString() + ": Sorry, im out of Stock."), player.getUUID());

            if (merchant.getOwner() != null)
                merchant.getOwner().sendMessage(new TextComponent("" + merchant.getName().getString() + ": Im out of Stock."), player.getUUID());
        }
        else if (!playerCanPay){
            player.sendMessage(new TextComponent("" + merchant.getName().getString() + ": Sorry, you need " + sollPrice + "x " + emerald +  "."), player.getUUID());
        }
        else if (!canAddItemToInv){
            player.sendMessage(new TextComponent("" + merchant.getName().getString() + ": Sorry, i cant take your Items currently."), player.getUUID());

            if (merchant.getOwner() != null)
                merchant.getOwner().sendMessage(new TextComponent("" + merchant.getName().getString() + ": My inventory is full, i cant accept new items!"), player.getUUID());
        }
    }

    public static void handleRecruiting(Player player, AbstractWorkerEntity workerEntity){
        String name = workerEntity.getName().getString() + ": ";
        String hire_costs = TEXT_HIRE_COSTS.getString();
        int costs = workerEntity.workerCosts();

        String recruit_info = String.format(hire_costs, costs);
        Inventory playerInv = player.getInventory();

        int playerEmeralds = 0;

        ItemStack emeraldItemStack = Items.EMERALD.getDefaultInstance();
        Item emerald = emeraldItemStack.getItem();//
        int sollPrice = workerEntity.workerCosts();


        //checkPlayerMoney
        for (int i = 0; i < playerInv.getContainerSize(); i++){
            ItemStack itemStackInSlot = playerInv.getItem(i);
            Item itemInSlot = itemStackInSlot.getItem();
            if (itemInSlot == emerald){
                playerEmeralds = playerEmeralds + itemStackInSlot.getCount();
            }
        }

        boolean playerCanPay = playerEmeralds >= sollPrice;

        if (playerCanPay){
            if(workerEntity.hire(player)) {

                //give player tradeGood
                //remove playerEmeralds ->add left
                //
                playerEmeralds = playerEmeralds - sollPrice;

                //merchantEmeralds = merchantEmeralds + sollPrice;

                //remove playerEmeralds
                for (int i = 0; i < playerInv.getContainerSize(); i++) {
                    ItemStack itemStackInSlot = playerInv.getItem(i);
                    Item itemInSlot = itemStackInSlot.getItem();
                    if (itemInSlot == emerald) {
                        playerInv.removeItemNoUpdate(i);
                    }
                }

                //add leftEmeralds to playerInventory
                ItemStack emeraldsLeft = emeraldItemStack.copy();
                emeraldsLeft.setCount(playerEmeralds);
                playerInv.add(emeraldsLeft);
            }
        }
        else
            player.sendMessage(new TextComponent(name + recruit_info), player.getUUID());
    }

}
