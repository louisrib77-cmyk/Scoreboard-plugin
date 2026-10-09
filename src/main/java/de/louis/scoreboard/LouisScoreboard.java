package de.louis.scoreboard;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.UUID;

public class LouisScoreboard extends JavaPlugin implements CommandExecutor {

    private volatile MoneyProvider moneyProvider;
    private MoneyProvider jobsProvider;
    private boolean vaultPresent;
    private BoardManager boards;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        vaultPresent = Bukkit.getPluginManager().getPlugin("Vault") != null;
        hookJobsPlugin();
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

    /** Liest das Geld direkt aus dem JobsPlugin (per Reflection, kein Compile-Link nötig). */
    private void hookJobsPlugin() {
        Plugin jobs = Bukkit.getPluginManager().getPlugin("JobsPlugin");
        if (jobs == null || !jobs.isEnabled()) {
            getLogger().info("JobsPlugin nicht gefunden - Geld kommt aus Vault bzw. 0.");
            return;
        }
        try {
            Method getDataManager = jobs.getClass().getMethod("getDataManager");
            Object dataManager = getDataManager.invoke(jobs);
            Method get = dataManager.getClass().getMethod("get", UUID.class);
            Method getMoney = get.getReturnType().getMethod("getMoney");
            jobsProvider = uuid -> {
                try {
                    Object data = get.invoke(dataManager, uuid);
                    return data == null ? 0.0 : (double) getMoney.invoke(data);
                } catch (ReflectiveOperationException e) {
                    return 0.0;
                }
            };
            getLogger().info("JobsPlugin gefunden - Geld wird von den Jobs angezeigt.");
        } catch (ReflectiveOperationException e) {
            getLogger().warning("JobsPlugin konnte nicht angebunden werden: " + e);
        }
    }

    double getMoney(Player player) {
        MoneyProvider mp = moneyProvider;
        if (mp != null) return mp.getBalance(player.getUniqueId());
        if (jobsProvider != null) return jobsProvider.getBalance(player.getUniqueId());
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
