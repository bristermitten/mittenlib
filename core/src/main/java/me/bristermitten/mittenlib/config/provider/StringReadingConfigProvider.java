package me.bristermitten.mittenlib.config.provider;

import me.bristermitten.mittenlib.config.DeserializationFunction;
import me.bristermitten.mittenlib.config.reader.ConfigReader;

import java.nio.file.Path;
import java.util.Optional;

/**
 * A {@link ConfigProvider} that reads from a String.
 *
 * @param <T> the type of the config
 */
public class StringReadingConfigProvider<T> implements ConfigProvider<T> {
    private final ConfigReader reader;
    private final DeserializationFunction<T> deserializer;
    private final String data;

    public StringReadingConfigProvider(String data, ConfigReader reader, DeserializationFunction<T> deserializer) {
        this.data = data;
        this.reader = reader;
        this.deserializer = deserializer;
    }


    @Override
    public T get() {
        return reader.load(deserializer, data).getOrThrow();
    }


    /**
     * Always empty, as this provider does not have a path.
     * If you are acquiring a String from a file source, you likely shouldn't be using this class - use {@link FileBasedConfigProvider} or {@link FileWatchingConfigProvider} instead
     *
     * @return an empty Optional
     * @see ConfigProvider#path()
     */
    @Override
    public Optional<Path> path() {
        return Optional.empty();
    }

    @Override
    public void clearCache() {
        // nothing to clear
    }
}

