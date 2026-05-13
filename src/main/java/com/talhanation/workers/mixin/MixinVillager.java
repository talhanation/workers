package com.talhanation.workers.mixins;

import com.talhanation.workers.config.WorkersServerConfig;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Villager.class)
public abstract class MixinVillager {

    @Shadow
    private int foodLevel;

    /**
     * Extends vanilla eatAndHeal to accept ANY food item based on its saturation value.
     * Vanilla only accepts Bread (+4), Carrot/Potato/Beetroot (+1) using nutrition.
     *
     * With this mixin enabled:
     *   - Any food item adds its saturation value (rounded) to foodLevel
     *   - Villagers trigger breeding at foodLevel >= VillagerBreedSaturationThreshold (default 15)
     *   - Example: 3x Bread (sat 6.0 each) = 18 >= 15 → breed
     *   - Example: 2x Cooked Beef (sat 12.8 each) = 25 >= 15 → breed
     *
     * Can be disabled via config: VillagerBreedMixinEnabled = false
     */
    @Inject(method = "eatAndHeal", at = @At("HEAD"), cancellable = true)
    private void workers_eatAnyFood(ItemStack itemStack, CallbackInfo ci) {
        try {
            if (!WorkersServerConfig.VillagerBreedMixinEnabled.get()) return;
        } catch (Exception e) {
            // Config not ready yet during bootstrap - skip
            return;
        }

        FoodProperties food = itemStack.getItem().getFoodProperties();
        if (food == null) return;

        // Use saturation: nutrition * saturationModifier * 2 (matches vanilla player convention)
        float saturation = food.getNutrition() * food.getSaturationModifier() * 2.0f;
        int saturationInt = Math.max(1, Math.round(saturation));

        int threshold = WorkersServerConfig.VillagerBreedSaturationThreshold.get();

        this.foodLevel += saturationInt;
        this.foodLevel = Math.min(this.foodLevel, threshold);

        // Broadcast eating particles (event byte 13)
        Villager self = (Villager) (Object) this;
        self.level().broadcastEntityEvent(self, (byte) 13);

        ci.cancel();
    }
}
