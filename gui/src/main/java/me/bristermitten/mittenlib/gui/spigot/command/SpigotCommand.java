package me.bristermitten.mittenlib.gui.spigot.command;

import me.bristermitten.mittenlib.gui.command.Command;

import java.util.function.Consumer;

public interface SpigotCommand<Msg> extends Command<SpigotCommandContext, Msg> {

    static <Msg> SpigotCommand<Msg> of(Consumer<SpigotCommandContext> command) {
        return (context, continuation) -> {
            command.accept(context);
        };
    }
}
