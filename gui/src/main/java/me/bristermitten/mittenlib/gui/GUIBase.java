package me.bristermitten.mittenlib.gui;

public interface GUIBase<Model, Command, View> {

    Model init();

    Model update(Model model, Command command);

    View render(Model model);
}
