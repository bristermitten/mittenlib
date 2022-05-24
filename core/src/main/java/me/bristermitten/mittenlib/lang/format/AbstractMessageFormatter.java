package me.bristermitten.mittenlib.lang.format;

import me.bristermitten.mittenlib.lang.format.hook.FormattingHook;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Set;

public abstract class AbstractMessageFormatter implements MessageFormatter {
    protected final @Unmodifiable Set<FormattingHook> hooks;

    @Inject
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
