package me.bristermitten.mittenlib.lang.format.hook;

import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

public interface FormattingHook {
    boolean shouldRegister();

    String format(String message, @Nullable OfflinePlayer player);
}
