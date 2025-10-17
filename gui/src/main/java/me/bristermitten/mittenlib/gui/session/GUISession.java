package me.bristermitten.mittenlib.gui.session;

import me.bristermitten.mittenlib.gui.GUIBase;
import me.bristermitten.mittenlib.gui.command.Command;
import me.bristermitten.mittenlib.gui.event.EventBus;
import me.bristermitten.mittenlib.gui.event.Subscription;
import me.bristermitten.mittenlib.gui.spigot.InventoryStorage;
import me.bristermitten.mittenlib.gui.spigot.SpigotGUIView;
import me.bristermitten.mittenlib.gui.view.InventoryViewer;
import me.bristermitten.mittenlib.gui.view.View;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Represents an active GUI session with state management and event handling.
 * Follows the Elm architecture pattern with immutable state updates.
 */
public class GUISession<Model, Msg, Cmd extends Command<Model>, V extends View<Msg, V, Viewer>, Viewer extends InventoryViewer<Msg, V>> {

    private final SessionID<Model, Msg, V, Viewer> sessionId;
    private final GUIBase<Model, Msg, V, Cmd> gui;
    private final Viewer viewer;
    private final EventBus eventBus;
    private final InventoryStorage inventoryStorage;
    private final AtomicReference<Model> currentModel;
    private final AtomicReference<V> currentView;
    private final CompletableFuture<Model> completionFuture;

    private volatile boolean active = true;
    private volatile boolean transitioning = false;
    private Subscription commandSubscription;

    public GUISession(SessionID<Model, Msg, V, Viewer> sessionId, GUIBase<Model, Msg, V, Cmd> gui, Viewer viewer, EventBus eventBus, InventoryStorage inventoryStorage) {
        this.sessionId = sessionId;
        this.gui = gui;
        this.viewer = viewer;
        this.eventBus = eventBus;
        this.inventoryStorage = inventoryStorage;
        this.currentModel = new AtomicReference<>();
        this.currentView = new AtomicReference<>();
        this.completionFuture = new CompletableFuture<>();
    }

    /**
     * Starts the GUI session by initializing the model and rendering the initial view.
     */
    public void start() {
        if (!active) {
            throw new IllegalStateException("Cannot start an inactive session");
        }

        // Initialize the model
        Model initialModel = gui.init();
        currentModel.set(initialModel);

        // Render the initial view
        V initialView = gui.render(initialModel);
        currentView.set(initialView);

        // Display the view
        initialView.display(viewer);

        // Store in inventory storage if it's a SpigotGUIView
        if (initialView instanceof SpigotGUIView) {
            ((SpigotGUIView<?>) initialView).storeInInventoryStorage(inventoryStorage);
        }

        // Subscribe to command events for this session
        setupCommandHandling();
    }

    /**
     * Processes a command and updates the GUI state.
     *
     * @param msg the command to process
     * @return true if the command was processed successfully
     */
    public boolean processCommand(Msg msg) {
        if (!active) {
            return false;
        }

        try {
            Model oldModel = currentModel.get();

            // Update the model
            Model newModel = null;
//                    gui.update(oldModel, msg); TODO:
            currentModel.set(newModel);

            // Render the new view
            V newView = gui.render(newModel);
            currentView.set(newView);

            transitioning = true;
            // Display the updated view
            newView.display(viewer);
            transitioning = false;

            // Store in inventory storage if it's a SpigotGUIView
            if (newView instanceof SpigotGUIView) {
                ((SpigotGUIView<?>) newView).storeInInventoryStorage(inventoryStorage);
            }

            return true;
        } catch (Exception e) {

            close();
            return false;
        }
    }

    /**
     * Closes the session and cleans up resources.
     */
    public void close() {
        if (active) {
            active = false;

            if (commandSubscription != null) {
                commandSubscription.cancel();
            }

            Model finalModel = currentModel.get();
            completionFuture.complete(finalModel);
        }
    }

    /**
     * Returns a future that completes when the session ends.
     *
     * @return the completion future with the final model
     */
    public CompletableFuture<Model> getCompletionFuture() {
        return completionFuture;
    }

    private void setupCommandHandling() {
        // This will be implemented when we have command events defined
        // For now, we'll rely on external command sending via processCommand
    }

    // Getters
    public SessionID<Model, Msg, V, Viewer> getSessionId() {
        return sessionId;
    }

    public Viewer getViewer() {
        return viewer;
    }

    public Model getCurrentModel() {
        return currentModel.get();
    }

    public V getCurrentView() {
        return currentView.get();
    }

    public boolean isActive() {
        return active;
    }

    public boolean isTransitioning() {
        return transitioning;
    }
}