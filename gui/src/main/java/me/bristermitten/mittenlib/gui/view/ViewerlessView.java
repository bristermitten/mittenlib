package me.bristermitten.mittenlib.gui.view;

public interface ViewerlessView<Command, This extends ViewerlessView<Command, This>> extends View<Command, This, NoInventoryViewer<Command, This>> {

    void display();

    @Override
    default void display(NoInventoryViewer<Command, This> inventoryViewer) {
        display();
    }
}
