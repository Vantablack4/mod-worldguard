package com.vantablack4.worldguard.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.MinecartItem;
import net.minecraft.world.item.context.UseOnContext;

import com.vantablack4.worldguard.hook.WorldGuardProtectionHooks;

@Mixin(MinecartItem.class)
public abstract class MinecartItemMixin {
    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void mod_worldguard$denyProtectedVehiclePlace(
        UseOnContext context,
        CallbackInfoReturnable<InteractionResult> callbackInfo
    ) {
        if (context.getPlayer() instanceof ServerPlayer serverPlayer
            && WorldGuardProtectionHooks.deniesVehiclePlace(serverPlayer, context.getLevel(), context.getClickedPos())) {
            callbackInfo.setReturnValue(InteractionResult.FAIL);
        }
    }
}
