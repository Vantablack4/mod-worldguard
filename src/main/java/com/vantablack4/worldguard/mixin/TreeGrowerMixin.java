package com.vantablack4.worldguard.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;

import com.vantablack4.worldguard.hook.WorldGuardProtectionHooks;

@Mixin(TreeGrower.class)
public abstract class TreeGrowerMixin {
    @Inject(method = "growTree", at = @At("HEAD"), cancellable = true)
    private void mod_worldguard$denyProtectedTreeGrowth(
        ServerLevel level,
        ChunkGenerator generator,
        BlockPos pos,
        BlockState state,
        RandomSource random,
        CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        if (WorldGuardProtectionHooks.deniesTreeFeatureGrowth(level, pos)) {
            callbackInfo.setReturnValue(false);
        }
    }
}
