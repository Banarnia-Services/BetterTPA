package de.banarnia.bettertpa.manager;

import de.banarnia.bettertpa.BetterTPA;
import de.banarnia.bettertpa.config.TPAConfig;
import de.banarnia.bettertpa.events.TpAcceptEvent;
import de.banarnia.bettertpa.events.TpDenyEvent;
import de.banarnia.bettertpa.events.requests.TpRequestEvent;
import de.banarnia.bettertpa.lang.Message;
import de.banarnia.bettertpa.requests.TPRequest;
import de.banarnia.bettertpa.requests.TpaRequest;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Array;
import java.util.*;

public class TPAManager {

    private BetterTPA plugin;
    private TPAConfig config;

    private BukkitAudiences adventure;
    private MiniMessage mm;

    private IgnoreManager ignoreManager;

    private HashMap<UUID, HashMap<UUID, TPRequest>> pendingRequests = new HashMap<>();  // <Receiver, <Sender, Request>>
    private HashMap<UUID, HashMap<UUID, Integer>> requestCooldowns = new HashMap<>();   // <Receiver, <Sender, TaskID>>

    public TPAManager(BetterTPA plugin, TPAConfig config, BukkitAudiences adventure, MiniMessage mm) {
        this.plugin = plugin;
        this.config = config;
        this.ignoreManager = new IgnoreManager();
        this.adventure = adventure;
        this.mm = mm;
    }

    // ~~~~~ Player Move ~~~~~

    public void handlePlayerMove(Player player) {
        // Check if player has a teleport request.
        List<TPRequest> requests = getPendingAndSentRequests(player);
        if (requests.isEmpty())
            return;

        // Check if any warmups are running.
        requests.forEach(request -> {
            if (request.hasWarmup() && request.getPlayerToTeleport().equals(player)) {
                request.deny();
                removeRequest(request);
                player.sendMessage(Message.TIMER_ERROR_TPA_WARMUP_MOVE.get());
            }
        });
    }

    // ~~~~~ Player Quit ~~~~~

    /**
     * Handle player quit.
     * Will clean up the cache, except request cooldowns.
     * @param player Quitting player.
     */
    public void handlePlayerQuit(Player player) {
        // Deny and remove all requests the player has sent or are still pending.
        getPendingAndSentRequests(player).forEach(request -> {
            request.deny();
            removeRequest(request);
        });
    }

    // ~~~~~ TPA accept ~~~~~

    /**
     * Accept a pending TPA-Request.
     * @param receiver Request receiver.
     * @param sender Request sender.
     * @param silent Set to true, to send messages.
     */
    public void acceptRequest(Player receiver, Player sender, boolean silent) {
        acceptRequest(getPendingRequest(receiver, sender), silent);
    }

    /**
     * Accept a pending TPA-Request.
     * @param request Request instance.
     */
    public void acceptRequest(TPRequest request, boolean silent) {
        if (request == null)
            return;

        // Check if request was already accepted or denied.
        if (request.wasExecuted())
            return;

        // Call event, as it may be cancellable.
        TpAcceptEvent event = new TpAcceptEvent(request);
        event.callEvent();
        if (event.isCancelled())
            return;

        Player sender = request.getSender();
        Player receiver = request.getReceiver();

        // Send messages.
        if (!silent) {
            sender.sendMessage(Message.COMMAND_INFO_TPA_ACCEPTED_SENDER.replace("%target%", receiver.getName()));
            receiver.sendMessage(Message.COMMAND_INFO_TPA_ACCEPTED_RECEIVER.replace("%target%", sender.getName()));
        }

        // Check if teleport should be delayed.
        if (config.getWarmupTime() > 0 && !request.hasWarmupBypassPermission()) {
            // Cancel request expiration.
            request.cancelExpirationTask();

            // Delay teleport due to warmup.
            request.setWarmupTaskId(Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                // Execute request.
                request.execute();

                // Remove request from cache.
                removeRequest(request);
            }, (long) config.getWarmupTime() * 20));

