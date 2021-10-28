package me.bristermitten.mittenlib.lang.format;

import me.bristermitten.mittenlib.lang.format.hook.FormattingHook;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.util.Set;

public abstract class AbstractMessageFormatter implements MessageFormatter {
    private final Set<FormattingHook> hooks;

    @Inject
    protected AbstractMessageFormatter(Set<FormattingHook> hooks) {
        this.hooks = hooks;
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
