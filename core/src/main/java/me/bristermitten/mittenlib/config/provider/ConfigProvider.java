package me.bristermitten.mittenlib.config.provider;

import javax.inject.Provider;
import java.nio.file.Path;
import java.util.Optional;

/**
 * A {@link Provider} for a config type {@link T},
 * optionally specifying a {@link Path} representing the config file.
 *
 * @param <T> the type of the config
 */
public interface ConfigProvider<T> extends Provider<T> {
    /**
     * The path of the source for the config, if available
     * This is not always required - if the config came from a URL or String, for example, then this would be expected
     * to return an empty Optional
     * <p>
     * However, if it is coming from the Filesystem (in most cases, it will), then a filled optional should
     * be returned to allow things like {@link FileWatchingConfigProvider}
     *
     * @return The path for of the config's source
     */
    Optional<Path> path();
}
