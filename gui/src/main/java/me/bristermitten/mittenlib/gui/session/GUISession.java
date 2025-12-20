package me.bristermitten.mittenlib.gui.session;

import me.bristermitten.mittenlib.gui.GUIBase;
import me.bristermitten.mittenlib.gui.UpdateResult;
import me.bristermitten.mittenlib.gui.command.Command;
import me.bristermitten.mittenlib.gui.command.CommandContext;
import me.bristermitten.mittenlib.gui.view.InventoryViewer;
import me.bristermitten.mittenlib.gui.view.View;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Represents an active GUI session with state management and event handling.
 * Follows the Elm architecture pattern with immutable state updates.
 */
public class GUISession<Model,
        Msg,
        V extends View<Msg, V, Viewer>,
        Viewer extends InventoryViewer<Msg, V>,
        Ctx extends CommandContext> {

    /**
     * The unique session ID.
     */
    private final SessionID<Model, Msg, V, Viewer> sessionId;

    /**
     * The viewer (e.g. player) interacting with the GUI.
     */
    private final Viewer viewer;

    /**
     * The GUI itself
     */
    private final GUIBase<Model, Msg, V, Ctx, ? extends Command<Ctx, Msg>> gui;

    /**
     * Renderer for a view
     */
    private final Consumer<V> renderer;

    private final CommandRunner<Ctx, Msg> commandRunner;

    // State
    private final AtomicReference<Model> currentModel;
    private final AtomicReference<V> currentLayout;
    private final CompletableFuture<Model> completionFuture;

    private volatile boolean active = true;
    private volatile boolean transitioning = false;

    public GUISession(SessionID<Model, Msg, V, Viewer> sessionId,
                      GUIBase<Model, Msg, V, Ctx, ? extends Command<Ctx, Msg>> gui,
                      Viewer viewer,
                      Model initialModel,
                      Consumer<V> renderer,
                      CommandRunner<Ctx, Msg> commandRunner) {
        this.sessionId = sessionId;
        this.gui = gui;
        this.viewer = viewer;
        this.renderer = renderer;

        this.currentModel = new AtomicReference<>(initialModel);
        this.commandRunner = commandRunner;
        this.currentLayout = new AtomicReference<>();
        this.completionFuture = new CompletableFuture<>();
    }


    /**
     * Starts the GUI session by rendering the initial view.
     * Note: The model is assumed to be initialized before passing to constructor
     * (or you can move init() here).
     */
    public void start() {
        if (!active) {
            throw new IllegalStateException("Cannot start an inactive session");
        }

        // 1. Render initial state
        renderAndFlush(currentModel.get());
    }


    /**
     * The core "Game Loop".
     * Processes a message, updates the model, renders the view, and runs commands.
     *
     * @param msg the message to process
     * @return true if processed, false if session is inactive
     */
    public boolean processMessage(Msg msg) {
        if (!active) {
            return false;
        }

        // 1. Update (Pure Logic)
        // We synchronize to ensure messages are processed in order
        synchronized (this) {
            try {
                Model oldModel = currentModel.get();

                UpdateResult<Model, Msg, Ctx, ? extends Command<Ctx, Msg>> result = gui.update(oldModel, msg);

                Model newModel = result.getModel();
                currentModel.set(newModel);

                // 2. Render & Display (Side Effect -> Screen)
                transitioning = true;
                renderAndFlush(newModel);
                transitioning = false;

                // 3. Run Commands (Side Effect -> Logic)
                if (result.getCommand() != null) {
                    // Commands might emit new messages (e.g. "DataLoaded"), so we pass a dispatcher
                    commandRunner.run(result.getCommand(), this::processMessage);
                }

                return true;

            } catch (Exception e) {
                // In a robust system, you might send a Msg.Error here instead of closing
                e.printStackTrace();
                close();
                return false;
            }
        }
    }

    private void renderAndFlush(Model model) {
        // Calculate the Layout (Pure)
        V layout = gui.render(model);
        currentLayout.set(layout);

        // Push to screen (Impure)
        renderer.accept(layout);
    }

    public void close() {
        if (active) {
            active = false;
            completionFuture.complete(currentModel.get());
        }
    }

    public SessionID<Model, Msg, V, Viewer> getSessionId() {
        return sessionId;
    }

    public Viewer getViewer() {
        return viewer;
    }

    public boolean isActive() {
        return active;
    }

    public CompletableFuture<Model> getCompletionFuture() {
        return completionFuture;
    }

    public V getCurrentLayout() {
        return currentLayout.get();
    }

    public Model getCurrentModel() {
        return currentModel.get();
    }

    public boolean isTransitioning() {
        return transitioning;
    }

    @FunctionalInterface
    public interface CommandRunner<Ctx extends CommandContext, Msg> {
        void run(Command<Ctx, Msg> cmd, Consumer<Msg> dispatcher);
    }
}