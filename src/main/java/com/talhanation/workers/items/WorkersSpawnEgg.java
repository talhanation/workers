package com.talhanation.workers.items;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.items.RecruitsSpawnEgg;
import com.talhanation.workers.entities.MerchantEntity;
import com.talhanation.workers.world.WorkersMerchantTrade;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.function.Supplier;

public class WorkersSpawnEgg extends RecruitsSpawnEgg {
    public WorkersSpawnEgg(Supplier<? extends EntityType<? extends AbstractRecruitEntity>> entityType, int primaryColor, int secondaryColor, Properties properties) {
        super(entityType, primaryColor, secondaryColor, properties);
    }
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        } else {
            ItemStack stack = context.getItemInHand();
            BlockPos pos = context.getClickedPos();
            EntityType<?> entitytype = this.getType(stack.getTag());
            Entity entity = entitytype.create(world);
            CompoundTag entityTag = stack.getTag();
            if (entity instanceof AbstractRecruitEntity) {
                AbstractRecruitEntity recruit = (AbstractRecruitEntity)entity;
                if (entityTag != null) {
                    if(entity instanceof MerchantEntity merchant){
                        fillMerchant(merchant, entityTag, pos);
                    }
                    else fillRecruit(recruit, entityTag, pos);
                    world.addFreshEntity(recruit);
                    if (!context.getPlayer().isCreative()) {
                        stack.shrink(1);
                    }

                    return InteractionResult.SUCCESS;
                }
            }

            return super.useOn(context);
        }
    }

    public void fillMerchant(MerchantEntity merchant, CompoundTag entityTag, BlockPos pos){
        fillRecruit(merchant, entityTag, pos);

        entityTag.put("Trades", WorkersMerchantTrade.listToNbt(merchant.getTrades()));
        entityTag.putBoolean("isCreative", merchant.isCreative());
        entityTag.putInt("TraderProgress", merchant.getTraderProgress());
        entityTag.putInt("TraderLevel", merchant.getTraderLevel());
    }
}
