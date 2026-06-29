package com.vantablack4.worldguard.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandSignedPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.phys.Vec3;

import com.vantablack4.worldguard.session.WorldGuardSessionHooks;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
    @Shadow
    public ServerPlayer player;

    @Unique
    private Vec3 mod_worldguard$previousPosition;

    @Shadow
    public abstract void teleport(double x, double y, double z, float yRot, float xRot);

    @Inject(method = "handleChatCommand", at = @At("HEAD"), cancellable = true)
    private void mod_worldguard$denyProtectedCommand(
        ServerboundChatCommandPacket packet,
        CallbackInfo callbackInfo
    ) {
        if (packet != null && !WorldGuardSessionHooks.allowCommand(player, packet.command())) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "handleSignedChatCommand", at = @At("HEAD"), cancellable = true)
    private void mod_worldguard$denyProtectedSignedCommand(
        ServerboundChatCommandSignedPacket packet,
        CallbackInfo callbackInfo
    ) {
        if (packet != null && !WorldGuardSessionHooks.allowCommand(player, packet.command())) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "handleMovePlayer", at = @At("HEAD"))
    private void mod_worldguard$capturePreviousPosition(
        ServerboundMovePlayerPacket packet,
        CallbackInfo callbackInfo
    ) {
        mod_worldguard$previousPosition = player.position();
    }

    @Inject(method = "handleMovePlayer", at = @At("RETURN"))
    private void mod_worldguard$denyProtectedMovement(
        ServerboundMovePlayerPacket packet,
        CallbackInfo callbackInfo
    ) {
        Vec3 previousPosition = mod_worldguard$previousPosition;
        mod_worldguard$previousPosition = null;
        if (previousPosition != null && !WorldGuardSessionHooks.allowMovement(player, previousPosition)) {
            teleport(previousPosition.x, previousPosition.y, previousPosition.z, player.getYRot(), player.getXRot());
            player.resetFallDistance();
        }
    }
}
