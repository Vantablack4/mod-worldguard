package com.vantablack4.worldguard.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.SculkSpreader;

import com.vantablack4.worldguard.hook.WorldGuardProtectionHooks;

@Mixin(SculkSpreader.class)
public abstract class SculkSpreaderMixin {
    @Shadow
    public abstract boolean isWorldGeneration();

    @Inject(method = "updateCursors", at = @At("HEAD"), cancellable = true)
    private void mod_worldguard$denyProtectedSculkGrowth(
        LevelAccessor level,
        BlockPos catalystPos,
        RandomSource random,
        boolean shouldConvertBlocks,
        CallbackInfo callbackInfo
    ) {
        if (!isWorldGeneration() && WorldGuardProtectionHooks.deniesSculkGrowth(level, catalystPos)) {
            callbackInfo.cancel();
        }
    }
}
