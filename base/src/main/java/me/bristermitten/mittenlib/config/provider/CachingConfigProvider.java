package me.bristermitten.mittenlib.config.provider;

import me.bristermitten.mittenlib.util.Cached;

import java.nio.file.Path;
import java.util.Optional;

public class CachingConfigProvider<T> implements ConfigProvider<T> {
    private final ConfigProvider<T> delegate;
    private Cached<T> cached;

    public CachingConfigProvider(ConfigProvider<T> delegate) {
        this.cached = new Cached<>(delegate::get);
        this.delegate = delegate;
    }

    @Override
    public T get() {
        return cached.get();
    }

    public void invalidate() {
        cached.invalidate();
    }

    @Override
    public Optional<Path> path() {
        return delegate.path();
    }
}
