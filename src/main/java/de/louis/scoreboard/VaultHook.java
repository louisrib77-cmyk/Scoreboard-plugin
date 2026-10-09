package de.louis.scoreboard;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

/** Wird nur geladen, wenn Vault installiert ist. */
final class VaultHook {
    private VaultHook() {}

    static double getBalance(OfflinePlayer player) {
        RegisteredServiceProvider<Economy> rsp =
                Bukkit.getServicesManager().getRegistration(Economy.class);
        return rsp == null ? 0.0 : rsp.getProvider().getBalance(player);
    }
}
