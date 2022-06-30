package me.bristermitten.mittenlib.commands.handlers;

import co.aikar.commands.BukkitCommandExecutionContext;
import co.aikar.commands.contexts.IssuerAwareContextResolver;

/**
 * An {@link ArgumentContext} that is aware of the issuer of the command and the argument's value.
 */
public interface IssuerAwareArgumentContext<T> extends ArgumentContext<T>, IssuerAwareContextResolver<T, BukkitCommandExecutionContext> {
}
