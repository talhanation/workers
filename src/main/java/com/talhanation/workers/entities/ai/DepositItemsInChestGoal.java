package com.talhanation.workers.entities.ai;

import com.talhanation.workers.Translatable;
import com.talhanation.workers.entities.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.Optional;

public class DepositItemsInChestGoal extends Goal {
    private final AbstractWorkerEntity worker;
    public BlockPos chestPos;
    public Container container;
    public boolean messageCantFindChest;
    public boolean messageNoFood;
    public boolean messageChestFull;
    public int timer = 0;
    public boolean setTimer = false;
    public boolean noSpaceInvMessage;
    public boolean noToolMessage;
    public boolean messageNoChest;
    public DepositItemsInChestGoal(AbstractWorkerEntity worker) {
        this.worker = worker;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return this.worker.getStatus() == AbstractWorkerEntity.Status.DEPOSIT;
    }

    public boolean canContinueToUse() {
        return canUse() && worker.getStatus() != AbstractWorkerEntity.Status.FOLLOW;
    }
    @Override
    public boolean isInterruptable() {
        return false;
    }

    @Override
    public void start() {
        super.start();
        messageNoFood = true;
        noSpaceInvMessage = true;
        noToolMessage = true;
        messageChestFull = true;
        messageNoChest = true;

        this.chestPos = worker.getChestPos();
        if (chestPos != null) {
            container = getContainer(chestPos);
            if(container == null){
                if(messageCantFindChest && worker.getOwner() != null){
                    this.worker.tellPlayer(worker.getOwner(), Translatable.TEXT_CANT_FIND_CHEST);
                    this.messageCantFindChest = false;
                }
                this.worker.clearChestPos();
            }
        }
    }

    @Override
    public void stop() {
        super.stop();
        if(container != null) this.interactChest(container,false);
        timer = 0;
        setTimer = false;
        this.worker.resetFarmedItems();

        if(this.worker.getStatus() != AbstractWorkerEntity.Status.FOLLOW) this.worker.setStatus(AbstractWorkerEntity.Status.IDLE);
    }
    @Override
    public void tick() {
        this.chestPos = worker.getChestPos();

        if (chestPos != null) {
			
            this.worker.getNavigation().moveTo(chestPos.getX(), chestPos.getY(), chestPos.getZ(), 1.1D);

            if (chestPos.closerThan(worker.getOnPos(), 2.5)) {
                this.worker.getNavigation().stop();
                this.worker.getLookControl().setLookAt(chestPos.getX(), chestPos.getY() + 1, chestPos.getZ(), 10.0F, (float) this.worker.getMaxHeadXRot());

                if(container != null){
                    if(!setTimer){
                        this.interactChest(container, true);
                        this.depositItems(container);

                        if(!canAddItemsInInventory()){
                            if(worker.getOwner() != null && noSpaceInvMessage){
                                worker.tellPlayer(worker.getOwner(), Translatable.TEXT_NO_SPACE_INV);
                                noSpaceInvMessage = false;
                            }

                        }

                        this.reequipMainTool();
                        this.reequipSecondTool();

                        if(this.worker instanceof MinerEntity){
                            if(!hasEnoughOfItem(Items.TORCH, 16)) this.getItemFromChest(Items.TORCH);
                        }
                        //TODO: ADD fisherman takes boat
                        if(this.worker instanceof FarmerEntity){
                            if(!hasEnoughOfItem(Items.BONE_MEAL, 32)) this.getItemFromChest(Items.BONE_MEAL);
                        }

                        if(this.worker instanceof ChickenFarmerEntity chickenFarmer){
                            if(chickenFarmer.getUseEggs()) this.getItemFromChest(Items.EGG);
                            if(!hasEnoughOfItem(Items.WHEAT_SEEDS, 32)) this.getItemFromChest(Items.WHEAT_SEEDS);
                            if(!hasEnoughOfItem(Items.PUMPKIN_SEEDS, 32)) this.getItemFromChest(Items.PUMPKIN_SEEDS);
                            if(!hasEnoughOfItem(Items.MELON_SEEDS, 32)) this.getItemFromChest(Items.MELON_SEEDS);
                            if(!hasEnoughOfItem(Items.BEETROOT_SEEDS, 32)) this.getItemFromChest(Items.BEETROOT_SEEDS);
                        }

                        if(this.worker instanceof ShepherdEntity || this.worker instanceof CattleFarmerEntity){
                            if(!hasEnoughOfItem(Items.WHEAT, 32)) this.getItemFromChest(Items.WHEAT);
                        }

                        if(this.worker instanceof SwineherdEntity){
                            if(!hasEnoughOfItem(Items.CARROT, 32)) this.getItemFromChest(Items.CARROT);
                        }

                        if(this.worker instanceof CattleFarmerEntity){
                            if(!hasEnoughOfItem(Items.MILK_BUCKET, 3)) this.getItemFromChest(Items.BUCKET);
                        }

                        if(this.worker.needsToGetFood() && !this.hasFoodInInv()){
                            if (isFoodInChest(container)) {
                                for (int i = 0; i < 3; i++) {
                                    ItemStack foodItem = this.getFoodFromInv(container);
                                    ItemStack food;

                                    if (foodItem != null){
                                        food = foodItem.copy();
                                        food.setCount(1);
                                        worker.getInventory().addItem(food);
                                        foodItem.shrink(1);
                                    }
                                }
                            }
                            else {
                                if(worker.getOwner() != null && messageNoFood){
                                    worker.tellPlayer(worker.getOwner(), Translatable.TEXT_NO_FOOD);
                                    messageNoFood = false;
                                }
                            }
                        }

                        if(((!worker.hasMainToolInInv() || worker.needsMainTool) && worker.hasAMainTool()) || ((!worker.hasSecondToolInInv() || worker.needsSecondTool) && worker.hasASecondTool())){
                            if(worker.getOwner() != null && noToolMessage) {
                                worker.tellPlayer(worker.getOwner(), Translatable.TEXT_OUT_OF_TOOLS());
                                noToolMessage = false;
                            }
                        }

                        timer = 30;
                        setTimer = true;
                    }
                }
                else {
                    container = getContainer(chestPos);

                    if(container == null){
                        if(messageCantFindChest && worker.getOwner() != null){
                            this.worker.tellPlayer(worker.getOwner(), Translatable.TEXT_CANT_FIND_CHEST);
                            this.messageCantFindChest = false;
                        }
                        this.worker.clearChestPos();
                    }
                }

            }
        }
        else {
            if(messageNoChest && worker.getOwner() != null){
                this.worker.tellPlayer(worker.getOwner(), Translatable.NEED_CHEST);
                messageNoChest = false;
            }
        }

        if(setTimer){
            if(timer > 0) timer--;
            if(timer == 0) stop();
        }
    }

