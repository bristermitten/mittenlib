package me.bristermitten.mittenlib.gui;

import me.bristermitten.mittenlib.gui.command.Command;
import org.jetbrains.annotations.NotNull;

/**
 * Base interface for GUI applications, based on the Model-View-Command pattern (Elm Architecture).
 *
 * @param <Model> the type of the model that holds the state of the application
 * @param <Msg>   the type of messages that can be executed to update the model
 * @param <View>  the type of view that renders the model and waits for commands
 * @param <Cmd>   the type of commands that can be sent to perform side effects
 */
public interface GUIBase<Model, Msg, View, Cmd extends Command<Msg>> {

    @NotNull Model init();

    @NotNull UpdateResult<Model, Msg, Cmd> update(Model model, Msg message);

    @NotNull View render(Model model);
}
