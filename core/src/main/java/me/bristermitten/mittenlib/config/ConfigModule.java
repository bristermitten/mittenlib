package me.bristermitten.mittenlib.config;

import com.google.gson.TypeAdapterFactory;
import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import me.bristermitten.mittenlib.config.paths.ConfigInitializationStrategy;
import me.bristermitten.mittenlib.config.paths.ConfigPathResolver;
import me.bristermitten.mittenlib.config.paths.JarResourcesConfigPathResolver;
import me.bristermitten.mittenlib.config.paths.NoOpConfigInitializationStrategy;
import me.bristermitten.mittenlib.config.provider.ConfigProvider;
import me.bristermitten.mittenlib.config.provider.DelegatingConfigProvider;
import me.bristermitten.mittenlib.config.provider.construct.ConfigProviderFactory;
import me.bristermitten.mittenlib.config.provider.construct.ConfigProviderImprover;
import me.bristermitten.mittenlib.config.provider.construct.SimpleConfigProviderFactory;
import me.bristermitten.mittenlib.config.provider.construct.SimpleConfigProviderImprover;
import me.bristermitten.mittenlib.config.reader.ObjectLoader;
import me.bristermitten.mittenlib.config.reader.SearchingObjectLoader;
import me.bristermitten.mittenlib.config.writer.ConfigSaver;
import me.bristermitten.mittenlib.config.tree.DataTreeTypeAdapter;
import me.bristermitten.mittenlib.config.tree.DataTreeTypeAdapterFactory;
import me.bristermitten.mittenlib.config.writer.ObjectWriter;
import me.bristermitten.mittenlib.config.writer.SearchingObjectWriter;
import me.bristermitten.mittenlib.files.json.ExtraTypeAdapter;
import me.bristermitten.mittenlib.util.CompositeType;

import java.util.Objects;
import java.util.Set;

/**
 * Guice module for config handling
 * <p>
 * Responsible for quite a lot (probably too much) of the config handling, binding / registering:
 * <ul>
 *     <li>{@link ObjectLoader}</li>
 *     <li>{@link ConfigInitializationStrategy}</li>
 *     <li>{@link ConfigPathResolver}</li>
 *     <li>{@link ConfigProviderFactory}</li>
 *     <li>{@link ConfigProviderImprover}</li>
 *     <li>{@link ConfigProvider}s (as a {@link Set}). This can be used to create a reload command, for example, if the file watcher isn't being used.</li>
 *     <li>{@link Configuration}s (as a {@link Set})</li>
 * </ul>
 */

public class ConfigModule extends AbstractModule {
    private final Set<Configuration<?>> configurations;

    /**
     * Create a new ConfigModule with no configurations registered.
     * Use this when using statically bound modules.
     */
    public ConfigModule() {
        this.configurations = java.util.Collections.emptySet();
    }

    /**
     * Create a new ConfigModule
     *
     * @param configurations the configurations to register
     */
    public ConfigModule(Set<Configuration<?>> configurations) {
        this.configurations = configurations;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void configure() {
        bind(ObjectLoader.class).to(SearchingObjectLoader.class);
        bind(ObjectWriter.class).to(SearchingObjectWriter.class);
        bind(ConfigInitializationStrategy.class).to(NoOpConfigInitializationStrategy.class);
        bind(ConfigPathResolver.class).to(JarResourcesConfigPathResolver.class);
        bind(ConfigSaver.class);
        bind(ConfigProviderFactory.class).to(SimpleConfigProviderFactory.class);
        bind(ConfigProviderImprover.class).to(SimpleConfigProviderImprover.class);

        Multibinder.newSetBinder(binder(), new TypeLiteral<ExtraTypeAdapter<?>>() {
                })
                .addBinding()
                .to(DataTreeTypeAdapter.class);
        Multibinder.newSetBinder(binder(), TypeAdapterFactory.class)
                .addBinding()
                .to(DataTreeTypeAdapterFactory.class);

        Multibinder<Configuration<?>> configurationMultibinder = Multibinder.newSetBinder(binder(), new TypeLiteral<Configuration<?>>() {
        });

        Multibinder<ConfigProvider<?>> configProviderMultibinder = Multibinder.newSetBinder(binder(), new TypeLiteral<ConfigProvider<?>>() {
        });

        configurations.forEach(configuration -> {
            final Class<?> key = configuration.getType();

            // Bind Configuration<T> dynamically
            final TypeLiteral<Configuration<?>> configType =
                    (TypeLiteral<Configuration<?>>) TypeLiteral.get(new CompositeType(Configuration.class, key));
            bind(configType).toInstance(configuration);
            configurationMultibinder.addBinding().toInstance(configuration);

            // Bind ConfigProvider<T> to DelegatingConfigProvider<T> in Singleton scope
            final TypeLiteral<ConfigProvider<?>> providerType =
                    (TypeLiteral<ConfigProvider<?>>) TypeLiteral.get(new CompositeType(ConfigProvider.class, key));
            final TypeLiteral<DelegatingConfigProvider<?>> delegatingProviderType =
                    (TypeLiteral<DelegatingConfigProvider<?>>) TypeLiteral.get(new CompositeType(DelegatingConfigProvider.class, key));

            bind(providerType).to(delegatingProviderType).in(Singleton.class);
            configProviderMultibinder.addBinding().to(delegatingProviderType);

            // Bind T to the provider key
            bind((Class<? super Object>) key).toProvider(Key.get(providerType));
        });
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConfigModule that = (ConfigModule) o;
        return Objects.equals(configurations, that.configurations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(configurations);
    }
}
