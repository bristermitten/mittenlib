package me.bristermitten.mittenlib.lang.format;

import me.bristermitten.mittenlib.lang.format.hook.FormattingHook;
import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A MessageFormatter is responsible for formatting a String into a {@link Component},
 * applying some {@link FormattingHook}s to the String.
 */
public interface MessageFormatter {
    /**
     * Completely format a String into a {@link Component},
     * applying all {@link FormattingHook}s to the String.
     *
     * @param message the message to format
     * @param player  the player to format for. This can be used for player-specific formatting, such as PlaceholderAPI
     * @return the formatted String as a {@link Component}
     */
    @NotNull Component format(@NotNull String message, @Nullable OfflinePlayer player);

    /**
     * Apply all {@link FormattingHook}s to a String, returning the formatted String.
     * This method is handy when {@link Component}s cannot be used but may leave the String not completely formatted,
     * for example preserving legacy formatting codes ({@code &c})
     *
     * @param message the message to format
     * @param player  the player to format for. This can be used for player-specific formatting, such as PlaceholderAPI
     * @return the formatted String
     */

    @NotNull String preFormat(@NotNull String message, @Nullable OfflinePlayer player);

    /**
     * Creates a new {@link MessageFormatter} that applies the given {@link FormattingHook}s to the String, alongside the existing hooks.
     * This method should always create a new {@link MessageFormatter}, and should not modify the existing one.
     *
     * @param hooks the hooks to add
     * @return the new {@link MessageFormatter}
     */
    @NotNull MessageFormatter withExtraHooks(@NotNull FormattingHook... hooks);

}
