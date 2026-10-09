package de.louis.scoreboard;

import java.util.UUID;

/** Wird vom Jobs-Plugin gesetzt, damit das Scoreboard das Job-Geld anzeigt. */
@FunctionalInterface
public interface MoneyProvider {
    double getBalance(UUID playerId);
}
