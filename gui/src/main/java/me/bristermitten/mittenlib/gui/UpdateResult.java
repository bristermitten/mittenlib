package me.bristermitten.mittenlib.gui;

import me.bristermitten.mittenlib.gui.command.Command;
import org.jspecify.annotations.NonNull;


public class UpdateResult<Model, Msg, Cmd extends Command<Msg>> {
    private final Model model;
    private final Cmd command;

    public UpdateResult(@NonNull Model model, @NonNull Cmd command) {
        this.model = model;
        this.command = command;
    }

    public Model getModel() {
        return model;
    }

    public Cmd getCommand() {
        return command;
    }
}
