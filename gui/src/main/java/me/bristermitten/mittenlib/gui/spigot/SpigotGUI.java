package me.bristermitten.mittenlib.gui.spigot;

import me.bristermitten.mittenlib.gui.GUIBase;
import me.bristermitten.mittenlib.gui.spigot.command.SpigotCommand;
import me.bristermitten.mittenlib.gui.spigot.command.SpigotCommandContext;

/**
 * Base class for Spigot GUIs.
 *
 * @param <Model> the model type
 * @param <Msg>   the message type
 */
public abstract class SpigotGUI<Model, Msg> implements GUIBase<Model,
        Msg,
        SpigotGUIView<Msg>,
        SpigotCommandContext,
        SpigotCommand<Msg>> {
}
