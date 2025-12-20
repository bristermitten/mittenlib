package me.bristermitten.mittenlib.gui.spigot.command;

import me.bristermitten.mittenlib.gui.command.Command;

import java.util.function.Consumer;

/**
 * A command that can be executed in a Spigot GUI context.
 *
 * @param <Msg> the type of messages the command can produce
 */
public interface SpigotCommand<Msg> extends Command<SpigotCommandContext, Msg> {

    /**
     * Wrap a simple consumer into a SpigotCommand.
     * <p>
     * Note: Prefer creating specific commands where possible.
     *
     * @param command the command to wrap
     * @param <Msg>   the type of messages the command can produce
     * @return the wrapped command
     */
    static <Msg> SpigotCommand<Msg> of(Consumer<SpigotCommandContext> command) {
        return (context, continuation) -> {
            command.accept(context);
        };
    }

    /**
     * A command that closes the inventory.
     *
     * @param <Msg> the type of messages the command can produce
     * @return the close command
     */
    static <Msg> SpigotCommand<Msg> close() {
        return (context, continuation) -> context.closeInventory();
    }


}
