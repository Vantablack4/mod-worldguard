package com.vantablack4.worldguard.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.CaveVinesBlock;
import net.minecraft.world.level.block.CaveVinesPlantBlock;
import net.minecraft.world.level.block.state.BlockState;

import com.vantablack4.worldguard.hook.WorldGuardProtectionHooks;

@Mixin({
    CaveVinesBlock.class,
    CaveVinesPlantBlock.class
})
public abstract class CaveVinesBerryGrowthMixin {
    @Redirect(
        method = "performBonemeal",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"
        )
    )
    private boolean mod_worldguard$denyProtectedCaveVinesBerryGrowth(
        ServerLevel level,
        BlockPos pos,
        BlockState state,
        int flags
    ) {
        return !WorldGuardProtectionHooks.deniesGrowingPlantGrowth(level, pos, state)
            && level.setBlock(pos, state, flags);
    }
}
