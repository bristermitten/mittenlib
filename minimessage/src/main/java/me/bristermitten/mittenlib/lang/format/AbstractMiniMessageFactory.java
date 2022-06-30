package me.bristermitten.mittenlib.lang.format;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A factory for creating {@link MiniMessage instances}.
 * The standard implementation is {@link DefaultMiniMessageFactory}.
 */
public interface AbstractMiniMessageFactory {
    /**
     * Create a {@link MiniMessage} instance for a player.
     * No guarantees are made about when this method will be called.
     * The implementation is not required to use its player argument.
     * Implementations are also not required to return a new instance each time.
     * @param player the player to create the {@link MiniMessage} for
     * @return a {@link MiniMessage} instance
     */
    @NotNull MiniMessage create(@Nullable OfflinePlayer player);
}
