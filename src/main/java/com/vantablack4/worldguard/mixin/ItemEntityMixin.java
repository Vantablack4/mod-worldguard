package com.vantablack4.worldguard.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;

import com.vantablack4.worldguard.session.WorldGuardSessionHooks;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void mod_worldguard$denyProtectedItemPickup(Player player, CallbackInfo callbackInfo) {
        if (player instanceof ServerPlayer serverPlayer
            && WorldGuardSessionHooks.denyItemPickup(serverPlayer, (ItemEntity) (Object) this)) {
            callbackInfo.cancel();
        }
    }
}
