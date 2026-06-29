package com.vantablack4.worldguard.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.server.level.ServerPlayer;

import com.vantablack4.worldguard.session.WorldGuardSessionHooks;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerRegenerationMixin {
    @Redirect(
        method = "tickRegeneration",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;heal(F)V"
        )
    )
    private void mod_worldguard$skipDeniedPeacefulNaturalRegen(ServerPlayer player, float amount) {
        if (!WorldGuardSessionHooks.deniesNaturalHealthRegen(player)) {
            player.heal(amount);
        }
    }
}
