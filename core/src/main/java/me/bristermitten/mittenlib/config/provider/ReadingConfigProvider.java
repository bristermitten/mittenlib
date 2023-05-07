package me.bristermitten.mittenlib.config.provider;

import me.bristermitten.mittenlib.config.Configuration;
import me.bristermitten.mittenlib.config.reader.ConfigReader;

import java.nio.file.Path;
import java.util.Optional;

/**
 * A {@link ConfigProvider} that reads from a file, using a {@link ConfigReader}
 *
 * @param <T> the type of the config
 */
public class ReadingConfigProvider<T> implements ConfigProvider<T> {
    private final Configuration<T> config;
    private final ConfigReader reader;
    private final Path path;

    /**
     * Create a new ReadingConfigProvider
     *
     * @param path   the path to read from
     * @param config the configuration to read
     * @param reader the reader to use
     */
    public ReadingConfigProvider(Path path, Configuration<T> config, ConfigReader reader) {
        this.path = path;
        this.config = config;
        this.reader = reader;
    }

    @Override
    public T get() {
        return reader.load(config.getType(), path, config.getDeserializeFunction()).getOrThrow();
    }

    @Override
    public Optional<Path> path() {
        return Optional.of(path);
    }

    @Override
    public void clearCache() {
        //no-op
    }
}

