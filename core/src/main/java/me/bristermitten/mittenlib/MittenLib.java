package me.bristermitten.mittenlib;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;
import java.util.*;
import me.bristermitten.mittenlib.config.*;
import me.bristermitten.mittenlib.config.paths.PluginConfigInitializationStrategy;
import me.bristermitten.mittenlib.config.paths.PluginConfigPathResolver;
import me.bristermitten.mittenlib.files.FileTypeModule;
import me.bristermitten.mittenlib.lang.LangModule;
import me.bristermitten.mittenlib.watcher.FileWatcherModule;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class MittenLib<T extends Plugin> {

    private final Map<Class<? extends Module>, Module> modules = new LinkedHashMap<>();
    private final Map<Class<? extends MittenLibConfigLoader>, MittenLibConfigLoader> configModules =
            new LinkedHashMap<>();
    private final Set<Configuration<?>> manualConfigs = new LinkedHashSet<>();

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

    /**
     * Register a module which installs certain config types. This module should typically be a
     * generated {@code ConfigLoaderModule}, which will be installed, and overridden with
     * plugin-specific {@link PluginConfigModule} bindings.
     *
     * @param module the module to register
     * @return this
     */
    public MittenLib<T> config(@NotNull MittenLibConfigLoader module) {
        this.configModules.put(module.getClass(), module);
        return this;
    }

    /**
     * Register a configuration that will be loaded from a file.
     *
     * @param config the configuration to register
     * @return this
     */
    public MittenLib<T> config(@NotNull Configuration<?> config) {
        this.manualConfigs.add(config);
        return this;
    }

    /**
     * Register a configuration that will be loaded from a file.
     *
     * @param fileName the name of the file to load
     * @param type the type to deserialize to
     * @param <C> the type of the configuration
     * @return this
     */
    public <C> MittenLib<T> config(@NotNull String fileName, @NotNull Class<C> type) {
        return config(new Configuration<>(fileName, type, type));
    }

    /**
     * Register a configuration that will be loaded from a file.
     *
     * @param fileName the name of the file to load
     * @param type the type to deserialize to
     * @param implementationType the concrete implementation type of the configuration
     * @param <C> the type of the configuration
     * @return this
     */
    public <C> MittenLib<T> config(
            @NotNull String fileName, @NotNull Class<C> type, @NotNull Class<? extends C> implementationType) {
        return config(new Configuration<>(fileName, type, implementationType));
    }

    /**
     * Register multiple configurations.
     *
     * @param configs the configurations to register
     * @return this
     */
    public MittenLib<T> configs(@NotNull Configuration<?>... configs) {
        Collections.addAll(this.manualConfigs, configs);
        return this;
    }

    /**
     * Register multiple configurations.
     *
     * @param configs the configurations to register
     * @return this
     */
    public MittenLib<T> configs(@NotNull Collection<Configuration<?>> configs) {
        this.manualConfigs.addAll(configs);
        return this;
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

    /** Finalizes the setup process and returns the {@link Injector} */
    public @NotNull Injector setup() {
        List<Module> allModules = new ArrayList<>(modules.values());

        if (!configModules.isEmpty() || !manualConfigs.isEmpty()) {
            configModules.values().forEach(module -> allModules.add(module.asModule()));

            if (!manualConfigs.isEmpty()) {
                allModules.add(new ConfigDataModule(manualConfigs));
            }

            allModules.add(new ConfigInfrastructureModule(
                    PluginConfigInitializationStrategy.class, PluginConfigPathResolver.class));
        }

        return Guice.createInjector(allModules);
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
