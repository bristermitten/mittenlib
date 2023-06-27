package me.bristermitten.mittenlib.config.provider;

import me.bristermitten.mittenlib.config.Configuration;
import me.bristermitten.mittenlib.config.reader.ConfigReader;

import java.nio.file.Path;
import java.util.Optional;

/**
 * A {@link ConfigProvider} that reads a String to load a config
 *
 * @param <T> the type of the config
 */
public class StringReadingConfigProvider<T> implements ConfigProvider<T> {
    private final Configuration<T> config;
    private final ConfigReader reader;
    private final String data;

    public StringReadingConfigProvider(String data, Configuration<T> config, ConfigReader reader) {
        this.data = data;
        this.config = config;
        this.reader = reader;
    }

    @Override
    public T get() {
        return reader.load(config.getType(), data, config.getDeserializeFunction()).getOrThrow();
    }

    /**
     * Always empty, as this provider does not have a path.
     * If you are acquiring a String from a file source, you likely shouldn't be using this class - use {@link ReadingConfigProvider} or {@link FileWatchingConfigProvider} instead
     *
     * @return an empty Optional
     * @see ConfigProvider#path()
     */
    @Override
    public Optional<Path> path() {
        return Optional.empty();
    }
}

