package com.talhanation.workers.network;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.network.MessageWriteSpawnEgg;
import com.talhanation.workers.entities.MerchantEntity;
import com.talhanation.workers.init.ModItems;
import com.talhanation.workers.world.WorkersMerchantTrade;
import net.minecraft.world.entity.EntityType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public class WorkersMessageWriteSpawnEgg extends MessageWriteSpawnEgg {

    public CompoundTag fillRecruitsInfo(CompoundTag entityTag, AbstractRecruitEntity recruitEntity) {
        entityTag = super.fillRecruitsInfo(entityTag, recruitEntity);

        if(recruitEntity instanceof MerchantEntity merchant){
            entityTag.putBoolean("isCreative", merchant.isCreative());
            entityTag.put("Trades", WorkersMerchantTrade.listToNbt(merchant.getTrades()));
            entityTag.putInt("TraderProgress", merchant.getTraderProgress());
            entityTag.putInt("TraderLevel", merchant.getTraderLevel());
        }

        return entityTag;
    }

    public ItemStack getItemStack(EntityType type){
        ItemStack itemStack = super.getItemStack(type);
        if(itemStack.isEmpty()) {
            if (type.getDescriptionId().equals("entity.workers.merchant")) {
                itemStack = new ItemStack(ModItems.MERCHANT_SPAWN_EGG.get());
            }
        }
        return itemStack;

    }

}

