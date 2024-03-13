package com.talhanation.workers;

import com.talhanation.workers.config.WorkersModConfig;
import com.talhanation.workers.entities.*;
import com.talhanation.workers.inventory.CommandMenu;
import com.talhanation.workers.network.MessageOpenCommandScreen;
import com.talhanation.workers.network.MessageToClientUpdateCommandScreen;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.talhanation.workers.Translatable.*;

public class CommandEvents {
	public static void setStartPosWorker(UUID player_uuid, AbstractWorkerEntity worker, BlockPos blockpos) {
        LivingEntity owner = worker.getOwner();

        if (worker.isTame() && Objects.equals(worker.getOwnerUUID(), player_uuid)) {
            if (owner != null) {
                if (worker instanceof MinerEntity miner) {
                    Direction playerDirection = owner.getDirection();
                    miner.setMineDirection(playerDirection);
                }

                if (worker instanceof FishermanEntity fisherman) {
                    Direction playerDirection = owner.getDirection();
                    fisherman.setFishingDirection(playerDirection);
                }
            }
            worker.setStartPos(blockpos);

            if( !(worker instanceof  MerchantEntity)){
                worker.setFollow(false);
                worker.setIsWorking(true);
            }


            if (worker instanceof IBoatController sailor) sailor.setSailPos(blockpos);
        }
    }

    public static void setChestPosWorker(UUID player_uuid, AbstractWorkerEntity worker, BlockPos blockpos) {
        LivingEntity owner = worker.getOwner();
        UUID expectedOwnerUuid = worker.getOwnerUUID();
        if (!worker.isTame() || expectedOwnerUuid == null || owner == null) {
            return;
        }
        if (expectedOwnerUuid.equals(player_uuid)) {
            BlockState selectedBlock = worker.getCommandSenderWorld().getBlockState(blockpos);
            if (selectedBlock.is(Blocks.CHEST) || selectedBlock.is(Blocks.BARREL)) {
                worker.setChestPos(blockpos);
                worker.setNeedsChest(false);
                worker.tellPlayer(owner, TEXT_CHEST);
            } else {
                worker.tellPlayer(owner, TEXT_CHEST_ERROR);
            }
        }
    }

    public static void setBedPosWorker(UUID player_uuid, AbstractWorkerEntity worker, BlockPos blockpos) {
        LivingEntity owner = worker.getOwner();
        UUID expectedOwnerUuid = worker.getOwnerUUID();
        if (!worker.isTame() || expectedOwnerUuid == null || owner == null) {
            return;
        }
        if (expectedOwnerUuid.equals(player_uuid)) {
            BlockState selectedBlock = worker.getCommandSenderWorld().getBlockState(blockpos);
            if (selectedBlock.isBed(worker.getCommandSenderWorld(), blockpos, owner)) {
                BlockPos bedHead;
                if (selectedBlock.getValue(BlockStateProperties.BED_PART) == BedPart.HEAD) {
                    bedHead = blockpos;
                } else {
                    bedHead = blockpos.relative(selectedBlock.getValue(BlockStateProperties.HORIZONTAL_FACING));
                }
                worker.setBedPos(bedHead);
                worker.setNeedsBed(false);
                worker.tellPlayer(owner, TEXT_BED);
            } else {
                worker.tellPlayer(owner, TEXT_BED_ERROR);
            }
        }
    }

    public static void setHomePosWorker(UUID player_uuid, AbstractWorkerEntity worker, BlockPos blockpos) {
        LivingEntity owner = worker.getOwner();
        UUID expectedOwnerUuid = worker.getOwnerUUID();
        if (!worker.isTame() || expectedOwnerUuid == null || owner == null) {
            return;
        }
        if (expectedOwnerUuid.equals(player_uuid)) {
            worker.setHomePos(blockpos);
            worker.setNeedsHome(false);
            worker.tellPlayer(owner, TEXT_HOME);

            setChestPosWorker(worker, blockpos, owner);
            setBedPosWorker(worker, blockpos, owner);
        }
    }

