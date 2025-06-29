package de.banarnia.bettertpa.config;

import de.banarnia.api.config.Config;
import de.banarnia.bettertpa.BetterTPA;

public class TPAConfig {

    private final BetterTPA plugin;

    private final Config config;

    public TPAConfig(BetterTPA plugin, Config config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void reload() {
        config.loadConfig();
    }

    public int getRequestDuration() {
        return config.getInt("request-duration", 60);
    }

    public int getWarmupTime() {
        return config.getInt("teleport-warmup-time", 0);
    }

    public boolean cancelWarmupOnMove() { return config.getBoolean("cancel-warmup-on-move", false); }

    public int getRequestCooldown() {
        return config.getInt("request-cooldown", 10);
    }

    public String getLanguage() {
        return config.getString("language", "en");
    }

    public boolean debugMode() {
        return config.getBoolean("debug");
    }

}
