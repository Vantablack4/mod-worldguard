package com.vantablack4.worldguard.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.BambooSaplingBlock;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.PitcherCropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.TorchflowerCropBlock;
import net.minecraft.world.level.block.state.BlockState;

import com.vantablack4.worldguard.hook.WorldGuardProtectionHooks;

@Mixin({
    BambooSaplingBlock.class,
    BambooStalkBlock.class,
    CactusBlock.class,
    CocoaBlock.class,
    CropBlock.class,
    NetherWartBlock.class,
    PitcherCropBlock.class,
    StemBlock.class,
    SugarCaneBlock.class,
    SweetBerryBushBlock.class,
    TorchflowerCropBlock.class
})
public abstract class CropGrowthBlockMixin {
    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void mod_worldguard$denyProtectedCropGrowth(
        BlockState state,
        ServerLevel level,
        BlockPos pos,
        RandomSource random,
        CallbackInfo callbackInfo
    ) {
        if (WorldGuardProtectionHooks.deniesCropGrowth(level, pos)) {
            callbackInfo.cancel();
        }
    }
}
