package me.bristermitten.mittenlib.config.paths;

import java.nio.file.Path;

/**
 * Responsible for turning simple file names into {@link Path}s
 */
public interface ConfigPathResolver {
    /**
     * Resolve a file name to a {@link Path}
     *
     * @param configFileName the file name to resolve
     * @return the resolved path, which may not exist
     */
    Path getConfigPath(String configFileName);
}
