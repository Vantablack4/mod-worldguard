package com.vantablack4.worldguard.mixin;

import java.util.function.Predicate;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.portal.TeleportTransition;

import com.vantablack4.worldguard.session.WorldGuardSessionHooks;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
    @Redirect(
        method = "broadcastChatMessage(Lnet/minecraft/network/chat/PlayerChatMessage;Ljava/util/function/Predicate;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/network/chat/ChatType$Bound;)V",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/function/Predicate;test(Ljava/lang/Object;)Z"
        )
    )
    private boolean mod_worldguard$excludeChatBlockedRecipientsFromFilterTracking(
        Predicate<ServerPlayer> predicate,
        Object recipient
    ) {
        return predicate.test((ServerPlayer) recipient)
            && WorldGuardSessionHooks.allowsReceiveChat((ServerPlayer) recipient);
    }

    @Redirect(
        method = "broadcastChatMessage(Lnet/minecraft/network/chat/PlayerChatMessage;Ljava/util/function/Predicate;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/network/chat/ChatType$Bound;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;sendChatMessage(Lnet/minecraft/network/chat/OutgoingChatMessage;ZLnet/minecraft/network/chat/ChatType$Bound;)V"
        )
    )
    private void mod_worldguard$skipReceiveChatBlockedRecipient(
        ServerPlayer recipient,
        OutgoingChatMessage outgoingMessage,
        boolean filterMaskEnabled,
        ChatType.Bound bound,
        PlayerChatMessage message,
        Predicate<ServerPlayer> predicate,
        ServerPlayer sender,
        ChatType.Bound originalBound
    ) {
        if (WorldGuardSessionHooks.allowsReceiveChat(recipient)) {
            recipient.sendChatMessage(outgoingMessage, filterMaskEnabled, bound);
        }
    }

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
