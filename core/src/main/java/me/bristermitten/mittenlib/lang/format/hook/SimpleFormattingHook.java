package me.bristermitten.mittenlib.lang.format.hook;

import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;

/**
 * A formatting hook that is always applied, and uses a lambda to format the string.
 */
public class SimpleFormattingHook implements FormattingHook {
    private final BiFunction<String, @Nullable OfflinePlayer, String> formatter;

    /**
     * Creates a SimpleFormattingHook configured with supplied BiFunction.
     *
     * @param formatter a BiFunction that provides an implementation for {@link #format(String, OfflinePlayer)}.
     * @throws NullPointerException if formatter is null
     * @see #format(String, OfflinePlayer) for documentation on the function's behaviour
     */
    public SimpleFormattingHook(BiFunction<String, @Nullable OfflinePlayer, String> formatter) {
        this.formatter = formatter;
    }

    @Override
    public boolean shouldRegister() {
        return true;
    }

    @Override
    public @NotNull String format(@NotNull String message, @Nullable OfflinePlayer player) {
        return formatter.apply(message, player);
    }
}
