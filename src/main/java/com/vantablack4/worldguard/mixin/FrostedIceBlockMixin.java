package com.vantablack4.worldguard.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.FrostedIceBlock;
import net.minecraft.world.level.block.state.BlockState;

import com.vantablack4.worldguard.hook.WorldGuardProtectionHooks;

@Mixin(FrostedIceBlock.class)
public abstract class FrostedIceBlockMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void mod_worldguard$denyProtectedFrostedIceMelt(
        BlockState state,
        ServerLevel level,
        BlockPos pos,
        RandomSource random,
        CallbackInfo callbackInfo
    ) {
        if (WorldGuardProtectionHooks.deniesFrostedIceMelt(level, pos)) {
            callbackInfo.cancel();
        }
    }
}
