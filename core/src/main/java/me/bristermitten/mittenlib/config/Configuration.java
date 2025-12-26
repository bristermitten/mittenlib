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
    private final @Nullable SerializationFunction<T> serializeFunction;

    /**
     * Create a new Configuration
     *
     * @param fileName            the name of the file to load
     * @param type                the type to deserialize to
     * @param deserializeFunction the function to use to deserialize the data
     */
    public Configuration(String fileName, Class<T> type, @Nullable DeserializationFunction<T> deserializeFunction) {
        this(fileName, type, deserializeFunction, null);
    }

    /**
     * Create a new Configuration
     *
     * @param fileName            the name of the file to load
     * @param type                the type to deserialize to
     * @param deserializeFunction the function to use to deserialize the data
     * @param serializeFunction   the function to use to serialize the data
     */
    public Configuration(String fileName, Class<T> type, @Nullable DeserializationFunction<T> deserializeFunction, @Nullable SerializationFunction<T> serializeFunction) {
        this.fileName = fileName;
        this.type = type;
        this.deserializeFunction = deserializeFunction;
        this.serializeFunction = serializeFunction;
    }

    /**
     * Returns the name of the file to load.
     *
     * @return the name of the file to load
     */

    public String getFileName() {
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

    /**
     * Returns the function to use to deserialize the data.
     *
     * @return the function to use to deserialize the data
     */
    @Nullable
    public DeserializationFunction<T> getDeserializeFunction() {
        return deserializeFunction;
    }

    /**
     * Returns the function to use to serialize the data.
     *
     * @return the function to use to serialize the data
     */
    @Nullable
    public SerializationFunction<T> getSerializeFunction() {
        return serializeFunction;
    }
}
