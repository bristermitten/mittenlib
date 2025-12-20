package me.bristermitten.mittenlib.gui;

import me.bristermitten.mittenlib.gui.command.Command;
import me.bristermitten.mittenlib.gui.command.CommandContext;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;


public class UpdateResult<Model, Msg, Ctx extends CommandContext, Cmd extends Command<Ctx, Msg>> {
    private final Model model;
    private final Cmd command;

    public UpdateResult(@NonNull Model model, @Nullable Cmd command) {
        this.model = model;
        this.command = command;
    }

    public static <Model, Msg, Ctx extends CommandContext, Cmd extends Command<Ctx, Msg>> UpdateResult<Model, Msg, Ctx, Cmd> of(@NonNull Model model, @NonNull Cmd command) {
        return new UpdateResult<>(model, command);
    }

    public static <Model, Msg, Ctx extends CommandContext, Cmd extends Command<Ctx, Msg>> UpdateResult<Model, Msg, Ctx, Cmd> pure(@NonNull Model model) {
        return new UpdateResult<>(model, null);
    }

    public Model getModel() {
        return model;
    }

    public Cmd getCommand() {
        return command;
    }
}
