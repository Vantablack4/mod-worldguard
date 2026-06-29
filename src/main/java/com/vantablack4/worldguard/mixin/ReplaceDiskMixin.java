package com.vantablack4.worldguard.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.enchantment.effects.ReplaceDisk;
import net.minecraft.world.level.block.state.BlockState;

import com.vantablack4.worldguard.hook.WorldGuardProtectionHooks;

@Mixin(ReplaceDisk.class)
public abstract class ReplaceDiskMixin {
    @Redirect(
        method = "apply",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"
        )
    )
    private boolean mod_worldguard$denyProtectedFrostedIceForm(ServerLevel level, BlockPos pos, BlockState state) {
        return !WorldGuardProtectionHooks.deniesFrostedIceForm(level, pos, state) && level.setBlockAndUpdate(pos, state);
    }
}
