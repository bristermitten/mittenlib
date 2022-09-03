package me.bristermitten.mittenlib.config.paths;

import me.bristermitten.mittenlib.util.Result;
import me.bristermitten.mittenlib.util.Unit;

/**
 * Responsible for setting up access to a config file
 */
public interface ConfigInitializationStrategy {
    Result<Unit> initializeConfig(String filePath);
}
