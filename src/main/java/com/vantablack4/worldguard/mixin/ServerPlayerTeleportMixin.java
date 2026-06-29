package com.vantablack4.worldguard.mixin;

import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.portal.TeleportTransition;

import com.vantablack4.worldguard.session.WorldGuardSessionHooks;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerTeleportMixin {
    @Inject(
        method = "teleport(Lnet/minecraft/world/level/portal/TeleportTransition;)Lnet/minecraft/server/level/ServerPlayer;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void mod_worldguard$denyProtectedTeleportTransition(
        TeleportTransition transition,
        CallbackInfoReturnable<ServerPlayer> callbackInfo
    ) {
        if (!WorldGuardSessionHooks.allowTeleport(
            (ServerPlayer) (Object) this,
            transition.newLevel(),
            transition.position().x,
            transition.position().y,
            transition.position().z
        )) {
            callbackInfo.setReturnValue(null);
        }
    }

    @Inject(
        method = "teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FFZ)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void mod_worldguard$denyProtectedTeleport(
        ServerLevel level,
        double x,
        double y,
        double z,
        Set<Relative> relatives,
        float yRot,
        float xRot,
        boolean shouldDismount,
        CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        if (!WorldGuardSessionHooks.allowTeleport((ServerPlayer) (Object) this, level, x, y, z)) {
            callbackInfo.setReturnValue(false);
        }
    }
}
