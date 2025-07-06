package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import com.talhanation.workers.world.NeededItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public class GetNeededItemsFromChestsGoal extends AbstractChestGoal {
    int timer;
    public State state;
    public String errorMessage;
    public boolean errorMessageDone;
    private int retryTime;
    public GetNeededItemsFromChestsGoal(AbstractWorkerEntity worker) {
        super(worker);
        setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return worker.needsToGetItems() && super.canUse();
    }

    @Override
    public void start() {
        super.start();
        errorMessageDone = false;
        if(worker.chestPositions.isEmpty()) {
            setState(State.ERROR_NO_CHESTS);
            errorMessage = worker.getName().getString() + ": I have no Chests assigned";
            return;
        }
        this.blockPosStack = new Stack<>();
        for(BlockPos pos : worker.chestPositions){
            this.blockPosStack.push(pos);
        }
        setState(State.SELECT_CHEST);
    }
    public void tick(){
        if(this.worker.getCommandSenderWorld().isClientSide()) return;

        switch (state){
            case SELECT_CHEST -> {
                if(!blockPosStack.isEmpty()){
                    blockPosStack.sort(Comparator.comparing(pos -> pos.getCenter().distanceToSqr(worker.position())));
                    blockPosStack.sort(Comparator.reverseOrder());

                    chestPos = blockPosStack.pop();
                }
                else{
                    setState(State.ERROR_ITEMS_NOT_FOUND);
                    errorMessage = worker.getName().getString() + ": I need " + worker.neededItems;
                    return;
                }
                setState(State.MOVE_TO_CHEST);
            }

            case MOVE_TO_CHEST -> {
                if(moveToPosition(chestPos)) return;

                setState(State.CHECK_CHEST);
            }

            case CHECK_CHEST -> {
                container = getContainer(chestPos);
                if(container == null){
                    if(!blockPosStack.isEmpty()) chestPos = blockPosStack.pop();

                    setState(State.SELECT_CHEST);
                    return;
                }

                setState(State.OPEN_CHEST);
            }

            case OPEN_CHEST -> {
                worker.getLookControl().setLookAt(chestPos.getCenter());
                this.interactChest(container, true);

                if(timer++ < 40){
                    return;
                }
                timer = 0;

                setState(State.TAKE_NEEDED_ITEMS);
            }

            case TAKE_NEEDED_ITEMS -> {
                worker.getLookControl().setLookAt(chestPos.getCenter());
                if(takeNeededItems()){
                    setState(State.CLOSE_CHEST_DONE);
                }
                else{
                    setState(State.CLOSE_CHEST_NOT_DONE);
                }
            }

            case CLOSE_CHEST_DONE -> {
                worker.getLookControl().setLookAt(chestPos.getCenter());
                this.interactChest(container, false);

                if(timer++ < 20){
                    return;
                }
                timer = 0;

                setState(State.DONE);
            }

            case CLOSE_CHEST_NOT_DONE -> {
                worker.getLookControl().setLookAt(chestPos.getCenter());
                this.interactChest(container, false);

                if(timer++ < 20){
                    return;
                }
                timer = 0;

                setState(State.SELECT_CHEST);
            }

            case DONE -> {
                worker.neededItems.clear();
            }

            case ERROR_NO_CHESTS -> {
                if(!errorMessageDone){
                    if(worker.getOwner() != null) worker.getOwner().sendSystemMessage(Component.literal(errorMessage));
                    errorMessageDone = true;
                }

                if(!worker.chestPositions.isEmpty()){
                    this.start();
                }
            }

            case ERROR_ITEMS_NOT_FOUND -> {
                if(!errorMessageDone){
                    errorMessageDone = true;
                    if(worker.getOwner() != null) worker.getOwner().sendSystemMessage(Component.literal(errorMessage));

                    worker.neededItems.removeIf(neededItem -> neededItem.optional);
                }

                if(++retryTime >= 20*15 * 2){
                    retryTime = 0;
                    this.start();
                }
            }
        }
    }


    private boolean takeNeededItems() {
        SimpleContainer inventory = worker.getInventory();

        if (container == null || container.isEmpty()) {
            return false;
        }

        List<NeededItem> neededItems = worker.neededItems;
        if (neededItems.isEmpty()) return false;

        boolean anyTaken = false;

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack itemInChest = container.getItem(i);
            if (itemInChest.isEmpty()) continue;

            for (int j = neededItems.size() - 1; j >= 0; j--) {
                NeededItem needed = neededItems.get(j);

                if (needed.matches(itemInChest)) {
                    int neededCount = needed.count;
                    int availableCount = itemInChest.getCount();
                    int toExtract = Math.min(neededCount, availableCount);

                    ItemStack extracted = itemInChest.split(toExtract);
                    ItemStack leftover = inventory.addItem(extracted);

                    if (!leftover.isEmpty()) {
                        itemInChest.grow(toExtract);
                        return false;
                    }


                    NeededItem.applyToNeededItems(extracted, neededItems);
                    anyTaken = true;

                    if (itemInChest.isEmpty()) {
                        break;
                    }
                }
            }
        }


        if (!anyTaken) {
            return false;
        }


        for (NeededItem needed : neededItems) {
            if (!needed.optional) {
                return false;
            }
        }

        return true;
    }

    public void setState(State state) {
        //if(worker.getOwner() != null) worker.getOwner().sendSystemMessage(Component.literal(state.toString()));
        this.state = state;
    }

    public enum State{
        SELECT_CHEST,
        MOVE_TO_CHEST,
        CHECK_CHEST,
        OPEN_CHEST,
        TAKE_NEEDED_ITEMS,
        CLOSE_CHEST_NOT_DONE,
        CLOSE_CHEST_DONE,
        DONE,
        ERROR_NO_CHESTS,
        ERROR_ITEMS_NOT_FOUND

    }
}
