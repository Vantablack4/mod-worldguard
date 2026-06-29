package com.vantablack4.worldguard.mixin;

import java.util.Objects;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Spawner;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import com.vantablack4.worldguard.hook.WorldGuardProtectionHooks;

@Mixin(SpawnEggItem.class)
public abstract class SpawnEggItemMixin {
    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void mod_worldguard$denyProtectedEntityPlaceFromBlockUse(
        UseOnContext context,
        CallbackInfoReturnable<InteractionResult> callbackInfo
    ) {
        if (!(context.getPlayer() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        BlockPos clicked = context.getClickedPos();
        BlockPos target = levelHasSpawner(context.getLevel(), clicked)
            ? clicked
            : spawnTarget(context.getLevel(), clicked, context.getClickedFace());
        if (WorldGuardProtectionHooks.deniesEntityPlace(serverPlayer, context.getLevel(), target)) {
            callbackInfo.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void mod_worldguard$denyProtectedEntityPlaceFromFluidUse(
        Level level,
        Player player,
        InteractionHand hand,
        CallbackInfoReturnable<InteractionResult> callbackInfo
    ) {
        if (!(player instanceof ServerPlayer serverPlayer) || level.isClientSide()) {
            return;
        }

        BlockHitResult hit = playerPovHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK || !(level.getBlockState(hit.getBlockPos()).getBlock() instanceof LiquidBlock)) {
            return;
        }
        if (WorldGuardProtectionHooks.deniesEntityPlace(serverPlayer, level, hit.getBlockPos())) {
            callbackInfo.setReturnValue(InteractionResult.FAIL);
        }
    }

    private static boolean levelHasSpawner(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof Spawner;
    }

    private static BlockPos spawnTarget(Level level, BlockPos clicked, Direction direction) {
        if (level.getBlockState(clicked).getCollisionShape(level, clicked).isEmpty()) {
            return clicked;
        }
        return clicked.relative(Objects.requireNonNull(direction, "direction"));
    }

    private static BlockHitResult playerPovHitResult(Level level, Player player, ClipContext.Fluid fluidMode) {
        var start = player.getEyePosition();
        var end = start.add(player.calculateViewVector(player.getXRot(), player.getYRot()).scale(player.blockInteractionRange()));
        return level.clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, fluidMode, player));
    }
}
