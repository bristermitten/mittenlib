package me.bristermitten.mittenlib.gui.spigot.command;

import me.bristermitten.mittenlib.gui.command.Command;
import me.bristermitten.mittenlib.gui.spigot.SpigotGUIView;

public interface SpigotCommand<T> extends Command<T> {
    T run(SpigotGUIView<?> view);
}
