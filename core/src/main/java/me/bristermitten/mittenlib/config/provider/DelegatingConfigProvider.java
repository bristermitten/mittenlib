package me.bristermitten.mittenlib.config.provider;

import me.bristermitten.mittenlib.config.Configuration;
import me.bristermitten.mittenlib.config.provider.construct.ConfigProviderFactory;
import me.bristermitten.mittenlib.config.provider.construct.ConfigProviderImprover;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;


/**
 * A {@link ConfigProvider} which delegates to another {@link ConfigProvider}, improving it with a {@link ConfigProviderImprover}
 * <p>
 * This is used to resolve some circular stuff in Guice ({@link #initialize(ConfigProviderFactory, ConfigProviderImprover)}
 * as the values might not be available at construction time.
 *
 * @param <T> the type of the config
 */
public class DelegatingConfigProvider<T> implements ConfigProvider<T>, WrappingConfigProvider<T> {
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

    private void requireInitialized() {
        Objects.requireNonNull(delegate, "Not initialized!");
    }

    @Override
    public Optional<Path> path() {
        requireInitialized();
        return delegate.path();
    }

    @Override
    public T get() {
        requireInitialized();
        return delegate.get();
    }

    public ConfigProvider<T> getDelegate() {
        requireInitialized();
        return delegate;
    }

    @Override
    @NotNull
    public ConfigProvider<T> getWrapped() {
        return getDelegate();
    }
}
