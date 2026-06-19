package me.bristermitten.mittenlib.config;

import com.google.gson.TypeAdapterFactory;
import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import me.bristermitten.mittenlib.config.paths.ConfigInitializationStrategy;
import me.bristermitten.mittenlib.config.paths.ConfigPathResolver;
import me.bristermitten.mittenlib.config.paths.JarResourcesConfigPathResolver;
import me.bristermitten.mittenlib.config.paths.NoOpConfigInitializationStrategy;
import me.bristermitten.mittenlib.config.provider.ConfigProvider;
import me.bristermitten.mittenlib.config.provider.construct.ConfigProviderFactory;
import me.bristermitten.mittenlib.config.provider.construct.ConfigProviderImprover;
import me.bristermitten.mittenlib.config.provider.construct.SimpleConfigProviderFactory;
import me.bristermitten.mittenlib.config.provider.construct.SimpleConfigProviderImprover;
import me.bristermitten.mittenlib.config.reader.ObjectLoader;
import me.bristermitten.mittenlib.config.reader.SearchingObjectLoader;
import me.bristermitten.mittenlib.config.tree.DataTreeTypeAdapter;
import me.bristermitten.mittenlib.config.tree.DataTreeTypeAdapterFactory;
import me.bristermitten.mittenlib.config.writer.ConfigWriter;
import me.bristermitten.mittenlib.config.writer.ObjectWriter;
import me.bristermitten.mittenlib.config.writer.SearchingObjectWriter;
import me.bristermitten.mittenlib.files.json.ExtraTypeAdapter;

/**
 * Guice module for configuration infrastructure. This module binds the core components required for
 * configuration loading and writing.
 */
public class ConfigInfrastructureModule extends AbstractModule {

    private final Class<? extends ConfigInitializationStrategy> initializationStrategy;
    private final Class<? extends ConfigPathResolver> pathResolver;

    /** Create a new ConfigInfrastructureModule with default (non-plugin specific) implementations. */
    public ConfigInfrastructureModule() {
        this(NoOpConfigInitializationStrategy.class, JarResourcesConfigPathResolver.class);
    }

    /**
     * Create a new ConfigInfrastructureModule with specified implementations.
     *
     * @param initializationStrategy the strategy to use for initializing configs
     * @param pathResolver the resolver to use for finding config files
     */
    public ConfigInfrastructureModule(
            Class<? extends ConfigInitializationStrategy> initializationStrategy,
            Class<? extends ConfigPathResolver> pathResolver) {
        this.initializationStrategy = initializationStrategy;
        this.pathResolver = pathResolver;
    }

    @Override
    protected void configure() {
        bind(ObjectLoader.class).to(SearchingObjectLoader.class);
        bind(ObjectWriter.class).to(SearchingObjectWriter.class);
        bind(ConfigInitializationStrategy.class).to(initializationStrategy);
        bind(ConfigPathResolver.class).to(pathResolver);
        bind(ConfigWriter.class);
        bind(ConfigProviderFactory.class).to(SimpleConfigProviderFactory.class);
        bind(ConfigProviderImprover.class).to(SimpleConfigProviderImprover.class);

        Multibinder.newSetBinder(binder(), new TypeLiteral<ExtraTypeAdapter<?>>() {})
                .addBinding()
                .to(DataTreeTypeAdapter.class);
        Multibinder.newSetBinder(binder(), TypeAdapterFactory.class)
                .addBinding()
                .to(DataTreeTypeAdapterFactory.class);

        Multibinder.newSetBinder(binder(), new TypeLiteral<Configuration<?>>() {});
        Multibinder.newSetBinder(binder(), new TypeLiteral<ConfigProvider<?>>() {});
    }
}
