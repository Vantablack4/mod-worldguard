package com.vantablack4.worldguard.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.MangrovePropaguleBlock;
import net.minecraft.world.level.block.state.BlockState;

import com.vantablack4.worldguard.hook.WorldGuardProtectionHooks;

@Mixin(MangrovePropaguleBlock.class)
public abstract class MangrovePropaguleBlockMixin {
    @Redirect(
        method = {
            "randomTick",
            "performBonemeal"
        },
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"
        )
    )
    private boolean mod_worldguard$denyProtectedHangingPropaguleGrowth(
        ServerLevel level,
        BlockPos pos,
        BlockState state,
        int flags
    ) {
        return !WorldGuardProtectionHooks.deniesCropGrowth(level, pos) && level.setBlock(pos, state, flags);
    }
}
