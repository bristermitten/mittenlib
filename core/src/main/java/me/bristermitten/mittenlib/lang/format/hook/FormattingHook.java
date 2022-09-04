package me.bristermitten.mittenlib.lang.format.hook;

import me.bristermitten.mittenlib.lang.format.MessageFormatter;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A hook for formatting a String. This is generally called by a {@link MessageFormatter} as part of a larger formatting
 * process.
 */
public interface FormattingHook {
    /**
     * If the hook should be used. This should generally be called once per format process.
     * Note that this method does not accept any parameters. It should be used as a pre-check to determine if the hook
     * should be used at all, rather than conditional based on the input.
     * For example, checking if an API (e.g. PlaceholderAPI) is present on the server before running any formatting
     * to avoid {@link ClassNotFoundException}s, or checking the server version.
     * If conditional formatting is desired, do this in {@link #format(String, OfflinePlayer)}, and return the unchanged
     * input if the hook should not be used.
     *
     * @return if the hook should be used for this format process
     */
    boolean shouldRegister();

    /**
     * Format a String, returning the formatted String.
     * This method should generally be idempotent, and should not be called if {@link #shouldRegister()} returns false.
     *
     * @param message the message to format
     * @param player  the player to format for. This can be used for player-specific formatting, such as PlaceholderAPI
     * @return the formatted String
     */
    @NotNull String format(@NotNull String message, @Nullable OfflinePlayer player);
}