            // Send message.
            String seconds = String.valueOf((int) config.getWarmupTime());
            request.getPlayerToTeleport().sendMessage(Message.TIMER_INFO_TPA_WARMUP.replace("%time%", seconds));
        } else {
            // Execute request.
            request.execute();

            // Remove request from cache.
            removeRequest(request);
        }
    }

    // ~~~~~ TPA deny ~~~~~

    /**
     * Deny a pending TPA-Request.
     * @param request Request that was denied.
     * @param silent Set to true to send messages.
     */
    public void denyRequest(TPRequest request, boolean silent) {
        if (request == null)
            return;

        // Check if request was already accepted or denied.
        if (request.wasExecuted())
            return;

        // Call event.
        new TpDenyEvent(request).callEvent();

        // Send messages.
        if (!silent) {
            Player sender = request.getSender();
            Player receiver = request.getReceiver();

            sender.sendMessage(Message.COMMAND_ERROR_TPA_DENIED_SENDER.replace("%target%", receiver.getName()));
            receiver.sendMessage(Message.COMMAND_ERROR_TPA_DENIED_RECEIVER.replace("%target%", sender.getName()));
        }

        // Mark request as denied.
        request.deny();
        removeRequest(request);

        // Set request cooldown.
        if (config.getRequestCooldown() > 0 && !request.hasCooldownBypassPermission()) {
            UUID senderId = request.getSender().getUniqueId();
            UUID receiverId = request.getReceiver().getUniqueId();

            int taskId = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                removeCooldown(senderId, receiverId);
            }, (long) config.getRequestCooldown() * 20);

            setCooldown(request.getSender(), request.getReceiver(), taskId);
        }
    }

    // ~~~~~ TPA request ~~~~~

    /**
     * Send a tpa request from sender to receiver.
     * @param request Request instance.
     * @param silent Set to true to send messages.
     */
    public void sendRequest(TPRequest request, boolean silent) {
        if (request == null)
            return;

        Player sender = request.getSender();
        Player receiver = request.getReceiver();

        // Check if sender equals receiver.
        if (sender == receiver) {
            if (!silent)
                sender.sendMessage(Message.COMMAND_ERROR_TPA_SELF.get());

            return;
        }

        // Check if there is already a pending request.
        if (checkPendingRequest(sender, receiver, silent))
            return;

        // Check if there is a cooldown.
        if (checkCooldown(sender, receiver, silent))
            return;

        // Check if player is ignored.
        if (checkIgnore(sender, receiver, silent))
            return;

        // Create request and throw event.
        TpRequestEvent event = request.createRequestEvent();
        event.callEvent();

        // Check if event was cancelled.
        if (event.isCancelled()) {
            if (!silent) {
                String message = Message.COMMAND_ERROR_TPA_EVENT_CANCELLED.get();
                sender.sendMessage(message);
            }

            return;
        }

        // Add to pending events.
        addRequest(sender, receiver, request);

        // Send message.
        if (!silent) {
            sender.sendMessage(Message.COMMAND_INFO_TPA_SENDER.replace("%target%", receiver.getName()));

            // Send Minimessage.
            String msg = request instanceof TpaRequest ? Message.COMMAND_INFO_TPAREQUEST_RECEIVER.get() :
                                                         Message.COMMAND_INFO_TPAHEREREQUEST_RECEIVER.get();
            msg = msg.replace("%target%", sender.getName());
            Component cmp = mm.deserialize(msg);
            adventure.player(receiver).sendMessage(cmp);
        }
    }

    /**
     * Check if the sender already has a pending request to that player.
     * @param sender Request sender.
     * @param receiver Request receiver.
     * @param silent Set to true to send message.
     * @return True, if receiver has a pending request by sender. Else false.
     */
    public boolean checkPendingRequest(Player sender, Player receiver, boolean silent) {
        if (!hasPendingRequest(receiver, sender))
            return false;

        if (!silent) {
            String message = Message.COMMAND_ERROR_TPA_PENDING.get();
            sender.sendMessage(message);
        }

        return true;
    }

    /**
     * Check if the sender has a cooldown on submitting a new tpa request to a player.
     * @param sender Sender that initiates the request.
     * @param receiver Request receiver.
     * @param silent Set to true to send message.
     * @return True, if cooldown is still active.
     */
    public boolean checkCooldown(Player sender, Player receiver, boolean silent) {
        if (!hasCooldown(sender, receiver))
            return false;

        if (!silent) {
            String message = Message.COMMAND_ERROR_TPA_COOLDOWN.get();
            sender.sendMessage(message);
        }

        return true;
    }

    /**
     * Check if the sender is ignored by the receiver.
     * @param sender Sender that might be ignored.
     * @param receiver Receiver that might have ignored the sender.
     * @param silent Set to true to send message.
     * @return True, if player is ignored. Else false.
     */
    public boolean checkIgnore(Player sender, Player receiver, boolean silent) {
        if (!ignoreManager.isIgnored(sender, receiver) || ignoreManager.hasIgnoreBypassPermission(sender))
            return false;

        if (!silent) {
            String message = Message.COMMAND_ERROR_TPA_IGNORED.replace("%target%", receiver.getName());
            sender.sendMessage(message);
        }

        return true;
    }

    // ~~~~~ Requests ~~~~~

    /**
     * Add a new pending request.
     * @param sender Request sender.
     * @param receiver Request receiver.
     * @param request Request instance.
     */
    public void addRequest(Player sender, Player receiver, TPRequest request) {
        // Check if receiver already has a pending request.
        if (hasPendingRequest(receiver, sender))
            return;

        if (!hasPendingRequest(receiver))
            pendingRequests.put(receiver.getUniqueId(), new HashMap<>());

        pendingRequests.get(receiver.getUniqueId()).put(sender.getUniqueId(), request);

        request.setExpirationTaskId(Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            // Check if request was already denied or accepted.
            if (request.wasExecuted())
                return;

            // Check if both players are still online.
            if (!request.bothPlayersOnline())
                return;

            // Send info to both players.
            sender.sendMessage(Message.TIMER_ERROR_TPA_EXPIRED_SENDER.replace("%target%", receiver.getName()));
            receiver.sendMessage(Message.TIMER_ERROR_TPA_EXPIRED_RECEIVER.replace("%target%", sender.getName()));

            // Remove request.
            removeRequest(request);
        }, (long) config.getRequestDuration() * 20));
    }

    /**
     * Check if a player has a pending request of another player.
     * @param receiver Request receiver.
     * @param sender Request sender.
     * @return True, if receiver has a pending request of the sender. Else false.
     */
    public boolean hasPendingRequest(Player receiver, Player sender) {
        // Check if receiver has any pending requests at all.
        if (!hasPendingRequest(receiver))
            return false;

        return pendingRequests.get(receiver.getUniqueId()).containsKey(sender.getUniqueId());
    }

    /**
     * Check if a player has pending requests.
     * @param receiver Receiver to check.
     * @return True, if player has pending request. Else false.
     */
    public boolean hasPendingRequest(Player receiver) {
        return pendingRequests.containsKey(receiver.getUniqueId());
    }

    /**
     * Get a pending tpa request, if exists.
     * @param receiver Request receiver.
     * @param sender Request sender.
     * @return TPRequest instance, if exists. Else null.
     */
    public TPRequest getPendingRequest(Player receiver, Player sender) {
        if (!hasPendingRequest(receiver, sender))
            return null;

        return pendingRequests.get(receiver.getUniqueId()).get(sender.getUniqueId());
    }

    /**
     * Remove a pending request from the cache.
     * @param request Request instance.
     */
    public void removeRequest(TPRequest request) {
        if (!hasPendingRequest(request.getReceiver(), request.getSender()))
            return;

        // Get UUIDs.
        UUID senderId = request.getSender().getUniqueId();
        UUID receiverId = request.getReceiver().getUniqueId();

        // Remove request.
        pendingRequests.get(receiverId).remove(senderId);

        // Check if receiver has any requests left to clear up the cache.
        if (!hasPendingRequest(request.getReceiver()))
            pendingRequests.remove(receiverId);
    }

    /**
     * Get all pending request of the receiver.
     * @param receiver Receiving player.
     * @return List of all pending Request. Can be empty.
     */
    public List<TPRequest> getPendingRequests(Player receiver) {
        if (!hasPendingRequest(receiver))
            return Collections.emptyList();

        return new ArrayList<>(pendingRequests.get(receiver.getUniqueId()).values());
    }

    /**
     * Get all active sent requests of the sender.
     * @param sender Sending player.
     * @return List of all sent requests.
     */
    public List<TPRequest> getSentRequests(Player sender) {
        List<TPRequest> sentRequests = new ArrayList<>();
        for (UUID receiverId : pendingRequests.keySet()) {
            for (Map.Entry<UUID, TPRequest> entry : pendingRequests.get(receiverId).entrySet())
                if (entry.getKey().equals(sender.getUniqueId()))
                    sentRequests.add(entry.getValue());
        }

        return sentRequests;
    }

    /**
     * Get all pending and sent requests of the player.
     * @param player Player that received or sent requests.
     * @return List of all requests.
     */
    public List<TPRequest> getPendingAndSentRequests(Player player) {
        List<TPRequest> requests = new ArrayList<>();
        requests.addAll(getPendingRequests(player));
        requests.addAll(getSentRequests(player));

        return requests;
    }

    // ~~~~~ Cooldown ~~~~~

    /**
     * Set a cooldown for the player.
     * @param sender Sender.
     * @param receiver Receiver.
     * @param taskId Cooldown task id.
     */
    public void setCooldown(Player sender, Player receiver, int taskId) {
        // Check if there is already a cooldown.
        if (hasCooldown(sender, receiver))
            cancelCooldown(sender, receiver);

        if (!requestCooldowns.containsKey(receiver.getUniqueId()))
            requestCooldowns.put(receiver.getUniqueId(), new HashMap<>());

        requestCooldowns.get(receiver.getUniqueId()).put(sender.getUniqueId(), taskId);
    }

    public void removeCooldown(UUID senderId, UUID receiverId) {
        // Cleanup cache.
        requestCooldowns.get(receiverId).remove(senderId);
        if (!requestCooldowns.containsKey(receiverId))
            requestCooldowns.remove(receiverId);
    }

    /**
     * Cancel a running request cooldown.
     * @param sender Sender.
     * @param receiver Receiver.
     */
    public void cancelCooldown(Player sender, Player receiver) {
        cancelCooldown(sender.getUniqueId(), receiver.getUniqueId());
    }

    /**
     * Cancel a running request cooldown.
     * @param senderId Sender id.
     * @param receiverId Receiver id.
     */
    public void cancelCooldown(UUID senderId, UUID receiverId) {
        if (!hasCooldown(senderId, receiverId))
            return;

        // Cancel task.
        int taskId = requestCooldowns.get(receiverId).get(senderId);
        Bukkit.getScheduler().cancelTask(taskId);

        // Cleanup cache.
        removeCooldown(senderId, receiverId);
    }

    /**
     * Check if sender has a request cooldown for this player.
     * @param sender Sender.
     * @param receiver Receiver.
     * @return True, if sender has a request cooldown. Else false.
     */
    public boolean hasCooldown(Player sender, Player receiver) {
        return hasCooldown(sender.getUniqueId(), receiver.getUniqueId());
    }

    /**
     * Check if sender has a request cooldown for this player.
     * @param senderId Sender id.
     * @param receiverId Receiver id.
     * @return True, if sender has a request cooldown. Else false.
     */
    public boolean hasCooldown(UUID senderId, UUID receiverId) {
        return requestCooldowns.containsKey(receiverId) && requestCooldowns.get(receiverId).containsKey(senderId);
    }

    // Getter & Setter

    public TPAConfig getConfig() {
        return config;
    }

    public IgnoreManager getIgnoreManager() {
        return ignoreManager;
    }
}
