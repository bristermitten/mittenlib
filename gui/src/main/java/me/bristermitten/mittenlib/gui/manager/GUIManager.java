package me.bristermitten.mittenlib.gui.manager;

import me.bristermitten.mittenlib.gui.GUIBase;
import me.bristermitten.mittenlib.gui.session.GUISession;
import me.bristermitten.mittenlib.gui.session.SessionID;
import me.bristermitten.mittenlib.gui.view.InventoryViewer;
import me.bristermitten.mittenlib.gui.view.View;

import java.util.Optional;

/**
 * Manages GUI sessions and their lifecycles.
 * Provides a centralised way to create, manage, and clean up GUI instances.
 */
public interface GUIManager {

    /**
     * Creates and starts a new GUI session for the given viewer.
     *
     * @param gui       the GUI implementation to run
     * @param viewer    the viewer (e.g., player) who will interact with the GUI
     * @param <Model>   the model type
     * @param <Command> the command type
     * @param <V>       the view type
     * @param <Viewer>  the viewer type
     * @return a unique session ID for this GUI instance
     */
    <Model, Command, V extends View<Command, V, Viewer>, Viewer extends InventoryViewer<Command, V>>
    SessionID<Model, Command, V, Viewer> startSession(GUIBase<Model, Command, V> gui, Viewer viewer);

    /**
     * Sends a command to an active GUI session.
     *
     * @param sessionId the session ID
     * @param command   the command to send
     * @return true if the command was sent successfully, false if the session doesn't exist
     */
    <Model, Command, V extends View<Command, V, Viewer>, Viewer extends InventoryViewer<Command, V>>
    boolean sendCommand(SessionID<Model, Command, V, Viewer> sessionId, Command command);

    /**
     * Closes a GUI session and cleans up resources.
     *
     * @param sessionId the session ID to close
     * @return true if the session was closed, false if it didn't exist
     */
    <Model, Command, V extends View<Command, V, Viewer>, Viewer extends InventoryViewer<Command, V>>
    boolean closeSession(SessionID<Model, Command, V, Viewer> sessionId);

    /**
     * Gets an active GUI session by ID.
     *
     * @param sessionId the session ID
     * @return the session if it exists and is active
     */
    <Model, Command, V extends View<Command, V, Viewer>, Viewer extends InventoryViewer<Command, V>>
    Optional<GUISession<Model, Command, V, Viewer>> getSession(SessionID<Model, Command, V, Viewer> sessionId);

    /**
     * Finds a GUI session by viewer.
     *
     * @param viewer the viewer to search for
     * @return the session if found
     */
    <Model, Command, V extends View<Command, V, Viewer>, Viewer extends InventoryViewer<Command, V>>
    Optional<GUISession<Model, Command, V, Viewer>> getSessionByViewer(Viewer viewer);

    /**
     * Closes all active sessions and shuts down the manager.
     */
    void shutdown();
}