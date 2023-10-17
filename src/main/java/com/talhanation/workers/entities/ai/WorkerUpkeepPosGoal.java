package com.talhanation.workers.entities.ai;

import com.talhanation.workers.Translatable;
import com.talhanation.workers.entities.AbstractWorkerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;

public class WorkerUpkeepPosGoal extends Goal {
    public AbstractWorkerEntity worker;
    public BlockPos chestPos;
    //public Entity mobInv;
    public Container container;
    public boolean message;
    public boolean noSpaceInvMessage;

    public WorkerUpkeepPosGoal(AbstractWorkerEntity worker) {
        this.worker = worker;
    }

    @Override
    public boolean canUse() {
        return worker.needsToEat() && worker.getChestPos() != null && !worker.needsToSleep() && !worker.isSleeping() && !worker.getFollow(); //|| worker.needsTools()
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
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
            if(foodItem.isEdible()){
                return true;
            }
        }
        return false;
    }

    @Override
    public void start() {
        super.start();
        message = true;
        noSpaceInvMessage = true;
    }

    @Override
    public void tick() {
        super.tick();
        this.chestPos = worker.getChestPos();

        if (chestPos != null && !this.hasFoodInInv()){
            BlockEntity entity = worker.getCommandSenderWorld().getBlockEntity(chestPos);

            if (entity instanceof Container containerEntity) {
                this.container = containerEntity;
            }

            this.worker.getNavigation().moveTo(chestPos.getX(), chestPos.getY(), chestPos.getZ(), 1.15D);

            if (chestPos.closerThan(worker.getOnPos(), 3) && container != null) {
                this.worker.getNavigation().stop();
                this.worker.getLookControl().setLookAt(chestPos.getX(), chestPos.getY() + 1, chestPos.getZ(), 10.0F, (float) this.worker.getMaxHeadXRot());
                if (isFoodInChest(container)) {
                    for (int i = 0; i < 3; i++) {
                        ItemStack foodItem = this.getFoodFromInv(container);
                        ItemStack food;
                        canAddFood();
                        if (foodItem != null){
                            if(canAddFood()){
                                food = foodItem.copy();
                                food.setCount(1);
                                worker.getInventory().addItem(food);
                                foodItem.shrink(1);
                            }
                            else{
                                if(worker.getOwner() != null && noSpaceInvMessage){
                                    worker.tellPlayer(worker.getOwner(), Translatable.TEXT_NO_SPACE_INV);
                                    noSpaceInvMessage = false;
                                    stop();
                                }
                            }
                        }

                    }
                }
                else {
                    if(worker.getOwner() != null && message){
                        worker.tellPlayer(worker.getOwner(), Translatable.TEXT_NO_FOOD);
                        message = false;
                    }
                }
            }
            else stop();
        }
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

    private boolean canAddFood(){
        for(int i = 6; i < 14; i++){
            if(worker.getInventory().getItem(i).isEmpty())
                return true;
        }
        return false;
    }
}