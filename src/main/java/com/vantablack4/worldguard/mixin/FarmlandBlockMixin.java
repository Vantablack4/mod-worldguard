package com.vantablack4.worldguard.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;

import com.vantablack4.worldguard.hook.WorldGuardProtectionHooks;

@Mixin(FarmlandBlock.class)
public abstract class FarmlandBlockMixin {
    @Redirect(
        method = "fallOn",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/FarmlandBlock;turnToDirt(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"
        )
    )
    private void mod_worldguard$denyProtectedTrample(
        Entity entity,
        BlockState state,
        Level level,
        BlockPos pos
    ) {
        if (!WorldGuardProtectionHooks.deniesTrample(level, entity, pos)) {
            FarmlandBlock.turnToDirt(entity, state, level, pos);
        }
    }

    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void mod_worldguard$denyProtectedDrying(
        BlockState state,
        ServerLevel level,
        BlockPos pos,
        RandomSource random,
        CallbackInfo callbackInfo
    ) {
        if (WorldGuardProtectionHooks.deniesFarmlandDry(level, pos)) {
            callbackInfo.cancel();
        }
    }
}
