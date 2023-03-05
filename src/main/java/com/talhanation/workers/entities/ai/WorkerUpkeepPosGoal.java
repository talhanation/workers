package com.talhanation.workers.entities.ai;

import com.talhanation.workers.Translatable;
import com.talhanation.workers.entities.AbstractWorkerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;

public class WorkerUpkeepPosGoal extends Goal {
    public AbstractWorkerEntity worker;
    public BlockPos chestPos;
    //public Entity mobInv;
    public Container container;
    public boolean message;

    public WorkerUpkeepPosGoal(AbstractWorkerEntity worker) {
        this.worker = worker;
    }

    @Override
    public boolean canUse() {
        return worker.needsToEat() && worker.getChestPos() != null;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    private boolean hasFoodInInv(){
        return worker.getInventory().items
                .stream()
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
    }

    @Override
    public void tick() {
        super.tick();
        this.chestPos = findInvPos();

        if (chestPos != null && !this.hasFoodInInv()){
            BlockEntity entity = worker.level.getBlockEntity(chestPos);

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
                        if (foodItem != null && canAddFood()){

                            food = foodItem.copy();
                            food.setCount(1);
                            worker.getInventory().addItem(food);
                            foodItem.shrink(1);
                        }
                    }
                }
                else {
                    if(worker.getOwner() != null && message){
                        //TODO: tell Player No food
                        worker.tellPlayer(worker.getOwner(), Translatable.TEXT_NO_FOOD);
                        message = false;
                    }
                }
            }
            else stop();
        }
        else {
            this.chestPos = findInvPos();
            //Main.LOGGER.debug("Chest not found");
        }
    }



    @Nullable
    private BlockPos findInvPos() {
        if(this.worker.getChestPos() != null) {
            //Main.LOGGER.debug("up keep pos not null");
            BlockPos chestPos;
            int range = 8;

            for (int x = -range; x < range; x++) {
                for (int y = -range; y < range; y++) {
                    for (int z = -range; z < range; z++) {
                        chestPos = worker.getChestPos().offset(x, y, z);
                        BlockEntity block = worker.level.getBlockEntity(chestPos);
                        if (block instanceof Container)
                            return chestPos;
                    }
                }
            }
        }
        //Main.LOGGER.debug("UpkeepPos NULL");
        //else entity around upkeepPos
        return null;
    }
    @Nullable
    private ItemStack getFoodFromInv(Container inv){
        ItemStack itemStack = null;
        for(int i = 0; i < inv.getContainerSize(); i++){
            if(inv.getItem(i).isEdible()){
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