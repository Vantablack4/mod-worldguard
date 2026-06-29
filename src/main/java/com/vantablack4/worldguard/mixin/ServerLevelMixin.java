package com.vantablack4.worldguard.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;

import com.vantablack4.worldguard.hook.WorldGuardProtectionHooks;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @Inject(method = "addEntity", at = @At("HEAD"), cancellable = true)
    private void mod_worldguard$denyProtectedMobSpawning(Entity entity, CallbackInfoReturnable<Boolean> callbackInfo) {
        if (WorldGuardProtectionHooks.deniesMobSpawn((ServerLevel) (Object) this, entity)) {
            callbackInfo.setReturnValue(false);
        }
    }

    @Redirect(
        method = "tickPrecipitation",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z",
            ordinal = 0
        )
    )
    private boolean mod_worldguard$denyProtectedIceForm(ServerLevel level, BlockPos pos, BlockState state) {
        return !WorldGuardProtectionHooks.deniesPrecipitationIce(level, pos) && level.setBlockAndUpdate(pos, state);
    }

    @Redirect(
        method = "tickPrecipitation",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z",
            ordinal = 1
        )
    )
    private boolean mod_worldguard$denyProtectedSnowAccumulation(ServerLevel level, BlockPos pos, BlockState state) {
        return !WorldGuardProtectionHooks.deniesPrecipitationSnow(level, pos) && level.setBlockAndUpdate(pos, state);
    }

    @Redirect(
        method = "tickPrecipitation",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z",
            ordinal = 2
        )
    )
    private boolean mod_worldguard$denyProtectedSnowFall(ServerLevel level, BlockPos pos, BlockState state) {
        return !WorldGuardProtectionHooks.deniesPrecipitationSnow(level, pos) && level.setBlockAndUpdate(pos, state);
    }
}
