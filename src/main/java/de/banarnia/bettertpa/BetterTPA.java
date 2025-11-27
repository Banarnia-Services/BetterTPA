package de.banarnia.bettertpa;

import co.aikar.commands.BukkitCommandManager;
import co.aikar.commands.CommandManager;
import de.banarnia.api.config.Config;
import de.banarnia.api.config.YamlConfig;
import de.banarnia.api.config.YamlVersionConfig;
import de.banarnia.api.lang.LanguageHandler;
import de.banarnia.bettertpa.commands.TPAListCommand;
import de.banarnia.bettertpa.commands.TPAcceptCommand;
import de.banarnia.bettertpa.commands.TPDenyCommand;
import de.banarnia.bettertpa.commands.ignore.TPAIgnoreAllCommand;
import de.banarnia.bettertpa.commands.ignore.TPAIgnoreCommand;
import de.banarnia.bettertpa.commands.requests.TPACommand;
import de.banarnia.bettertpa.commands.requests.TPAHereAllCommand;
import de.banarnia.bettertpa.commands.requests.TPAHereCommand;
import de.banarnia.bettertpa.config.TPAConfig;
import de.banarnia.bettertpa.lang.Message;
import de.banarnia.bettertpa.listener.TPAListener;
import de.banarnia.bettertpa.manager.TPAManager;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class BetterTPA extends JavaPlugin {

    private CommandManager commandManager;
    private TPAConfig tpaConfig;
    private LanguageHandler languageHandler;

    private TPAManager manager;

    private BukkitAudiences adventure;
    private MiniMessage mm;

    @Override
    public void onLoad() {
        super.onLoad();
    }

    @Override
    public void onEnable() {
        super.onEnable();

        // BStats
        int pluginId = 23446;
        Metrics metrics = new Metrics(this, pluginId);

        // ACF
        this.commandManager = new BukkitCommandManager(this);
        commandManager.usePerIssuerLocale(true);

        // Load config.
        Config config = YamlVersionConfig.of(this, getDataFolder(), "config.yml",
                "config.yml", "1.1");
        this.tpaConfig = new TPAConfig(this, config);

        // Add language handler.
        // English language files will be automatically created on first load.
        File langFolder = new File(getDataFolder(), "lang");
        YamlConfig.fromResource(this, "lang/de.yml", langFolder, "de.yml");
        this.languageHandler = new LanguageHandler(this, tpaConfig.getLanguage());
        this.languageHandler.register(Message.class);

        // Adventure.
        this.adventure = BukkitAudiences.create(this);
        this.mm = MiniMessage.miniMessage();

        // Manager.
        this.manager = new TPAManager(this, tpaConfig, adventure, mm);

        // Commands.
        commandManager.registerCommand(new TPAIgnoreCommand(manager.getIgnoreManager()));
        commandManager.registerCommand(new TPAIgnoreAllCommand(manager.getIgnoreManager()));
        commandManager.registerCommand(new TPACommand(manager, languageHandler));
        commandManager.registerCommand(new TPAHereCommand(manager));
        commandManager.registerCommand(new TPAHereAllCommand(manager));
        TPAListCommand listCommand = new TPAListCommand(manager, adventure, mm);
        commandManager.registerCommand(listCommand);
        TPAcceptCommand acceptCommand = new TPAcceptCommand(manager, listCommand);
        commandManager.registerCommand(acceptCommand);
        commandManager.registerCommand(new TPDenyCommand(manager, acceptCommand));

        // Listener.
        Bukkit.getPluginManager().registerEvents(new TPAListener(manager), this);

        // Command Context.
        registerCommandCompletion();
    }

    private void registerCommandCompletion() {
        // Exclude self from tpa completion.
        commandManager.getCommandCompletions().registerCompletion("tpaPlayers", c -> {
            if (!c.getIssuer().isPlayer())
                return new ArrayList<>();

            return Bukkit.getOnlinePlayers().stream().filter(player -> player != c.getIssuer().getIssuer()).map(Player::getName).collect(Collectors.toList());
        });
    }

}
