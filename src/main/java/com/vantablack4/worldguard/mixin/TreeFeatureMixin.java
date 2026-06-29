package com.vantablack4.worldguard.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelWriter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;

import com.vantablack4.worldguard.hook.WorldGuardProtectionHooks;

@Mixin(TreeFeature.class)
public abstract class TreeFeatureMixin {
    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void mod_worldguard$denyProtectedTreeFeature(
        FeaturePlaceContext<TreeConfiguration> context,
        CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        if (WorldGuardProtectionHooks.deniesTreeFeatureGrowth(context.level(), context.origin())) {
            callbackInfo.setReturnValue(false);
        }
    }

    @Inject(method = "setBlockKnownShape", at = @At("HEAD"), cancellable = true)
    private static void mod_worldguard$denyProtectedGeneratedTreeBlock(
        LevelWriter level,
        BlockPos pos,
        BlockState state,
        CallbackInfo callbackInfo
    ) {
        if (WorldGuardProtectionHooks.deniesGeneratedTreeBlock(level, pos)) {
            callbackInfo.cancel();
        }
    }
}
