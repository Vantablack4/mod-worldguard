package com.vantablack4.worldguard.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.EndCrystalItem;
import net.minecraft.world.item.context.UseOnContext;

import com.vantablack4.worldguard.hook.WorldGuardProtectionHooks;

@Mixin(EndCrystalItem.class)
public abstract class EndCrystalItemMixin {
    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void mod_worldguard$denyProtectedEntityPlace(
        UseOnContext context,
        CallbackInfoReturnable<InteractionResult> callbackInfo
    ) {
        if (!(context.getPlayer() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        BlockPos target = context.getClickedPos().above();
        if (WorldGuardProtectionHooks.deniesEntityPlace(serverPlayer, context.getLevel(), target)) {
            callbackInfo.setReturnValue(InteractionResult.FAIL);
        }
    }
}
