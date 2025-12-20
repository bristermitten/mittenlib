package me.bristermitten.mittenlib.gui;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import me.bristermitten.mittenlib.gui.factory.MinecraftGUIFactory;
import me.bristermitten.mittenlib.gui.factory.SpigotMinecraftGUIFactory;
import me.bristermitten.mittenlib.gui.manager.GUIManager;
import me.bristermitten.mittenlib.gui.manager.SpigotGUIManager;
import me.bristermitten.mittenlib.gui.spigot.SpigotEventHandler;
import org.bukkit.event.Listener;

/**
 * Guice module for the GUI framework.
 * Provides dependency injection configuration for all GUI components.
 */
public class GUIModule extends AbstractModule {

    @Override
    protected void configure() {
        // Core GUI components
        bind(GUIManager.class).to(SpigotGUIManager.class).in(Scopes.SINGLETON);
        bind(MinecraftGUIFactory.class).to(SpigotMinecraftGUIFactory.class).in(Scopes.SINGLETON);

        // Spigot-specific components
        bind(SpigotEventHandler.class);
        Multibinder.newSetBinder(binder(), Listener.class)
                .addBinding().to(SpigotEventHandler.class);
    }
}