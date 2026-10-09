package de.louis.scoreboard;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class LouisScoreboard extends JavaPlugin implements CommandExecutor {

    private volatile MoneyProvider moneyProvider;
    private boolean vaultPresent;
    private BoardManager boards;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        vaultPresent = Bukkit.getPluginManager().getPlugin("Vault") != null;
        boards = new BoardManager(this);
        boards.start();
        var cmd = getCommand("sb");
        if (cmd != null) cmd.setExecutor(this);
    }

    @Override
    public void onDisable() {
        if (boards != null) boards.stop();
    }

    /**
     * Für das Jobs-Plugin: Geldquelle setzen, z. B.
     * scoreboard.setMoneyProvider(uuid -> jobsManager.getMoney(uuid));
     * Hat Vorrang vor Vault.
     */
    public void setMoneyProvider(MoneyProvider provider) {
        this.moneyProvider = provider;
    }

    double getMoney(Player player) {
        MoneyProvider mp = moneyProvider;
        if (mp != null) return mp.getBalance(player.getUniqueId());
        if (vaultPresent) return VaultHook.getBalance(player);
        return 0.0;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("louisscoreboard.reload")) {
                sender.sendMessage("Dafür hast du keine Rechte.");
                return true;
            }
            reloadConfig();
            boards.reload();
            sender.sendMessage("Scoreboard neu geladen.");
            return true;
        }
        if (sender instanceof Player p) {
            boolean on = boards.toggle(p);
            p.sendMessage(on ? "Scoreboard eingeschaltet." : "Scoreboard ausgeschaltet.");
        } else {
            sender.sendMessage("Nur für Spieler.");
        }
        return true;
    }
}
