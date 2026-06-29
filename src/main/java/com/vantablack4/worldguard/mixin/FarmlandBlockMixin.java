package com.vantablack4.worldguard.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;

import com.vantablack4.worldguard.hook.WorldGuardProtectionHooks;

@Mixin(FarmlandBlock.class)
public abstract class FarmlandBlockMixin {
    @Inject(
        method = "turnToDirt(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void mod_worldguard$denyProtectedTurnToDirt(
        Entity entity,
        BlockState state,
        Level level,
        BlockPos pos,
        CallbackInfo callbackInfo
    ) {
        if (WorldGuardProtectionHooks.deniesFarmlandTurnToDirt(level, entity, pos)) {
            callbackInfo.cancel();
        }
    }

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
        if (!WorldGuardProtectionHooks.deniesFarmlandTurnToDirt(level, entity, pos)) {
            FarmlandBlock.turnToDirt(entity, state, level, pos);
        }
    }

    @Redirect(
        method = "randomTick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"
        )
    )
    private boolean mod_worldguard$denyProtectedMoistureMutation(
        ServerLevel level,
        BlockPos pos,
        BlockState state,
        int flags
    ) {
        return !WorldGuardProtectionHooks.deniesFarmlandMutation(level, pos, state)
            && level.setBlock(pos, state, flags);
    }

    @Redirect(
        method = "randomTick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/FarmlandBlock;turnToDirt(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"
        )
    )
    private void mod_worldguard$denyProtectedRandomDryConversion(
        Entity entity,
        BlockState state,
        Level level,
        BlockPos pos
    ) {
        if (!WorldGuardProtectionHooks.deniesFarmlandDry(level, pos)) {
            FarmlandBlock.turnToDirt(entity, state, level, pos);
        }
    }

    @Redirect(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/FarmlandBlock;turnToDirt(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"
        )
    )
    private void mod_worldguard$denyProtectedScheduledDryConversion(
        Entity entity,
        BlockState state,
        Level level,
        BlockPos pos
    ) {
        if (!WorldGuardProtectionHooks.deniesFarmlandDry(level, pos)) {
            FarmlandBlock.turnToDirt(entity, state, level, pos);
        }
    }
}
