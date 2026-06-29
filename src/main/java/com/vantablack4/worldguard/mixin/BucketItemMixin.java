package com.vantablack4.worldguard.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;

import com.vantablack4.worldguard.hook.WorldGuardProtectionHooks;

@Mixin(BucketItem.class)
public abstract class BucketItemMixin {
    @Shadow
    public abstract Fluid getContent();

    @Inject(method = "emptyContents", at = @At("HEAD"), cancellable = true)
    private void mod_worldguard$denyProtectedBucketEmpty(
        LivingEntity entity,
        Level level,
        BlockPos pos,
        BlockHitResult hitResult,
        CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        if (WorldGuardProtectionHooks.deniesBucketPlace(level, entity, pos, getContent())) {
            callbackInfo.setReturnValue(false);
        }
    }
}
