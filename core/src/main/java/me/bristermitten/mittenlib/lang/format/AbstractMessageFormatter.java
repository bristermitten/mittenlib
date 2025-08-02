package me.bristermitten.mittenlib.lang.format;

import me.bristermitten.mittenlib.lang.format.hook.FormattingHook;
import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collections;
import java.util.Set;

/**
 * An abstract implementation of {@link MessageFormatter} that provides a default implementation of {@link #preFormat(String, OfflinePlayer)}.
 * This implementation keeps a set of {@link FormattingHook}s, and calls them sequentially.
 * <p>
 * This implementation does not implement {@link #format(String, OfflinePlayer)}, leaving the {@link Component}
 * creation to the subclasses.
 */
public abstract class AbstractMessageFormatter implements MessageFormatter {
    /**
     * The set of {@link FormattingHook}s to use.
     */
    protected final @Unmodifiable Set<FormattingHook> hooks;

    /**
     * Create a new {@link AbstractMessageFormatter} with the given {@link FormattingHook}s.
     *
     * @param hooks the hooks to use
     */
    protected AbstractMessageFormatter(Set<FormattingHook> hooks) {
        this.hooks = Collections.unmodifiableSet(hooks);
    }

    @Override
    public @NotNull String preFormat(@NotNull String message, @Nullable OfflinePlayer player) {
        for (FormattingHook hook : hooks) {
            if (hook.shouldRegister()) {
                message = hook.format(message, player);
            }
        }
        return message;
    }
}
