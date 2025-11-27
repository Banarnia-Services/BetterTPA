package de.banarnia.bettertpa.commands.requests;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandIssuer;
import co.aikar.commands.annotation.*;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import de.banarnia.api.lang.LanguageHandler;
import de.banarnia.bettertpa.lang.Message;
import de.banarnia.bettertpa.manager.TPAManager;
import de.banarnia.bettertpa.requests.TpaRequest;
import org.bukkit.entity.Player;

@CommandAlias("tpa")
public class TPACommand extends BaseCommand {

    private TPAManager manager;
    private LanguageHandler languageHandler;

    public TPACommand(TPAManager manager, LanguageHandler languageHandler) {
        this.manager = manager;
        this.languageHandler = languageHandler;
    }

    @Default
    @CommandCompletion("@tpaPlayers")
    public void tpa(Player sender, OnlinePlayer target) {
        manager.sendRequest(new TpaRequest(sender, target.getPlayer()), false);
    }

    @Subcommand("reload")
    @CommandPermission("bettertpa.reload")
    public void reload(CommandIssuer sender) {
        manager.getConfig().reload();
        languageHandler.reload();
        sender.sendMessage(Message.COMMAND_INFO_RELOAD.get());
    }

}
