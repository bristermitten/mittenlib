package me.bristermitten.mittenlib.config.provider;

import java.nio.file.Path;
import java.util.Optional;
import me.bristermitten.mittenlib.config.tree.DataTree;
import me.bristermitten.mittenlib.util.Cached;
import me.bristermitten.mittenlib.util.Result;
import org.jetbrains.annotations.NotNull;

/**
 * A ConfigProvider that caches the config to avoid repeated file reads.
 *
 * @param <T> The type of the config
 */
public class CachingConfigProvider<T>
        implements ConfigProvider<T>, WrappingConfigProvider<T>, SaveableConfigProvider<T> {
    private final ConfigProvider<T> delegate;
    private final Cached<T> cached;

    /**
     * Create a new CachingConfigProvider, lazily computing the config.
     *
     * @param delegate The delegate to use to load the config
     */
    public CachingConfigProvider(ConfigProvider<T> delegate) {
        this.cached = new Cached<>(delegate::get);
        this.delegate = delegate;
    }

    @Override
    public T get() {
        return cached.get();
    }

    /**
     * Invalidate the cached config, causing it to be recomputed on the next call to {@link #get()}.
     *
     * @see Cached#invalidate()
     */
    public void invalidate() {
        cached.invalidate();
    }

    @Override
    public Optional<Path> path() {
        return delegate.path();
    }

    @Override
    public void clearCache() {
        invalidate();
        delegate.clearCache();
    }

    @Override
    @NotNull public ConfigProvider<T> getWrapped() {
        return new ConfigProvider<T>() {
            @Override
            public Optional<Path> path() {
                return delegate.path();
            }

            @Override
            public void clearCache() {
                // clear our cache as well as the delegate's
                CachingConfigProvider.this.clearCache();
            }

            @Override
            public T get() {
                return delegate.get();
            }
        };
    }

    @Override
    public Result<DataTree> save(T instance, boolean overrideExisting) {
        if (delegate instanceof SaveableConfigProvider) {
            return ((SaveableConfigProvider<T>) delegate).save(instance, overrideExisting);
        }
        return Result.fail(new UnsupportedOperationException(
                "Wrapped provider " + delegate.getClass().getName() + " is not saveable"));
    }
}
