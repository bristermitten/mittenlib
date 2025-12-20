package me.bristermitten.mittenlib.gui.command;

import me.bristermitten.mittenlib.gui.view.InventoryViewer;
import me.bristermitten.mittenlib.gui.view.View;

@FunctionalInterface
public interface CommandContextFactory<Ctx extends CommandContext, Msg,
        V extends View<Msg, V, Viewer>, Viewer extends InventoryViewer<Msg, V>> {
    Ctx create(Viewer viewer, V currentView);
}

