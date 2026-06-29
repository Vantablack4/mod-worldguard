package com.vantablack4.worldguard.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.wither.WitherBoss;

import com.vantablack4.worldguard.hook.WorldGuardProtectionHooks;

@Mixin(WitherBoss.class)
public abstract class WitherBossMixin {
    @Redirect(
        method = "customServerAiStep",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;)Z"
        )
    )
    private boolean mod_worldguard$denyProtectedWitherBlockDamage(
        ServerLevel level,
        BlockPos pos,
        boolean drop,
        Entity entity
    ) {
        return !WorldGuardProtectionHooks.deniesWitherBlockDamage(level, pos)
            && level.destroyBlock(pos, drop, entity, 512);
    }
}