    public static void setChestPosWorker(AbstractWorkerEntity worker, BlockPos homePos, LivingEntity owner) {
        int range = 8;
        
        for (int x = -range; x < range; x++) {
            for (int y = -range; y < range; y++) {
                for (int z = -range; z < range; z++) {
                    BlockPos chestPos = homePos.offset(x, y, z);
                    BlockState block = worker.getCommandSenderWorld().getBlockState(chestPos);
                    if (block == null) continue;
                    if (block.is(Blocks.CHEST) || block.is(Blocks.BARREL)) {
                        worker.setChestPos(chestPos);
                        worker.setNeedsChest(false);
                        return;
                    }
                }
            }
        }
        worker.setNeedsChest(true);
        worker.tellPlayer(owner, NEED_CHEST);
    }

    public static void setBedPosWorker(AbstractWorkerEntity worker, BlockPos homePos, LivingEntity owner) {
        int range = 8;
        
        for (int x = -range; x < range; x++) {
            for (int y = -range; y < range; y++) {
                for (int z = -range; z < range; z++) {
                    BlockPos bedPos = homePos.offset(x, y, z);
                    BlockState block = worker.getCommandSenderWorld().getBlockState(bedPos);
                    if (block == null) continue;
                    if (
                        block.isBed(worker.getCommandSenderWorld(), bedPos, worker) &&
                        block.getValue(BlockStateProperties.BED_PART) == BedPart.HEAD
                    ) {
                        worker.setBedPos(bedPos);
                        worker.setNeedsBed(false);
                        return;
                    }
                }
            }
        }
        worker.setNeedsBed(true);
        worker.tellPlayer(owner, NEED_BED);
    }
    public static void handleMerchantTrade(Player player, MerchantEntity merchant, int tradeID) {
        if(merchant.isCreative()){
            handleCreativeMerchantTrade(player, merchant, tradeID);
        }
        else
            handleSurvivalMerchantTrade(player, merchant, tradeID);
    }
    public static void handleSurvivalMerchantTrade(Player player, MerchantEntity merchant, int tradeID) {
        int[] PRICE_SLOT = new int[] { 0, 2, 4, 6 };
        int[] TRADE_SLOT = new int[] { 1, 3, 5, 7 };

        Inventory playerInv = player.getInventory();
        SimpleContainer merchantInv = merchant.getInventory();// supply and money
        SimpleContainer merchantTradeInv = merchant.getTradeInventory();// trade interface

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

        // checkPlayerMoney
        for (int i = 0; i < playerInv.getContainerSize(); i++) {
            ItemStack itemStackInSlot = playerInv.getItem(i);
            Item itemInSlot = itemStackInSlot.getItem();
            if (itemInSlot == emerald) {
                playerEmeralds = playerEmeralds + itemStackInSlot.getCount();
            }
        }
        // player.sendMessage(new StringTextComponent("PlayerEmeralds: " +
        // playerEmeralds), player.getUUID());

        // checkMerchantMoney
        for (int i = 0; i < merchantInv.getContainerSize(); i++) {
            ItemStack itemStackInSlot = merchantInv.getItem(i);
            Item itemInSlot = itemStackInSlot.getItem();
            if (itemInSlot == emerald) {
                merchantEmeralds = merchantEmeralds + itemStackInSlot.getCount();
            }
        }
        // player.sendMessage(new StringTextComponent("MerchantEmeralds: " +
        // merchantEmeralds), player.getUUID());

        // checkPlayerTradeGood
        for (int i = 0; i < playerInv.getContainerSize(); i++) {
            ItemStack itemStackInSlot = playerInv.getItem(i);
            Item itemInSlot = itemStackInSlot.getItem();
            if (itemInSlot == tradeItem) {
                playerTradeItem = playerTradeItem + itemStackInSlot.getCount();
            }
        }
        // player.sendMessage(new StringTextComponent("PlayerTradeItem: " +
        // playerTradeItem), player.getUUID());

        // checkMerchantTradeGood
        for (int i = 0; i < merchantInv.getContainerSize(); i++) {
            ItemStack itemStackInSlot = merchantInv.getItem(i);
            Item itemInSlot = itemStackInSlot.getItem();
            if (itemInSlot == tradeItem) {
                merchantTradeItem = merchantTradeItem + itemStackInSlot.getCount();
            }
        }
        // player.sendMessage(new StringTextComponent("MerchantTradeItem: " +
        // merchantTradeItem), player.getUUID());

        boolean merchantHasItems = merchantTradeItem >= tradeCount;
        boolean playerCanPay = playerEmeralds >= sollPrice;
        boolean canAddItemToInv = merchantInv.canAddItem(emeraldItemStack);
        boolean merchantWantsTrade = merchant.getTradeLimit(tradeID) == -1 ||  merchant.getCurrentTrades(tradeID) < merchant.getTradeLimit(tradeID);

        if(merchantWantsTrade){
            if (canAddItemToInv && merchantHasItems && playerCanPay) {
                if(playerInv.getFreeSlot() != -1){
                    // give player
                    // remove merchant ->add left
                    //

                    merchantTradeItem = merchantTradeItem - tradeCount;
                    // playerTradeItem = playerTradeItem + tradeCount;

                    // remove merchant tradeItem
                    for (int i = 0; i < merchantInv.getContainerSize(); i++) {
                        ItemStack itemStackInSlot = merchantInv.getItem(i);
                        Item itemInSlot = itemStackInSlot.getItem();
                        if (itemInSlot == tradeItem) {
                            merchantInv.removeItemNoUpdate(i);
                        }
                    }

                    // add tradeGoodLeft to merchantInv
                    ItemStack tradeGoodLeft = tradeItemStack.copy();
                    // int maxSize = tradeGoodLeft.getMaxStackSize();
                    for (int i = 0; i < 18; i++) {
                        if (merchantTradeItem > 64) {
                            tradeGoodLeft.setCount(merchantTradeItem);
                            merchantInv.addItem(tradeGoodLeft);

                            merchantTradeItem = merchantTradeItem - 64;

                            // player.sendMessage(new StringTextComponent("count: " + merchantTradeItem),
                            // player.getUUID());

                        } else {
                            tradeGoodLeft.setCount(merchantTradeItem);
                            merchantInv.addItem(tradeGoodLeft);
                            break;
                        }
                    }
                    // add tradeItem to playerInventory
                    ItemStack tradeGood = tradeItemStack.copy();
                    tradeGood.setCount(tradeCount);
                    playerInv.add(tradeGood);

                    // give player tradeGood
                    // remove playerEmeralds ->add left
                    //
                    playerEmeralds = playerEmeralds - sollPrice;

                    // merchantEmeralds = merchantEmeralds + sollPrice;

                    // remove playerEmeralds
                    for (int i = 0; i < playerInv.getContainerSize(); i++) {
                        ItemStack itemStackInSlot = playerInv.getItem(i);
                        Item itemInSlot = itemStackInSlot.getItem();
                        if (itemInSlot == emerald) {
                            playerInv.removeItemNoUpdate(i);
                        }
                    }

                    // add emeralds to merchantInventory
                    ItemStack emeraldsKar = emeraldItemStack.copy();
                    emeraldsKar.setCount(sollPrice);// später merchantEmeralds wenn ich alle s löschen tu
                    merchantInv.addItem(emeraldsKar);

                    // add leftEmeralds to playerInventory
                    ItemStack emeraldsLeft = emeraldItemStack.copy();
                    emeraldsLeft.setCount(playerEmeralds);// später merchantEmeralds wenn ich alle s löschen tu
                    playerInv.add(emeraldsLeft);
                    merchant.setCurrentTrades(tradeID, merchant.getCurrentTrades(tradeID) + 1);

                    // debug
                    // player.sendMessage(new StringTextComponent("###########################"),
                    // player.getUUID());
                    // player.sendMessage(new StringTextComponent("Soll Price: " + sollPrice),
                    // player.getUUID());
                    // player.sendMessage(new StringTextComponent("###########################"),
                    // player.getUUID());
                    // player.sendMessage(new StringTextComponent("MerchantEmeralds: " +
                    // merchantEmeralds), player.getUUID());
                    // player.sendMessage(new StringTextComponent("PlayerEmeralds: " +
                    // playerEmeralds), player.getUUID());
                }
                else
                    merchant.tellPlayer(player, TEXT_NO_SPACE_FOR_TRADE);
            }
            else {
                LivingEntity owner = merchant.getOwner();
                if (!merchantHasItems) {
                    merchant.tellPlayer(player, TEXT_OUT_OF_STOCK);
                    if (owner != null) {
                        merchant.tellPlayer(owner, TEXT_OUT_OF_STOCK_OWNER);
                    }
                } else if (!playerCanPay) {
                    merchant.tellPlayer(player, TEXT_NEED(sollPrice, emerald));
                } else if (!canAddItemToInv) {
                    merchant.tellPlayer(player, TEXT_INV_FULL);
                    if (owner != null) {
                        merchant.tellPlayer(owner, TEXT_INV_FULL_OWNER);
                    }
                }
            }
        }
        else {
            merchant.tellPlayer(player, TEXT_NO_NEED(tradeItem));
        }

    }

