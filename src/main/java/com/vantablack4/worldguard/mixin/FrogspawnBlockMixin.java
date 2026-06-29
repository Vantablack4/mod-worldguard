package com.vantablack4.worldguard.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FrogspawnBlock;
import net.minecraft.world.level.block.state.BlockState;

import com.vantablack4.worldguard.hook.WorldGuardProtectionHooks;

@Mixin(FrogspawnBlock.class)
public abstract class FrogspawnBlockMixin {
    @Inject(method = "entityInside", at = @At("HEAD"), cancellable = true)
    private void mod_worldguard$denyProtectedFrogspawnTrample(
        BlockState state,
        Level level,
        BlockPos pos,
        Entity entity,
        InsideBlockEffectApplier effects,
        boolean inside,
        CallbackInfo callbackInfo
    ) {
        if (WorldGuardProtectionHooks.deniesTrample(level, entity, pos)) {
            callbackInfo.cancel();
        }
    }
}
