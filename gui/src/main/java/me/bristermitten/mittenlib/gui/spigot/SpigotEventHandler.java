package me.bristermitten.mittenlib.gui.spigot;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.bristermitten.mittenlib.gui.command.CommandContext;
import me.bristermitten.mittenlib.gui.manager.GUIManager;
import me.bristermitten.mittenlib.gui.session.GUISession;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

import java.util.Optional;

/**
 * Handles Spigot inventory events and connects them to the GUI framework.
 * Replaces the empty InventoryClickListener with proper event handling.
 */
@Singleton
public class SpigotEventHandler implements Listener {

    private final GUIManager guiManager;
    private final InventoryStorage inventoryStorage;

    @Inject
    public SpigotEventHandler(GUIManager guiManager, InventoryStorage inventoryStorage) {
        this.guiManager = guiManager;
        this.inventoryStorage = inventoryStorage;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null) {
            return;
        }

        // Check if this is a GUI inventory
        Optional<SpigotGUIView<?>> guiView = inventoryStorage.getView(clickedInventory);
        if (!guiView.isPresent()) {
            return;
        }

        // Cancel the event to prevent item movement
        event.setCancelled(true);

        // Get the clicked slot
        int slot = event.getSlot();
        if (slot < 0) {
            return;
        }


        // noinspection unchecked
        this.processSession(
                player,
                (SpigotGUIView<Object>) guiView.get(), slot);
    }

    private <Msg> void processSession(Player player, SpigotGUIView<Msg> view, int slot) {
        // Find the GUI session for this player

        Optional<GUISession<Object, Object, SpigotGUIView<Object>, SpigotInventoryViewer<Object>, CommandContext>> session = guiManager.getSessionByViewer(
                new SpigotInventoryViewer<>(player)
        );
        if (!session.isPresent()) {
            return;
        }
        // Get the button for this slot

        Optional<? extends InventoryButton<Msg>> button = view.getButton(slot);

        if (!button.isPresent()) {
            return;
        }

        // Send the command to the session
        Msg command = button.get().getMessage();
        guiManager.sendMessage(session.get().getSessionId(), command);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getPlayer();
        Inventory closedInventory = event.getInventory();

        // Check if this was a GUI inventory
        Optional<SpigotGUIView<?>> guiView = inventoryStorage.getView(closedInventory);
        if (!guiView.isPresent()) {
            return;
        }

        // Find and close the GUI session for this player
        Optional<GUISession<Object, Object, SpigotGUIView<Object>, SpigotInventoryViewer<Object>, CommandContext>> session = guiManager.getSessionByViewer(
                new SpigotInventoryViewer<>(player)
        );

        session.filter(s -> !s.isTransitioning())
                .ifPresent(guiSession ->
                        guiManager.closeSession(guiSession.getSessionId()));
    }
}