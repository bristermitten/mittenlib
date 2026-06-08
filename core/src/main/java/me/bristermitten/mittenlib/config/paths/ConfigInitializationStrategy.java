package me.bristermitten.mittenlib.config.paths;

import me.bristermitten.mittenlib.util.Result;
import me.bristermitten.mittenlib.util.Unit;

/**
 * Responsible for setting up access to a config file. This should perform any logic necessary to
 * ensure the file exists and is in a readable, valid state. For example, in a plugin, the typical
 * implementation is to copy the default config file from the jar if it doesn't exist, which is
 * handled by {@link PluginConfigInitializationStrategy}
 */
public interface ConfigInitializationStrategy {
    /**
     * Attempt to initialize the config
     *
     * @param filePath the path to the config file
     * @return a {@link Result} representing whether the initialization was successful
     * @deprecated Use {@link #initializeConfig(String, Class)} to allow for dynamic initialization
     */
    @Deprecated
    default Result<Unit> initializeConfig(String filePath) {
        return initializeConfig(filePath, Object.class);
    }

    /**
     * Attempt to initialize the config
     *
     * @param filePath    the path to the config file
     * @param configClass the class of the config
     * @return a {@link Result} representing whether the initialization was successful
     */
    default <T> Result<Unit> initializeConfig(String filePath, Class<T> configClass) {
        return initializeConfig(filePath);
  }
}
