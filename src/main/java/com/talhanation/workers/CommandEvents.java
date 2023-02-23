package com.talhanation.workers;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import com.talhanation.workers.entities.MerchantEntity;
import com.talhanation.workers.entities.MinerEntity;
import com.talhanation.workers.inventory.CommandMenu;
import com.talhanation.workers.network.MessageOpenCommandScreen;
import com.talhanation.workers.network.MessageToClientUpdateCommandScreen;
import net.minecraft.network.chat.Component;
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
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

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
            }
            worker.setStartPos(blockpos);
            worker.setFollow(false);
            worker.setIsWorking(true);
        }
    }

    public static void setChestPosWorker(UUID player_uuid, AbstractWorkerEntity worker, BlockPos blockpos) {
        LivingEntity owner = worker.getOwner();
        UUID expectedOwnerUuid = worker.getOwnerUUID();
        if (!worker.isTame() || expectedOwnerUuid == null || owner == null) {
            return;
        }
        if (expectedOwnerUuid.equals(player_uuid)) {
            BlockState selectedBlock = worker.level.getBlockState(blockpos);
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
            BlockState selectedBlock = worker.level.getBlockState(blockpos);
            if (selectedBlock.isBed(worker.level, blockpos, owner)) {
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
                    BlockState block = worker.level.getBlockState(chestPos);
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
                    BlockState block = worker.level.getBlockState(bedPos);
                    if (block == null) continue;
                    if (
                        block.isBed(worker.level, bedPos, worker) && 
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

        if (canAddItemToInv && merchantHasItems && playerCanPay) {
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
        } else {
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

    public static void handleRecruiting(Player player, AbstractWorkerEntity workerEntity) {
        int costs = workerEntity.workerCosts();

        Inventory playerInv = player.getInventory();

        int playerEmeralds = 0;

        ItemStack emeraldItemStack = Items.EMERALD.getDefaultInstance();
        Item emerald = emeraldItemStack.getItem();//
        int sollPrice = workerEntity.workerCosts();

        // checkPlayerMoney
        for (int i = 0; i < playerInv.getContainerSize(); i++) {
            ItemStack itemStackInSlot = playerInv.getItem(i);
            Item itemInSlot = itemStackInSlot.getItem();
            if (itemInSlot == emerald) {
                playerEmeralds = playerEmeralds + itemStackInSlot.getCount();
            }
        }

        boolean playerCanPay = playerEmeralds >= sollPrice;

        if (playerCanPay) {
            if (workerEntity.hire(player)) {

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

                // add leftEmeralds to playerInventory
                ItemStack emeraldsLeft = emeraldItemStack.copy();
                emeraldsLeft.setCount(playerEmeralds);
                playerInv.add(emeraldsLeft);
            }
        } else {
            workerEntity.tellPlayer(player, TEXT_HIRE_COSTS(costs));
        }
    }

    // TODO: Remove home button.
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
        List<AbstractWorkerEntity> list = Objects.requireNonNull(player.level.getEntitiesOfClass(AbstractWorkerEntity.class, player.getBoundingBox().inflate(16D)));
        List<UUID> workers = new ArrayList<>();
        List<String> names = new ArrayList<>();

        for(AbstractWorkerEntity worker : list) {
            if (Objects.equals(worker.getOwnerUUID(), player.getUUID())){
                workers.add(worker.getUUID());
                names.add(worker.getName().getString());
            }
        }
        Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(()-> player), new MessageToClientUpdateCommandScreen(workers, names));
    }
}