    private Container getContainer(BlockPos chestPos) {
        BlockEntity entity = worker.getCommandSenderWorld().getBlockEntity(chestPos);
        BlockState blockState = worker.getCommandSenderWorld().getBlockState(chestPos);
        if (blockState.getBlock() instanceof ChestBlock chestBlock) {
            return ChestBlock.getContainer(chestBlock, blockState, worker.getCommandSenderWorld(), chestPos, false);
        } else if (entity instanceof Container containerEntity) {
            return containerEntity;
        }
        else {
            messageCantFindChest = true;
        }
        return null;
    }

    @Nullable
    private ItemStack getFoodFromInv(Container inv){
        ItemStack itemStack = null;
        for(int i = 0; i < inv.getContainerSize(); i++){
            ItemStack itemStack2 = inv.getItem(i);
            if(itemStack2.isEdible() && itemStack2.getFoodProperties(this.worker).getNutrition() > 3){
                itemStack = inv.getItem(i);
                break;
            }
        }
        return itemStack;
    }

    private boolean canAddItemsInInventory(){
        for(int i = 0; i < worker.getInventory().getContainerSize(); i++){
            if(worker.getInventory().getItem(i).isEmpty())
                return true;
        }
        return false;
    }


    public void interactChest(Container container, boolean open) {
        if(container instanceof CompoundContainer || container instanceof ChestBlockEntity){
            BlockState state = this.worker.getCommandSenderWorld().getBlockState(this.chestPos);
            Block block = state.getBlock();
            boolean isOpened = false;
            CompoundTag compoundTag = new CompoundTag();
            if(worker.getCommandSenderWorld().getBlockEntity(chestPos) instanceof ChestBlockEntity chestBlockEntity){
                compoundTag = chestBlockEntity.getPersistentData();
                if(compoundTag.contains("isOpened"))
                    isOpened = compoundTag.getBoolean("isOpened");
                else
                    compoundTag.putBoolean("isOpened", false);
            }

            if (open) {
                if(!isOpened){
                    this.worker.getCommandSenderWorld().blockEvent(this.chestPos, block, 1, 1);
                    this.worker.getCommandSenderWorld().playSound(null, chestPos, SoundEvents.CHEST_OPEN, worker.getSoundSource(), 0.7F, 0.8F + 0.4F * worker.getRandom().nextFloat());
                    compoundTag.putBoolean("isOpened", true);
                }
            }
            else {
                if(isOpened){
                    this.worker.getCommandSenderWorld().blockEvent(this.chestPos, block, 1, 0);
                    this.worker.getCommandSenderWorld().playSound(null, chestPos, SoundEvents.CHEST_CLOSE, worker.getSoundSource(), 0.7F, 0.8F + 0.4F * worker.getRandom().nextFloat());
                    compoundTag.putBoolean("isOpened", false);
                }
            }
            this.worker.getCommandSenderWorld().gameEvent(this.worker, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, chestPos);
        }
    }

