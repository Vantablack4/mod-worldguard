package com.vantablack4.worldguard.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.PowderSnowBlock;
import net.minecraft.world.level.block.state.BlockState;

import com.vantablack4.worldguard.hook.WorldGuardProtectionHooks;

@Mixin(PowderSnowBlock.class)
public abstract class PowderSnowBlockMixin {
    @Inject(method = "pickupBlock", at = @At("HEAD"), cancellable = true)
    private void mod_worldguard$denyProtectedPowderSnowPickup(
        LivingEntity entity,
        LevelAccessor level,
        BlockPos pos,
        BlockState state,
        CallbackInfoReturnable<ItemStack> callbackInfo
    ) {
        if (WorldGuardProtectionHooks.deniesBucketPickup(level, entity, pos)) {
            callbackInfo.setReturnValue(ItemStack.EMPTY);
        }
    }
}
