package me.bristermitten.mittenlib;

import me.bristermitten.mittenlib.watcher.FileWatcherService;

/**
 * Stores the name of the consumer of MittenLib (usually the plugin name).
 * Useful for logging, currently also used for the thread name in {@link FileWatcherService}
 */
public class MittenLibConsumer {
    private final String name;

    /**
     * Create a new MittenLibConsumer
     *
     * @param name the name of the consumer
     */
    public MittenLibConsumer(String name) {
        this.name = name;
    }

    /**
     * Returns the name of the consumer of MittenLib.
     *
     * @return the name of the consumer
     */
    public String getName() {
        return name;
    }
}
