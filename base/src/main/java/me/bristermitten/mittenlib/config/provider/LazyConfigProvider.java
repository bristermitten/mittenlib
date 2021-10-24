package me.bristermitten.mittenlib.config.provider;

import me.bristermitten.mittenlib.config.Configuration;

import javax.inject.Inject;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

public class LazyConfigProvider<T> implements ConfigProvider<T> {
    private final Configuration<T> configuration;
    private ConfigProvider<T> delegate;

    public LazyConfigProvider(Configuration<T> configuration) {
        this.configuration = configuration;
    }

    @Inject
    void initialize(ConfigProviderFactory factory) {
        this.delegate = factory.createProvider(configuration);
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