package me.bristermitten.mittenlib.config.provider;

import me.bristermitten.mittenlib.config.Configuration;
import me.bristermitten.mittenlib.config.reader.ConfigReader;
import me.bristermitten.mittenlib.config.tree.DataTree;
import me.bristermitten.mittenlib.files.yaml.YamlObjectWriter;
import me.bristermitten.mittenlib.util.Result;

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
    private final YamlObjectWriter writer;

    /**
     * Create a new ReadingConfigProvider
     *
     * @param path   the path to read from
     * @param config the configuration to read
     * @param reader the reader to use
     * @param writer the writer to use for saving
     */
    public ReadingConfigProvider(Path path, Configuration<T> config, ConfigReader reader, YamlObjectWriter writer) {
        this.path = path;
        this.config = config;
        this.reader = reader;
        this.writer = writer;
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

    /**
     * Saves the given config instance back to the file.
     * This can be used to save default values for missing fields.
     *
     * @param instance the config instance to save
     * @return a Result indicating success or failure
     */
    public Result<Void> save(T instance) {
        if (config.getSerializeFunction() == null) {
            return Result.fail(new UnsupportedOperationException("No serialization function provided for " + config.getType()));
        }
        DataTree tree = config.getSerializeFunction().apply(instance);
        return writer.write(tree, path);
    }
}

