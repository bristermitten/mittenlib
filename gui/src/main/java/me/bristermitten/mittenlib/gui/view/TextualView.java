package me.bristermitten.mittenlib.gui.view;

import java.util.Map;
import java.util.Scanner;

public class TextualView<Command> implements ViewerlessView<Command, TextualView<Command>> {

    private final String text;
    private final Map<String, Command> actions;

    public TextualView(String text, Map<String, Command> actions) {
        this.text = text;
        this.actions = actions;
    }

    public static <Command> TextualView<Command> of(String text, Map<String, Command> actions) {
        return new TextualView<>(text, actions);
    }

    @Override
    public void display() {
        System.out.println(text);
        System.out.println("Available actions:");
        for (Map.Entry<String, Command> entry : actions.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
    }

    @Override
    public Command waitForCommand() {
        System.out.print("Enter command: ");
        try (Scanner scanner = new Scanner(System.in)) {
            if (!scanner.hasNextLine()) {
                return null; // No input received
            }
            String input = scanner.nextLine();
            return actions.getOrDefault(input, null);
        }

    }
}
