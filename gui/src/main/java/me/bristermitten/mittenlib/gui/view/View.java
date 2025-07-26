package me.bristermitten.mittenlib.gui.view;

public interface View<Command, ThisView extends View<Command, ThisView, Viewer>, Viewer extends InventoryViewer<Command, ThisView>> {

    void display(Viewer inventoryViewer);

    Command waitForCommand();
}
