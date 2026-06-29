package com.vantablack4.worldguard.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.level.block.state.BlockState;

import com.vantablack4.worldguard.hook.WorldGuardProtectionHooks;

@Mixin(MushroomBlock.class)
public abstract class MushroomBlockMixin {
    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void mod_worldguard$denyProtectedMushroomGrowth(
        BlockState state,
        ServerLevel level,
        BlockPos pos,
        RandomSource random,
        CallbackInfo callbackInfo
    ) {
        if (WorldGuardProtectionHooks.deniesMushroomGrowth(level, pos)) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "growMushroom", at = @At("HEAD"), cancellable = true)
    private void mod_worldguard$denyProtectedHugeMushroomGrowth(
        ServerLevel level,
        BlockPos pos,
        BlockState state,
        RandomSource random,
        CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        if (WorldGuardProtectionHooks.deniesMushroomGrowth(level, pos)) {
            callbackInfo.setReturnValue(false);
        }
    }

    @Redirect(
        method = "randomTick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"
        )
    )
    private boolean mod_worldguard$denyProtectedMushroomSpreadTarget(
        ServerLevel level,
        BlockPos pos,
        BlockState state,
        int flags
    ) {
        return !WorldGuardProtectionHooks.deniesMushroomGrowth(level, pos) && level.setBlock(pos, state, flags);
    }
}
