package me.bristermitten.mittenlib.config.provider;

import me.bristermitten.mittenlib.watcher.FileWatcher;
import me.bristermitten.mittenlib.watcher.FileWatcherService;

import java.nio.file.Path;
import java.util.Optional;

public class FileWatchingConfigProvider<T> implements ConfigProvider<T> {
    private final CachingConfigProvider<T> delegate;

    public FileWatchingConfigProvider(CachingConfigProvider<T> delegate, FileWatcherService watcherService) {
        this.delegate = delegate;
        final Path path = delegate.path()
                .orElseThrow(() -> new IllegalArgumentException("FileWatchingConfigProvider requires delegate.path() to be present"));

        watcherService.addWatcher(new FileWatcher(
                path,
                pathWatchEvent -> delegate.invalidate()
        ));
    }

    @Override
    public T get() {
        return delegate.get();
    }

    @Override
    public Optional<Path> path() {
        return delegate.path();
    }
}
