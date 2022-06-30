package me.bristermitten.mittenlib.commands.handlers;

import co.aikar.commands.BukkitCommandIssuer;
import co.aikar.commands.CommandConditions;
import org.jetbrains.annotations.NotNull;

/**
 * A command condition that is referenced by name on the full command rather than an argument.
 */
public interface NamedCondition extends CommandConditions.Condition<BukkitCommandIssuer> {
    @NotNull String id();
}
