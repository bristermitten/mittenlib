package me.bristermitten.mittenlib.gui.manager;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.bristermitten.mittenlib.gui.GUIBase;
import me.bristermitten.mittenlib.gui.command.Command;
import me.bristermitten.mittenlib.gui.command.CommandContext;
import me.bristermitten.mittenlib.gui.command.CommandContextFactory;
import me.bristermitten.mittenlib.gui.event.EventBus;
import me.bristermitten.mittenlib.gui.session.GUISession;
import me.bristermitten.mittenlib.gui.session.SessionID;
import me.bristermitten.mittenlib.gui.spigot.InventoryStorage;
import me.bristermitten.mittenlib.gui.spigot.SpigotGUIView;
import me.bristermitten.mittenlib.gui.spigot.SpigotInventoryViewer;
import me.bristermitten.mittenlib.gui.spigot.command.DefaultSpigotCommandContext;
import me.bristermitten.mittenlib.gui.view.InventoryViewer;
import me.bristermitten.mittenlib.gui.view.View;
import org.jetbrains.annotations.NotNull;

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
    private final Map<SessionID<?, ?, ?, ?>, GUISession<?, ?, ?, ?, ?>> activeSessions;
    private final Map<Object, SessionID<?, ?, ?, ?>> viewerToSession;

    @Inject
    public SpigotGUIManager(EventBus eventBus, InventoryStorage inventoryStorage) {
        this.eventBus = eventBus;
        this.inventoryStorage = inventoryStorage;
        this.activeSessions = new ConcurrentHashMap<>();
        this.viewerToSession = new ConcurrentHashMap<>();
    }

    @Override
    public <Model, Msg, V extends View<Msg, V, Viewer>, Ctx extends CommandContext, Cmd extends Command<Ctx, Msg>, Viewer extends InventoryViewer<Msg, V>>
    SessionID<Model, Msg, V, Viewer> startSession(GUIBase<Model, Msg, V, Ctx, Cmd> gui, Viewer viewer) {

        // Close any existing session for this viewer
        Optional<GUISession<Model, Msg, V, Viewer, Ctx>> existingSession = getSessionByViewer(viewer);
        existingSession.ifPresent(session -> closeSession(session.getSessionId()));

        // Create new session
        SessionID<Model, Msg, V, Viewer> sessionId = new SessionID<>(UUID.randomUUID());

        GUISession<Model, Msg, V, Viewer, Ctx> session = getSession(gui, viewer, sessionId);

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

    private <Model, Msg, V extends View<Msg, V, Viewer>,
            Ctx extends CommandContext, Cmd extends Command<Ctx, Msg>,
            Viewer extends InventoryViewer<Msg, V>>
    @NotNull GUISession<Model, Msg, V, Viewer, Ctx> getSession(GUIBase<Model, Msg, V, Ctx, Cmd> gui,
                                                               Viewer viewer,
                                                               SessionID<Model, Msg, V, Viewer> sessionId) {
        CommandContextFactory<Ctx, Msg, V, Viewer> ctxFactory = (viewerArg, currentView) -> {
            if (viewerArg instanceof SpigotInventoryViewer && currentView instanceof SpigotGUIView) {
                @SuppressWarnings("unchecked")
                SpigotInventoryViewer<Msg> spViewer = (SpigotInventoryViewer<Msg>) viewerArg;
                @SuppressWarnings("unchecked")
                SpigotGUIView<Msg> spView = (SpigotGUIView<Msg>) currentView;
                // noinspection unchecked
                return (Ctx) new DefaultSpigotCommandContext<>(spViewer.getPlayer(), spViewer, Optional.of(spView), inventoryStorage);
            }
            throw new IllegalStateException("Unsupported viewer/view for Spigot context");
        };

        return new GUISession<>(sessionId, gui, viewer, eventBus, inventoryStorage, ctxFactory);
    }

    @Override
    public <Model, Msg, V extends View<Msg, V, Viewer>, Ctx extends CommandContext, Cmd extends Command<Ctx, Msg>, Viewer extends InventoryViewer<Msg, V>>
    boolean sendMessage(SessionID<Model, Msg, V, Viewer> sessionId, Msg command) {


        GUISession<Model, Msg, V, Viewer, ? extends CommandContext> session = getSession(sessionId).orElse(null);
        if (session != null && session.isActive()) {
            return session.processMessage(command);
        }
        return false;
    }

    @Override
    public <Model, Msg, V extends View<Msg, V, Viewer>, Ctx extends CommandContext, Cmd extends Command<Ctx, Msg>, Viewer extends InventoryViewer<Msg, V>>
    boolean closeSession(SessionID<Model, Msg, V, Viewer> sessionId) {
        GUISession<?, ?, ?, ?, ?> session = activeSessions.get(sessionId);
        if (session != null) {
            session.close();
            activeSessions.remove(sessionId);
            viewerToSession.remove(session.getViewer());
            return true;
        }
        return false;
    }

    @Override
    public <Model, Msg, V extends View<Msg, V, Viewer>, Ctx extends CommandContext, Viewer extends InventoryViewer<Msg, V>>
    Optional<GUISession<Model, Msg, V, Viewer, Ctx>> getSession(SessionID<Model, Msg, V, Viewer> sessionId) {
        //noinspection unchecked
        return Optional.ofNullable((GUISession<Model, Msg, V, Viewer, Ctx>) activeSessions.get(sessionId));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public <Model, Msg, V extends View<Msg, V, Viewer>, Ctx extends CommandContext, Viewer extends InventoryViewer<Msg, V>>
    Optional<GUISession<Model, Msg, V, Viewer, Ctx>> getSessionByViewer(Viewer viewer) {
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
