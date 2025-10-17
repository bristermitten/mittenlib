package me.bristermitten.mittenlib.gui.manager;

import me.bristermitten.mittenlib.gui.GUIBase;
import me.bristermitten.mittenlib.gui.command.Command;
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
     * @param gui      the GUI implementation to run
     * @param viewer   the viewer (e.g., player) who will interact with the GUI
     * @param <Model>  the model type
     * @param <Msg>    the message type
     * @param <V>      the view type
     * @param <Viewer> the viewer type
     * @return a unique session ID for this GUI instance
     */
    <Model, Msg, V extends View<Msg, V, Viewer>, Cmd extends Command<Model>, Viewer extends InventoryViewer<Msg, V>>
    SessionID<Model, Msg, V, Viewer> startSession(GUIBase<Model, Msg, V, Cmd> gui, Viewer viewer);

    /**
     * Sends a command to an active GUI session.
     *
     * @param sessionId the session ID
     * @param command   the command to send
     * @return true if the command was sent successfully, false if the session doesn't exist
     */
    <Model, Msg, V extends View<Msg, V, Viewer>, Cmd extends Command<Model>, Viewer extends InventoryViewer<Msg, V>>
    boolean sendMessage(SessionID<Model, Msg, V, Viewer> sessionId, Msg command);

    /**
     * Closes a GUI session and cleans up resources.
     *
     * @param sessionId the session ID to close
     * @return true if the session was closed, false if it didn't exist
     */
    <Model, Msg, V extends View<Msg, V, Viewer>, Cmd extends Command<Model>, Viewer extends InventoryViewer<Msg, V>>
    boolean closeSession(SessionID<Model, Msg, V, Viewer> sessionId);

    /**
     * Gets an active GUI session by ID.
     *
     * @param sessionId the session ID
     * @return the session if it exists and is active
     */
    <Model, Msg, V extends View<Msg, V, Viewer>, Cmd extends Command<Model>, Viewer extends InventoryViewer<Msg, V>>
    Optional<GUISession<Model, Msg, Cmd, V, Viewer>> getSession(SessionID<Model, Msg, V, Viewer> sessionId);

    /**
     * Finds a GUI session by viewer.
     *
     * @param viewer the viewer to search for
     * @return the session if found
     */
    <Model, Msg, V extends View<Msg, V, Viewer>, Cmd extends Command<Model>, Viewer extends InventoryViewer<Msg, V>>
    Optional<GUISession<Model, Msg, Cmd, V, Viewer>> getSessionByViewer(Viewer viewer);

    /**
     * Closes all active sessions and shuts down the manager.
     */
    void shutdown();
}