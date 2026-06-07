package me.bristermitten.mittenlib;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.Set;
import java.util.logging.Logger;

@Singleton
public class ListenerRegistration {
    private final Plugin plugin;
    private final Set<Listener> listeners;
    private final Logger logger;

    @Inject
    public ListenerRegistration(Plugin plugin, Set<Listener> listeners, Logger logger) {
        this.plugin = plugin;
        this.listeners = ImmutableSet.copyOf(listeners);
        this.logger = logger;
        init();
    }

    public void init() {
        if (listeners != null) {
            for (Listener listener : listeners) {
                plugin.getServer().getPluginManager().registerEvents(listener, plugin);
                logger.info("Registered listener: " + listener.getClass().getName());
            }
        }
    }

}
