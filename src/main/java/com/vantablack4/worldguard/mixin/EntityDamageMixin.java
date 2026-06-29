package com.vantablack4.worldguard.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;

import com.vantablack4.worldguard.hook.WorldGuardProtectionHooks;

@Mixin(Entity.class)
public abstract class EntityDamageMixin {
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void mod_worldguard$denyProtectedNonLivingHurt(
        DamageSource source,
        float amount,
        CallbackInfo callbackInfo
    ) {
        if (WorldGuardProtectionHooks.deniesNonLivingDamage((Entity) (Object) this, source)) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "hurtOrSimulate", at = @At("HEAD"), cancellable = true)
    private void mod_worldguard$denyProtectedNonLivingHurtOrSimulate(
        DamageSource source,
        float amount,
        CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        if (WorldGuardProtectionHooks.deniesNonLivingDamage((Entity) (Object) this, source)) {
            callbackInfo.setReturnValue(false);
        }
    }
}
