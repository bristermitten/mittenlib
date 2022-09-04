package me.bristermitten.mittenlib.config;

import org.jetbrains.annotations.Nullable;

/**
 * Represents a loadable configuration which will deserialize to an object of type {@link T}
 *
 * @param <T> the type of the config
 */
public class Configuration<T> {
    private final String fileName;
    private final Class<T> type;

    private final @Nullable DeserializationFunction<T> deserializeFunction;

    /**
     * Create a new Configuration
     *
     * @param fileName            the name of the file to load
     * @param type                the type to deserialize to
     * @param deserializeFunction the function to use to deserialize the data
     */
    public Configuration(String fileName, Class<T> type, @Nullable DeserializationFunction<T> deserializeFunction) {
        this.fileName = fileName;
        this.type = type;
        this.deserializeFunction = deserializeFunction;
    }

    /**
     * @return the name of the file to load
     */

    public String getFileName() {
        return fileName;
    }

    /**
     * @return the type to deserialize to
     */
    public Class<T> getType() {
        return type;
    }

    /**
     * @return the function to use to deserialize the data
     */
    @Nullable
    public DeserializationFunction<T> getDeserializeFunction() {
        return deserializeFunction;
    }
}
