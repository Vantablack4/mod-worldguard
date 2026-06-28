package com.vantablack4.worldguard.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;

import com.vantablack4.worldguard.hook.WorldGuardProtectionHooks;

@Mixin(PistonBaseBlock.class)
public abstract class PistonBaseBlockMixin {
    @Inject(method = "triggerEvent", at = @At("HEAD"), cancellable = true)
    private void mod_worldguard$denyProtectedPistonAction(
        BlockState state,
        Level level,
        BlockPos pos,
        int type,
        int data,
        CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        Direction facing = state.getValue(DirectionalBlock.FACING);
        boolean extending = type == PistonBaseBlock.TRIGGER_EXTEND;
        if (WorldGuardProtectionHooks.deniesPistonAction(level, pos, facing, extending)) {
            callbackInfo.setReturnValue(false);
        }
    }
}