    private void reequipMainTool(){
        boolean hasMainHand = worker.getInventory().hasAnyMatching(worker::isRequiredMainTool);

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);

            if((!hasMainHand && worker.isRequiredMainTool(stack) && worker.hasAMainTool())){
                take(stack);

                worker.needsMainTool = false;
                worker.updateNeedsTool();
                break;
            }

        }
    }

    private void reequipSecondTool(){
        boolean hasOffHand = worker.getInventory().hasAnyMatching(worker::isRequiredSecondTool);

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);

            if(!hasOffHand && worker.isRequiredSecondTool(stack) && worker.hasASecondTool()){
                take(stack);

                worker.needsSecondTool = false;
                worker.updateNeedsTool();
                break;
            }
        }
    }

    private boolean hasEnoughOfItem(Item item, int x){
        int amount = getAmountOfItem(item);
        return amount >= x;
    }

    public int getAmountOfItem(Item item){
        int amount = 0;
        for (int i = 0; i < worker.getInventory().getContainerSize(); i++) {
            ItemStack containerItem = worker.getInventory().getItem(i);
            if(containerItem.is(item)){
                amount += containerItem.getCount();
            }
        }
        return amount;
    }
    private void getItemFromChest(Item item){
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if(stack.is(item)){
                worker.getInventory().addItem(stack.copy());
                container.removeItemNoUpdate(i);
                break;
            }
        }
    }


    private void take(ItemStack stack){
        ItemStack tool = stack.copy();
        worker.getInventory().addItem(tool);
        stack.shrink(1);
    }

    private void depositItems(Container container) {
        SimpleContainer inventory = worker.getInventory();

        if (this.isContainerFull(container)) {

            if(worker.getOwner() != null && messageChestFull) {
                this.worker.tellPlayer(worker.getOwner(), Translatable.TEXT_CHEST_FULL);
                messageChestFull = false;
            }
            return;
        }

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            
            // This avoids depositing items such as tools, food, 
            // or anything the workers wouldn't pick up while they're working.
            // It also avoids depositing items that the worker needs to continue working.
            if (stack.is(Items.AIR) || stack.isEmpty() || !worker.wantsToPickUp(stack) || (worker.wantsToKeep(stack) && getAmountOfItem(stack.getItem()) <= 64)) {
                continue;
            }
            // Attempt to deposit the stack in the container, keep the remainder
            ItemStack remainder = this.deposit(stack, container);
            inventory.setItem(i, remainder);


            //Main.LOGGER.debug("Stored {} x {}", stack.getCount() - remainder.getCount(), stack.getDisplayName().getString());
            //Main.LOGGER.debug("Kept {} x {}", remainder.getCount(), stack.getDisplayName().getString());
        }
    }

    private boolean isContainerFull(Container container){
        for(int i = 0; i < container.getContainerSize(); i++){
            ItemStack itemStack = container.getItem(i);
            if(itemStack.isEmpty()) return false;
        }
        return true;
    }
    /**
     * Deposits a stack in a target container.
     * @return The shrinked stack with the remaining items that were not deposited.
     */
    private ItemStack deposit(ItemStack stack, Container container) {
        // Attempt to fill matching stacks first.
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack targetStack = container.getItem(i);
            if (targetStack.is(stack.getItem())) {
                int amountToDeposit = Math.min(stack.getCount(), targetStack.getMaxStackSize() - targetStack.getCount());

                targetStack.grow(amountToDeposit);
                stack.shrink(amountToDeposit);

                container.setItem(i, targetStack);

                if (stack.isEmpty()) {
                    return stack;
                }
            }
        }
        // Put the remainder in the first empty slot we can find.
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack targetStack = container.getItem(i);
            if (targetStack.isEmpty()) {
                container.setItem(i, stack);
                return ItemStack.EMPTY;
            }
        }
        // If we haven't returned at this point, the item can't be inserted. 
        // Return the remainder.
        return stack;
    }

    private boolean hasFoodInInv(){
        return worker.getInventory().items
                .stream()
                .filter(itemStack -> !itemStack.is(Items.PUFFERFISH))
                .filter(itemStack -> itemStack.isEdible() && itemStack.getFoodProperties(this.worker).getNutrition() > 4)
                .anyMatch(ItemStack::isEdible);
    }

    private boolean isFoodInChest(Container container){
        for(int i = 0; i < container.getContainerSize(); i++) {
            ItemStack foodItem = container.getItem(i);
            if(foodItem.isEdible() && foodItem.getFoodProperties(this.worker).getNutrition() > 4){
                return true;
            }
        }
        return false;
    }
}
