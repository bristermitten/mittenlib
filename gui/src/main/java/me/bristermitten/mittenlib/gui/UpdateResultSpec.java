package me.bristermitten.mittenlib.gui;

import me.bristermitten.mittenlib.codegen.Record;
import me.bristermitten.mittenlib.gui.command.Command;

@Record
public interface UpdateResultSpec<Model, Msg, Cmd extends Command<Msg>> {
    UpdateResultSpec<Model, Msg, Cmd> create(Model model, Cmd command);
}
