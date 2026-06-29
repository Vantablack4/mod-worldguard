package com.vantablack4.worldguard.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.phys.Vec3;

import com.vantablack4.worldguard.hook.WorldGuardProtectionHooks;

@Mixin(ExperienceOrb.class)
public abstract class ExperienceOrbMixin {
    @Inject(method = "awardWithDirection", at = @At("HEAD"), cancellable = true)
    private static void mod_worldguard$denyProtectedExperienceDrop(
        ServerLevel level,
        Vec3 pos,
        Vec3 direction,
        int amount,
        CallbackInfo callbackInfo
    ) {
        if (amount > 0 && WorldGuardProtectionHooks.deniesExperienceDrop(level, pos)) {
            callbackInfo.cancel();
        }
    }
}
