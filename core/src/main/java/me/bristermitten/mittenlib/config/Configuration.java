package me.bristermitten.mittenlib.config;

import org.jspecify.annotations.Nullable;

/**
 * Represents a loadable configuration from a file which will (de)serialzse to/from an object of type {@link T}
 *
 * @param <T> the type of the config's abstract representation.
 */
public class Configuration<T> {
    private final @Nullable String fileName;
    private final Class<T> type;

    /**
     * Create a new Configuration
     *
     * @param fileName the name of the file to load
     * @param type     the type to deserialize to
     */
    public Configuration(@Nullable String fileName, Class<T> type) {
        this.fileName = fileName;
        this.type = type;
    }

    /**
     * Returns the name of the file to load.
     *
     * @return the name of the file to load
     */

    public @Nullable String getFileName() {
        return fileName;
    }

    /**
     * Returns the type to deserialize to.
     *
     * @return the type to deserialize to
     */
    public Class<T> getType() {
        return type;
    }
}