    public static void handleRecruiting(Player player, AbstractWorkerEntity worker){
        int sollPrice = worker.workerCosts();
        Inventory playerInv = player.getInventory();
        int playerEmeralds = 0;

        String str = WorkersModConfig.WorkersCurrency.get();
        Optional<Holder<Item>> holder = ForgeRegistries.ITEMS.getHolder(ResourceLocation.tryParse(str));

        ItemStack currencyItemStack = holder.map(itemHolder -> itemHolder.value().getDefaultInstance()).orElseGet(Items.EMERALD::getDefaultInstance);

        Item currency = currencyItemStack.getItem();

        //checkPlayerMoney
        for (int i = 0; i < playerInv.getContainerSize(); i++){
            ItemStack itemStackInSlot = playerInv.getItem(i);
            Item itemInSlot = itemStackInSlot.getItem();
            if (itemInSlot.equals(currency)){
                playerEmeralds = playerEmeralds + itemStackInSlot.getCount();
            }
        }

        boolean playerCanPay = playerEmeralds >= sollPrice;

        if (playerCanPay){
            if(worker.hire(player)) {
                //give player tradeGood
                //remove playerEmeralds ->add left
                //
                playerEmeralds = playerEmeralds - sollPrice;

                //merchantEmeralds = merchantEmeralds + sollPrice;

                //remove playerEmeralds
                for (int i = 0; i < playerInv.getContainerSize(); i++) {
                    ItemStack itemStackInSlot = playerInv.getItem(i);
                    Item itemInSlot = itemStackInSlot.getItem();
                    if (itemInSlot.equals(currency)) {
                        playerInv.removeItemNoUpdate(i);
                    }
                }

                //add leftEmeralds to playerInventory
                ItemStack emeraldsLeft = currencyItemStack.copy();
                emeraldsLeft.setCount(playerEmeralds);
                playerInv.add(emeraldsLeft);
            }
        }
        else
             worker.tellPlayer(player, TEXT_HIRE_COSTS(sollPrice, currency.getDescription().getString()));
    }


