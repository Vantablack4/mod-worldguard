package com.vantablack4.worldguard.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.WeatheringCopperBarsBlock;
import net.minecraft.world.level.block.WeatheringCopperBulbBlock;
import net.minecraft.world.level.block.WeatheringCopperChainBlock;
import net.minecraft.world.level.block.WeatheringCopperDoorBlock;
import net.minecraft.world.level.block.WeatheringCopperFullBlock;
import net.minecraft.world.level.block.WeatheringCopperGrateBlock;
import net.minecraft.world.level.block.WeatheringCopperSlabBlock;
import net.minecraft.world.level.block.WeatheringCopperStairBlock;
import net.minecraft.world.level.block.WeatheringCopperTrapDoorBlock;
import net.minecraft.world.level.block.WeatheringLanternBlock;
import net.minecraft.world.level.block.WeatheringLightningRodBlock;
import net.minecraft.world.level.block.state.BlockState;

import com.vantablack4.worldguard.hook.WorldGuardProtectionHooks;

@Mixin({
    WeatheringCopperBarsBlock.class,
    WeatheringCopperBulbBlock.class,
    WeatheringCopperChainBlock.class,
    WeatheringCopperDoorBlock.class,
    WeatheringCopperFullBlock.class,
    WeatheringCopperGrateBlock.class,
    WeatheringCopperSlabBlock.class,
    WeatheringCopperStairBlock.class,
    WeatheringCopperTrapDoorBlock.class,
    WeatheringLanternBlock.class,
    WeatheringLightningRodBlock.class
})
public abstract class WeatheringCopperBlockMixin {
    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void mod_worldguard$denyProtectedCopperFade(
        BlockState state,
        ServerLevel level,
        BlockPos pos,
        RandomSource random,
        CallbackInfo callbackInfo
    ) {
        if (WorldGuardProtectionHooks.deniesCopperFade(level, pos)) {
            callbackInfo.cancel();
        }
    }
}
