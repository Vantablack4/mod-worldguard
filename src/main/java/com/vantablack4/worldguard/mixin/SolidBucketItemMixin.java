package com.vantablack4.worldguard.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.SolidBucketItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

import com.vantablack4.worldguard.hook.WorldGuardProtectionHooks;

@Mixin(SolidBucketItem.class)
public abstract class SolidBucketItemMixin {
    @Inject(method = "emptyContents", at = @At("HEAD"), cancellable = true)
    private void mod_worldguard$denyProtectedSolidBucketEmpty(
        LivingEntity entity,
        Level level,
        BlockPos pos,
        BlockHitResult hitResult,
        CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        if (WorldGuardProtectionHooks.deniesSolidBucketPlace(level, entity, pos)) {
            callbackInfo.setReturnValue(false);
        }
    }
}
