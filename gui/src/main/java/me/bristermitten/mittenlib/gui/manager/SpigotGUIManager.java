package me.bristermitten.mittenlib.gui.manager;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.bristermitten.mittenlib.gui.GUIBase;
import me.bristermitten.mittenlib.gui.event.EventBus;
import me.bristermitten.mittenlib.gui.session.GUISession;
import me.bristermitten.mittenlib.gui.session.SessionID;
import me.bristermitten.mittenlib.gui.spigot.InventoryStorage;
import me.bristermitten.mittenlib.gui.view.InventoryViewer;
import me.bristermitten.mittenlib.gui.view.View;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spigot implementation of GUIManager that manages GUI sessions and their lifecycles.
 * Uses dependency injection and reactive event handling.
 */
@Singleton
public class SpigotGUIManager implements GUIManager {

    private final EventBus eventBus;
    private final InventoryStorage inventoryStorage;
    private final Map<SessionID<?, ?, ?, ?>, GUISession<?, ?, ?, ?>> activeSessions;
    private final Map<Object, SessionID<?, ?, ?, ?>> viewerToSession;

    @Inject
    public SpigotGUIManager(EventBus eventBus, InventoryStorage inventoryStorage) {
        this.eventBus = eventBus;
        this.inventoryStorage = inventoryStorage;
        this.activeSessions = new ConcurrentHashMap<>();
        this.viewerToSession = new ConcurrentHashMap<>();
    }

    @Override
    public <Model, Command, V extends View<Command, V, Viewer>, Viewer extends InventoryViewer<Command, V>>
    SessionID<Model, Command, V, Viewer> startSession(GUIBase<Model, Command, V> gui, Viewer viewer) {

        // Close any existing session for this viewer
        Optional<GUISession<Model, Command, V, Viewer>> existingSession = getSessionByViewer(viewer);
        existingSession.ifPresent(session -> closeSession(session.getSessionId()));

        // Create new session
        SessionID<Model, Command, V, Viewer> sessionId = new SessionID<>(UUID.randomUUID());
        GUISession<Model, Command, V, Viewer> session = new GUISession<>(sessionId, gui, viewer, eventBus, inventoryStorage);

        // Store session mappings
        activeSessions.put(sessionId, session);
        viewerToSession.put(viewer, sessionId);

        // Start the session
        session.start();

        // Set up cleanup when session completes
        session.getCompletionFuture().whenComplete((model, throwable) -> {
            activeSessions.remove(sessionId);
            viewerToSession.remove(viewer);
        });

        return sessionId;
    }

    @Override
    public <Model, Command, V extends View<Command, V, Viewer>, Viewer extends InventoryViewer<Command, V>> boolean sendCommand(SessionID<Model, Command, V, Viewer> sessionId, Command command) {

        GUISession<Model, Command, V, Viewer> session = getSession(sessionId).orElse(null);
        if (session != null && session.isActive()) {
            return session.processCommand(command);
        }
        return false;
    }

    @Override
    public <Model, Command, V extends View<Command, V, Viewer>, Viewer extends InventoryViewer<Command, V>> boolean closeSession(SessionID<Model, Command, V, Viewer> sessionId) {
        GUISession<?, ?, ?, ?> session = activeSessions.get(sessionId);
        if (session != null) {
            session.close();
            activeSessions.remove(sessionId);
            viewerToSession.remove(session.getViewer());
            return true;
        }
        return false;
    }

    @Override
    public <Model, Command, V extends View<Command, V, Viewer>, Viewer extends InventoryViewer<Command, V>> Optional<GUISession<Model, Command, V, Viewer>> getSession(SessionID<Model, Command, V, Viewer> sessionId) {
        //noinspection unchecked
        return Optional.ofNullable((GUISession<Model, Command, V, Viewer>) activeSessions.get(sessionId));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public <Model, Command, V extends View<Command, V, Viewer>, Viewer extends InventoryViewer<Command, V>>
    Optional<GUISession<Model, Command, V, Viewer>> getSessionByViewer(Viewer viewer) {
        SessionID sessionId = viewerToSession.get(viewer);
        if (sessionId != null) {
            return getSession(sessionId);
        }
        return Optional.empty();
    }

    @Override
    public void shutdown() {
        // Close all active sessions
        activeSessions.values().forEach(GUISession::close);
        activeSessions.clear();
        viewerToSession.clear();
    }

    /**
     * Gets the number of active sessions.
     *
     * @return the number of active sessions
     */
    public int getActiveSessionCount() {
        return activeSessions.size();
    }
}