package me.bristermitten.mittenlib;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;
import me.bristermitten.mittenlib.collections.Sets;
import me.bristermitten.mittenlib.config.ConfigModule;
import me.bristermitten.mittenlib.config.Configuration;
import me.bristermitten.mittenlib.config.PluginConfigModule;
import me.bristermitten.mittenlib.files.FileTypeModule;
import me.bristermitten.mittenlib.lang.LangModule;
import me.bristermitten.mittenlib.watcher.FileWatcherModule;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MittenLib<T extends Plugin> {

    private final Map<Class<? extends Module>, Module> modules = new HashMap<>();

    public MittenLib(T plugin) {
        addModule(new MittenLibModule<>(plugin, Sets.of()));
    }

    public static <T extends Plugin> MittenLib<T> withDefaults(@NotNull T plugin) {
        return empty(plugin).addDefaultModules();
    }

    public static <T extends Plugin> MittenLib<T> empty(@NotNull T plugin) {
        return new MittenLib<>(plugin);
    }

    public MittenLib<T> addDefaultModules() {
        addModule(new LangModule());
        addModule(new FileWatcherModule());
        addModule(new FileTypeModule());
        addModule(new ListenersModule());
        return this;
    }

    public MittenLib<T> addConfigModules(Set<Configuration<?>> configs) {
        return addModule(
                Modules.override(new ConfigModule(configs))
                .with(new PluginConfigModule())
        );
    }

    public MittenLib<T> addConfigModules(Configuration<?>... configs) {
        return addConfigModules(new HashSet<>(Arrays.asList(configs)));
    }

    public MittenLib<T> addModule(Module module) {
        addModule0(module);
        return this;
    }

    public MittenLib<T> addModules(Module... modules) {
        for (Module module : modules) {
            addModule0(module);
        }
        return this;
    }

    public @NotNull Injector build() {
        return Guice.createInjector(new HashSet<>(modules.values()));
    }

    private void addModule0(Module module) {
        Class<? extends Module> moduleClass = module.getClass();
        boolean superClass = false;
        for (Map.Entry<Class<? extends Module>, Module> entry : new HashSet<>(modules.entrySet())) {
            if (entry.getKey().isAssignableFrom(moduleClass)) {
                final Module overriding = Modules.override(entry.getValue()).with(module);
                modules.put(entry.getKey(), overriding);
                superClass = true;
            }
        }
        if (superClass) {
            return;
        }
        modules.put(module.getClass(), module);
    }
}
