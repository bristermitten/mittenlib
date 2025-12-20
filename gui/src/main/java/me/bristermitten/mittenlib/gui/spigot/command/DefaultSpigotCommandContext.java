package me.bristermitten.mittenlib.gui.spigot.command;

import org.bukkit.entity.Player;

public class DefaultSpigotCommandContext implements SpigotCommandContext {
    private final Player player;

    public DefaultSpigotCommandContext(Player player) {
        this.player = player;
    }

    @Override
    public Player player() {
        return player;
    }

    @Override
    public void closeInventory() {
        player.closeInventory();
    }
}

