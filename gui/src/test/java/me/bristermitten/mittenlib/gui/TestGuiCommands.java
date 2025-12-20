package me.bristermitten.mittenlib.gui;

import me.bristermitten.mittenlib.gui.textual.TextualViewCommand;

public class TestGuiCommands {
    public static TextualViewCommand<CounterMessage> askForValue() {
        return (ctx, dispatch) -> {
            System.out.println("Please enter a number:");
            int input = ctx.scanner().nextInt();
            dispatch.accept(new CounterMessage.Set(input));
        };
    }
}
