package me.bristermitten.mittenlib.gui;

import me.bristermitten.mittenlib.gui.command.Command;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;


public class UpdateResult<Model, Msg, Cmd extends Command<Msg>> {
    private final Model model;
    private final Cmd command;

    public UpdateResult(@NonNull Model model, @Nullable Cmd command) {
        this.model = model;
        this.command = command;
    }

    public Model getModel() {
        return model;
    }

    public Cmd getCommand() {
        return command;
    }

    public static <Model, Msg, Cmd extends Command<Msg>> UpdateResult<Model, Msg, Cmd> of(@NonNull Model model, @NonNull Cmd command) {
        return new UpdateResult<>(model, command);
    }

    public static <Model, Msg, Cmd extends Command<Msg>> UpdateResult<Model, Msg, Cmd> pure(@NonNull Model model) {
        return new UpdateResult<>(model, null);
    }
}
