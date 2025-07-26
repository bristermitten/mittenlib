package me.bristermitten.mittenlib.gui;

import me.bristermitten.mittenlib.gui.view.InventoryViewer;
import me.bristermitten.mittenlib.gui.view.View;

public class GUIExecutor<Model,
        V extends View<Command, V, Viewer>,
        Viewer extends InventoryViewer<Command, V>,
        Command,
        GUI extends GUIBase<Model, Command, V>> {

    private final GUI gui;

    public GUIExecutor(GUI gui) {
        this.gui = gui;
    }


    public Model execute(Viewer inventoryViewer) {
        Model model = gui.init();
        V view = gui.render(model);

        while (true) {
            view.display(inventoryViewer);
            Command command = view.waitForCommand();
            if (command == null) {
                break; // Exit if no command is received
            }
            model = gui.update(model, command);
            view = gui.render(model);
        }

        return model; // Return the final model after execution
    }
}
