package me.bristermitten.mittenlib.gui.textual;

import me.bristermitten.mittenlib.gui.command.CommandContext;

import java.util.Scanner;

public interface TextualViewContext extends CommandContext {
    Scanner scanner();

}
