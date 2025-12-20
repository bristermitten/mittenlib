package me.bristermitten.mittenlib.gui.manager;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.bristermitten.mittenlib.gui.GUIBase;
import me.bristermitten.mittenlib.gui.command.Command;
import me.bristermitten.mittenlib.gui.command.CommandContext;
import me.bristermitten.mittenlib.gui.event.EventBus;
import me.bristermitten.mittenlib.gui.session.GUISession;
import me.bristermitten.mittenlib.gui.session.SessionID;
import me.bristermitten.mittenlib.gui.spigot.InventoryStorage;
import me.bristermitten.mittenlib.gui.spigot.SpigotGUIView;
import me.bristermitten.mittenlib.gui.spigot.SpigotInventoryViewer;
import me.bristermitten.mittenlib.gui.spigot.command.DefaultSpigotCommandContext;
import me.bristermitten.mittenlib.gui.spigot.command.SpigotCommandContext;
import me.bristermitten.mittenlib.gui.view.InventoryViewer;
import me.bristermitten.mittenlib.gui.view.View;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Spigot implementation of GUIManager that manages GUI sessions and their lifecycles.
 * Uses dependency injection and reactive event handling.
 */
@Singleton
public class SpigotGUIManager implements GUIManager<SpigotCommandContext> {

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

    @SuppressWarnings("rawtypes")
    @Override
    public <Model, Msg, V extends View<Msg, V, Viewer>, Cmd extends Command<SpigotCommandContext, Msg>, Viewer extends InventoryViewer<Msg, V>>
    SessionID<Model, Msg, V, Viewer> startSession(GUIBase<Model, Msg, V, SpigotCommandContext, Cmd> gui, Viewer viewer) {

        if (!(viewer instanceof SpigotInventoryViewer)) {
            throw new IllegalArgumentException("SpigotGUIManager requires SpigotInventoryViewer");
        }
        SpigotInventoryViewer spigotViewer = (SpigotInventoryViewer) viewer;
        Player player = spigotViewer.getPlayer();

        closeExistingSession(player);

        // Create new session
        SessionID<Model, Msg, V, Viewer> sessionId = new SessionID<>(UUID.randomUUID());

        GUISession<Model, Msg, V, Viewer, SpigotCommandContext> session = getSession(gui, viewer, sessionId);

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

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void closeExistingSession(Player player) {
        SessionID id = viewerToSession.get(player);
        if (id != null) closeSession(id);
    }

    private <Model, Msg, V extends View<Msg, V, Viewer>,
            Cmd extends Command<SpigotCommandContext, Msg>,
            Viewer extends InventoryViewer<Msg, V>>
    @NotNull GUISession<Model, Msg, V, Viewer, SpigotCommandContext> getSession(GUIBase<Model, Msg, V, SpigotCommandContext, Cmd> gui,
                                                                                Viewer viewer,
                                                                                SessionID<Model, Msg, V, Viewer> sessionId) {

        if (!(viewer instanceof SpigotInventoryViewer)) {
            throw new IllegalArgumentException("SpigotGUIManager requires a SpigotInventoryViewer, but got " + viewer.getClass().getSimpleName());
        }

        //noinspection unchecked
        SpigotInventoryViewer<Msg> spigotViewer = (SpigotInventoryViewer<Msg>) viewer;

        Consumer<V> renderer = layout -> {
            if (layout instanceof SpigotGUIView) {
                //noinspection unchecked
                updateInventory(spigotViewer.getPlayer(), (SpigotGUIView<Msg>) layout);
            } else {
                throw new IllegalStateException("SpigotGUIManager received a non-Spigot view: " + layout.getClass());
            }
        };

        GUISession.CommandRunner<SpigotCommandContext, Msg> commandRunner = (cmd, dispatch) -> {
            // Create a context linked to this player
            // Assuming your Command interface is: void run(Context ctx, Consumer<Msg> dispatch)
            DefaultSpigotCommandContext context = new DefaultSpigotCommandContext(spigotViewer.getPlayer());
            cmd.run(context, dispatch);
        };

        return new GUISession<>(sessionId,
                gui,
                viewer,
                gui.init(),
                renderer,
                commandRunner);
    }

    /**
     * The "Renderer" implementation for Spigot.
     * Handles opening/updating the inventory based on the Pure Layout.
     */
    private <Msg> void updateInventory(Player player, SpigotGUIView<Msg> layout) {
        Inventory current = player.getOpenInventory().getTopInventory();
        String title = layout.getTitle();
        int size = layout.getSize();

        Inventory target;

        // Reuse strategy: If title and size match, just update items.
        // Note: Bukkit doesn't allow easy title changes without reopening.
        boolean canReuse = current != null
                && current.getSize() == size
                && player.getOpenInventory().getTitle().equals(title);

        if (canReuse) {
            target = current;
        } else {
            // Create and open new
            target = Bukkit.createInventory(player, size, title); // You might need Component/Legacy adapter here
            player.openInventory(target);
        }

        // Sync items (Naive clear-and-set approach)
        // Optimization: You could diff 'current' vs 'layout' to only set changed slots
        target.clear();
        layout.getButtons().forEach((slot, button) ->
                target.setItem(slot, button.getItemStack())
        );
    }


    @Override
    public <Model, Msg, V extends View<Msg, V, Viewer>, Cmd extends Command<SpigotCommandContext, Msg>, Viewer extends InventoryViewer<Msg, V>>
    boolean sendMessage(SessionID<Model, Msg, V, Viewer> sessionId, Msg command) {


        GUISession<Model, Msg, V, Viewer, ? extends CommandContext> session = getSession(sessionId).orElse(null);
        if (session != null && session.isActive()) {
            return session.processMessage(command);
        }
        return false;
    }

    @Override
    public <Model, Msg, V extends View<Msg, V, Viewer>, Cmd extends Command<SpigotCommandContext, Msg>, Viewer extends InventoryViewer<Msg, V>>
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
    public <Model, Msg, V extends View<Msg, V, Viewer>, Viewer extends InventoryViewer<Msg, V>>
    Optional<GUISession<Model, Msg, V, Viewer, SpigotCommandContext>> getSession(SessionID<Model, Msg, V, Viewer> sessionId) {
        //noinspection unchecked
        return Optional.ofNullable((GUISession<Model, Msg, V, Viewer, SpigotCommandContext>) activeSessions.get(sessionId));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public <Msg, V extends View<Msg, V, Viewer>, Viewer extends InventoryViewer<Msg, V>> Optional<GUISession<?, ?, ?, ?, SpigotCommandContext>> getSessionByViewer(Viewer viewer) {
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
