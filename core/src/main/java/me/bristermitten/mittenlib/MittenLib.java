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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class MittenLib<T extends Plugin> {

    private final Map<Class<? extends Module>, Module> modules = new LinkedHashMap<>();

    public MittenLib(T plugin) {
        addModule(new MittenLibCoreModule<>(plugin));
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
        Module combined = Modules.override(new ConfigModule(configs))
                .with(new PluginConfigModule());
        return addModule(combined);
    }

    public MittenLib<T> addConfigModules(Configuration<?>... configs) {
        return addConfigModules(Sets.of(configs));
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

    /**
     * Removes a module by class. Prefer overriding where possible, but this can be useful in tests.
     */
    public MittenLib<T> removeModule(Class<? extends Module> moduleClass) {
        this.modules.remove(moduleClass);
        return this;
    }

    public @NotNull Injector build() {
        return Guice.createInjector(modules.values());
    }

    private void addModule0(Module module) {
        for (Map.Entry<Class<? extends Module>, Module> entry : modules.entrySet()) {
            Class<? extends Module> existingKey = entry.getKey();

            if (existingKey.isAssignableFrom(module.getClass())) {
                Module merged = Modules.override(entry.getValue()).with(module);
                entry.setValue(merged);
                return;
            }
        }

        this.modules.put(module.getClass(), module);
    }
}
