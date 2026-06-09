package me.bristermitten.mittenlib.config.provider;

import com.google.inject.Provider;
import me.bristermitten.mittenlib.config.Configuration;
import me.bristermitten.mittenlib.config.provider.construct.ConfigProviderFactory;
import me.bristermitten.mittenlib.config.provider.construct.ConfigProviderImprover;
import me.bristermitten.mittenlib.util.Cached;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Optional;

/**
 * A {@link ConfigProvider} which delegates to another {@link ConfigProvider}, improving it with a
 * {@link ConfigProviderImprover}
 *
 * <p>
 *
 * @param <T> the type of the config
 */
public class DelegatingConfigProvider<T> implements ConfigProvider<T>, WrappingConfigProvider<T> {
    private final Cached<ConfigProvider<T>> delegate;

    /**
     * Create a new DelegatingConfigProvider
     *
     * @param configuration the configuration to delegate to
     */
    public DelegatingConfigProvider(
            Configuration<T> configuration,
            Provider<ConfigProviderFactory> providerFactory,
            Provider<ConfigProviderImprover> improver) {
        this.delegate = new Cached<>(() -> improver.get()
                .improve(providerFactory.get().createProvider(configuration).getOrThrow()));
    }

    @Override
    public Optional<Path> path() {
        return delegate.get().path();
    }

    @Override
    public void clearCache() {
        delegate.get().clearCache();
    }

    @Override
    public T get() {
        return delegate.get().get();
    }

    public ConfigProvider<T> getDelegate() {
        return delegate.get();
    }

    @Override
    @NotNull public ConfigProvider<T> getWrapped() {
        return getDelegate();
    }
}
