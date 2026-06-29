package com.vantablack4.worldguard.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.portal.TeleportTransition;

import com.vantablack4.worldguard.session.WorldGuardSessionHooks;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
    @Redirect(
        method = "respawn",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;findRespawnPositionAndUseSpawnBlock(ZLnet/minecraft/world/level/portal/TeleportTransition$PostTeleportTransition;)Lnet/minecraft/world/level/portal/TeleportTransition;"
        )
    )
    private TeleportTransition mod_worldguard$useRegionSpawnForRespawn(
        ServerPlayer player,
        boolean searchForRespawnBlock,
        TeleportTransition.PostTeleportTransition postTeleportTransition
    ) {
        TeleportTransition vanillaTransition = player.findRespawnPositionAndUseSpawnBlock(
            searchForRespawnBlock,
            postTeleportTransition
        );
        return WorldGuardSessionHooks.respawnTransition(player, vanillaTransition);
    }
}
