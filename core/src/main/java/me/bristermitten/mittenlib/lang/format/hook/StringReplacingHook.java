package me.bristermitten.mittenlib.lang.format.hook;

import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;

/**
 * A formatting hook that applies string replacements
 */
public class StringReplacingHook implements FormattingHook {

    private final Set<Map.Entry<String, Object>> replacements;

    /**
     * Create a new StringReplacingHook
     *
     * @param replacements The replacements to apply
     *                     These should be in the format {@code key, value}, so the length should always be a multiple of 2
     *                     If a {@link Supplier} is used as a value, it will be called each time the hook is applied, but only if the key is found in the message
     */
    public StringReplacingHook(Object... replacements) {
        if (replacements.length % 2 != 0) {
            throw new IllegalArgumentException("Replacements must be in pairs");
        }
        this.replacements = new HashSet<>(replacements.length / 2);
        for (int i = 0; i < replacements.length; i += 2) {
            this.replacements.add(new AbstractMap.SimpleEntry<>((String) replacements[i], replacements[i + 1]));
        }
    }

    @Override
    public boolean shouldRegister() {
        return true;
    }

    @Override
    public @NotNull String format(@NotNull String message, @Nullable OfflinePlayer player) {
        for (Map.Entry<String, Object> replacement : replacements) {
            if (message.contains(replacement.getKey())) {
                message = message.replace(replacement.getKey(), getStringValue(replacement.getValue()));
            }
        }
        return message;
    }

    private String getStringValue(Object s) {
        if (s instanceof Supplier) {
            //noinspection rawtypes
            return getStringValue(((Supplier) s).get());
        }
        return Objects.toString(s);
    }
}
