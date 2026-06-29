package com.vantablack4.worldguard.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TurtleEggBlock;
import net.minecraft.world.level.block.state.BlockState;

import com.vantablack4.worldguard.hook.WorldGuardProtectionHooks;

@Mixin(TurtleEggBlock.class)
public abstract class TurtleEggBlockMixin {
    @Inject(method = "destroyEgg", at = @At("HEAD"), cancellable = true)
    private void mod_worldguard$denyProtectedEggTrample(
        Level level,
        BlockState state,
        BlockPos pos,
        Entity entity,
        int inverseChance,
        CallbackInfo callbackInfo
    ) {
        if (WorldGuardProtectionHooks.deniesTrample(level, entity, pos)) {
            callbackInfo.cancel();
        }
    }
}
