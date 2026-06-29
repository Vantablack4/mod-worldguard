package com.vantablack4.worldguard.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EndPortalBlock;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;

import com.vantablack4.worldguard.hook.WorldGuardProtectionHooks;

@Mixin({ NetherPortalBlock.class, EndPortalBlock.class })
public abstract class PortalBlockMixin {
    @Inject(method = "entityInside", at = @At("HEAD"), cancellable = true)
    private void mod_worldguard$denyProtectedPortalEntry(
        BlockState state,
        Level level,
        BlockPos pos,
        Entity entity,
        InsideBlockEffectApplier effects,
        boolean inside,
        CallbackInfo callbackInfo
    ) {
        if (WorldGuardProtectionHooks.deniesPortalEntry(level, entity, pos)) {
            callbackInfo.cancel();
        }
    }
}
