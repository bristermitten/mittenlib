package me.bristermitten.mittenlib.gui.view;

public interface View<Msg,
        ThisView extends View<Msg, ThisView, Viewer>,
        Viewer
                extends InventoryViewer<Msg, ThisView>> {

    void display(Viewer inventoryViewer);

    Msg waitForCommand();
}
