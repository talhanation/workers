package com.talhanation.workers.entities.workarea;

import com.talhanation.workers.client.gui.AnimalPenAreaScreen;
import com.talhanation.workers.entities.AnimalFarmerEntity;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.Mule;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Stack;

public class AnimalPenArea extends AbstractWorkAreaEntity {

    public static final EntityDataAccessor<Integer> ANIMAL_TYPE = SynchedEntityData.defineId(AnimalPenArea.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Boolean> SLAUGHTER = SynchedEntityData.defineId(AnimalPenArea.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> BREED = SynchedEntityData.defineId(AnimalPenArea.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> SPECIAL = SynchedEntityData.defineId(AnimalPenArea.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Integer> MAX_ANIMALS = SynchedEntityData.defineId(AnimalPenArea.class, EntityDataSerializers.INT);

    public Stack<Animal> animalsToBreed = new Stack<>();
    public Stack<Animal> animalsForSpecialTask = new Stack<>();
    public Stack<Animal> animalsToSlaughter = new Stack<>();
    private int breedTime;


    public AnimalPenArea(EntityType<?> type, Level level) {
        super(type, level);
    }
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ANIMAL_TYPE, 0);
        this.entityData.define(MAX_ANIMALS, 12);
        this.entityData.define(BREED, true);
        this.entityData.define(SLAUGHTER, true);
        this.entityData.define(SPECIAL, true);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setSlaughter(tag.getBoolean("slaughter"));
        this.setBreed(tag.getBoolean("breed"));
        this.setSpecial(tag.getBoolean("special"));
        this.setMaxAnimals(tag.getInt("maxAnimals"));

        this.setAnimalType(AnimalTypes.fromIndex(tag.getInt("animalType")));

        this.breedTime = tag.getInt("breedTime");
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("slaughter", getSlaughter());
        tag.putBoolean("breed", getBreed());
        tag.putBoolean("special", getSpecial());
        tag.putInt("maxAnimals", getMaxAnimals());

        tag.putInt("animalType", this.getAnimalType().getIndex());

        tag.putInt("breedTime", this.breedTime);
    }

    public Item getRenderItem(){
        return Items.LEAD;
    }

    @Override
    public void tick() {
        super.tick();
        if(this.getCommandSenderWorld().isClientSide()) return;
        if(breedTime > 0) breedTime--;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public Screen getScreen(Player player) {
        return new AnimalPenAreaScreen(this, player);
    }

    public boolean isWorkerPerfectCandidate(AnimalFarmerEntity animalWorker) {
        if(animalWorker.getMatchingItem(stack -> stack.getItem() instanceof AxeItem) == ItemStack.EMPTY) {
            return false;
        }
        Item breedItem = getAnimalType().breeditem;
        if(animalWorker.getMatchingItem(stack -> stack.is(breedItem)) == ItemStack.EMPTY) {
            return false;
        }

        if(this.getSpecial()){
            if(animalWorker.getMatchingItem(stack -> stack.is(getAnimalType().specialItem)) == ItemStack.EMPTY) {
                return false;
            }
        }

        return true;
    }

    public boolean isAir(BlockState state){
        return state.isAir();
    }

    public void setAnimalType(AnimalTypes type){
        this.entityData.set(ANIMAL_TYPE, type.getIndex());
    }

    public void setSpecial(boolean bool){
        this.entityData.set(SPECIAL, bool);
    }

    public void setSlaughter(boolean bool){
        this.entityData.set(SLAUGHTER, bool);
    }

    public void setBreed(boolean bool){
        this.entityData.set(BREED, bool);
    }

    public void setMaxAnimals(int max){
        this.entityData.set(MAX_ANIMALS, Math.min(max, 64));
    }

    public AnimalTypes getAnimalType(){
        int index = this.entityData.get(ANIMAL_TYPE);
        return AnimalTypes.fromIndex(index);
    }

    public boolean getSpecial() {
        return this.entityData.get(SPECIAL);
    }

    public boolean getSlaughter() {
        return this.entityData.get(SLAUGHTER);
    }

    public boolean getBreed() {
        return this.entityData.get(BREED);
    }

    public int getMaxAnimals(){
        return this.entityData.get(MAX_ANIMALS);
    }

    public void scanAnimalSlaughter(){
        if(area == null) area = this.getArea();

        animalsToSlaughter.clear();
        this.getCommandSenderWorld().getEntitiesOfClass(Animal.class, area)
            .forEach(animal -> {
                if(isCorrectAnimal(animal)){
                    if(!animal.isBaby()) animalsToSlaughter.push(animal);
                }
            }
        );
    }

    public void scanAnimalSpecial(){
        if(area == null) area = this.getArea();

        animalsForSpecialTask.clear();
        this.getCommandSenderWorld().getEntitiesOfClass(Animal.class, area)
            .forEach(animal -> {
                    if(isCorrectAnimal(animal)){
                        if(!animal.isBaby() && isForSpecial(animal)) animalsForSpecialTask.push(animal);
                    }
                }
            );
    }


    private boolean isForSpecial(Animal animal) {
        switch (getAnimalType()){
            case COW -> {
                return animal instanceof Cow;
            }
            case SHEEP -> {
                return animal instanceof Sheep sheep && !sheep.isSheared();
            }
            default -> {
                return false;
            }
        }
    }

    public boolean isBreedTime(){
       return this.breedTime <= 0;
    }

    public void setBreedTime(int i) {
        this.breedTime = i;
    }

    public void scanAnimalBreed(){
        if(area == null) area = this.getArea();

        animalsToBreed.clear();
        this.getCommandSenderWorld().getEntitiesOfClass(Animal.class, area)
            .forEach(animal -> {
                if(isCorrectAnimal(animal)){
                    if(!animal.isBaby()) animalsToBreed.push(animal);
                }
            }
        );
    }

    public boolean isCorrectAnimal(Animal animal) {
        switch (getAnimalType()){
            case CHICKEN -> {
                return animal instanceof Chicken;
            }
            case COW -> {
                return animal instanceof Cow;
            }
            case SHEEP -> {
                return animal instanceof Sheep;
            }
            case PIG -> {
                return animal instanceof Pig;
            }
            case GOAT -> {
                return animal instanceof Goat;
            }
            case CAMEL -> {
                return animal instanceof Camel;
            }
            case HORSE -> {
                return animal instanceof AbstractHorse && !(animal instanceof Camel);
            }
            case RABBIT -> {
                return animal instanceof Rabbit;
            }
            default -> {
                return false;
            }
        }
    }

    public enum AnimalTypes{
        CHICKEN(0, Items.WHEAT_SEEDS, Items.EGG),
        COW(1, Items.WHEAT, Items.BUCKET),
        SHEEP(2, Items.WHEAT, Items.SHEARS),
        PIG(3, Items.CARROT),
        GOAT(4, Items.WHEAT),
        CAMEL(5, Items.CACTUS),
        HORSE(6, Items.GOLDEN_CARROT),
        RABBIT(7, Items.DANDELION);

        private final int index;
        private final Item breeditem;
        private final Item specialItem;
        AnimalTypes(int index, Item breeditem, Item specialItem){
            this.index = index;
            this.breeditem = breeditem;
            this.specialItem = specialItem;
        }
        AnimalTypes(int index, Item breeditem){
            this(index, breeditem, ItemStack.EMPTY.getItem());
        }
        public int getIndex(){
            return this.index;
        }
        public Item getSpecialItem(){
            return this.specialItem;
        }
        public Item getBreedItem(){
            return this.breeditem;
        }
        public static AnimalTypes fromIndex(int index) {
            for (AnimalTypes animalTypes : AnimalTypes.values()) {
                if (animalTypes.getIndex() == index) {
                    return animalTypes;
                }
            }
            throw new IllegalArgumentException("Invalid State index: " + index);
        }
    }
}
