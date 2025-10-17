package me.bristermitten.mittenlib.gui.spigot.message;

import me.bristermitten.mittenlib.gui.command.Command;
import me.bristermitten.mittenlib.gui.spigot.SpigotGUIView;

public interface SpigotMessage<T> extends Command<T> {
    T run(SpigotGUIView<?> view);
}
