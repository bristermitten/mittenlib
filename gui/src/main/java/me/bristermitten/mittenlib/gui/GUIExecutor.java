package me.bristermitten.mittenlib.gui;

import me.bristermitten.mittenlib.gui.view.InventoryViewer;
import me.bristermitten.mittenlib.gui.view.View;

public class GUIExecutor<Model,
        V extends View<Msg, V, Viewer>,
        Viewer extends InventoryViewer<Msg, V>,
        Msg,
        Cmd extends me.bristermitten.mittenlib.gui.command.Command<Msg>,
        GUI extends GUIBase<Model, Msg, V, Cmd>> {

    private final GUI gui;

    public GUIExecutor(GUI gui) {
        this.gui = gui;
    }


    public Model execute(Viewer inventoryViewer) {

        Model model = gui.init();
        V view = gui.render(model);

        while (true) {
            view.display(inventoryViewer);
            Msg command = view.waitForCommand();
            if (command == null) {
                break; // Exit if no command is received
            }
            UpdateResult<Model, Msg, Cmd> cmd = gui.update(model, command);
            model = cmd.getModel();

            if (cmd.getCommand() != null) {
                throw new UnsupportedOperationException("Commands are not supported in this GUIExecutor implementation.");
            }

            view = gui.render(model);
        }

        return model; // Return the final model after execution
    }
}
