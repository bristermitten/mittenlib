package me.bristermitten.mittenlib.lang.format.hook;

import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SimpleColorCodeHook implements FormattingHook {
    @Override
    public boolean shouldRegister() {
        return true;
    }

    @Override
    public @NotNull String format(@NotNull String message, @Nullable OfflinePlayer player) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
