package me.bristermitten.mittenlib.annotation.benchmark;

import me.bristermitten.mittenlib.config.paths.ConfigInitializationStrategy;

public class NoOpConfigInitializationStrategy implements ConfigInitializationStrategy {
    @Override
    public void initializeConfig(String filePath) {
        // No op
    }
}
