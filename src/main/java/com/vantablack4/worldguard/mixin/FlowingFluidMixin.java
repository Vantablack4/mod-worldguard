package com.vantablack4.worldguard.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;

import com.vantablack4.worldguard.hook.WorldGuardProtectionHooks;

@Mixin(FlowingFluid.class)
public abstract class FlowingFluidMixin {
    @Inject(method = "spreadTo", at = @At("HEAD"), cancellable = true)
    private void mod_worldguard$denyProtectedFluidFlow(
        LevelAccessor level,
        BlockPos pos,
        BlockState state,
        Direction direction,
        FluidState fluidState,
        CallbackInfo callbackInfo
    ) {
        if (WorldGuardProtectionHooks.deniesFluidFlow(level, pos, fluidState)) {
            callbackInfo.cancel();
        }
    }
}
