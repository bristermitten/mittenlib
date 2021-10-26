package me.bristermitten.mittenlib.config.paths;

/**
 * Responsible for setting up access to a config file
 */
public interface ConfigInitializationStrategy {
    void initializeConfig(String filePath);
}
