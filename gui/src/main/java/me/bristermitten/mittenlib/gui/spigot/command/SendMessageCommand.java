package me.bristermitten.mittenlib.gui.spigot.command;

import java.util.function.Consumer;

public class SendMessageCommand<T> implements SpigotCommand<T> {
    private final String rawMessage;

    public SendMessageCommand(String rawMessage) {
        this.rawMessage = rawMessage;
    }

    @Override
    public void run(SpigotCommandContext context, Consumer<T> continuation) {
        context.player().sendMessage(rawMessage);
    }
}
