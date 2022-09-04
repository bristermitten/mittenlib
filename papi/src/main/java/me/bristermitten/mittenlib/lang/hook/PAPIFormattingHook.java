package me.bristermitten.mittenlib.lang.hook;

import me.bristermitten.mittenlib.lang.format.hook.FormattingHook;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A {@link FormattingHook} which applies PlaceholderAPI placeholders to the message.
 */
public class PAPIFormattingHook implements FormattingHook {
    @Override
    public boolean shouldRegister() {
        return Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
    }

    @Override
    public @NotNull String format(@NotNull String message, @Nullable OfflinePlayer player) {
        return PlaceholderAPI.setPlaceholders(player, message);
    }
}
