package com.vantablack4.worldguard.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.NetherFungusBlock;
import net.minecraft.world.level.block.state.BlockState;

import com.vantablack4.worldguard.hook.WorldGuardProtectionHooks;

@Mixin(NetherFungusBlock.class)
public abstract class NetherFungusBlockMixin {
    @Inject(method = "performBonemeal", at = @At("HEAD"), cancellable = true)
    private void mod_worldguard$denyProtectedFungusTreeGrowth(
        ServerLevel level,
        RandomSource random,
        BlockPos pos,
        BlockState state,
        CallbackInfo callbackInfo
    ) {
        if (WorldGuardProtectionHooks.deniesTreeFeatureGrowth(level, pos)) {
            callbackInfo.cancel();
        }
    }
}
