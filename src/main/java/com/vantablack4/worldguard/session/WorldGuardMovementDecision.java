package com.vantablack4.worldguard.session;

import java.util.Objects;

import com.vantablack4.worldguard.ProtectionDecision;

public record WorldGuardMovementDecision(
    boolean allowed,
    ProtectionDecision decision
) {
    public WorldGuardMovementDecision {
        decision = Objects.requireNonNull(decision, "decision");
    }

    public static WorldGuardMovementDecision allow() {
        return new WorldGuardMovementDecision(true, ProtectionDecision.allow());
    }

    public static WorldGuardMovementDecision deny(ProtectionDecision decision) {
        return new WorldGuardMovementDecision(false, decision);
    }
}
