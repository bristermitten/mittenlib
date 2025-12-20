package me.bristermitten.mittenlib.gui.spigot.command;

import me.bristermitten.mittenlib.gui.command.CommandContext;
import org.bukkit.entity.Player;

public interface SpigotCommandContext extends CommandContext {
    /**
     * The player executing the command.
     */
    Player player();

    /**
     * Helper to close the inventory (Valid side effect).
     * Note: In most cases, you should let the Session handle closing via Model updates,
     * but this is useful for "Force Close" commands.
     */
    void closeInventory();
}
