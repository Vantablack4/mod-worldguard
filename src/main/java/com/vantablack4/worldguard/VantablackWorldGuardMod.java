package com.vantablack4.worldguard;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class VantablackWorldGuardMod implements ModInitializer {
    public static final String MOD_ID = "mod_worldguard";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Vantablack WorldGuard initialized");
        if (FabricLoader.getInstance().getEnvironmentType() != EnvType.SERVER) {
            return;
        }

        WorldGuardConfig config = WorldGuardConfig.load();
        WorldGuardStorage storage = WorldGuardStorage.load(config.configDirectory());
        WorldGuardService service = new WorldGuardService(config, storage);
        new WorldGuardHooks(service).register();
        new WorldGuardCommands(config, storage).register();
    }
}
