package com.vantablack4.worldguard.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.TeleportTransition;

import com.vantablack4.worldguard.hook.WorldGuardProtectionHooks;

@Mixin(Entity.class)
public abstract class EntityTeleportMixin {
    @Inject(method = "teleport", at = @At("HEAD"), cancellable = true)
    private void mod_worldguard$denyProtectedEntityTeleport(
        TeleportTransition transition,
        CallbackInfoReturnable<Entity> callbackInfo
    ) {
        Entity entity = (Entity) (Object) this;
        if (!(entity instanceof ServerPlayer) && WorldGuardProtectionHooks.deniesEntityTeleport(entity, transition)) {
            callbackInfo.setReturnValue(null);
        }
    }
}
