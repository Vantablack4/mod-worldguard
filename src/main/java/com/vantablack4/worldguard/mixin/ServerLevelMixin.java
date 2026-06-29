package com.vantablack4.worldguard.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import com.vantablack4.worldguard.hook.WorldGuardProtectionHooks;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @Inject(method = "addEntity", at = @At("HEAD"), cancellable = true)
    private void mod_worldguard$denyProtectedMobSpawning(Entity entity, CallbackInfoReturnable<Boolean> callbackInfo) {
        if (WorldGuardProtectionHooks.deniesMobSpawn((ServerLevel) (Object) this, entity)) {
            callbackInfo.setReturnValue(false);
        }
    }
}
