package com.vantablack4.worldguard.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import com.vantablack4.worldguard.hook.WorldGuardProtectionHooks;

@Mixin(targets = "net.minecraft.world.entity.monster.EnderMan$EndermanLeaveBlockGoal")
public abstract class EndermanLeaveBlockGoalMixin {
    @Inject(method = "canPlaceBlock", at = @At("RETURN"), cancellable = true)
    private void mod_worldguard$denyProtectedEndermanPlace(
        Level level,
        BlockPos pos,
        BlockState placedState,
        BlockState currentState,
        BlockState belowState,
        BlockPos belowPos,
        CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        if (callbackInfo.getReturnValueZ() && WorldGuardProtectionHooks.deniesEndermanGrief(level, pos)) {
            callbackInfo.setReturnValue(false);
        }
    }
}
