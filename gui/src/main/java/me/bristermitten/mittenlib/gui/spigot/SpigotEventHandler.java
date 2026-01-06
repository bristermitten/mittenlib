package me.bristermitten.mittenlib.gui.spigot;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.bristermitten.mittenlib.gui.manager.SpigotGUIManager;
import me.bristermitten.mittenlib.gui.session.GUISession;
import me.bristermitten.mittenlib.gui.session.SessionID;
import me.bristermitten.mittenlib.gui.spigot.command.SpigotCommandContext;
import me.bristermitten.mittenlib.util.lambda.PureFunction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

import java.util.Optional;
import java.util.OptionalInt;

/**
 * Handles Spigot inventory events and connects them to the GUI framework.
 * Replaces the empty InventoryClickListener with proper event handling.
 */
@Singleton
public class SpigotEventHandler implements Listener {

    private final SpigotGUIManager guiManager;

    @Inject
    public SpigotEventHandler(SpigotGUIManager guiManager) {
        this.guiManager = guiManager;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
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
        Optional<GUISession<?, ?, ?, ?, SpigotCommandContext>> sessionByViewer = guiManager.getSessionByViewer(
                new SpigotInventoryViewer<>(player)
        );
        if (!sessionByViewer.isPresent()) {
            return;
        }

        GUISession<?, ?, ?, ?, ?> session = sessionByViewer.get();

        // Cancel the event to prevent item movement
        event.setCancelled(true);

        // Get the clicked slot
        int slot = event.getSlot();
        if (slot < 0) {
            return;
        }


        Object layoutObj = session.getCurrentView();


        if (layoutObj instanceof SpigotGUIView) { // should we error if this is not the case?
            SpigotGUIView<?> layout = (SpigotGUIView<?>) layoutObj;

            layout.getButton(slot).ifPresent(button -> {
                ClickInput clickInput = new ClickInput(
                        event.getClick(),
                        event.getAction(),
                        event.getHotbarButton() == -1 ? OptionalInt.empty() : OptionalInt.of(event.getHotbarButton()),
                        event.getCursor()
                );
                PureFunction<ClickInput, ?> messageFunction = button.getMessageFunction();
                Object message = messageFunction.apply(clickInput);
                if (message != null) {
                    guiManager.sendMessage((SessionID) session.getSessionId(), message);
                }

            });
        }
    }


    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getPlayer();

        guiManager.getSessionByViewer(new SpigotInventoryViewer<>(player))
                .ifPresent(session -> {
                    if (!session.isTransitioning()) {
                        guiManager.closeSession(session.getSessionId());
                    }
                });
    }
}