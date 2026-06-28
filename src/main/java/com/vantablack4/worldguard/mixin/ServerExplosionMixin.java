package com.vantablack4.worldguard.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ServerExplosion;

import com.vantablack4.worldguard.hook.WorldGuardProtectionHooks;

@Mixin(ServerExplosion.class)
public abstract class ServerExplosionMixin {
    @Shadow
    @Final
    private ServerLevel level;

    @Shadow
    @Final
    private Entity source;

    @Inject(method = "interactWithBlocks", at = @At("HEAD"))
    private void mod_worldguard$filterProtectedExplosionBlocks(List<BlockPos> positions, CallbackInfo callbackInfo) {
        WorldGuardProtectionHooks.removeExplosionBlockDamageDenied(level, source, positions);
    }

    @Inject(method = "createFire", at = @At("HEAD"))
    private void mod_worldguard$filterProtectedExplosionFire(List<BlockPos> positions, CallbackInfo callbackInfo) {
        WorldGuardProtectionHooks.removeExplosionFireDenied(level, source, positions);
    }
}
