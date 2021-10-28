package me.bristermitten.mittenlib.lang.hook;

import me.bristermitten.mittenlib.lang.format.hook.FormattingHook;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

public class PAPIFormattingHook implements FormattingHook {
    @Override
    public boolean shouldRegister() {
        return Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
    }

    @Override
    public String format(String message, @Nullable OfflinePlayer player) {
        return PlaceholderAPI.setPlaceholders(player, message);
    }
}
