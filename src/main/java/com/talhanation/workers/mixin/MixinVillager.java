package com.talhanation.workers.mixin;

import com.talhanation.workers.config.WorkersServerConfig;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Villager.class)
public abstract class MixinVillager {

    @Shadow private int foodLevel;

    /** Injected into every Villager instance. Tracks the game-time of the last breed event. */
    @Unique private long workers_lastBreedGameTime = -1L;

    // ── canBreed ──────────────────────────────────────────────────────────────

    /**
     * Replaces the breed check:
     *  - Any food item counts, saturation-based
     *  - Hard cooldown of one breed per game-day (24 000 ticks) per villager
     */
    @Inject(method = "canBreed", at = @At("HEAD"), cancellable = true)
    private void workers_canBreed(CallbackInfoReturnable<Boolean> cir) {
        try {
            if (!WorkersServerConfig.VillagerBreedMixinEnabled.get()) return;
        } catch (Exception e) {
            return;
        }

        Villager self      = (Villager) (Object) this;
        int      threshold = WorkersServerConfig.VillagerBreedSaturationThreshold.get();

        // Once-per-day cooldown
        long gameTime = self.level().getGameTime();
        if (workers_lastBreedGameTime >= 0 && gameTime - workers_lastBreedGameTime < 3000L) {
            cir.setReturnValue(false);
            return;
        }

        boolean result = workers_countSaturationInInventory(self.getInventory()) >= threshold
                && !self.isSleeping()
                && self.getAge() == 0;

        cir.setReturnValue(result);
    }

    // ── wantsMoreFood ─────────────────────────────────────────────────────────

    /**
     * Replaces wantsMoreFood() so the Brain triggers eating for any food item,
     * not just the 4 vanilla FOOD_POINTS items.
     */
    @Inject(method = "wantsMoreFood", at = @At("HEAD"), cancellable = true)
    private void workers_wantsMoreFood(CallbackInfoReturnable<Boolean> cir) {
        try {
            if (!WorkersServerConfig.VillagerBreedMixinEnabled.get()) return;
        } catch (Exception e) {
            return;
        }

        int threshold = WorkersServerConfig.VillagerBreedSaturationThreshold.get();
        cir.setReturnValue(
                workers_countSaturationInInventory(((Villager) (Object) this).getInventory()) < threshold
        );
    }

    // ── eatAndDigestFood ──────────────────────────────────────────────────────

    /**
     * Replaces eatAndDigestFood() to consume any food item by saturation value.
     * Sets workers_lastBreedGameTime only when food was actually consumed, so
     * the once-per-day cooldown is not triggered by empty calls.
     * Cancels before vanilla eatUntilFull() + digestFood(12) run, preventing
     * foodLevel from going negative when threshold > 12.
     */
    @Inject(method = "eatAndDigestFood", at = @At("HEAD"), cancellable = true)
    private void workers_eatAndDigestFood(CallbackInfo ci) {
        try {
            if (!WorkersServerConfig.VillagerBreedMixinEnabled.get()) return;
        } catch (Exception e) {
            return;
        }

        Villager        self      = (Villager) (Object) this;
        int             threshold = WorkersServerConfig.VillagerBreedSaturationThreshold.get();
        SimpleContainer inv       = self.getInventory();
        int             consumed  = 0;

        for (int i = 0; i < inv.getContainerSize() && consumed < threshold; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;

            FoodProperties food = stack.getItem().getFoodProperties();
            if (food == null) continue;

            int satInt = workers_saturationOf(food);

            while (consumed < threshold) {
                if (inv.getItem(i).isEmpty()) break;
                inv.removeItem(i, 1);
                consumed += satInt;
            }
        }

        // Only start the cooldown if food was actually consumed (= real breed event)
        if (consumed > 0) {
            this.workers_lastBreedGameTime = self.level().getGameTime();
        }

        this.foodLevel = 0;
        ci.cancel();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static int workers_countSaturationInInventory(SimpleContainer inv) {
        int total = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;
            FoodProperties food = stack.getItem().getFoodProperties();
            if (food == null) continue;
            total += workers_saturationOf(food) * stack.getCount();
        }
        return total;
    }

    private static int workers_saturationOf(FoodProperties food) {
        float sat = food.getNutrition() * food.getSaturationModifier() * 2.0f;
        return Math.max(1, Math.round(sat));
    }
}