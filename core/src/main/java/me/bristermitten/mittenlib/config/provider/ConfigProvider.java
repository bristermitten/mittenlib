package me.bristermitten.mittenlib.config.provider;

import com.google.inject.Provider;
import java.net.URL;
import java.nio.file.Path;
import java.util.Optional;

/**
 * A {@link Provider} for a config type {@link T}, optionally specifying a {@link Path} representing
 * the config file.
 *
 * @param <T> the type of the config
 */
public interface ConfigProvider<T> extends Provider<T> {
    /**
     * The path of the source for the config, if available
     *
     * <p>This is not always required - if the config came from a {@link URL} or {@link String}, for
     * example, then this would be expected to return an {@link Optional#empty()}
     *
     * <p>However, if it is coming from the file system (in most cases, it will), then a filled
     * optional should be returned to allow things like {@link FileWatchingConfigProvider} to be
     * implemented.
     *
     * @return The path for of the config's source
     */
    Optional<Path> path();

    /**
     * Clear any and all cached config, if applicable. If this is a {@link WrappingConfigProvider},
     * then this method should also be called on the result of {@link
     * WrappingConfigProvider#getWrapped()}.
     *
     * <p>This may be a no-op if the config is not cached.
     */
    void clearCache();
}
