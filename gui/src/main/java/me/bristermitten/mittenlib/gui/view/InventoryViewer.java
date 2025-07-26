package me.bristermitten.mittenlib.gui.view;

public interface InventoryViewer<Command,
        V extends View<Command, V, ? extends InventoryViewer<Command, V>>> {
    void display(V view);
}
