package me.bristermitten.mittenlib.config;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import me.bristermitten.mittenlib.config.paths.*;
import me.bristermitten.mittenlib.config.provider.ConfigProvider;
import me.bristermitten.mittenlib.config.provider.DelegatingConfigProvider;
import me.bristermitten.mittenlib.config.provider.construct.ConfigProviderFactory;
import me.bristermitten.mittenlib.config.provider.construct.ConfigProviderImprover;
import me.bristermitten.mittenlib.config.provider.construct.SimpleConfigProviderFactory;
import me.bristermitten.mittenlib.config.provider.construct.SimpleConfigProviderImprover;
import me.bristermitten.mittenlib.config.reader.ObjectLoader;
import me.bristermitten.mittenlib.config.reader.SearchingObjectLoader;
import me.bristermitten.mittenlib.util.CompositeType;

import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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
        bind(ConfigInitializationStrategy.class).to(NoOpConfigInitializationStrategy.class);
        bind(ConfigPathResolver.class).to(JarResourcesConfigPathResolver.class);
        bind(ConfigProviderFactory.class).to(SimpleConfigProviderFactory.class);
        bind(ConfigProviderImprover.class).to(SimpleConfigProviderImprover.class);

        Multibinder<Configuration<?>> configurationMultibinder = Multibinder.newSetBinder(binder(), new TypeLiteral<Configuration<?>>() {
        });

        Multibinder<ConfigProvider<?>> configProviderMultibinder = Multibinder.newSetBinder(binder(), new TypeLiteral<ConfigProvider<?>>() {
        });

        configurations.stream()
                .collect(Collectors.toMap(Function.identity(), DelegatingConfigProvider::new))
                .forEach((configuration, provider) -> {
                    configProviderMultibinder.addBinding().toInstance(provider);

                    final Class<?> key = configuration.getType();
                    // beware of evil generic type erasure hell
                    bind((Class<? super Object>) key).toProvider(provider); // bind the type itself, T to Provider<T>

                    // bind the provider to its instance
                    final TypeLiteral<ConfigProvider<?>> providerType =
                            (TypeLiteral<ConfigProvider<?>>) TypeLiteral.get(new CompositeType(ConfigProvider.class, key));
                    bind(providerType).toInstance(provider);
                    configurationMultibinder.addBinding().toInstance(configuration);
                });
    }
}
