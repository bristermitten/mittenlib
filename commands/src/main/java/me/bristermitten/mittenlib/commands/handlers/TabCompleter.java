package me.bristermitten.mittenlib.commands.handlers;

import co.aikar.commands.BukkitCommandCompletionContext;
import co.aikar.commands.CommandCompletions;
import org.jetbrains.annotations.NotNull;

/**
 * A tab completer for a command argument.
 */
public interface TabCompleter extends CommandCompletions.AsyncCommandCompletionHandler<BukkitCommandCompletionContext> {
    @NotNull String id();
}
