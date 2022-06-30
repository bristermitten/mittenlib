package me.bristermitten.mittenlib.commands.handlers;

import co.aikar.commands.BukkitCommandExecutionContext;
import co.aikar.commands.BukkitCommandIssuer;
import co.aikar.commands.CommandConditions;
import org.jetbrains.annotations.NotNull;

/**
 * A condition that can be placed upon an argument of a given type.
 * @param <T> The type of the argument.
 */
public interface ArgumentCondition<T> extends CommandConditions.ParameterCondition<T, BukkitCommandExecutionContext, BukkitCommandIssuer> {
    @NotNull String id();

    @NotNull Class<T> type();
}
