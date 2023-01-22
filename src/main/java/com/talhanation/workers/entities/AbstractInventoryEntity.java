package com.talhanation.workers.entities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

public abstract class AbstractInventoryEntity extends TamableAnimal {

    private final SimpleContainer inventory = new SimpleContainer(18);

    public AbstractInventoryEntity(EntityType<? extends TamableAnimal> entityType, Level world) {
        super(entityType, world);
    }

    ///////////////////////////////////TICK/////////////////////////////////////////

    public void aiStep() {
        super.aiStep();
    }

    public void tick() {
        super.tick();
    }

    ////////////////////////////////////DATA////////////////////////////////////


    protected void defineSynchedData() {
        super.defineSynchedData();
    }

    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        ListTag list = new ListTag();
        for (int i = 0; i < this.inventory.getContainerSize(); ++i) {
            ItemStack itemstack = this.inventory.getItem(i);
            if (!itemstack.isEmpty()) {
                CompoundTag compoundnbt = new CompoundTag();
                compoundnbt.putByte("Slot", (byte) i);
                itemstack.save(compoundnbt);
                list.add(compoundnbt);
            }
        }

        nbt.put("Inventory", list);
    }

    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        ListTag list = nbt.getList("Inventory", 10);
        for (int i = 0; i < list.size(); ++i) {
            CompoundTag compoundnbt = list.getCompound(i);
            int j = compoundnbt.getByte("Slot") & 255;

            this.inventory.setItem(j, ItemStack.of(compoundnbt));
        }
    }


    ////////////////////////////////////GET////////////////////////////////////

    public SimpleContainer getInventory() {
        return this.inventory;
    }


    ////////////////////////////////////SET////////////////////////////////////


    ////////////////////////////////////OTHER FUNCTIONS////////////////////////////////////

    public void upgradeTool(){
        for(int i = 0; i < this.inventory.getContainerSize(); i++){
            ItemStack firstTool = inventory.getItem(i);

            if (firstTool.getItem() instanceof AxeItem && this instanceof LumberjackEntity){
                this.setItemInHand(InteractionHand.MAIN_HAND, firstTool);
                break;
            }

            if (firstTool.getItem() instanceof PickaxeItem && this instanceof MinerEntity){
                this.setItemInHand(InteractionHand.MAIN_HAND, firstTool);
                break;
            }

            if (firstTool.getItem() instanceof HoeItem && this instanceof FarmerEntity){
                this.setItemInHand(InteractionHand.MAIN_HAND, firstTool);
                break;
            }

            if (firstTool.getItem() instanceof FishingRodItem && this instanceof FishermanEntity){
                this.setItemInHand(InteractionHand.MAIN_HAND, firstTool);
                break;
            }
        }
    }

    public void die(DamageSource dmg) {
        super.die(dmg);
        for (int i = 0; i < this.inventory.getContainerSize(); i++)
        Containers.dropItemStack(this.level, getX(), getY(), getZ(), this.inventory.getItem(i));
    }
}
