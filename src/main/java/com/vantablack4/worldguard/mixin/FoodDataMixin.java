package com.vantablack4.worldguard.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.objectweb.asm.Opcodes;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;

import com.vantablack4.worldguard.session.WorldGuardSessionHooks;

@Mixin(FoodData.class)
public abstract class FoodDataMixin {
    @Shadow
    private int foodLevel;

    @Redirect(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Ljava/lang/Boolean;booleanValue()Z"
        )
    )
    private boolean mod_worldguard$allowNaturalFoodDataRegen(Boolean vanillaRule, ServerPlayer player) {
        return vanillaRule.booleanValue() && !WorldGuardSessionHooks.deniesNaturalHealthRegen(player);
    }

    @Redirect(
        method = "tick",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/world/food/FoodData;foodLevel:I",
            opcode = Opcodes.PUTFIELD,
            ordinal = 0
        )
    )
    private void mod_worldguard$skipDeniedNaturalFoodLevelDrain(
        FoodData foodData,
        int newFoodLevel,
        ServerPlayer player
    ) {
        if (newFoodLevel < foodLevel && WorldGuardSessionHooks.deniesNaturalHungerDrain(player)) {
            return;
        }
        foodLevel = newFoodLevel;
    }
}
