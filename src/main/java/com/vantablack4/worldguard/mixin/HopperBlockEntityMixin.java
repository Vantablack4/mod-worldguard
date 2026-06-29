package com.vantablack4.worldguard.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

import com.vantablack4.worldguard.hook.WorldGuardProtectionHooks;

@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin {
    @Inject(method = "ejectItems", at = @At("HEAD"), cancellable = true)
    private static void mod_worldguard$denyProtectedHopperEject(
        Level level,
        BlockPos pos,
        HopperBlockEntity hopper,
        CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        if (WorldGuardProtectionHooks.deniesHopperEject(level, pos, hopper)) {
            callbackInfo.setReturnValue(false);
        }
    }

    @Inject(method = "suckInItems", at = @At("HEAD"), cancellable = true)
    private static void mod_worldguard$denyProtectedHopperSuck(
        Level level,
        Hopper hopper,
        CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        if (WorldGuardProtectionHooks.deniesHopperSuck(level, hopper)) {
            callbackInfo.setReturnValue(false);
        }
    }

    @Inject(
        method = "addItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/Container;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/Direction;)Lnet/minecraft/world/item/ItemStack;",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void mod_worldguard$denyProtectedContainerTransfer(
        Container source,
        Container destination,
        ItemStack stack,
        Direction direction,
        CallbackInfoReturnable<ItemStack> callbackInfo
    ) {
        if (!stack.isEmpty() && WorldGuardProtectionHooks.deniesContainerTransfer(source, destination)) {
            callbackInfo.setReturnValue(stack);
        }
    }

    @Inject(
        method = "addItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/entity/item/ItemEntity;)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void mod_worldguard$denyProtectedItemEntityTransfer(
        Container destination,
        ItemEntity item,
        CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        if (WorldGuardProtectionHooks.deniesHopperItemPickup(destination, item)) {
            callbackInfo.setReturnValue(false);
        }
    }
}
