package me.bristermitten.mittenlib.config;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import me.bristermitten.mittenlib.config.paths.ConfigInitializationStrategy;
import me.bristermitten.mittenlib.config.paths.ConfigPathResolver;
import me.bristermitten.mittenlib.config.paths.PluginConfigInitializationStrategy;
import me.bristermitten.mittenlib.config.paths.PluginConfigPathResolver;
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
 * Responsible for quite a lot (probably too much) of the config handling,
 * registering {@link Configuration}s, {@link ObjectLoader}s, {@link ConfigProvider}s, etc.
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
        bind(ConfigInitializationStrategy.class).to(PluginConfigInitializationStrategy.class);
        bind(ConfigPathResolver.class).to(PluginConfigPathResolver.class);
        bind(ConfigProviderFactory.class).to(SimpleConfigProviderFactory.class);
        bind(ConfigProviderImprover.class).to(SimpleConfigProviderImprover.class);

        Multibinder<Configuration<?>> configurationMultibinder = Multibinder.newSetBinder(binder(), new TypeLiteral<Configuration<?>>() {
        });
        configurations.stream()
                .collect(Collectors.toMap(Function.identity(), DelegatingConfigProvider::new))
                .forEach((configuration, provider) -> {
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
