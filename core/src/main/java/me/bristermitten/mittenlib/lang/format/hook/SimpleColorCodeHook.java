package me.bristermitten.mittenlib.lang.format.hook;

import me.bristermitten.mittenlib.util.Version;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

public class SimpleColorCodeHook implements FormattingHook {
    @Override
    public boolean shouldRegister() {
        return Version.getServerVersion().isOlderThan(Version.VER_1_16);
    }

    @Override
    public String format(String message, @Nullable OfflinePlayer player) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
