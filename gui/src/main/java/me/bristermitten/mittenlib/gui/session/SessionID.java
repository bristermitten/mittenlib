package me.bristermitten.mittenlib.gui.session;

import me.bristermitten.mittenlib.gui.view.InventoryViewer;
import me.bristermitten.mittenlib.gui.view.View;

import java.util.UUID;

public class SessionID<Model, Msg, V extends View<Msg, V, Viewer>, Viewer extends InventoryViewer<Msg, V>> {
    private final UUID id;

    public SessionID(UUID id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        SessionID<?, ?, ?, ?> sessionID = (SessionID<?, ?, ?, ?>) obj;

        return id.equals(sessionID.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "SessionID{" +
                "id=" + id +
                '}';
    }
}
