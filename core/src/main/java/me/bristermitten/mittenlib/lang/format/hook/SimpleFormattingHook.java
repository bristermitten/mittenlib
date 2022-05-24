package me.bristermitten.mittenlib.lang.format.hook;

import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;

/**
 * A formatting hook that is always applied, and uses a lambda to format the string.
 */
public class SimpleFormattingHook implements FormattingHook {
    private final BiFunction<String, @Nullable OfflinePlayer, String> formatter;

    public SimpleFormattingHook(BiFunction<String, @Nullable OfflinePlayer, String> formatter) {
        this.formatter = formatter;
    }

    @Override
    public boolean shouldRegister() {
        return true;
    }

    @Override
    public String format(String message, @Nullable OfflinePlayer player) {
        return formatter.apply(message, player);
    }
}
