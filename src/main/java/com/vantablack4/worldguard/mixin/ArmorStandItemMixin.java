package com.vantablack4.worldguard.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ArmorStandItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;

import com.vantablack4.worldguard.hook.WorldGuardProtectionHooks;

@Mixin(ArmorStandItem.class)
public abstract class ArmorStandItemMixin {
    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void mod_worldguard$denyProtectedEntityPlace(
        UseOnContext context,
        CallbackInfoReturnable<InteractionResult> callbackInfo
    ) {
        if (context.getClickedFace() == Direction.DOWN || !(context.getPlayer() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        BlockPos target = new BlockPlaceContext(context).getClickedPos();
        if (WorldGuardProtectionHooks.deniesEntityPlace(serverPlayer, context.getLevel(), target)) {
            callbackInfo.setReturnValue(InteractionResult.FAIL);
        }
    }
}
