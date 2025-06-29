package de.banarnia.bettertpa.listener;

import de.banarnia.bettertpa.manager.TPAManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class TPAListener implements Listener {

    private TPAManager manager;

    public TPAListener(TPAManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void handleQuit(PlayerQuitEvent event) {
        manager.handlePlayerQuit(event.getPlayer());
    }

    @EventHandler
    public void handlePlayerMove(PlayerMoveEvent event) {
        // Check if warmup cancel on player move is configured.
        if (!manager.getConfig().cancelWarmupOnMove())
                return;

        // Check if player moves within the same block.
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockY() == event.getTo().getBlockY() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ())
            return;

        manager.handlePlayerMove(event.getPlayer());
    }

}
