package me.bristermitten.mittenlib.gui;

import me.bristermitten.mittenlib.gui.view.View;

public class GUIExecutor<Model, V extends View<Command>, Command, GUI extends GUIBase<Model, Command, V>> {

    private final GUI gui;

    public GUIExecutor(GUI gui) {
        this.gui = gui;
    }


    public Model execute() {
        Model model = gui.init();
        V view = gui.render(model);

        while (true) {
            view.display();
            Command command = (view).waitForCommand();
            if (command == null) {
                break; // Exit if no command is received
            }
            model = gui.update(model, command);
            view = gui.render(model);
        }

        return model; // Return the final model after execution
    }
}
