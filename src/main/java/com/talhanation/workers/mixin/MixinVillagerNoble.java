package com.talhanation.workers.mixin;

import com.talhanation.recruits.entities.VillagerNobleEntity;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = VillagerNobleEntity.class, remap = false)
public abstract class MixinVillagerNoble {

    @Inject(method = "Lcom/talhanation/recruits/entities/VillagerNobleEntity;doTrade(Lnet/minecraft/resources/ResourceLocation;)V", at = @At("TAIL"))
    private void workers_doTrade(ResourceLocation resourceLocation, CallbackInfo ci) {
        if (resourceLocation == null || !"workers".equals(resourceLocation.getNamespace())) return;

        int bonus = 60 - 15;
        ((VillagerNobleEntity) (Object) this).addTraderProgress(bonus);
    }
}