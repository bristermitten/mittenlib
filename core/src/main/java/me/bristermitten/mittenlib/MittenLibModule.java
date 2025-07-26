package me.bristermitten.mittenlib;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Main Guice Module for MittenLib.
 * <p>
 * <strong>You probably shouldn't use this!</strong> You most likely want to create an Injector with
 * {@link MittenLib#build()}, which automatically installs this module.
 * <p>
 * This module binds the following:
 * <ul>
 *     <li>{@link Plugin} to the plugin type</li>
 *     <li>The plugin type to the plugin instance</li>
 *     <li>{@link MittenLibConsumer} to a new instance of {@link MittenLibConsumer}, using the given plugin name</li>
 *     <li>Any child modules provide</li>
 * </ul>
 *
 * @param <T> the type of the plugin
 */
public class MittenLibModule<T extends Plugin> extends AbstractModule {
    private final T plugin;
    private final Set<Module> modules;

    /**
     * Create a new MittenLibModule
     *
     * @param plugin  the plugin instance
     * @param modules child modules to install
     */
    public MittenLibModule(@Nullable T plugin, @NotNull Set<Module> modules) {
        this.plugin = plugin;
        this.modules = modules;
    }

    @Override
    protected void configure() {
        if (plugin != null) {
            bind(Plugin.class).to(plugin.getClass());
            //noinspection unchecked
            bind((Class<T>) plugin.getClass()).toInstance(plugin);
            bind(MittenLibConsumer.class).toInstance(new MittenLibConsumer(plugin.getName()));

            bind(ListenerRegistration.class).asEagerSingleton();
        }

        for (Module module : modules) {
            install(module);
        }
    }
}
