package me.bristermitten.mittenlib.demo;

import com.google.inject.Injector;
import me.bristermitten.mittenlib.MittenLib;
import me.bristermitten.mittenlib.gui.GUIModule;
import me.bristermitten.mittenlib.gui.manager.GUIManager;
import me.bristermitten.mittenlib.gui.session.SessionID;
import me.bristermitten.mittenlib.gui.spigot.SpigotGUIView;
import me.bristermitten.mittenlib.gui.spigot.SpigotInventoryViewer;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class DemoPlugin extends JavaPlugin implements Listener {

    DemoCounterGUI counterGUI;
    Injector injector;

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        injector = MittenLib.withDefaults(this)
                .addModule(new GUIModule())
                .build();


        counterGUI = injector.getInstance(DemoCounterGUI.class);

    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(this, () -> {
            SpigotInventoryViewer<CounterMessage> viewer = new SpigotInventoryViewer<>(event.getPlayer());
            GUIManager guiManager = injector.getInstance(GUIManager.class);
            SessionID<Counter, CounterMessage, SpigotGUIView<CounterMessage>, SpigotInventoryViewer<CounterMessage>> sessionID = guiManager
                    .startSession(counterGUI, viewer);

            guiManager.getSession(sessionID)
                    .get()
                    .start();

        }, 3 * 20L);
    }
}
