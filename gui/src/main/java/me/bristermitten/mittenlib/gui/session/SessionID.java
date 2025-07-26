package me.bristermitten.mittenlib.gui.session;

import java.util.UUID;

public class SessionID<Model, Command, V, Viewer> {
    private final UUID id;

    public SessionID(UUID id) {
        this.id = id;
    }
}
