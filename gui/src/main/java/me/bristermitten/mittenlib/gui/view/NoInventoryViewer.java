package me.bristermitten.mittenlib.gui.view;

public interface NoInventoryViewer<Command,
        This extends View<Command, This, ? extends InventoryViewer<Command, This>>>
        extends InventoryViewer<Command, This> {
}
