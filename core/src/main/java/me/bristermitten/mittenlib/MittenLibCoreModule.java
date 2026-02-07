package me.bristermitten.mittenlib;

import com.google.inject.AbstractModule;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

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
 * </ul>
 *
 * @param <T> the type of the plugin
 */
public class MittenLibCoreModule<T extends Plugin> extends AbstractModule {
    private final T plugin;

    /**
     * Create a new MittenLibModule
     *
     * @param plugin  the plugin instance
     */
    public MittenLibCoreModule(@Nullable T plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        if (plugin != null) {
            bind(Plugin.class).to(plugin.getClass());
            if (plugin instanceof JavaPlugin) {
                // I know we should avoid conditional bindings
                // but binding JavaPlugin is a sensible default for plugins that extend it
                // but we also don't want to require it
                // in cases of testing etc
                //noinspection unchecked
                bind(JavaPlugin.class).to((Class<? extends JavaPlugin>) plugin.getClass());
            }
            //noinspection unchecked
            bind((Class<T>) plugin.getClass()).toInstance(plugin);
            bind(MittenLibConsumer.class).toInstance(new MittenLibConsumer(plugin.getName()));
        }
    }
}
