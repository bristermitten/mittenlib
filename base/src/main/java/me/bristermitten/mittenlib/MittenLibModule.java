package me.bristermitten.mittenlib;

import com.google.inject.AbstractModule;
import org.bukkit.plugin.Plugin;

public class MittenLibModule<T extends Plugin> extends AbstractModule {
    private final T plugin;

    public MittenLibModule(T plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        bind(Plugin.class).toInstance(plugin);
        //noinspection unchecked
        bind((Class<T>) plugin.getClass()).toInstance(plugin);

        // TODO install the rest of the modules
    }
}
