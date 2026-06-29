package com.vantablack4.worldguard.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.AbstractHugeMushroomFeature;

import com.vantablack4.worldguard.hook.WorldGuardProtectionHooks;

@Mixin(AbstractHugeMushroomFeature.class)
public abstract class AbstractHugeMushroomFeatureMixin {
    @Inject(method = "placeMushroomBlock", at = @At("HEAD"), cancellable = true)
    private void mod_worldguard$denyProtectedGeneratedMushroomBlock(
        LevelAccessor level,
        BlockPos.MutableBlockPos pos,
        BlockState state,
        CallbackInfo callbackInfo
    ) {
        if (WorldGuardProtectionHooks.deniesGeneratedMushroomBlock(level, pos)) {
            callbackInfo.cancel();
        }
    }
}
