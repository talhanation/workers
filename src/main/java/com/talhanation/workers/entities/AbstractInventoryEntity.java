package com.talhanation.workers.entities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractInventoryEntity extends TamableAnimal {

    protected SimpleContainer inventory = new SimpleContainer(18);

    public AbstractInventoryEntity(EntityType<? extends TamableAnimal> entityType, Level world) {
        super(entityType, world);
    }

    /////////////////////////////////// TICK/////////////////////////////////////////

    public void aiStep() {
        super.aiStep();
    }

    public void tick() {
        super.tick();
        if(this.getMainHandItem().isEmpty()) upgradeTool();
    }

    //////////////////////////////////// DATA////////////////////////////////////

    protected void defineSynchedData() {
        super.defineSynchedData();
    }

    public void addAdditionalSaveData(@NotNull CompoundTag nbt) {
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

    public void readAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        ListTag list = nbt.getList("Inventory", 10);
        for (int i = 0; i < list.size(); ++i) {
            CompoundTag compoundnbt = list.getCompound(i);
            int j = compoundnbt.getByte("Slot") & 255;

            this.inventory.setItem(j, ItemStack.of(compoundnbt));
        }
    }

    //////////////////////////////////// GET////////////////////////////////////

    public SimpleContainer getInventory() {
        return this.inventory;
    }
    /*
    public boolean hasFullInventory() {
        for (int i = 0; i < this.inventory.getContainerSize(); i++) {
            if (this.inventory.getItem(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }
    */

    ////////////////////////////////////SET////////////////////////////////////

    public void updateInventory(int slot, ItemStack stack) {
        this.inventory.setItem(slot, stack);
    }

    ////////////////////////////////////OTHER FUNCTIONS////////////////////////////////////

    public void upgradeTool() {
        for (int i = 0; i < this.inventory.getContainerSize(); i++) {
            ItemStack item = inventory.getItem(i);
            Item itemType = item.getItem();
            
            if (itemType instanceof AxeItem && this instanceof LumberjackEntity) {
                this.handleToolUpgrade(item, i);
            }

            if (itemType instanceof PickaxeItem && this instanceof MinerEntity) {
                this.handleToolUpgrade(item, i);
            }

            if (itemType instanceof HoeItem && this instanceof FarmerEntity) {
                this.handleToolUpgrade(item, i);
            }

            if (itemType instanceof FishingRodItem && this instanceof FishermanEntity) {
                this.handleToolUpgrade(item, i);
            }

            if ((itemType instanceof ShearsItem || itemType instanceof AxeItem)  && this instanceof ShepherdEntity) {
                this.handleToolUpgrade(item, i);
            }

            if (itemType instanceof AxeItem  && (this instanceof CattleFarmerEntity || this instanceof ChickenFarmerEntity || this instanceof SwineherdEntity))
                this.handleToolUpgrade(item, i);
        }
    }

    public void upgradeArmor() {
        for (int i = 0; i < this.inventory.getContainerSize(); i++) {
            ItemStack itemStack = inventory.getItem(i);

            if (itemStack.getItem() instanceof ArmorItem armorItem) {
                EquipmentSlot slot = armorItem.getSlot();
                this.setItemSlot(slot, itemStack);
            }
        }
    }

    private void handleToolUpgrade(ItemStack tool, int inventoryIndex) {
        if (tool.getDamageValue() >= tool.getMaxDamage()) {
            // Delete tool if it is broken
            this.inventory.setItem(inventoryIndex, ItemStack.EMPTY);
        } else {
            // Equip tool otherwise
            this.equipTool(tool);
        }
    }

    public void equipTool(ItemStack tool) {
        this.setItemInHand(InteractionHand.MAIN_HAND, tool);
        this.updateUsingItem(tool);

        if(this instanceof AbstractWorkerEntity worker){
            if(worker.isRequiredMainTool(tool)) worker.needsMainTool = false;
            if(worker.isRequiredSecondTool(tool)) worker.needsSecondTool = false;
        }
    }

    @Override
    public boolean equipItemIfPossible(ItemStack p_21541_) {
        return super.equipItemIfPossible(p_21541_);
    }

    public void die(@NotNull DamageSource dmg) {
        Containers.dropContents(level, this, inventory);
        super.die(dmg);
    }
}
