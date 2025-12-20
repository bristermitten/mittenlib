package me.bristermitten.mittenlib.gui.spigot.command;

import me.bristermitten.mittenlib.gui.command.CommandContext;
import org.bukkit.entity.Player;

/**
 * The context for a Spigot command execution.
 */
public interface SpigotCommandContext extends CommandContext {
    /**
     * The player executing the command / viewing the GUI.
     */
    Player player();

    /**
     * Close the inventory of the player associated with this context.
     * Note: In most cases, closing the inventory should be done with a Message and Command that explicitly closes it.
     */
    void closeInventory();
}
