package com.vantablack4.worldguard.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import com.vantablack4.worldguard.WorldGuardFlag;
import com.vantablack4.worldguard.session.WorldGuardSessionHooks;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerDropMixin {
    @Inject(
        method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void mod_worldguard$denyProtectedItemDrop(
        ItemStack stack,
        boolean throwRandomly,
        boolean retainOwnership,
        CallbackInfoReturnable<ItemEntity> callbackInfo
    ) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (!stack.isEmpty() && WorldGuardSessionHooks.denyAction(player, player.blockPosition(), WorldGuardFlag.ITEM_DROP)) {
            callbackInfo.setReturnValue(null);
        }
    }
}
