package me.bristermitten.mittenlib.config.provider;

import me.bristermitten.mittenlib.watcher.FileWatcher;
import me.bristermitten.mittenlib.watcher.FileWatcherService;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * A {@link ConfigProvider} that watches a file for changes, reloading the config when the file changes
 *
 * @param <T> the type of the config
 */
public class FileWatchingConfigProvider<T> implements ConfigProvider<T>, WrappingConfigProvider<T> {
    private final CachingConfigProvider<T> delegate;

    /**
     * Create a new FileWatchingConfigProvider
     *
     * @param delegate       the delegate to watch.
     *                       This must have a present {@link ConfigProvider#path()}, which indicates the file to watch.
     * @param watcherService the service to use to watch the file
     * @throws IllegalArgumentException if the given {@code delegate} does not have a present {@link ConfigProvider#path()}
     */

    public FileWatchingConfigProvider(CachingConfigProvider<T> delegate, FileWatcherService watcherService) {
        this.delegate = delegate;
        final Path path = delegate.path()
                .orElseThrow(() -> new IllegalArgumentException("FileWatchingConfigProvider requires delegate.path() to be present"));

        try {
            watcherService.addWatcher(new FileWatcher(
                    path,
                    pathWatchEvent -> delegate.invalidate()
            )).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public T get() {
        return delegate.get();
    }

    @Override
    public Optional<Path> path() {
        return delegate.path();
    }

    @Override
    public void clearCache() {
        delegate.clearCache();
    }

    @Override
    @NotNull
    public ConfigProvider<T> getWrapped() {
        return delegate;
    }
}
