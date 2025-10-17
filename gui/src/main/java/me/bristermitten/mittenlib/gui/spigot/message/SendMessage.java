package me.bristermitten.mittenlib.gui.spigot.message;

import me.bristermitten.mittenlib.gui.spigot.SpigotGUIView;

public class SendMessage<T> implements SpigotMessage<T> {
    private final T value;
    private final String rawMessage;

    public SendMessage(T value, String rawMessage) {
        this.value = value;
        this.rawMessage = rawMessage;
    }

    @Override
    public T run(SpigotGUIView<?> view) {
        return null;
    }
}
