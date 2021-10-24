package me.bristermitten.mittenlib.config.provider;

import javax.inject.Provider;
import java.nio.file.Path;
import java.util.Optional;

public interface ConfigProvider<T> extends Provider<T> {
    /**
     * The path of the source for the config, if available
     * This is not always required - if the config came from a URL, for example, then this would be expected
     * to return an empty Optional
     * <p>
     * However if it is coming from the File System (in most cases, it will), then a filled optional should
     * be returned to allow things like {@link FileWatchingConfigProvider}
     */
    Optional<Path> path();
}
