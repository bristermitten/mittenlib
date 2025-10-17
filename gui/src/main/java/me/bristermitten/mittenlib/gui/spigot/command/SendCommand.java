package me.bristermitten.mittenlib.gui.spigot.command;

import me.bristermitten.mittenlib.gui.spigot.SpigotGUIView;

public class SendCommand<T> implements SpigotCommand<T> {
    private final T value;
    private final String rawMessage;

    public SendCommand(T value, String rawMessage) {
        this.value = value;
        this.rawMessage = rawMessage;
    }

    @Override
    public T run(SpigotGUIView<?> view) {
        return null;
    }
}
