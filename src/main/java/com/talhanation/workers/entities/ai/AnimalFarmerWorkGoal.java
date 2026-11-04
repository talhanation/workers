package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.AnimalFarmerEntity;
import com.talhanation.workers.entities.workarea.AnimalPenArea;
import com.talhanation.workers.world.NeededItem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.*;

public class AnimalFarmerWorkGoal extends Goal {

    public AnimalFarmerEntity animalFarmerEntity;
    public State state;
    public boolean errorMessageDone;
    public Animal animal;
    public AnimalPenArea.AnimalTypes animalType;
    public int amountToBreed;
    public int amountToSlaughter;
    public Stack<Animal> stackOfAnimals = new Stack<>();
    public List<NeededItem> neededItems = new ArrayList<>();
    public int time;
    public int moveTime;
    public boolean isHolding;

    // Gathering: animals of the pen's type are brought into the pen with leads from the inventory, one lead per
    // animal, up to MAX_LEASHED at once. On the way to the pen he takes animals close to him along, as a task
    // he looks for them around the pen. Near the pen the animals are put into it. Animals he could not
    // reach or bring are left alone for a while.
    private static final int GATHER_IGNORE_TIME = 6000;
    private static final int GATHER_REACH_TIMEOUT = 30;
    private static final int GATHER_LEAD_TIMEOUT = 90;
    private static final int GATHER_ON_THE_WAY_INTERVAL = 3;
    private static final int MAX_LEASHED = 4;
    private static final double GATHER_WAIT_DISTANCE = 5.0D;
    private static final double GATHER_ON_THE_WAY_RANGE = 12.0D;
    private static final double GATHER_NEXT_RANGE = 16.0D;
    public final List<Animal> leashedAnimals = new ArrayList<>();
    private final Map<UUID, Long> ignoredAnimals = new HashMap<>();
    private int gatherTime;
    private int gatherScanTime;

