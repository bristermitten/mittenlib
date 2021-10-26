package me.bristermitten.mittenlib.config.paths;

import java.nio.file.Path;

public interface ConfigPathResolver {
    Path getConfigPath(String configFileName);
}
