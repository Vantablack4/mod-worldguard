package com.vantablack4.worldguard.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;

import com.vantablack4.worldguard.hook.WorldGuardProtectionHooks;

@Mixin(PointedDripstoneBlock.class)
public abstract class PointedDripstoneBlockMixin {
    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void mod_worldguard$denyProtectedRockGrowth(
        BlockState state,
        ServerLevel level,
        BlockPos pos,
        RandomSource random,
        CallbackInfo callbackInfo
    ) {
        if (WorldGuardProtectionHooks.deniesRockGrowth(level, pos)) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "createDripstone", at = @At("HEAD"), cancellable = true)
    private static void mod_worldguard$denyProtectedRockGrowthTarget(
        LevelAccessor level,
        BlockPos pos,
        Direction direction,
        DripstoneThickness thickness,
        CallbackInfo callbackInfo
    ) {
        if (WorldGuardProtectionHooks.deniesRockGrowth(level, pos)) {
            callbackInfo.cancel();
        }
    }
}