    public AnimalFarmerWorkGoal(AnimalFarmerEntity animalFarmerEntity) {
        this.animalFarmerEntity = animalFarmerEntity;
        setFlags(EnumSet.of(Flag.LOOK, Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return !animalFarmerEntity.needsToSleep() && animalFarmerEntity.shouldWork() && !animalFarmerEntity.needsToGetToChest() && this.isAreaNotRemoved();
    }

    private boolean isAreaNotRemoved() {
        if(animalFarmerEntity.currentAnimalPen == null || !animalFarmerEntity.currentAnimalPen.isRemoved()) return true;
        else {
            animalFarmerEntity.currentAnimalPen = null;
        }
        return false;
    }
    @Override
    public void start() {
        super.start();
        if(this.animalFarmerEntity.getCommandSenderWorld().isClientSide()) return;
        animalFarmerEntity.setAggroState(3);
        if(animalFarmerEntity.getFollowState() == 0) animalFarmerEntity.setFollowState(6); //Working
        setState(State.SELECT_WORK_AREA);
    }

    @Override
    public void stop() {
        super.stop();
        animalFarmerEntity.setAggroState(0);
        //interrupted (deposit, needed items, sleep...) -> don't walk away with feed in hand
        this.putAwayTemptItem();
        //and don't take leashed animals along
        this.releaseLeashedAnimals();
    }

    @Override
    public void tick() {
        super.tick();
        if(this.animalFarmerEntity.getCommandSenderWorld().isClientSide()) return;
        if(state == null) return;
        if(animal != null) this.animalFarmerEntity.getLookControl().setLookAt(animal);
        //leading animals: wait for them, the lead breaks at more than 10 blocks
        if(this.isAnimalLeftBehind()) this.animalFarmerEntity.getNavigation().stop();
        if(this.animalFarmerEntity.tickCount % 20 != 0) return;

        switch(state){
            case SELECT_WORK_AREA ->{
                if(this.animalFarmerEntity.currentAnimalPen != null) setState(State.MOVE_TO_WORK_AREA);

                List<AnimalPenArea> areas = getAvailableWorkAreasByPriority((ServerLevel) animalFarmerEntity.getCommandSenderWorld(), animalFarmerEntity, animalFarmerEntity.currentAnimalPen);

                if (!areas.isEmpty()) {
                    this.animalFarmerEntity.currentAnimalPen = areas.get(0);
                }

                if(this.animalFarmerEntity.currentAnimalPen == null) return;

                this.animalFarmerEntity.currentAnimalPen.setBeingWorkedOn(true);
                this.animalFarmerEntity.currentAnimalPen.setTime(0);
                this.animalType = this.animalFarmerEntity.currentAnimalPen.getAnimalType();


                setState(State.MOVE_TO_WORK_AREA);
            }

            case MOVE_TO_WORK_AREA ->{
                //on the way: animals for the pen close to him are taken along
                if(this.gatherOnTheWay()) return;

                this.animal = null;
                if(this.moveToPosition(this.getPenCenter(), 100)) return;

                setState(State.MOVE_TO_CENTER);
            }

            case MOVE_TO_CENTER ->{
                this.animal = null;
                if(this.deliverLeashedAnimals()) return;
                //position() is the corner of the area (often on the fence) -> walk into the pen instead
                if(!this.isInsidePen() && ++moveTime < 15 && this.moveToPosition(this.getPenCenter(), 4)) return;

                setState(State.PREPARE_GATHER);
            }

            case PREPARE_GATHER -> {
                AnimalPenArea pen = animalFarmerEntity.currentAnimalPen;
                this.deliverLeashedAnimals();

                if(!pen.getGather()){
                    this.releaseLeashedAnimals();
                    setState(State.PREPARE_BREED);
                    return;
                }

                //the lead comes from the inventory, only asked for when there is something to gather
                if(leashedAnimals.isEmpty()){
                    if(this.getLeadCount() <= 0){
                        if(this.findAnimalToGather(pen.getGatherRange()) != null){
                            this.neededItems.add(new NeededItem(stack -> stack.is(Items.LEAD), 1, true, pen.getUUID()));
                        }
                        setState(State.PREPARE_BREED);
                        return;
                    }
                }

                this.animal = null;
                setState(State.GATHER);
            }

            case GATHER -> {
                if(animalFarmerEntity.currentAnimalPen.getGather() && this.gather()) return;

                this.releaseLeashedAnimals();
                setState(State.PREPARE_BREED);
            }

            case PREPARE_BREED -> {
                if(!animalFarmerEntity.currentAnimalPen.getBreed() || !animalFarmerEntity.currentAnimalPen.isBreedTime()){
                    setState(State.PREPARE_SPECIAL_TASK);
                    return;
                }

                animalFarmerEntity.currentAnimalPen.scanAnimalBreed();
                this.stackOfAnimals = animalFarmerEntity.currentAnimalPen.animalsToBreed;

                if(stackOfAnimals.isEmpty()){
                    setState(State.PREPARE_SPECIAL_TASK);
                    return;
                }

                amountToBreed = stackOfAnimals.size();
                amountToBreed -= amountToBreed % 2;

                Item breedItem = animalType.getBreedItem();
                animalFarmerEntity.switchMainHandItem(itemStack -> itemStack.is(breedItem));
                ItemStack mainHandItem = animalFarmerEntity.getMainHandItem();

                boolean hasBreedItem = mainHandItem.is(breedItem);
                if(!hasBreedItem){
                    this.neededItems.add(new NeededItem(stack -> stack.is(breedItem), amountToBreed, true, this.animalFarmerEntity.currentAnimalPen.getUUID()));
                    this.animal = null;
                    setState(State.PREPARE_SPECIAL_TASK);
                    return;
                }

                setState(State.BREED);
            }

            case BREED -> {
                if(animalFarmerEntity.currentAnimalPen.getBreed() && animalFarmerEntity.currentAnimalPen.isBreedTime() && this.breed()) return;

                this.animalFarmerEntity.currentAnimalPen.setBreedTime(2000);
                setState(State.PREPARE_SPECIAL_TASK);
            }

            case PREPARE_SPECIAL_TASK -> {
                if(!animalFarmerEntity.currentAnimalPen.getSpecial()){
                    setState(State.PREPARE_SLAUGHTER);
                    return;
                }

                if(!animalFarmerEntity.hasFreeInvSlot()){
                    animalFarmerEntity.forcedDeposit = true;
                    return;
                }

                this.animalFarmerEntity.currentAnimalPen.scanAnimalSpecial();
                this.stackOfAnimals = animalFarmerEntity.currentAnimalPen.animalsForSpecialTask;

                if(stackOfAnimals.isEmpty() && animalType != AnimalPenArea.AnimalTypes.CHICKEN){
                    setState(State.PREPARE_SLAUGHTER);
                    return;
                }
                setState(State.SPECIAL_TASK);
            }

            case SPECIAL_TASK -> {
                if(animalFarmerEntity.currentAnimalPen.getSpecial() && this.doSpecialTask()) return;
                setState(State.PREPARE_SLAUGHTER);
            }

            case PREPARE_SLAUGHTER -> {
                if(!animalFarmerEntity.currentAnimalPen.getSlaughter()){
                    setState(State.DONE);
                    return;
                }

                if(!animalFarmerEntity.hasFreeInvSlot()){
                    animalFarmerEntity.forcedDeposit = true;
                    return;
                }

                this.animalFarmerEntity.currentAnimalPen.scanAnimalSlaughter();
                this.stackOfAnimals = animalFarmerEntity.currentAnimalPen.animalsToSlaughter;

                int max = animalFarmerEntity.currentAnimalPen.getMaxAnimals();
                int size = stackOfAnimals.size();

                if(max >= size){
                    setState(State.DONE);
                    return;
                }

                amountToSlaughter = size - max;

                animalFarmerEntity.switchMainHandItem(itemStack -> itemStack.getItem() instanceof AxeItem);

                boolean hasAxe = animalFarmerEntity.getMainHandItem().getItem() instanceof AxeItem;
                if(!hasAxe){
                    animalFarmerEntity.addNeededItem(new NeededItem(stack -> stack.getItem() instanceof AxeItem, 1, true));
                    this.animal = null;
                    return;
                }

                setState(State.SLAUGHTER);
            }
            case SLAUGHTER -> {
                if(animalFarmerEntity.currentAnimalPen.getSlaughter() && this.slaughter()) return;

                setState(State.DONE);
            }

            case DONE -> {
                animal = null;
                animalFarmerEntity.currentAnimalPen = null;
                animalFarmerEntity.switchMainHandItem(ItemStack::isEmpty);

                if(this.animalFarmerEntity.getFollowState() == 6) this.animalFarmerEntity.setFollowState(0);//Wander
                setState(State.SELECT_WORK_AREA);

                if(!this.neededItems.isEmpty()){
                    for(NeededItem neededItem : neededItems){
                        this.animalFarmerEntity.addNeededItem(neededItem);
                    }
                    this.neededItems.clear();
                }
            }

            case ERROR ->{
                if(!errorMessageDone){
                    errorMessageDone = true;
                }

                if(++time > 1000){
                    time = 0;
                    this.start();
                }

            }
        }
    }

    /** Task: animals around the pen, several on one trip if he has the leads. False when nothing is left. */
    private boolean gather() {
        AnimalPenArea pen = animalFarmerEntity.currentAnimalPen;
        if(this.animal == null){
            if(this.canLeashMore()){
                //the first one anywhere around the pen, then only the next ones close to him
                AABB range = leashedAnimals.isEmpty() ? pen.getGatherRange() : animalFarmerEntity.getBoundingBox().inflate(GATHER_NEXT_RANGE, 4.0D, GATHER_NEXT_RANGE);
                this.animal = this.findAnimalToGather(range);
                if(this.animal != null) gatherTime = 0;
            }

            if(this.animal == null){
                if(leashedAnimals.isEmpty()) return false;
                return this.leadToPen();
            }
        }

        this.goToAnimal();
        return true;
    }

    /** On the way to the pen: animals close to him are taken along. True while he walks to one. */
    private boolean gatherOnTheWay() {
        if(this.animal == null){
            if(gatherScanTime-- > 0) return false;
            gatherScanTime = GATHER_ON_THE_WAY_INTERVAL;

            if(!animalFarmerEntity.currentAnimalPen.getGather() || !this.canLeashMore()) return false;

            this.animal = this.findAnimalToGather(animalFarmerEntity.getBoundingBox().inflate(GATHER_ON_THE_WAY_RANGE, 4.0D, GATHER_ON_THE_WAY_RANGE));
            if(this.animal == null) return false;
            gatherTime = 0;
        }
        return this.goToAnimal();
    }

    /** Walks to the target animal and puts a lead on it. False when that is done or he gave up. */
    private boolean goToAnimal() {
        //someone else took it, it went into a pen or died meanwhile
        if(!animalFarmerEntity.currentAnimalPen.canBeGathered(animal)){
            this.animal = null;
            return false;
        }
        if(++gatherTime > GATHER_REACH_TIMEOUT){
            this.ignoreAnimal(animal);
            this.animal = null;
            return false;
        }
        if(this.moveToPosition(animal.position(), 6)) return true;

        ItemStack lead = animalFarmerEntity.getMatchingItem(stack -> stack.is(Items.LEAD));
        if(lead != null){
            //the lead is on the animal now, he gets it back when he lets it go (a broken lead drops as item)
            lead.shrink(1);
            animal.setLeashedTo(animalFarmerEntity, true);
            this.animalFarmerEntity.swing(InteractionHand.MAIN_HAND);
            this.leashedAnimals.add(animal);
            gatherTime = 0;
        }
        this.animal = null;
        return false;
    }

    private boolean leadToPen() {
        this.updateLeashedAnimals();
        if(leashedAnimals.isEmpty()) return true;
        if(this.deliverLeashedAnimals()) return true;

        if(++gatherTime > GATHER_LEAD_TIMEOUT){
            leashedAnimals.forEach(this::ignoreAnimal);
            this.releaseLeashedAnimals();
            return true;
        }

        this.moveToPosition(this.getPenCenter(), 4);
        return true;
    }

    /** Close to the pen: the leashed animals go straight in. */
    private boolean deliverLeashedAnimals() {
        this.updateLeashedAnimals();
        if(leashedAnimals.isEmpty()) return false;
        if(!animalFarmerEntity.currentAnimalPen.getArea().inflate(3.0D, 2.0D, 3.0D).contains(animalFarmerEntity.position())) return false;

        Vec3 center = this.getPenCenter();
        for(Animal led : new ArrayList<>(leashedAnimals)){
            this.releaseLeashedAnimal(led);
            led.teleportTo(center.x(), center.y(), center.z());
            //the lead kept it close to the farmer, now it may walk around in the pen
            led.clearRestriction();
            led.getNavigation().stop();
        }
        return true;
    }

    /** Broken leads (lying on the ground now) and dead animals are forgotten. */
    private void updateLeashedAnimals() {
        leashedAnimals.removeIf(led -> !led.isAlive() || led.getLeashHolder() != animalFarmerEntity);
    }

    private boolean isAnimalLeftBehind() {
        for(Animal led : leashedAnimals){
            if(led.isAlive() && animalFarmerEntity.distanceTo(led) > GATHER_WAIT_DISTANCE) return true;
        }
        return false;
    }

    //all animals are gathered, also above Max. Animals: the ones too many are slaughtered as usual
    private boolean canLeashMore() {
        return leashedAnimals.size() < MAX_LEASHED && this.getLeadCount() > 0;
    }

    private int getLeadCount() {
        return animalFarmerEntity.countMatchingItems(stack -> stack.is(Items.LEAD));
    }

    @Nullable
    private Animal findAnimalToGather(AABB range) {
        long gameTime = animalFarmerEntity.getCommandSenderWorld().getGameTime();
        ignoredAnimals.values().removeIf(ignoreTime -> gameTime - ignoreTime > GATHER_IGNORE_TIME);

        Animal nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for(Animal candidate : animalFarmerEntity.currentAnimalPen.getAnimalsToGather(range)){
            if(ignoredAnimals.containsKey(candidate.getUUID())) continue;

            double distance = candidate.distanceToSqr(animalFarmerEntity);
            if(distance < nearestDistance){
                nearestDistance = distance;
                nearest = candidate;
            }
        }
        return nearest;
    }

    /** Takes the leads off the animals and puts them back into the inventory. */
    private void releaseLeashedAnimals() {
        for(Animal led : new ArrayList<>(leashedAnimals)){
            this.releaseLeashedAnimal(led);
        }
        leashedAnimals.clear();
    }

    private void releaseLeashedAnimal(Animal led) {
        if(led.getLeashHolder() == animalFarmerEntity){
            led.dropLeash(true, false);
            ItemStack rest = animalFarmerEntity.getInventory().addItem(new ItemStack(Items.LEAD));
            if(!rest.isEmpty()) animalFarmerEntity.spawnAtLocation(rest);
        }
        leashedAnimals.remove(led);
    }

    private void ignoreAnimal(Animal animal) {
        ignoredAnimals.put(animal.getUUID(), animalFarmerEntity.getCommandSenderWorld().getGameTime());
    }

    private boolean breed() {
        if(this.animal == null) {
            if(!stackOfAnimals.isEmpty()) animal = stackOfAnimals.pop();
            else return false;
        }
        if(!this.isAnimalInPen(animal)){
            this.animal = null;
            return true;
        }
        if(this.moveToPosition(animal.position(), 6)) return true;
        this.animalFarmerEntity.getLookControl().setLookAt(animal);

        Item breedItem = animalType.getBreedItem();
        animalFarmerEntity.switchMainHandItem(itemStack -> itemStack.is(breedItem));
        ItemStack mainHandItem = animalFarmerEntity.getMainHandItem();

        boolean hasBreedItem = mainHandItem.is(breedItem);
        if(!hasBreedItem){
            animalFarmerEntity.addNeededItem(new NeededItem(stack -> stack.is(breedItem), amountToBreed, false));
            this.animal = null;
            return false;
        }

        this.animalFarmerEntity.swing(InteractionHand.MAIN_HAND);
        this.animalFarmerEntity.getMainHandItem().shrink(1);
        amountToBreed--;
        animal.setAge(0);

        animal.setInLove(null);

        this.animal = null;
        return true;
    }

    private boolean doSpecialTask() {
        Item specialItem = animalType.getSpecialItem();

        animalFarmerEntity.switchMainHandItem(itemStack -> itemStack.is(specialItem));

        ItemStack mainHandItem = animalFarmerEntity.getMainHandItem();

        boolean hasSpecialItem = mainHandItem.is(specialItem);
        if(!hasSpecialItem){
            boolean chicken = animalType == AnimalPenArea.AnimalTypes.CHICKEN;

            this.neededItems.add(new NeededItem(stack -> stack.is(specialItem),  chicken ? 32 : 1, !chicken, this.animalFarmerEntity.currentAnimalPen.getUUID()));
            this.animal = null;
            setState(State.DONE);
            return true;
        }
        if(animalType == AnimalPenArea.AnimalTypes.CHICKEN){

            return throwEggs();
        }

        if(this.animal == null) {
            if(!stackOfAnimals.isEmpty()) animal = stackOfAnimals.pop();
            else return false;
        }
        if(!this.isAnimalInPen(animal)){
            this.animal = null;
            return true;
        }
        if(this.moveToPosition(animal.position(), 6)) return true;

        if(animalType == AnimalPenArea.AnimalTypes.SHEEP){
            return sheerSheep();
        } else if (animalType == AnimalPenArea.AnimalTypes.COW) {
            return milkCow();
        }

        return false;
    }

    public boolean slaughter() {
        animalFarmerEntity.switchMainHandItem(itemStack -> itemStack.getItem() instanceof AxeItem);

        boolean hasAxe = animalFarmerEntity.getMainHandItem().getItem() instanceof AxeItem;
        if(!hasAxe){
            animalFarmerEntity.addNeededItem(new NeededItem(stack -> stack.getItem() instanceof AxeItem, 1, true));
            this.animal = null;
            setState(State.ERROR);
            return false;
        }

        int max = animalFarmerEntity.currentAnimalPen.getMaxAnimals();
        int size = stackOfAnimals.size();

        if(max >= size || amountToSlaughter == 0){
            setState(State.DONE);
            return false;
        }

        if(this.animal == null) {
            if(!stackOfAnimals.isEmpty()) animal = stackOfAnimals.pop();
            else return false;
        }
        if(!this.isAnimalInPen(animal)){
            this.animal = null;
            return true;
        }

        if(this.moveToPosition(animal.position(), 6)) return true;
        this.animalFarmerEntity.getLookControl().setLookAt(animal);

        animalFarmerEntity.playSound(SoundEvents.PLAYER_ATTACK_STRONG);
        this.animalFarmerEntity.swing(InteractionHand.MAIN_HAND);

        animal.kill();
        amountToSlaughter--;

        animalFarmerEntity.damageMainHandItem();

        animal = null;
        return true;
    }


    private boolean throwEggs() {
        if(!this.animalFarmerEntity.getMainHandItem().is(Items.EGG)){
            return false;
        }
        if(this.moveToPosition(animalFarmerEntity.currentAnimalPen.getArea().getCenter(), 10)){
            return true;
        }

        animalFarmerEntity.getCommandSenderWorld().playSound(null, animalFarmerEntity.getX(), animalFarmerEntity.getY(), animalFarmerEntity.getZ(), SoundEvents.EGG_THROW, SoundSource.PLAYERS, 0.5F, 0.4F / (animalFarmerEntity.getRandom().nextFloat() * 0.4F + 0.8F));
        ThrownEgg thrownegg = new ThrownEgg(animalFarmerEntity.getCommandSenderWorld(), animalFarmerEntity);
        thrownegg.setItem(new ItemStack(Items.EGG));

        double d0 = animalFarmerEntity.currentAnimalPen.getArea().getCenter().x() - this.animalFarmerEntity.getX();
        double d2 = animalFarmerEntity.currentAnimalPen.getArea().getCenter().z() - this.animalFarmerEntity.getZ();

        thrownegg.shoot(d0, 0, d2, 0.1F, 0F);

        if(animalFarmerEntity.getCommandSenderWorld().addFreshEntity(thrownegg)){
            this.animalFarmerEntity.getMainHandItem().shrink(1);
        }

        return true;
    }

    public boolean sheerSheep() {
        if(animal == null) return false;

        if(animal instanceof Sheep sheep){
            sheep.shear(SoundSource.PLAYERS);
            sheep.setSheared(true);

            this.animalFarmerEntity.swing(InteractionHand.MAIN_HAND);
            this.animalFarmerEntity.damageMainHandItem();
        }
        this.animal = null;
        return true;
    }

    public boolean milkCow() {
        if(animal == null) return false;

        animalFarmerEntity.getMainHandItem().shrink(1);
        animalFarmerEntity.getInventory().addItem(Items.MILK_BUCKET.getDefaultInstance());
        animal.playSound(SoundEvents.COW_MILK, 1.0F, 1.0F);

        animal = null;
        return true;
    }

    public void setState(State state) {
        //if(animalFarmerEntity.getOwner() != null) animalFarmerEntity.getOwner().sendSystemMessage(Component.literal(state.toString()));
        this.state = state;
        this.moveTime = 0;

        //only hold feed while breeding, otherwise the animals follow him out of the gate
        if(state != State.PREPARE_BREED && state != State.BREED) this.putAwayTemptItem();
    }

    private void putAwayTemptItem(){
        if(this.animalFarmerEntity.getCommandSenderWorld().isClientSide()) return;
        if(!isTemptItem(animalFarmerEntity.getMainHandItem())) return;

        animalFarmerEntity.switchMainHandItem(ItemStack::isEmpty);
        if(isTemptItem(animalFarmerEntity.getMainHandItem())){
            animalFarmerEntity.switchMainHandItem(stack -> !isTemptItem(stack));
        }
    }

    //same items as WorkerTemptGoal in VillagerEvents
    public static boolean isTemptItem(ItemStack stack){
        if(stack.isEmpty()) return false;
        return stack.is(Items.WHEAT) || Chicken.FOOD_ITEMS.test(stack) || Pig.FOOD_ITEMS.test(stack);
    }

    private Vec3 getPenCenter(){
        Vec3 center = animalFarmerEntity.currentAnimalPen.getArea().getCenter();
        return new Vec3(center.x(), animalFarmerEntity.currentAnimalPen.getY(), center.z());
    }

    private boolean isInsidePen(){
        AABB inner = animalFarmerEntity.currentAnimalPen.getArea().inflate(-1.0D, 0, -1.0D);
        return inner.contains(animalFarmerEntity.position());
    }

    //animal escaped or died since the scan -> don't follow it out of the pen
    private boolean isAnimalInPen(Animal animal){
        return animal.isAlive() && animalFarmerEntity.currentAnimalPen.getArea().contains(animal.position());
    }

    @Override
    public boolean canContinueToUse() {
        return canUse() || this.isHolding;
    }

    @Override
    public boolean isInterruptable() {
        return true;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    public static List<AnimalPenArea> getAvailableWorkAreasByPriority(ServerLevel level, AnimalFarmerEntity animalFarmerEntity, @Nullable AnimalPenArea currentArea) {
        List<AnimalPenArea> list = animalFarmerEntity.findWorkAreas(AnimalPenArea.class, 64);

        Map<AnimalPenArea, Integer> priorityMap = new HashMap<>();

        for (AnimalPenArea area : list) {
            if (area == null || area == currentArea || !area.canWorkHere(animalFarmerEntity)) continue;

            int priority = 0;

            boolean perfectCandidate = area.isWorkerPerfectCandidate(animalFarmerEntity);

            if (perfectCandidate) {
                priority += 10;
            } else {
                priority += 1;
            }

            if (!area.isBeingWorkedOn()) {
                priority += 3;
            }

            priority += area.time;

            priorityMap.put(area, priority);
        }

        List<AnimalPenArea> sorted = new ArrayList<>(priorityMap.keySet());
        sorted.sort((a, b) -> Integer.compare(priorityMap.get(b), priorityMap.get(a)));

        return sorted;
    }

    public boolean moveToPosition(Vec3 pos, int threshold){
        if(pos == null){
            return false;
        }
        else{
            double distance = animalFarmerEntity.getHorizontalDistanceTo(pos);
            if(distance < threshold){
                return false;
            }
            else{
                animalFarmerEntity.getNavigation().moveTo(pos.x(), pos.y(), pos.z(), 0.8F);
                // start() already claimed the working state; calling it here every
                // tick would override owner commands and pull the worker back to work.
                animalFarmerEntity.getLookControl().setLookAt(pos);
            }
            return true;
        }
    }

    public enum State{
        SELECT_WORK_AREA,
        MOVE_TO_WORK_AREA,
        MOVE_TO_CENTER,
        PREPARE_GATHER,
        GATHER,
        PREPARE_BREED,
        BREED,
        PREPARE_SLAUGHTER,
        SLAUGHTER,
        PREPARE_SPECIAL_TASK,
        SPECIAL_TASK,
        DONE,
        ERROR;

    }
}