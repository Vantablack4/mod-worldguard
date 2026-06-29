package com.vantablack4.worldguard.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.LavaFluid;

import com.vantablack4.worldguard.hook.WorldGuardProtectionHooks;

@Mixin(LavaFluid.class)
public abstract class LavaFluidMixin {
    @Redirect(
        method = "randomTick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"
        )
    )
    private boolean mod_worldguard$denyProtectedLavaFire(ServerLevel level, BlockPos pos, BlockState state) {
        return !WorldGuardProtectionHooks.deniesLavaFire(level, pos) && level.setBlockAndUpdate(pos, state);
    }
}
