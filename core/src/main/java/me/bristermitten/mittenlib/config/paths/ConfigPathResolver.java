package me.bristermitten.mittenlib.config.paths;

import me.bristermitten.mittenlib.util.Result;

import java.nio.file.Path;

/**
 * Responsible for turning simple file names into {@link Path}s
 */
public interface ConfigPathResolver {
    /**
     * Resolve a file name to a {@link Path}
     *
     * @param configFileName the file name to resolve
     * @return the resolved path, or an error if the file name could not be resolved.
     * Note that this may return a successful result with a path that does not exist. The failure case is specifically for error states.
     */
    Result<Path> getConfigPath(String configFileName);
}
