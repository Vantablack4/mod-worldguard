package com.vantablack4.worldguard.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;

import com.vantablack4.worldguard.hook.WorldGuardProtectionHooks;

@Mixin(EnderDragon.class)
public abstract class EnderDragonMixin {
    @Redirect(
        method = "checkWalls",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z"
        )
    )
    private boolean mod_worldguard$denyProtectedDragonBlockDamage(
        ServerLevel level,
        BlockPos pos,
        boolean moving
    ) {
        return !WorldGuardProtectionHooks.deniesEnderDragonBlockDamage(level, pos)
            && level.removeBlock(pos, moving);
    }
}
