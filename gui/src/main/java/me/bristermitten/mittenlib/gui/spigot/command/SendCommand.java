package me.bristermitten.mittenlib.gui.spigot.command;

import me.bristermitten.mittenlib.gui.command.Command;

import java.util.function.Consumer;

public class SendCommand<T> implements Command<SpigotCommandContext, T> {
    private final T value;
    private final String rawMessage;

    public SendCommand(T value, String rawMessage) {
        this.value = value;
        this.rawMessage = rawMessage;
    }

    @Override
    public void run(SpigotCommandContext context, Consumer<T> dispatch) {
        if (dispatch != null) {
            dispatch.accept(value);
        }
    }
}
