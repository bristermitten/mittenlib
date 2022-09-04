package me.bristermitten.mittenlib.config.paths;

import me.bristermitten.mittenlib.util.Result;
import me.bristermitten.mittenlib.util.Unit;

/**
 * Responsible for setting up access to a config file
 */
public interface ConfigInitializationStrategy {
    /**
     * Attempt to initialize the config
     *
     * @param filePath the path to the config file
     * @return a Result representing whether the initialization was successful
     */
    Result<Unit> initializeConfig(String filePath);
}
