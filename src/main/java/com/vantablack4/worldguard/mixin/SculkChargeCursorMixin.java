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

@Mixin(targets = "net.minecraft.world.level.block.SculkSpreader$ChargeCursor")
public abstract class SculkChargeCursorMixin {
    @Shadow
    private BlockPos pos;

    @Inject(method = "update", at = @At("HEAD"), cancellable = true)
    private void mod_worldguard$denyProtectedSculkCursorTarget(
        LevelAccessor level,
        BlockPos catalystPos,
        RandomSource random,
        SculkSpreader spreader,
        boolean shouldConvertBlocks,
        CallbackInfo callbackInfo
    ) {
        if (this.pos != null && WorldGuardProtectionHooks.deniesSculkGrowth(level, this.pos)) {
            callbackInfo.cancel();
        }
    }
}
