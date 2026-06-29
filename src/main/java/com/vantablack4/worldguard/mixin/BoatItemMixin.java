package com.vantablack4.worldguard.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import com.vantablack4.worldguard.hook.WorldGuardProtectionHooks;

@Mixin(BoatItem.class)
public abstract class BoatItemMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void mod_worldguard$denyProtectedVehiclePlace(
        Level level,
        Player player,
        InteractionHand hand,
        CallbackInfoReturnable<InteractionResult> callbackInfo
    ) {
        if (!(player instanceof ServerPlayer serverPlayer) || level.isClientSide()) {
            return;
        }

        BlockHitResult hit = playerPovHitResult(level, player, ClipContext.Fluid.ANY);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        if (WorldGuardProtectionHooks.deniesVehiclePlace(serverPlayer, level, BlockPos.containing(hit.getLocation()))) {
            callbackInfo.setReturnValue(InteractionResult.FAIL);
        }
    }

    private static BlockHitResult playerPovHitResult(Level level, Player player, ClipContext.Fluid fluidMode) {
        var start = player.getEyePosition();
        var end = start.add(player.calculateViewVector(player.getXRot(), player.getYRot()).scale(player.blockInteractionRange()));
        return level.clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, fluidMode, player));
    }
}
