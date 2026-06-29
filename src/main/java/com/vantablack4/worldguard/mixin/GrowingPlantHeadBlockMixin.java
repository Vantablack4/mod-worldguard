package com.vantablack4.worldguard.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.state.BlockState;

import com.vantablack4.worldguard.hook.WorldGuardProtectionHooks;

@Mixin(GrowingPlantHeadBlock.class)
public abstract class GrowingPlantHeadBlockMixin {
    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void mod_worldguard$denyProtectedGrowingPlant(
        BlockState state,
        ServerLevel level,
        BlockPos pos,
        RandomSource random,
        CallbackInfo callbackInfo
    ) {
        if (WorldGuardProtectionHooks.deniesVineGrowth(level, pos)) {
            callbackInfo.cancel();
        }
    }

    @Redirect(
        method = {
            "randomTick",
            "performBonemeal"
        },
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"
        )
    )
    private boolean mod_worldguard$denyProtectedGrowingPlantTarget(
        ServerLevel level,
        BlockPos pos,
        BlockState state
    ) {
        return !WorldGuardProtectionHooks.deniesGrowingPlantGrowth(level, pos, state)
            && level.setBlockAndUpdate(pos, state);
    }
}
