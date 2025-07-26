package me.bristermitten.mittenlib.gui;

/**
 * Base interface for GUI applications, based on the Model-View-Command pattern (Elm Architecture).
 *
 * @param <Model>   the type of the model that holds the state of the application
 * @param <Command> the type of commands that can be executed to update the model
 * @param <View>    the type of view that renders the model and waits for commands
 */
public interface GUIBase<Model, Command, View> {

    Model init();

    Model update(Model model, Command command);

    View render(Model model);
}
