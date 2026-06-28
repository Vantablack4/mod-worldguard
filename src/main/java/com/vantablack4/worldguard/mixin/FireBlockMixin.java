package com.vantablack4.worldguard.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;

import com.vantablack4.worldguard.hook.WorldGuardProtectionHooks;

@Mixin(FireBlock.class)
public abstract class FireBlockMixin {
    @Inject(method = "checkBurnOut", at = @At("HEAD"), cancellable = true)
    private void mod_worldguard$denyProtectedBurnOut(
        Level level,
        BlockPos pos,
        int chance,
        RandomSource random,
        int age,
        CallbackInfo callbackInfo
    ) {
        if (WorldGuardProtectionHooks.deniesFireMutation(level, pos)) {
            callbackInfo.cancel();
        }
    }

    @Redirect(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z",
            ordinal = 1
        )
    )
    private boolean mod_worldguard$denyProtectedFireSpread(
        ServerLevel level,
        BlockPos pos,
        BlockState state,
        int flags
    ) {
        return !WorldGuardProtectionHooks.deniesFireMutation(level, pos) && level.setBlock(pos, state, flags);
    }
}
