package me.bristermitten.mittenlib.gui.view;

public interface View<Command> {

    void display();

    Command waitForCommand();
}