     public static void handleCreativeMerchantTrade(Player player, MerchantEntity merchant, int tradeID) {
         int[] PRICE_SLOT = new int[]{0, 2, 4, 6};
         int[] TRADE_SLOT = new int[]{1, 3, 5, 7};

         Inventory playerInv = player.getInventory();
         SimpleContainer merchantTradeInv = merchant.getTradeInventory();// trade interface

         int playerEmeralds = 0;
         int playerTradeItem = 0;

         ItemStack emeraldItemStack = merchantTradeInv.getItem(PRICE_SLOT[tradeID]);
         Item emerald = emeraldItemStack.getItem();//
         int sollPrice = emeraldItemStack.getCount();

         ItemStack tradeItemStack = merchantTradeInv.getItem(TRADE_SLOT[tradeID]);
         Item tradeItem = tradeItemStack.getItem();
         int tradeCount = tradeItemStack.getCount();

         // checkPlayerMoney
         for (int i = 0; i < playerInv.getContainerSize(); i++) {
             ItemStack itemStackInSlot = playerInv.getItem(i);
             Item itemInSlot = itemStackInSlot.getItem();
             if (itemInSlot == emerald) {
                 playerEmeralds = playerEmeralds + itemStackInSlot.getCount();
             }
         }

         // checkPlayerTradeGood
         for (int i = 0; i < playerInv.getContainerSize(); i++) {
             ItemStack itemStackInSlot = playerInv.getItem(i);
             Item itemInSlot = itemStackInSlot.getItem();
             if (itemInSlot == tradeItem) {
                 playerTradeItem = playerTradeItem + itemStackInSlot.getCount();
             }
         }

         boolean playerCanPay = playerEmeralds >= sollPrice;
         boolean merchantWantsTrade = merchant.getTradeLimit(tradeID) == -1 || merchant.getCurrentTrades(tradeID) < merchant.getTradeLimit(tradeID);

         if(merchantWantsTrade) {
             if (playerCanPay) {
                 if (playerInv.getFreeSlot() != -1) {// give player
                     // remove merchant ->add left
                     //

                     // playerTradeItem = playerTradeItem + tradeCount;

                     // add tradeItem to playerInventory
                     ItemStack tradeGood = tradeItemStack.copy();
                     tradeGood.setCount(tradeCount);
                     playerInv.add(tradeGood);

                     // give player tradeGood
                     // remove playerEmeralds ->add left
                     playerEmeralds = playerEmeralds - sollPrice;

                     // remove playerEmeralds
                     for (int i = 0; i < playerInv.getContainerSize(); i++) {
                         ItemStack itemStackInSlot = playerInv.getItem(i);
                         Item itemInSlot = itemStackInSlot.getItem();
                         if (itemInSlot == emerald) {
                             playerInv.removeItemNoUpdate(i);
                         }
                     }

                     // add leftEmeralds to playerInventory
                     ItemStack emeraldsLeft = emeraldItemStack.copy();
                     emeraldsLeft.setCount(playerEmeralds);// später merchantEmeralds wenn ich alle s löschen tu
                     playerInv.add(emeraldsLeft);
                     merchant.setCurrentTrades(tradeID, merchant.getCurrentTrades(tradeID) + 1);
                 }
                 else
                     merchant.tellPlayer(player, TEXT_NO_SPACE_FOR_TRADE);
             }
             else
                 merchant.tellPlayer(player, TEXT_NEED(sollPrice, emerald));
         }
         else
             merchant.tellPlayer(player, TEXT_NO_NEED(tradeItem));

     }

