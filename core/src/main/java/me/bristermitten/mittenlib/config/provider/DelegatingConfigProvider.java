package me.bristermitten.mittenlib.config.provider;

import me.bristermitten.mittenlib.config.Configuration;
import me.bristermitten.mittenlib.config.provider.construct.ConfigProviderFactory;
import me.bristermitten.mittenlib.config.provider.construct.ConfigProviderImprover;

import javax.inject.Inject;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

public class DelegatingConfigProvider<T> implements ConfigProvider<T> {
    private final Configuration<T> configuration;
    private ConfigProvider<T> delegate;

    public DelegatingConfigProvider(Configuration<T> configuration) {
        this.configuration = configuration;
    }

    @Inject
    void initialize(ConfigProviderFactory factory, ConfigProviderImprover improver) {
        this.delegate = improver.improve(factory.createProvider(configuration));
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
