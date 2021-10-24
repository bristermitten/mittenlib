package me.bristermitten.mittenlib;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import me.bristermitten.mittenlib.config.ConfigModule;
import me.bristermitten.mittenlib.config.Configuration;
import me.bristermitten.mittenlib.files.FileTypeModule;
import me.bristermitten.mittenlib.lang.LangModule;
import me.bristermitten.mittenlib.watcher.FileWatcherModule;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public final class MittenLib<T extends Plugin> {

    private final T plugin;
    private final Map<Class<? extends Module>, Module> modules = new HashMap<>();

    public MittenLib(T plugin) {
        this.plugin = plugin;
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
        return this;
    }

    public MittenLib<T> addConfigModules(Set<Configuration<?>> configs) {
        addModule(new ConfigModule(configs));
        return this;
    }

    public MittenLib<T> addConfigModules(Configuration<?>... configs) {
        return addConfigModules(
                new HashSet<>(Arrays.asList(configs))
        );
    }

    public MittenLib<T> addModule(Module module) {
        addModule0(module);
        return this;
    }

    public @NotNull Injector build() {
        return Guice.createInjector(new MittenLibModule<>(plugin, new HashSet<>(modules.values())));
    }

    private void addModule0(Module module) {
        modules.put(module.getClass(), module);
    }
}
