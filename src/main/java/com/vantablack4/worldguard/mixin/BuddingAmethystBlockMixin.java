package com.vantablack4.worldguard.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BuddingAmethystBlock;
import net.minecraft.world.level.block.state.BlockState;

import com.vantablack4.worldguard.hook.WorldGuardProtectionHooks;

@Mixin(BuddingAmethystBlock.class)
public abstract class BuddingAmethystBlockMixin {
    @Redirect(
        method = "randomTick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"
        )
    )
    private boolean mod_worldguard$denyProtectedAmethystGrowth(ServerLevel level, BlockPos pos, BlockState state) {
        return !WorldGuardProtectionHooks.deniesRockGrowth(level, pos) && level.setBlockAndUpdate(pos, state);
    }
}
