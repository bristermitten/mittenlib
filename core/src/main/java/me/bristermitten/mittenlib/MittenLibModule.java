package me.bristermitten.mittenlib;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import org.bukkit.plugin.Plugin;

import java.util.Set;

public class MittenLibModule<T extends Plugin> extends AbstractModule {
    private final T plugin;
    private final Set<Module> modules;

    public MittenLibModule(T plugin, Set<Module> modules) {
        this.plugin = plugin;
        this.modules = modules;
    }

    @Override
    protected void configure() {
        if (plugin != null) {
            bind(Plugin.class).toInstance(plugin);
            //noinspection unchecked
            bind((Class<T>) plugin.getClass()).toInstance(plugin);
            bind(MittenLibConsumer.class).toInstance(new MittenLibConsumer(plugin.getName()));
        }

        for (Module module : modules) {
            install(module);
        }
    }
}
