package me.bristermitten.mittenlib.gui.spigot.message;

import me.bristermitten.mittenlib.gui.message.Message;
import me.bristermitten.mittenlib.gui.spigot.SpigotGUIView;

public interface SpigotMessage<T> extends Message<T> {
    T run(SpigotGUIView<?> view);
}
