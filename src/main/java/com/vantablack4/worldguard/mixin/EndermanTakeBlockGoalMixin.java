package com.vantablack4.worldguard.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

import com.vantablack4.worldguard.hook.WorldGuardProtectionHooks;

@Mixin(targets = "net.minecraft.world.entity.monster.EnderMan$EndermanTakeBlockGoal")
public abstract class EndermanTakeBlockGoalMixin {
    @Unique
    private boolean mod_worldguard$blockedProtectedTake;

    @Inject(method = "tick", at = @At("HEAD"))
    private void mod_worldguard$resetProtectedTake(CallbackInfo callbackInfo) {
        mod_worldguard$blockedProtectedTake = false;
    }

    @Redirect(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z"
        )
    )
    private boolean mod_worldguard$denyProtectedTake(Level level, BlockPos pos, boolean moving) {
        if (WorldGuardProtectionHooks.deniesEndermanGrief(level, pos)) {
            mod_worldguard$blockedProtectedTake = true;
            return false;
        }
        return level.removeBlock(pos, moving);
    }

    @Redirect(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;gameEvent(Lnet/minecraft/core/Holder;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/gameevent/GameEvent$Context;)V"
        )
    )
    private void mod_worldguard$suppressProtectedTakeEvent(
        Level level,
        Holder<GameEvent> event,
        BlockPos pos,
        GameEvent.Context context
    ) {
        if (!mod_worldguard$blockedProtectedTake) {
            level.gameEvent(event, pos, context);
        }
    }

    @Redirect(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/monster/EnderMan;setCarriedBlock(Lnet/minecraft/world/level/block/state/BlockState;)V"
        )
    )
    private void mod_worldguard$suppressProtectedCarry(EnderMan enderman, BlockState state) {
        if (!mod_worldguard$blockedProtectedTake) {
            enderman.setCarriedBlock(state);
        }
    }
}
