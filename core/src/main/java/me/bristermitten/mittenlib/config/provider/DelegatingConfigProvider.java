package me.bristermitten.mittenlib.config.provider;

import me.bristermitten.mittenlib.config.Configuration;
import me.bristermitten.mittenlib.config.provider.construct.ConfigProviderFactory;
import me.bristermitten.mittenlib.config.provider.construct.ConfigProviderImprover;

import javax.inject.Inject;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;


/**
 * A {@link ConfigProvider} which delegates to another {@link ConfigProvider}, improving it with a {@link ConfigProviderImprover}
 * This is basically just used to resolve some circular stuff in Guice ({@link #initialize(ConfigProviderFactory, ConfigProviderImprover)}
 *
 * @param <T> the type of the config
 */
public class DelegatingConfigProvider<T> implements ConfigProvider<T> {
    private final Configuration<T> configuration;
    private ConfigProvider<T> delegate;

    /**
     * Create a new DelegatingConfigProvider
     *
     * @param configuration the configuration to delegate to
     */
    public DelegatingConfigProvider(Configuration<T> configuration) {
        this.configuration = configuration;
    }

    @Inject
    void initialize(ConfigProviderFactory factory, ConfigProviderImprover improver) {
        this.delegate = improver.improve(factory.createProvider(configuration).getOrThrow());
    }

    @Override
    public Optional<Path> path() {
        Objects.requireNonNull(delegate, "Not initialized!");
        return delegate.path();
    }

    @Override
    public T get() {
        Objects.requireNonNull(delegate, "Not initialized!");
        return delegate.get();
    }
}
