package de.banarnia.bettertpa.lang;

import de.banarnia.api.lang.ILanguage;

public enum Message implements ILanguage {
    PREFIX("§8[§6BetterTPA§8]§7"),

    // TPA
    TIMER_INFO_TPA_WARMUP("%prefix% You will be teleported in §e%time% seconds§7."),
    TIMER_ÌNFO_TPA_TELEPORT("%prefix% Teleporting to §e%target%§7..."),
    TIMER_ERROR_TPA_WARMUP_MOVE("%prefix% §7Teleport cancelled because you moved during warmup."),
    TIMER_ERROR_TPA_EXPIRED_SENDER("%prefix% §e%target% §7did not react to your TPA-Request in time."),
    TIMER_ERROR_TPA_EXPIRED_RECEIVER("%prefix% §7TPA-Request from §e%target% §7expired."),

    COMMAND_INFO_TPA_SENDER("%prefix% You've sent a TPA-Request to §e%target%§7."),
    COMMAND_INFO_TPA_ACCEPTED_RECEIVER("%prefix% You accepted the TPA-Request from §e%target%§7."),
    COMMAND_INFO_TPA_ACCEPTED_SENDER("%prefix% §e%target% §7accepted your TPA-Request."),
    COMMAND_ERROR_TPA_DENIED_RECEIVER("%prefix% You denied the TPA-Request of §e%target%§7."),
    COMMAND_ERROR_TPA_DENIED_SENDER("%prefix% §e%target% §cdenied the TPA-Request."),
    COMMAND_ERROR_TPA_EVENT_CANCELLED("%prefix% §cThe event was cancelled."),
    COMMAND_ERROR_TPA_SELF("%prefix% §cYou cannot send TPA-Request to yourself."),
    COMMAND_ERROR_TPA_PENDING("%prefix% §cThere is still a pending TPA-Request to that player."),
    COMMAND_ERROR_TPA_COOLDOWN("%prefix% §cYou have to wait before executing a new request to that player."),
    COMMAND_ERROR_TPA_IGNORED("%prefix% §e%target% §cdoes not accept any TPA-Requests right now."),

    COMMAND_ERROR_TPACCEPT_EMPTY("%prefix% §cYou do not have any pending TPA-Requests."),
    COMMAND_ERROR_TPACCEPT_INVALID("%prefix% §cYou do not have any pending TPA-Requests of §e%target%§7."),

    COMMAND_INFO_TPAIGNORE_IGNORE("%prefix% You will no longer receive TPA-Requests from §e%target%§7."),
    COMMAND_INFO_TPAIGNORE_UNIGNORE("%prefix% You will now receive TPA-Requests from §e%target% §7again."),
    COMMAND_ERROR_TPAIGNORE_BYPASS("%prefix% §cYou cannot ignore this player."),
    COMMAND_ERROR_TPAIGNORE_SELF("%prefix% §cYou cannot ignore yourself."),

    COMMAND_INFO_TPAIGNOREALL_IGNORE("%prefix% You will no longer receive TPA-Requests."),
    COMMAND_INFO_TPAIGNOREALL_UNIGNORE("%prefix% You will now receive TPA-Requests again."),

    COMMAND_INFO_RELOAD("%prefix% The config has been reloaded."),

    COMMAND_INFO_TPAREQUEST_RECEIVER(
            "<dark_grey>-----------------------------------------\n" +
                        "<gold> » <yellow>%target% <grey>wants to teleport to you!\n" +
                        "\n" +
                        "<green>                    <hover:show_text:\"<grey>Click to <green>accept\"><click:run_command:/tpaccept>Accept</click></hover>      <red><hover:show_text:\"<grey>Click to <red>deny\"><click:run_command:/tpdeny>Deny</click></hover>\n" +
                        "<dark_grey>-----------------------------------------"
    ),

    COMMAND_INFO_TPAHEREREQUEST_RECEIVER(
            "<dark_grey>-----------------------------------------\n" +
                    "<gold> » <yellow>%target% <grey>wants you to teleport to him!\n" +
                    "\n" +
                    "<green>                    <hover:show_text:\"<grey>Click to <green>accept\"><click:run_command:/tpaccept>Accept</click></hover>      <red><hover:show_text:\"<grey>Click to <red>deny\"><click:run_command:/tpdeny>Deny</click></hover>\n" +
                    "<dark_grey>-----------------------------------------"
    ),
    COMMAND_INFO_LIST_HEADER("§6» §7Pending §eTPA-Requests§7:"),
    COMMAND_INFO_LIST_TPA(" <dark_grey>» <green><hover:show_text:'<grey>Click to <green>accept'><click:run_command:'/tpaccept %target%'>Accept</click></hover></green>  <red><hover:show_text:'<grey>Click to <red>deny'><click:run_command:'/tpdeny %target%'>Deny</click></hover></red> <grey>- <yellow>%target% <grey>→ You"),
    COMMAND_INFO_LIST_TPAHERE(" <dark_grey>» <green><hover:show_text:'<grey>Click to <green>accept'><click:run_command:'/tpaccept %target%'>Accept</click></hover></green>  <red><hover:show_text:'<grey>Click to <red>deny'><click:run_command:'/tpdeny %target%'>Deny</click></hover></red> <grey>- <grey>You → <yellow>%target%"),
    ;

    String defaultMessage, message;

    Message(String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }

    @Override
    public String getKey() {
        return this.toString().toLowerCase().replace("_", "-");
    }

    @Override
    public String getDefaultMessage() {
        return defaultMessage;
    }

    @Override
    public String get() {
        String message = this.message != null ? this.message : defaultMessage;
        if (this == PREFIX)
            return message;

        return message.replace("%prefix%", PREFIX.get());
    }

    @Override
    public void set(String message) {
        this.message = message;
    }
}