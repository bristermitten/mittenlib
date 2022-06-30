package me.bristermitten.mittenlib.commands.handlers;

import co.aikar.commands.BukkitCommandExecutionContext;
import co.aikar.commands.contexts.IssuerOnlyContextResolver;

/**
 * An {@link ArgumentContext} that is only aware of the issuer of the command, and not the argument's value.
 */
public interface IssuerOnlyArgumentContext<T> extends ArgumentContext<T>, IssuerOnlyContextResolver<T, BukkitCommandExecutionContext> {
}
