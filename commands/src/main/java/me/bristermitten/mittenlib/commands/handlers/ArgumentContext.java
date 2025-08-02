package me.bristermitten.mittenlib.commands.handlers;

import co.aikar.commands.BukkitCommandExecutionContext;
import co.aikar.commands.contexts.ContextResolver;
import org.jetbrains.annotations.NotNull;

/**
 * Handles resolution of an argument to a value of type {@link T}.
 *
 * @param <T> The type of the argument
 */
public interface ArgumentContext<T> extends ContextResolver<T, BukkitCommandExecutionContext> {
    /**
     * The type of the argument that this context applies to.
     * @return The type that this context applies to.
     */
    @NotNull Class<T> type();
}