    public static void openCommandScreen(Player player) {
        if (player instanceof ServerPlayer) {
            NetworkHooks.openScreen((ServerPlayer) player, new MenuProvider() {

                @Override
                public @NotNull Component getDisplayName() {
                    return Component.literal("command_screen");
                }
                @Override
                public @NotNull AbstractContainerMenu createMenu(int i, @NotNull Inventory playerInventory, @NotNull Player playerEntity) {
                    return new CommandMenu(i, playerEntity);
                }
            }, packetBuffer -> {packetBuffer.writeUUID(player.getUUID());});
        } else {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageOpenCommandScreen(player));
        }
    }

    public static void updateCommandScreen(ServerPlayer player, ServerLevel level){
        //TODO: Blockstate
        List<AbstractWorkerEntity> list = Objects.requireNonNull(player.getCommandSenderWorld().getEntitiesOfClass(AbstractWorkerEntity.class, player.getBoundingBox().inflate(64D)));
        List<UUID> workers = new ArrayList<>();
        List<String> names = new ArrayList<>();

        list.sort(Comparator.comparing(AbstractWorkerEntity::getDistanceToOwner));

        for(AbstractWorkerEntity worker : list) {
            if (Objects.equals(worker.getOwnerUUID(), player.getUUID())){
                workers.add(worker.getUUID());
                String name = worker.getName().getString()  + " / " + worker.getProfessionName();
                names.add(name);
            }
        }

        Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(()-> player), new MessageToClientUpdateCommandScreen(workers, names));
    }
}
